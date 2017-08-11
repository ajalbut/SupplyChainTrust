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
	def setup(x, y, initialStock, supplyRule, agentsPerLevel){
		setColor(blue())
		this.upstreamLevel = retailers()
		this.downstreamLevel = []
		super.setup(x, y, initialStock, supplyRule, agentsPerLevel)
	}

	def receiveOrders(){}

	def fillOrders(){}

	def makeOrders(){
		def totalOrderSize
		if (ticks() > 4) {
			totalOrderSize = 8.0 * this.agentsPerLevel
		} else {
			totalOrderSize = 4.0 * this.agentsPerLevel
		}
		this.distributeOrdersToSend(totalOrderSize)
	}
}
