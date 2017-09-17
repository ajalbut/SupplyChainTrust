package SupplyChainTrust.relogo

import static repast.simphony.relogo.Utility.*
import static repast.simphony.relogo.UtilityG.*

import SupplyChainTrust.ReLogoTurtle
import repast.simphony.relogo.Plural
import repast.simphony.relogo.Stop
import repast.simphony.relogo.Utility
import repast.simphony.relogo.UtilityG
import repast.simphony.relogo.schedule.Go
import repast.simphony.relogo.schedule.Setup

class ChainLevel extends ReLogoTurtle {
	static RHO = 0.5
	static ALPHA = 0.5
	static BETA = 1.0
	static THETA = 0.5

	def supplyRule
	def desiredStock
	def currentStock
	def expectedDemand

	Map backlog = [:]
	Map productPipelines = [:]
	Map orderPipelines = [:]
	Map ordersReceived = [:]
	Map ordersSentChecklist = [:]
	Map shipmentsReceivedChecklist = [:]
	Map trustUpstreams = [:]
	def maxTrustUpstreams = 1.0
	def maxTrustDownstreams = 1.0

	def supplier
	def upstreamLevel
	def downstreamLevel

	def initialProductPipeline = [4.0, 4.0]
	def initialOrderPipeline = [4.0]
	def initialOrdersSentChecklist = [4.0, 4.0, 4.0]
	def initialShipmentsReceivedChecklist = [4.0, 4.0]
	def pipelineSize = initialProductPipeline.size() + initialOrderPipeline.size()

	def setup(x, y, initialStock){
		setxy(x,y)
		setShape("square")
		this.supplyRule = UserObserver.supplyRule
		this.currentStock = initialStock
		this.desiredStock = 0.0
		this.expectedDemand = 4.0
		if (this.upstreamLevel.size()) {
			this.supplier = this.upstreamLevel[UserObserver.random.nextInt(this.upstreamLevel.size())]
		}
	}

	def initializeState() {
		for (ChainLevel upstream in this.upstreamLevel) {
			if (upstream == this.supplier) {
				this.ordersSentChecklist[upstream.getWho()] = initialOrdersSentChecklist.clone()
				this.shipmentsReceivedChecklist[upstream.getWho()] = initialShipmentsReceivedChecklist.clone()
				this.productPipelines[upstream.getWho()] = this.initialProductPipeline.clone()
				def route = createLinkTo(upstream, { hideLink()})
				route.color = scaleColor(red(), 1.0, 0.0, 1.0)
			} else {
				this.ordersSentChecklist[upstream.getWho()] = initialOrdersSentChecklist.clone().collect{0.0}
				this.shipmentsReceivedChecklist[upstream.getWho()] = initialShipmentsReceivedChecklist.clone().collect{0.0}
				this.productPipelines[upstream.getWho()] = this.initialProductPipeline.clone().collect{0.0}
			}
			this.trustUpstreams[upstream.getWho()] = 1.0
		}
		for (ChainLevel downstream in this.downstreamLevel) {
			if (this == downstream.supplier) {
				this.ordersReceived[downstream.getWho()] = 4.0
				this.orderPipelines[downstream.getWho()] = initialOrderPipeline.clone()
			} else {
				this.ordersReceived[downstream.getWho()] = 0.0
				this.orderPipelines[downstream.getWho()] = initialOrderPipeline.clone().collect{0.0}
			}
			this.backlog[downstream.getWho()] = 0.0
		}
	}

	def receiveShipments(){
		for (ChainLevel upstream in this.upstreamLevel) {
			def shipmentReceived = this.productPipelines[upstream.getWho()].pop()
			this.currentStock += shipmentReceived
			this.shipmentsReceivedChecklist[upstream.getWho()].add(0, shipmentReceived)
		}
	}

	def fillOrders(){
		def totalOrdersToFill = this.backlog.values().sum() + this.ordersReceived.values().sum()
		def totalShipmentsSent = (this.currentStock >= totalOrdersToFill) ? totalOrdersToFill : this.currentStock
		for (ChainLevel downstream in this.downstreamLevel) {
			def shipmentSent
			if (this.supplyRule == 'BACKORDER') {
				def ordersToFill = this.backlog[downstream.getWho()]  + this.ordersReceived[downstream.getWho()]
				shipmentSent = totalOrdersToFill ? totalShipmentsSent * ordersToFill / totalOrdersToFill : 0.0
			}
			downstream.productPipelines[this.getWho()].add(0, shipmentSent)
			this.backlog[downstream.getWho()] = Math.max(0.0, this.backlog[downstream.getWho()] + this.ordersReceived[downstream.getWho()] - shipmentSent)
			this.currentStock -= shipmentSent
		}
	}

	def receiveOrders(){
		this.expectedDemand = this.THETA * this.ordersReceived.values().sum() + (1 - this.THETA) * this.expectedDemand
		for (ChainLevel downstream in this.downstreamLevel) {
			def orderReceived = this.orderPipelines[downstream.getWho()].pop()
			this.ordersReceived[downstream.getWho()] = orderReceived
		}
	}

	def makeOrders(){
		def supplyLine = this.calculateSupplyLine()
		def orderSize = this.calculateOrderSize(supplyLine)
		this.placeOrder(orderSize)
	}

	def calculateSupplyLine() {
		def supplyLine = 0.0
		for (ChainLevel upstream in this.upstreamLevel) {
			supplyLine += this.productPipelines[upstream.getWho()].sum()
			supplyLine += upstream.backlog[this.getWho()]
			supplyLine += upstream.ordersReceived[this.getWho()]
		}
		return supplyLine
	}

	def calculateOrderSize(supplyLine) {
		def desiredSupplyLine = this.pipelineSize * this.expectedDemand
		def effectiveStock = this.currentStock - this.backlog.values().sum()
		def totalOrders = this.expectedDemand + this.ALPHA * (this.desiredStock - effectiveStock + this.BETA * (desiredSupplyLine - supplyLine))
		return Math.max(0.0, totalOrders)
	}

	def placeOrder(orderSize) {
		def order
		for (ChainLevel upstream in this.upstreamLevel) {
			if (upstream == this.supplier) {
				order = orderSize
			} else {
				order = 0.0
			}
			upstream.orderPipelines[this.getWho()].add(0,  order)
			this.ordersSentChecklist[upstream.getWho()].add(0,  order)
		}
	}

	def updateUpstreamTrust(){
		def maxTrustUpstreams = 0.0
		for (ChainLevel upstream in this.upstreamLevel) {
			def orderToCheck = this.ordersSentChecklist[upstream.getWho()].pop()
			def shipmentReceived = this.shipmentsReceivedChecklist[upstream.getWho()].pop()
			def newEvaluation
			def updatedTrust = 0.0
			if (shipmentReceived >= orderToCheck) {
				newEvaluation = this.RHO * orderToCheck + (1 - this.RHO) * this.trustUpstreams[upstream.getWho()]
				updatedTrust = Math.max(newEvaluation, this.trustUpstreams[upstream.getWho()])
			} else {
				newEvaluation = this.RHO * shipmentReceived + (1 - this.RHO) * this.trustUpstreams[upstream.getWho()]
				updatedTrust = Math.min(newEvaluation, this.trustUpstreams[upstream.getWho()])
			}
			this.trustUpstreams[upstream.getWho()] = updatedTrust
			if (updatedTrust > maxTrustUpstreams) {
				maxTrustUpstreams = updatedTrust
			}
		}
		this.maxTrustUpstreams = maxTrustUpstreams
	}

	def getStockMinusBackorder() {
		def stockMinusBackorder = this.currentStock
		if (this.backlog.values().size()) {
			stockMinusBackorder -= this.backlog.values().sum()
		}
		return stockMinusBackorder
	}

	def getCurrentTrustFromDownstreams() {
		def trust = 0.0
		for (ChainLevel downstream in this.downstreamLevel) {
			trust += downstream.trustUpstreams[this.getWho()]
		}
		return trust
	}

	def refreshView() {
		def effectiveStock = this.currentStock
		if (this.backlog.values().sum()) {
			effectiveStock -= this.backlog.values().sum()
		}
		this.label = "" + round(100 * effectiveStock) / 100

		ask(myOutLinks()){die()}
		for (ChainLevel upstream in this.upstreamLevel) {
			if (upstream == this.supplier) {
				def route = createLinkTo(upstream)
				route.color = scaleColor(red(), this.trustUpstreams[upstream.getWho()], 0.0, maxTrustUpstreams)
			}
		}
	}
}
