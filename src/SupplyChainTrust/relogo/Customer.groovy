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

class Customer extends ChainLevel {
	def setup(x, y){
		setColor(blue())
		this.upstreamLevel = retailers()
		this.downstreamLevel = []
		super.setup(x, y)
	}

	def initializeStock() {
		return 0.0
	}

	def receiveOrders(){}

	def fillOrders(){}

	def calculateSupplyLine(){}

	def calculateOrderSize(supplyLine) {
		return this."$orderSizeRule"()
	}

	def beerGameOrderSize() {
		def orderSize
		if (ticks() > 4) {
			orderSize = 8.0
		} else {
			orderSize = 4.0
		}
		return orderSize
	}

	def randomOrderSize() {
		return random.nextInt(11) * 1.0
	}

	def trustBasedOrderSize() {
		return this.trustUpstreams[this.supplier.getWho()] * 10.0
	}

	def getEffectiveStock() {
		return this.currentStock
	}

	def getUtility() {
		return 0.0
	}
}
