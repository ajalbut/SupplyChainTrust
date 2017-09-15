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

	Parameters p = RunEnvironment.getInstance().getParameters();
	def supplyRule = p.getValue("supplyRule")
	def agentsPerLevel = p.getValue("agentsPerLevel")
	def maxStep = p.getValue("maxStep")

	@Setup
	def setup(){
		clearAll()

		createFactories(this.agentsPerLevel)
		createDistributors(this.agentsPerLevel)
		createWholesalers(this.agentsPerLevel)
		createRetailers(this.agentsPerLevel)
		createCustomers(this.agentsPerLevel)

		def factories = factories()
		def distributors = distributors()
		def wholesalers = wholesalers()
		def retailers = retailers()
		def customers = customers()

		Random random = new Random()
		for (def i = 0; i < this.agentsPerLevel; i++) {
			def xvalue
			if (this.agentsPerLevel > 1) {
				xvalue = 24 * (i/(this.agentsPerLevel - 1)) - 12
			} else {
				xvalue = 0
			}
			ask(factories[i]){ setup(xvalue, 12, randomInitialStock(random), this.supplyRule, this.agentsPerLevel) }
			ask(distributors[i]){ setup(xvalue, 6, randomInitialStock(random), this.supplyRule, this.agentsPerLevel) }
			ask(wholesalers[i]){ setup(xvalue, 0 , randomInitialStock(random), this.supplyRule, this.agentsPerLevel) }
			ask(retailers[i]){ setup(xvalue, -6, randomInitialStock(random), this.supplyRule, this.agentsPerLevel) }
			ask(customers[i]) { setup(xvalue, -12, 0.0, this.supplyRule, this.agentsPerLevel) }
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
		ask(chainLevels()){refreshView()}
		if (ticks() == this.maxStep) {
			stop()
		}
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