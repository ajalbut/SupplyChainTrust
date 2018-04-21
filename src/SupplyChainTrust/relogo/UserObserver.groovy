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

		def strategyConstructorMap = [
			'safe': new SafeStrategy(name: 'safe', color: blue(), desiredStock: desiredStock),
			'risky': new RiskyStrategy(name: 'risky', color: red(), desiredStock: 0.0),
			'random': new RandomStrategy(name: 'random', color: green(), desiredStock: BigDecimal.valueOf(random.nextFloat()) * desiredStock)
		]
		def strategyProportionList = strategies.split('_')
		def strategyList = []
		for (strategyProportion in strategyProportionList) {
			def (strategy, proportion) = strategyProportion.split(':')
			strategyList += Collections.nCopies(proportion.toInteger(), strategyConstructorMap[strategy])
		}

		for (def i = 0; i < agentsPerLevel; i++) {
			def xvalue
			if (agentsPerLevel > 1) {
				xvalue = 24 * (i/(agentsPerLevel - 1)) - 12
			} else {
				xvalue = 0
			}
			ask(factories[i]){ setup(xvalue, 12, strategyList[i % strategyList.size()], i < agentsPerLevel * liarChance)}
			ask(distributors[i]){ setup(xvalue, 6, strategyList[i % strategyList.size()], i < agentsPerLevel * liarChance)}
			ask(wholesalers[i]){ setup(xvalue, 0, strategyList[i % strategyList.size()], i < agentsPerLevel * liarChance)}
			ask(retailers[i]){ setup(xvalue, -6, strategyList[i % strategyList.size()], i < agentsPerLevel * liarChance)}
			ask(customers[i]) { setup(xvalue, -12, strategyList[i % strategyList.size()], i < agentsPerLevel * liarChance)}
		}
		ask(chainLevels()){initializeState()}
	}

	@Go
	def go(){
		tick()
		def step = ticks()
		def chainLevels = chainLevels()
		ask(chainLevels){receiveShipments()}
		ask(chainLevels){fillOrders()}
		ask(chainLevels){payStockCosts()}
		ask(chainLevels){updateTrust()}
		ask(chainLevels){receiveOrders()}
		if (step % contractSteps == 0) {
			ask(chainLevels){updateImages()}
			ask(chainLevels){calculateSaleMarkup()}
			ask(chainLevels){decideNextSupplier()}
		}
		ask(chainLevels){makeOrders()}
		ask(chainLevels){refreshView()}
		if (step == maxStep) {
			stop()
		}
	}

	def distributorsOrdersSent(){
		return mean(factories().collect{it.getOrderPipelineSum()})
	}

	def wholesalersOrdersSent(){
		return mean(distributors().collect{it.getOrderPipelineSum()})
	}

	def retailersOrdersSent(){
		return mean(wholesalers().collect{it.getOrderPipelineSum()})
	}

	def customersOrdersSent(){
		return mean(retailers().collect{it.getOrderPipelineSum()})
	}

	def factoriesStock(){
		return mean(factories().collect{it.getEffectiveStock()})
	}

	def distributorsStock(){
		return mean(distributors().collect{it.getEffectiveStock()})
	}

	def wholesalersStock(){
		return mean(wholesalers().collect{it.getEffectiveStock()})
	}

	def retailersStock(){
		return mean(retailers().collect{it.getEffectiveStock()})
	}

	def totalUtility() {
		return sum(chainLevels().collect{it.getUtility()})
	}

	def receivedCustomerOrders() {
		return sum(retailers().collect{it.getOrdersReceivedSum()})
	}

	def safeCash() {
		return getStrategyIndicator('safe', 'getCash')
	}

	def riskyCash() {
		return getStrategyIndicator('risky', 'getCash')
	}

	def randomCash() {
		return getStrategyIndicator('random', 'getCash')
	}

	def safeUtility() {
		return getStrategyIndicator('safe', 'getUtility')
	}

	def riskyUtility() {
		return getStrategyIndicator('risky', 'getUtility')
	}

	def randomUtility() {
		return getStrategyIndicator('random', 'getUtility')
	}

	def safeClientCount() {
		return getStrategyIndicator('safe', 'getClientCount')
	}

	def riskyClientCount() {
		return getStrategyIndicator('risky', 'getClientCount')
	}

	def randomClientCount() {
		return getStrategyIndicator('random', 'getClientCount')
	}

	def safeTrust() {
		return getStrategyIndicator('safe', 'getMeanTrust')
	}

	def riskyTrust() {
		return getStrategyIndicator('risky', 'getMeanTrust')
	}

	def randomTrust() {
		return getStrategyIndicator('random', 'getMeanTrust')
	}

	def safeProfitMargin() {
		return getStrategyIndicator('safe', 'getProfitMargin')
	}

	def riskyProfitMargin() {
		return getStrategyIndicator('risky', 'getProfitMargin')
	}

	def randomProfitMargin() {
		return getStrategyIndicator('random', 'getProfitMargin')
	}

	def sincereTrust() {
		return getSincereLiarIndicator(true, 'getMeanTrust')
	}

	def liarTrust() {
		return getSincereLiarIndicator(false, 'getMeanTrust')
	}

	def safeSincereTrust() {
		return getStrategySincereLiarIndicator('safe', true, 'getMeanTrust')
	}

	def riskySincereTrust() {
		return getStrategySincereLiarIndicator('risky', true, 'getMeanTrust')
	}

	def safeLiarTrust() {
		return getStrategySincereLiarIndicator('safe', false, 'getMeanTrust')
	}

	def riskyLiarTrust() {
		return getStrategySincereLiarIndicator('risky', false, 'getMeanTrust')
	}

	def sincereCash() {
		return getSincereLiarIndicator(true, 'getCash')
	}

	def liarCash() {
		return getSincereLiarIndicator(false, 'getCash')
	}

	def safeSincereCash() {
		return getStrategySincereLiarIndicator('safe', true, 'getCash')
	}

	def riskySincereCash() {
		return getStrategySincereLiarIndicator('risky', true, 'getCash')
	}

	def safeLiarCash() {
		return getStrategySincereLiarIndicator('safe', false, 'getCash')
	}

	def riskyLiarCash() {
		return getStrategySincereLiarIndicator('risky', false, 'getCash')
	}

	def trustedPeersRatio() {
		return mean(filter({it.turtleType != 'Factory'}, chainLevels()).collect{it.getTrustedPeersRatio()})
	}

	def getStrategyIndicator(strategyName, indicatorMethod) {
		return mean(filter({it.turtleType != 'Customer' & it.strategy.name == strategyName}, chainLevels()).collect{it."$indicatorMethod"()})
	}

	def getSincereLiarIndicator(sincere, indicatorMethod) {
		return mean(filter({it.turtleType != 'Customer' & it.liar == !sincere}, chainLevels()).collect{it."$indicatorMethod"()})
	}

	def getStrategySincereLiarIndicator(strategyName, sincere, indicatorMethod) {
		return mean(filter({it.turtleType != 'Customer' & it.strategy.name == strategyName & it.liar == !sincere}, chainLevels()).collect{it."$indicatorMethod"()})
	}
}