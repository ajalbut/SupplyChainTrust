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

@Plural ("Factories")
class Factory extends ChainLevel {
	def productionOrder = [4.0]

	def setup(x, y, Strategy strategy){
		this.upstreamLevel = []
		this.downstreamLevel = distributors()
		this.minMarkup = productionCost + minProfit
		super.setup(x, y, strategy)
		this.pipelineSize = this.initialProductPipeline.size()
		this.productPipelines[this.getWho()] = this.initialProductPipeline.clone()
	}

	def receiveShipments(){
		this.productPipelines[this.getWho()].add(0, this.productionOrder.pop())
		this.currentStock += this.productPipelines[this.getWho()].pop()
	}

	def updateTrust(){}

	def decideNextSupplier(){}

	def calculateSupplyLine() {
		return this.productPipelines[this.getWho()].sum()
	}

	def placeOrder(orderSize) {
		this.productionOrder.add(0, orderSize)
	}
}
