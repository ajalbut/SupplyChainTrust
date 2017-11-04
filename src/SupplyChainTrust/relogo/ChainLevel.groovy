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
	Strategy strategy

	def currentStock
	def expectedDemand

	def cash
	def minMarkup
	def maxMarkup
	def saleMarkup

	Map backlog = [:]
	Map productPipelines = [:]
	Map orderPipelines = [:]
	Map ordersReceived = [:]

	Map ordersSentChecklist = [:]
	Map shipmentsReceivedChecklist = [:]
	Map totalOrdersReceived = [:]
	Map currentShipmentToReceive = [:]
	Map currentShipmentReceivedOnTime = [:]
	Map totalShipmentsToReceive = [:]
	Map totalShipmentsReceived = [:]
	Map totalShipmentsReceivedOnTime = [:]

	Map trustUpstreams = [:]

	ChainLevel supplier
	def upstreamLevel
	def downstreamLevel

	def initialProductPipeline = [4.0, 4.0]
	def initialOrderPipeline = [4.0]
	def initialOrdersSentChecklist = [4.0, 4.0, 4.0, 4.0]
	def initialShipmentsReceivedChecklist = []
	def pipelineSize = initialProductPipeline.size() + initialOrderPipeline.size()

	def setup(x, y, Strategy strategy){
		setxy(x,y)
		setShape("square")
		setColor(strategy.color)
		this.strategy = strategy
		this.currentStock = 1.0 * this.strategy.desiredStock
		this.expectedDemand = 4.0
		this.cash = 0.0
		if (this.minMarkup) {
			this.maxMarkup = this.minMarkup - minProfit + maxProfit
		}
		if (this.upstreamLevel.size()) {
			this.supplier = this.upstreamLevel[random.nextInt(this.upstreamLevel.size())]
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
			this.totalShipmentsToReceive[upstream.getWho()] = 0.0
			this.totalShipmentsReceived[upstream.getWho()] = 0.0
			this.totalShipmentsReceivedOnTime[upstream.getWho()] = 0.0
			this.trustUpstreams[upstream.getWho()] = 0.5
		}
		for (ChainLevel downstream in this.downstreamLevel) {
			if (this == downstream.supplier) {
				this.ordersReceived[downstream.getWho()] = 4.0
				this.orderPipelines[downstream.getWho()] = initialOrderPipeline.clone()
			} else {
				this.ordersReceived[downstream.getWho()] = 0.0
				this.orderPipelines[downstream.getWho()] = initialOrderPipeline.clone().collect{0.0}
			}
			this.totalOrdersReceived[downstream.getWho()] = 0.0
			this.backlog[downstream.getWho()] = 0.0
		}
		this.calculateSaleMarkup()
	}

	def receiveShipments(){
		for (ChainLevel upstream in this.upstreamLevel) {
			def shipmentReceived = this.productPipelines[upstream.getWho()].pop()
			this.currentStock += shipmentReceived
			this.shipmentsReceivedChecklist[upstream.getWho()].add(0, shipmentReceived)
		}
	}

	def fillOrders(){
		def sortedDownstreams = this."$supplyRule"()

		while (sortedDownstreams.size()) {
			ChainLevel downstream = sortedDownstreams.pop()
			def orderToFill = this.backlog[downstream.getWho()] + this.ordersReceived[downstream.getWho()]
			if (this.currentStock && orderToFill) {
				def shipmentSent = this.currentStock > orderToFill ? orderToFill : this.currentStock
				downstream.productPipelines[this.getWho()].add(0, shipmentSent)
				this.backlog[downstream.getWho()] = (orderToFill - shipmentSent).max(0.0)
				this.currentStock -= shipmentSent
				this.cash -= productionCost * shipmentSent
			} else {
				downstream.productPipelines[this.getWho()].add(0, 0.0)
				this.backlog[downstream.getWho()] += this.ordersReceived[downstream.getWho()]
			}
		}
	}

	def updateTrust(){
		for (ChainLevel upstream in this.upstreamLevel) {
			def shipmentToReceive = this.ordersSentChecklist[upstream.getWho()].pop()
			def shipmentReceived = this.shipmentsReceivedChecklist[upstream.getWho()].pop()
			this.currentShipmentToReceive[upstream.getWho()] = shipmentToReceive
			this.currentShipmentReceivedOnTime[upstream.getWho()] = shipmentToReceive.min(shipmentReceived)
			this.totalShipmentsToReceive[upstream.getWho()] += shipmentToReceive
			this.totalShipmentsReceived[upstream.getWho()] += shipmentReceived
			this.totalShipmentsReceivedOnTime[upstream.getWho()] += shipmentToReceive.min(shipmentReceived)
		}

		this."$trustRule"()
	}

	def trustByCurrentOnTimeDeliveryRate() {
		for (ChainLevel upstream in this.upstreamLevel) {
			if (this.currentShipmentToReceive[upstream.getWho()]) {
				this.trustUpstreams[upstream.getWho()] = this.currentShipmentReceivedOnTime[upstream.getWho()] / this.currentShipmentToReceive[upstream.getWho()]
			}
		}
	}

	def trustByAccumulatedOnTimeDeliveryRate() {
		for (ChainLevel upstream in this.upstreamLevel) {
			if (this.totalShipmentsToReceive[upstream.getWho()]) {
				this.trustUpstreams[upstream.getWho()] = this.totalShipmentsReceivedOnTime[upstream.getWho()] / this.totalShipmentsToReceive[upstream.getWho()]
			}
		}
	}

	def trustByAccumulatedDeliveryRate() {
		for (ChainLevel upstream in this.upstreamLevel) {
			if (this.totalShipmentsToReceive[upstream.getWho()]) {
				this.trustUpstreams[upstream.getWho()] = this.totalShipmentsReceived[upstream.getWho()] / this.totalShipmentsToReceive[upstream.getWho()]
			}
		}
	}

	def largestDueOrdersFirst(){
		return this.downstreamLevel.clone().sort{ ChainLevel a, ChainLevel b ->
			this.ordersReceived[a.getWho()] + this.backlog[a.getWho()] <=> this.ordersReceived[b.getWho()] + this.backlog[b.getWho()]
		}
	}

	def largestAccumulatedOrdersFirst(){
		return this.downstreamLevel.clone().sort{ ChainLevel a, ChainLevel b ->
			this.totalOrdersReceived[a.getWho()] <=> this.totalOrdersReceived[b.getWho()]
		}
	}

	def calculateSaleMarkup() {
		this.strategy.calculateSaleMarkup(this)
	}

	def decideNextSupplier() {
		this.strategy.decideNextSupplier(this)
	}

	def receiveOrders(){
		this.expectedDemand = THETA * this.ordersReceived.values().sum() + (1 - THETA) * this.expectedDemand
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
		def totalOrders = this.expectedDemand + ALPHA * (this.strategy.desiredStock - effectiveStock + BETA * (desiredSupplyLine - supplyLine))
		return totalOrders.max(0.0)
	}

	def placeOrder(orderSize) {
		def order
		for (ChainLevel upstream in this.upstreamLevel) {
			if (upstream == this.supplier) {
				order = orderSize

				def orderValue = this.supplier.saleMarkup * orderSize
				this.cash -= orderValue
				upstream.cash += orderValue
			} else {
				order = 0.0
			}
			upstream.orderPipelines[this.getWho()].add(0,  order)
			this.ordersSentChecklist[upstream.getWho()].add(0,  order)
		}
	}

	def payStockCosts() {
		this.cash -= this.currentStock
	}

	def getOrderPipelineSum() {
		return this.orderPipelines.values().flatten().sum()
	}

	def getOrdersReceivedSum() {
		return this.ordersReceived.values().sum()
	}

	def getEffectiveStock() {
		return this.currentStock - this.backlog.values().sum()
	}

	def getUtility() {
		return 0.5 * this.currentStock + this.backlog.values().sum()
	}

	def getClientCount() {
		return filter({it.supplier == this}, this.downstreamLevel).size()
	}

	def getMeanTrust() {
		return this.downstreamLevel.collect{it.trustUpstreams[this.getWho()]}.sum()/this.downstreamLevel.size()
	}

	def getProfitMargin() {
		if (this.supplier) {
			return this.saleMarkup - productionCost - this.supplier.saleMarkup
		} else {
			return 0.0
		}
	}

	def getTrustInSupplier() {
		if (this.supplier) {
			return this.trustUpstreams[this.supplier.getWho()]
		} else {
			return -1
		}
	}

	def refreshView() {
		this.label = "" + round(100 * this.getEffectiveStock()) / 100 + "," + round(100 * this.cash) / 100

		ask(myOutLinks()){ die() }
		for (ChainLevel upstream in this.upstreamLevel) {
			if (upstream == this.supplier) {
				def route = createLinkTo(upstream)
				route.color = scaleColor(red(), this.trustUpstreams[upstream.getWho()], -1.0, 1.0)
			}
		}
	}
}
