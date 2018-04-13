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
		addGlobal('desiredStock', new BigDecimal(p.getValue('desiredStock')))
		addGlobal('agentsPerLevel', p.getValue("agentsPerLevel"))
		addGlobal('candidatesPerStep', p.getValue("candidatesPerStep"))
		addGlobal('confidenceThreshold', new BigDecimal(p.getValue("confidenceThreshold")))
		addGlobal('priceThreshold', new BigDecimal(p.getValue("priceThreshold")))
		addGlobal('productionCost', new BigDecimal(p.getValue("productionCost")))
		addGlobal('minProfit', new BigDecimal(p.getValue("minProfit")))
		addGlobal('maxProfit', new BigDecimal(p.getValue("maxProfit")))
		addGlobal('priceOffset', new BigDecimal(p.getValue("priceOffset")))
		addGlobal('contractSteps', p.getValue("contractSteps"))
		addGlobal('maxStep', p.getValue("maxStep"))
		addGlobal('strategies', p.getValue("strategies"))
		addGlobal('ALPHA', new BigDecimal(p.getValue("ALPHA")))
		addGlobal('BETA', new BigDecimal(p.getValue("BETA")))
		addGlobal('THETA', new BigDecimal(p.getValue("THETA")))
		addGlobal('EPSILON1', new BigDecimal(p.getValue("EPSILON1")))
		addGlobal('EPSILON2', new BigDecimal(p.getValue("EPSILON2")))
		addGlobal('random', new Random())
	}
}