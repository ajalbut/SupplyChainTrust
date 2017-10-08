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

		def strategies = [
			new Strategy(name: 'safe', color: blue(), desiredStock: desiredStock),
			new Strategy(name: 'risky', color: red(), desiredStock: 0.0)
		]

		for (def i = 0; i < agentsPerLevel; i++) {
			def xvalue
			if (agentsPerLevel > 1) {
				xvalue = 24 * (i/(agentsPerLevel - 1)) - 12
			} else {
				xvalue = 0
			}
			ask(factories[i]){ setup(xvalue, 12, strategies[i % strategies.size()])}
			ask(distributors[i]){ setup(xvalue, 6, strategies[i % strategies.size()])}
			ask(wholesalers[i]){ setup(xvalue, 0, strategies[i % strategies.size()])}
			ask(retailers[i]){ setup(xvalue, -6, strategies[i % strategies.size()])}
			ask(customers[i]) { setup(xvalue, -12, strategies[i % strategies.size()])}
		}
		ask(chainLevels()){initializeState()}
	}

	@Go
	def go(){
		tick()
		def chainLevels = chainLevels()
		ask(chainLevels){receiveShipments()}
		ask(chainLevels){fillOrders()}
		ask(chainLevels){updateTrust()}
		ask(chainLevels){decideNextSupplier()}
		ask(chainLevels){receiveOrders()}
		ask(chainLevels){makeOrders()}
		ask(chainLevels){refreshView()}
		if (ticks() == maxStep) {
			stop()
		}
	}

	def distributorsOrdersSent(){
		return sum(factories().collect{it.getOrderPipelineSum()})
	}

	def wholesalersOrdersSent(){
		return sum(distributors().collect{it.getOrderPipelineSum()})
	}

	def retailersOrdersSent(){
		return sum(wholesalers().collect{it.getOrderPipelineSum()})
	}

	def customersOrdersSent(){
		return sum(retailers().collect{it.getOrderPipelineSum()})
	}

	def totalOrdersSent() {
		return distributorsOrdersSent() + wholesalersOrdersSent() + retailersOrdersSent() + customersOrdersSent()
	}

	def factoriesStock(){
		return sum(factories().collect{it.getEffectiveStock()})
	}

	def distributorsStock(){
		return sum(distributors().collect{it.getEffectiveStock()})
	}

	def wholesalersStock(){
		return sum(wholesalers().collect{it.getEffectiveStock()})
	}

	def retailersStock(){
		return sum(retailers().collect{it.getEffectiveStock()})
	}

	def totalStock() {
		return factoriesStock() + distributorsStock() + wholesalersStock() + retailersStock()
	}

	def safeUtility() {
		return sum(filter({it.strategy.name == 'safe'}, chainLevels()).collect{it.getUtility()})
	}

	def riskyUtility() {
		return sum(filter({it.strategy.name == 'risky'}, chainLevels()).collect{it.getUtility()})
	}

	def totalUtility() {
		return sum(chainLevels().collect{it.getUtility()})
	}

	def receivedCustomerOrders() {
		return sum(retailers().collect{it.getOrdersReceivedSum()})
	}
}