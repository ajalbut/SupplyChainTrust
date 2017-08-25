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
	def desiredStock = 0.0
	def currentStock
	def expectedDemand = 12.0

	Map backlog = [:]
	Map productPipelines = [:]
	Map orderPipelines = [:]
	Map ordersReceived = [:]
	Map ordersSentChecklist = [:]
	Map shipmentsReceivedChecklist = [:]
	Map shipmentsSentChecklist = [:]
	Map ordersReceivedChecklist = [:]
	Map trustUpstreams = [:]
	Map trustDownstreams = [:]
	def maxTrustUpstreams = 1.0
	def maxTrustDownstreams = 1.0

	def upstreamLevel
	def downstreamLevel

	def initialProductPipeline = [4.0, 4.0]
	def initialOrderPipeline = [4.0]
	def initialOrdersSentChecklist = [4.0, 4.0, 4.0]
	def initialShipmentsReceivedChecklist = [4.0, 4.0]
	def initialShipmentsSentChecklist = [4.0, 4.0, 4.0]
	def initialOrdersReceivedChecklist = [4.0]
	def pipelineSize = initialProductPipeline.size() + initialOrderPipeline.size()

	def setup(x, y, initialStock){
		setxy(x,y)
		setShape("square")
		this.currentStock = initialStock

		for (ChainLevel upstream in this.upstreamLevel) {
			if(upstream.getWho() != this.getWho()) {
				this.ordersSentChecklist[upstream.getWho()] = initialOrdersSentChecklist.clone()
				this.shipmentsReceivedChecklist[upstream.getWho()] = initialShipmentsReceivedChecklist.clone()
				this.trustUpstreams[upstream.getWho()] = 1.0
				def route = createLinkFrom(upstream, { hideLink()})
				route.color = scaleColor(red(), 1.0, 0.0, 1.0)
			} else {
				this.initialProductPipeline = [12.0, 12.0]
				this.pipelineSize = this.initialProductPipeline.size()
			}
			this.productPipelines[upstream.getWho()] = initialProductPipeline.clone()
		}
		for (ChainLevel downstream in this.downstreamLevel) {
			this.ordersReceived[downstream.getWho()] = 4.0
			this.backlog[downstream.getWho()] = 0.0
			this.orderPipelines[downstream.getWho()] = initialOrderPipeline.clone()
			this.shipmentsSentChecklist[downstream.getWho()] = initialShipmentsSentChecklist.clone()
			this.ordersReceivedChecklist[downstream.getWho()] = initialOrdersReceivedChecklist.clone()
			this.trustDownstreams[downstream.getWho()] = 1.0
		}
	}

	def receiveShipments(){
		for (ChainLevel upstream in this.upstreamLevel) {
			def shipmentReceived = this.productPipelines[upstream.getWho()].pop()
			this.currentStock += shipmentReceived
			if(upstream.getWho() != this.getWho()) {
				this.shipmentsReceivedChecklist[upstream.getWho()].add(0, shipmentReceived)
			}
		}
	}

	def fillOrders(){
		def totalOrdersToFill = this.backlog.values().sum() + this.ordersReceived.values().sum()
		def totalTrustDownstreams = this.trustDownstreams.values().sum()
		def totalShipmentsSent = (this.currentStock >= totalOrdersToFill) ? totalOrdersToFill : this.currentStock
		for (ChainLevel downstream in this.downstreamLevel) {
			def shipmentSent
			if (this.supplyRule == 'BACKORDER') {
				def ordersToFill = this.backlog[downstream.getWho()]  + this.ordersReceived[downstream.getWho()]
				shipmentSent = totalOrdersToFill ? totalShipmentsSent * ordersToFill / totalOrdersToFill : 0.0
			} else {
				shipmentSent = totalTrustDownstreams ? totalShipmentsSent * this.trustDownstreams[downstream.getWho()] / totalTrustDownstreams : 0.0
			}
			downstream.productPipelines[this.getWho()].add(0, shipmentSent)
			this.backlog[downstream.getWho()] = Math.max(0.0, this.backlog[downstream.getWho()] + this.ordersReceived[downstream.getWho()] - shipmentSent)
			this.currentStock -= shipmentSent
			this.shipmentsSentChecklist[downstream.getWho()].add(0, shipmentSent)
		}
	}

	def receiveOrders(){
		this.expectedDemand = this.THETA * this.ordersReceived.values().sum() + (1 - this.THETA) * this.expectedDemand
		for (ChainLevel downstream in this.downstreamLevel) {
			def orderReceived = this.orderPipelines[downstream.getWho()].pop()
			this.ordersReceived[downstream.getWho()] = orderReceived
			this.ordersReceivedChecklist[downstream.getWho()].add(0, this.ordersReceived[downstream.getWho()])
		}
	}

	def makeOrders(){
		def totalOrderSize = this.calculateTotalOrderSize()
		this.distributeOrdersToSend(totalOrderSize)
	}

	def calculateTotalOrderSize() {
		def desiredSupplyLine = this.pipelineSize * this.expectedDemand

		def supplyLine = 0.0
		for (ChainLevel upstream in this.upstreamLevel) {
			supplyLine += this.productPipelines[upstream.getWho()].sum()
			if(upstream.getWho() != this.getWho()) {
				supplyLine += upstream.backlog[this.getWho()]
				supplyLine += upstream.ordersReceived[this.getWho()]
			}
		}

		def effectiveStock = this.currentStock - this.backlog.values().sum()
		def totalOrders = this.expectedDemand + this.ALPHA * (this.desiredStock - effectiveStock + this.BETA * (desiredSupplyLine - supplyLine))
		return Math.max(0.0, totalOrders)
	}

	def distributeOrdersToSend(totalOrdersSent) {
		def freeUpstreams = []
		def inverseBackOrderSum = 0.0
		def totalTrustUpstreams = 0.0
		for (ChainLevel upstream in this.upstreamLevel) {
			if (this.supplyRule == 'BACKORDER') {
				def ordersToFill = upstream.backlog[this.getWho()]
				if (1000000 * round(ordersToFill) / 1000000 == 0.0) {
					freeUpstreams.push(upstream.getWho())
				} else {
					inverseBackOrderSum += 1 / ordersToFill
				}
			} else {
				totalTrustUpstreams += this.trustUpstreams[upstream.getWho()]
			}
		}
		for (ChainLevel upstream in this.upstreamLevel) {
			def orderSent
			if (this.supplyRule == 'BACKORDER') {
				def ordersToFill = upstream.backlog[this.getWho()]
				if(length(freeUpstreams)) {
					orderSent = (freeUpstreams.contains(upstream.getWho())) ? totalOrdersSent / length(freeUpstreams) : 0.0
				} else {
					orderSent = totalOrdersSent * (1 / ordersToFill) / inverseBackOrderSum
				}
			} else {
				orderSent = totalTrustUpstreams ? totalOrdersSent * this.trustUpstreams[upstream.getWho()] / totalTrustUpstreams : 0.0
			}
			upstream.orderPipelines[this.getWho()].add(0, orderSent)
			this.ordersSentChecklist[upstream.getWho()].add(0, orderSent)
		}
	}

	def updateUpstreamTrust(){
		def maxTrustUpstreams = 0.0
		for (ChainLevel upstream in this.upstreamLevel) {
			if(upstream.getWho() != this.getWho()) {
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
		}
		this.maxTrustUpstreams = maxTrustUpstreams
	}

	def updateDownstreamTrust(){
		def maxTrustDownstreams = 0.0
		for (ChainLevel downstream in this.downstreamLevel) {
			def shipmentToCheck = this.shipmentsSentChecklist[downstream.getWho()].pop()
			def orderReceived = this.ordersReceivedChecklist[downstream.getWho()].pop()
			def newEvaluation
			def updatedTrust = 0.0
			if (orderReceived >= shipmentToCheck) {
				newEvaluation = this.RHO * shipmentToCheck + (1 - this.RHO) * this.trustDownstreams[downstream.getWho()]
				updatedTrust = Math.max(newEvaluation, this.trustDownstreams[downstream.getWho()])
			} else {
				newEvaluation = this.RHO * orderReceived + (1 - this.RHO) * this.trustDownstreams[downstream.getWho()]
				updatedTrust = Math.min(newEvaluation, this.trustDownstreams[downstream.getWho()])
			}
			this.trustDownstreams[downstream.getWho()] = updatedTrust
			if (updatedTrust > maxTrustDownstreams) {
				maxTrustDownstreams = updatedTrust
			}
		}
		this.maxTrustDownstreams = maxTrustDownstreams
	}

	def getStockMinusBackorder() {
		def stockMinusBackorder = this.currentStock
		if (this.backlog.values().size()) {
			stockMinusBackorder -= this.backlog.values().sum()
		}
		return stockMinusBackorder
	}

	def getCurrentTrustFromUpstreams() {
		def trust = 0.0
		for (ChainLevel upstream in this.upstreamLevel) {
			if(upstream.getWho() != this.getWho()) {
				trust += upstream.trustDownstreams[this.getWho()]
			}
		}
		return trust
	}

	def getCurrentTrustFromDownstreams() {
		def trust = 0.0
		for (ChainLevel downstream in this.downstreamLevel) {
			trust += downstream.trustUpstreams[this.getWho()]
		}
		return trust
	}

	def refreshTrustLinks(visibility) {
		def effectiveStock = this.currentStock
		if (this.backlog.values().sum()) {
			effectiveStock -= this.backlog.values().sum()
		}
		label = "" + round(100 * effectiveStock) / 100

		ask(myOutLinks()){die()}
		if (visibility == 'upstream') {
			for (ChainLevel upstream in this.upstreamLevel) {
				if(upstream.getWho() != this.getWho()) {
					def route = createLinkTo(upstream)
					route.color = scaleColor(red(), this.trustUpstreams[upstream.getWho()], 0.0, maxTrustUpstreams)
				}
			}
		} else if (visibility == 'downstream') {
			for (ChainLevel downstream in this.downstreamLevel) {
				def route = createLinkTo(downstream)
				route.color = scaleColor(blue(), this.trustDownstreams[downstream.getWho()], 0.0, maxTrustDownstreams)
			}
		}
	}
}
