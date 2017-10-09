package SupplyChainTrust.relogo

import repast.simphony.parameter.Parameters
import repast.simphony.relogo.factories.AbstractReLogoGlobalsAndPanelFactory
import repast.simphony.engine.environment.RunEnvironment

public class UserGlobalsAndPanelFactory extends AbstractReLogoGlobalsAndPanelFactory{
	public void addGlobalsAndPanelComponents(){
		Parameters p = RunEnvironment.getInstance().getParameters();
		addGlobal('supplyRule', p.getValue("supplyRule"))
		addGlobal('trustRule', p.getValue("trustRule"))
		addGlobal('orderSizeRule', p.getValue("orderSizeRule"))
		addGlobal('initialStockRule', p.getValue('initialStockRule'))
		addGlobal('initialStockValue', p.getValue('initialStockValue'))
		addGlobal('desiredStock', p.getValue('desiredStock'))
		addGlobal('agentsPerLevel', p.getValue("agentsPerLevel"))
		addGlobal('productionCost', p.getValue("productionCost"))
		addGlobal('minProfit', p.getValue("minProfit"))
		addGlobal('maxProfit', p.getValue("maxProfit"))
		addGlobal('maxStep', p.getValue("maxStep"))
		addGlobal('ALPHA', p.getValue("ALPHA"))
		addGlobal('BETA', p.getValue("BETA"))
		addGlobal('THETA', p.getValue("THETA"))
		addGlobal('random', new Random())
	}
}