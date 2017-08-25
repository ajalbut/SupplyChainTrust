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

class Retailer extends ChainLevel {
	def setup(x, y, initialStock){
		setColor(green())
		this.upstreamLevel = wholesalers()
		this.downstreamLevel = customers()
		super.setup(x, y, initialStock)
	}
}
