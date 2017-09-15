package SupplyChainTrust.relogo

import static repast.simphony.relogo.Utility.*;
import static repast.simphony.relogo.UtilityG.*;

import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.parameter.Parameters
import repast.simphony.relogo.Stop;
import repast.simphony.relogo.Utility;
import repast.simphony.relogo.UtilityG;
import repast.simphony.relogo.schedule.Go;
import repast.simphony.relogo.schedule.Setup;
import SupplyChainTrust.ReLogoObserver;

class UserObserver extends ReLogoObserver{

	def visibility = 'upstream'
	Parameters p = RunEnvironment.getInstance().getParameters();
	def supplyRule = p.getValue("supplyRule")
	def numberFactories = p.getValue("numberFactories")
	def numberDistributors = p.getValue("numberDistributors")
	def numberWholesalers = p.getValue("numberWholesalers")
	def numberRetailers = p.getValue("numberRetailers")
	def numberCustomers = p.getValue("numberCustomers")
	def maxStep = p.getValue("maxStep")

	@Setup
	def setup(){
		clearAll()

		createFactories(this.numberFactories)
		createDistributors(this.numberDistributors)
		createWholesalers(this.numberWholesalers)
		createRetailers(this.numberRetailers)
		createCustomers(this.numberCustomers)

		def factories = factories()
		def distributors = distributors()
		def wholesalers = wholesalers()
		def retailers = retailers()
		def customers = customers()

		Random random = new Random()
		for (def i = 0; i < this.numberFactories; i++) {
			ask(factories[i]){ setup(this.calculateXValue(i, this.numberFactories), 12, randomInitialStock(random), this.supplyRule, this.numberFactories) }
		}
		for (def i = 0; i < this.numberDistributors; i++) {
			ask(distributors[i]){ setup(this.calculateXValue(i, this.numberDistributors), 6, randomInitialStock(random), this.supplyRule, this.numberDistributors) }
		}
		for (def i = 0; i < this.numberWholesalers; i++) {
			ask(wholesalers[i]){ setup(this.calculateXValue(i, this.numberWholesalers), 0 , randomInitialStock(random), this.supplyRule, this.numberWholesalers) }
		}
		for (def i = 0; i < this.numberRetailers; i++) {
			ask(retailers[i]){ setup(this.calculateXValue(i, this.numberRetailers), -6, randomInitialStock(random), this.supplyRule, this.numberRetailers) }
		}
		for (def i = 0; i < this.numberCustomers; i++) {
			ask(customers[i]) { setup(this.calculateXValue(i, this.numberCustomers), -12, 0.0, this.supplyRule, this.numberCustomers) }
		}
	}

	def calculateXValue(count, numberAgents) {
		if (numberAgents > 1) {
			return 24 * (count/(numberAgents - 1)) - 12
		} else {
			return 0
		}
	}

	@Go
	def go(){
		tick()
		ask(chainLevels()){receiveShipments()}
		ask(chainLevels()){updateUpstreamTrust()}
		ask(chainLevels()){fillOrders()}
		ask(chainLevels()){receiveOrders()}
		ask(chainLevels()){makeOrders()}
		ask(chainLevels()){updateDownstreamTrust()}
		ask(chainLevels()){refreshTrustLinks(this.visibility)}
		if (ticks() == this.maxStep) {
			stop()
		}
	}

	def toggleTrustVisibility(){
		if (this.visibility == 'upstream') {
			this.visibility = 'downstream'
		} else {
			this.visibility = 'upstream'
		}
		ask(chainLevels()) { refreshTrustLinks(this.visibility) }
	}

	def distributorsOrdersSent(){
		def orders = 0
		ask(factories()){ orders += orderPipelines.values()[0].sum()}
		return orders
	}

	def wholesalersOrdersSent(){
		def orders = 0.0
		ask(distributors()){ orders += orderPipelines.values()[0].sum()}
		return orders
	}

	def retailersOrdersSent(){
		def orders = 0.0
		ask(wholesalers()){ orders += orderPipelines.values()[0].sum()}
		return orders
	}

	def customersOrdersSent(){
		def orders = 0.0
		ask(retailers()){ orders += orderPipelines.values()[0].sum()}
		return orders
	}

	def stepTotalOrdersSent() {
		return distributorsOrdersSent() + wholesalersOrdersSent() + retailersOrdersSent() + customersOrdersSent()
	}

	def factoriesStock(){
		def stock = 0
		ask(factories()){ stock += currentStock - backlog.values().sum()}
		return stock
	}

	def distributorsStock(){
		def stock = 0
		ask(distributors()){ stock += currentStock - backlog.values().sum()}
		return stock
	}

	def wholesalersStock(){
		def stock = 0
		ask(wholesalers()){ stock += currentStock - backlog.values().sum()}
		return stock
	}

	def retailersStock(){
		def stock = 0
		ask(retailers()){ stock += currentStock - backlog.values().sum()}
		return stock
	}

	def stepTotalStock() {
		return factoriesStock() + distributorsStock() + wholesalersStock() + retailersStock()
	}

	def getGlobalUtility() {
		def stepUtility = 0
		ask(factories()){ stepUtility += 0.5 * currentStock + backlog.values().sum()}
		ask(distributors()){ stepUtility += 0.5 * currentStock + backlog.values().sum()}
		ask(wholesalers()){ stepUtility += 0.5 * currentStock + backlog.values().sum()}
		ask(retailers()){ stepUtility += 0.5 * currentStock + backlog.values().sum()}
		return stepUtility
	}

	def randomInitialStock(Random random){
		return 1.0 * random.nextInt(41)
	}
}