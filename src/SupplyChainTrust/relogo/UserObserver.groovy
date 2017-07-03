package SupplyChainTrust.relogo

import static repast.simphony.relogo.Utility.*;
import static repast.simphony.relogo.UtilityG.*;
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

		createFactories(3)
		createDistributors(3)
		createWholesalers(3)
		createRetailers(3)
		createCustomers(3)

		def factories = factories()
		def distributors = distributors()
		def wholesalers = wholesalers()
		def retailers = retailers()
		def customers = customers()

		Random random = new Random()
		ask(factories[0]){ setup(-12, 12, randomInitialStock(random)) }
		ask(factories[1]){ setup(0, 12, randomInitialStock(random)) }
		ask(factories[2]){ setup(12, 12, randomInitialStock(random)) }
		ask(distributors[0]){ setup(-12, 6, randomInitialStock(random)) }
		ask(distributors[1]){ setup(0, 6, randomInitialStock(random)) }
		ask(distributors[2]){ setup(12, 6, randomInitialStock(random)) }
		ask(wholesalers[0]){ setup(-12, 0 , randomInitialStock(random)) }
		ask(wholesalers[1]){ setup(0, 0, randomInitialStock(random)) }
		ask(wholesalers[2]){ setup(12, 0, randomInitialStock(random)) }
		ask(retailers[0]){ setup(-12, -6, randomInitialStock(random)) }
		ask(retailers[1]){ setup(0, -6, randomInitialStock(random)) }
		ask(retailers[2]){ setup(12, -6, randomInitialStock(random)) }
		ask(customers[0]) { setup(-12, -12, 0.0) }
		ask(customers[1]) { setup(0, -12, 0.0) }
		ask(customers[2]) { setup(12, -12, 0.0) }
	}

	def visibility = 'upstream'

	@Go
	def go(){
		def rule = supplyRule

		tick()
		ask(chainLevels()){setSupplyRule(rule)}
		ask(chainLevels()){receiveShipments()}
		ask(chainLevels()){updateUpstreamTrust()}
		ask(chainLevels()){fillOrders()}
		ask(chainLevels()){receiveOrders()}
		ask(chainLevels()){makeOrders()}
		ask(chainLevels()){updateDownstreamTrust()}
		ask(chainLevels()){refreshTrustLinks(visibility)}
		if (ticks() == 100) {
			stop()
		}
	}

	def toggleTrustVisibility(){
		if (visibility == 'upstream') {
			visibility = 'downstream'
		} else {
			visibility = 'upstream'
		}
		ask(chainLevels()) { refreshTrustLinks(visibility) }
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

	def getFactory0Stock(){
		return factories()[0].getStockMinusBackorder()
	}

	def getDistributor0Stock(){
		return distributors()[0].getStockMinusBackorder()
	}

	def getWholesaler0Stock(){
		return wholesalers()[0].getStockMinusBackorder()
	}

	def getRetailer0Stock(){
		return retailers()[0].getStockMinusBackorder()
	}

	def getFactory1Stock(){
		return factories()[1].getStockMinusBackorder()
	}

	def getDistributor1Stock(){
		return distributors()[1].getStockMinusBackorder()
	}

	def getWholesaler1Stock(){
		return wholesalers()[1].getStockMinusBackorder()
	}

	def getRetailer1Stock(){
		return retailers()[1].getStockMinusBackorder()
	}

	def getFactory2Stock(){
		return factories()[2].getStockMinusBackorder()
	}

	def getDistributor2Stock(){
		return distributors()[2].getStockMinusBackorder()
	}

	def getWholesaler2Stock(){
		return wholesalers()[2].getStockMinusBackorder()
	}

	def getRetailer2Stock(){
		return retailers()[2].getStockMinusBackorder()
	}

	def getDistributor0TrustFromUpstreams(){
		return distributors()[0].getCurrentTrustFromUpstreams()
	}

	def getWholesaler0TrustFromUpstreams(){
		return wholesalers()[0].getCurrentTrustFromUpstreams()
	}

	def getRetailer0TrustFromUpstreams(){
		return retailers()[0].getCurrentTrustFromUpstreams()
	}

	def getCustomer0TrustFromUpstreams(){
		return customers()[0].getCurrentTrustFromUpstreams()
	}

	def getDistributor1TrustFromUpstreams(){
		return distributors()[1].getCurrentTrustFromUpstreams()
	}

	def getWholesaler1TrustFromUpstreams(){
		return wholesalers()[1].getCurrentTrustFromUpstreams()
	}

	def getRetailer1TrustFromUpstreams(){
		return retailers()[1].getCurrentTrustFromUpstreams()
	}

	def getCustomer1TrustFromUpstreams(){
		return customers()[1].getCurrentTrustFromUpstreams()
	}

	def getDistributor2TrustFromUpstreams(){
		return distributors()[2].getCurrentTrustFromUpstreams()
	}

	def getWholesaler2TrustFromUpstreams(){
		return wholesalers()[2].getCurrentTrustFromUpstreams()
	}

	def getRetailer2TrustFromUpstreams(){
		return retailers()[2].getCurrentTrustFromUpstreams()
	}

	def getCustomer2TrustFromUpstreams(){
		return customers()[2].getCurrentTrustFromUpstreams()
	}

	def getFactory0TrustFromDownstreams(){
		return factories()[0].getCurrentTrustFromDownstreams()
	}

	def getDistributor0TrustFromDownstreams(){
		return distributors()[0].getCurrentTrustFromDownstreams()
	}

	def getWholesaler0TrustFromDownstreams(){
		return wholesalers()[0].getCurrentTrustFromDownstreams()
	}

	def getRetailer0TrustFromDownstreams(){
		return retailers()[0].getCurrentTrustFromDownstreams()
	}

	def getFactory1TrustFromDownstreams(){
		return factories()[1].getCurrentTrustFromDownstreams()
	}

	def getDistributor1TrustFromDownstreams(){
		return distributors()[1].getCurrentTrustFromDownstreams()
	}

	def getWholesaler1TrustFromDownstreams(){
		return wholesalers()[1].getCurrentTrustFromDownstreams()
	}

	def getRetailer1TrustFromDownstreams(){
		return retailers()[1].getCurrentTrustFromDownstreams()
	}

	def getFactory2TrustFromDownstreams(){
		return factories()[2].getCurrentTrustFromDownstreams()
	}

	def getDistributor2TrustFromDownstreams(){
		return distributors()[2].getCurrentTrustFromDownstreams()
	}

	def getWholesaler2TrustFromDownstreams(){
		return wholesalers()[2].getCurrentTrustFromDownstreams()
	}

	def getRetailer2TrustFromDownstreams(){
		return retailers()[2].getCurrentTrustFromDownstreams()
	}
	
	def randomInitialStock(Random random){
		return 1.0 * random.nextInt(41)
	}
	
}