package SupplyChainTrust.relogo

import static repast.simphony.relogo.Utility.*;
import static repast.simphony.relogo.UtilityG.*;

import repast.simphony.parameter.Parameters
import repast.simphony.relogo.Stop;
import repast.simphony.relogo.Utility;
import repast.simphony.relogo.UtilityG;
import repast.simphony.relogo.schedule.Go;
import repast.simphony.relogo.schedule.Setup;
import SupplyChainTrust.ReLogoObserver;

class UserObserver extends ReLogoObserver{

	@Setup
	def setup(){
		clearAll()

		createFactories(agentsPerLevel)
		createDistributors(agentsPerLevel)
		createWholesalers(agentsPerLevel)
		createRetailers(agentsPerLevel)
		createCustomers(agentsPerLevel)

		def factories = factories()
		def distributors = distributors()
		def wholesalers = wholesalers()
		def retailers = retailers()
		def customers = customers()

		for (def i = 0; i < agentsPerLevel; i++) {
			def xvalue
			if (agentsPerLevel > 1) {
				xvalue = 24 * (i/(agentsPerLevel - 1)) - 12
			} else {
				xvalue = 0
			}
			ask(factories[i]){ setup(xvalue, 12)}
			ask(distributors[i]){ setup(xvalue, 6)}
			ask(wholesalers[i]){ setup(xvalue, 0)}
			ask(retailers[i]){ setup(xvalue, -6)}
			ask(customers[i]) { setup(xvalue, -12)}
		}
		ask(chainLevels()){initializeState()}
	}

	@Go
	def go(){
		tick()
		ask(chainLevels()){receiveShipments()}
		ask(chainLevels()){fillOrders()}
		ask(chainLevels()){updateTrust()}
		ask(chainLevels()){decideNextSupplier()}
		ask(chainLevels()){receiveOrders()}
		ask(chainLevels()){makeOrders()}
		ask(chainLevels()){refreshView()}
		if (ticks() == maxStep) {
			stop()
		}
	}

	def distributorsOrdersSent(){
		def orders = 0
		ask(factories()){ orders += orderPipelines.values().flatten().sum()}
		return orders
	}

	def wholesalersOrdersSent(){
		def orders = 0.0
		ask(distributors()){ orders += orderPipelines.values().flatten().sum()}
		return orders
	}

	def retailersOrdersSent(){
		def orders = 0.0
		ask(wholesalers()){ orders += orderPipelines.values().flatten().sum()}
		return orders
	}

	def customersOrdersSent(){
		def orders = 0.0
		ask(retailers()){ orders += orderPipelines.values().flatten().sum()}
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
}