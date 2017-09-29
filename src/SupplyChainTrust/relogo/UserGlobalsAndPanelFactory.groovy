package SupplyChainTrust.relogo

import repast.simphony.parameter.Parameters
import repast.simphony.relogo.factories.AbstractReLogoGlobalsAndPanelFactory
import repast.simphony.engine.environment.RunEnvironment

public class UserGlobalsAndPanelFactory extends AbstractReLogoGlobalsAndPanelFactory{
	public void addGlobalsAndPanelComponents(){
		addMonitor("getGlobalUtility", 1)

		Parameters p = RunEnvironment.getInstance().getParameters();
		addGlobal('supplyRule', p.getValue("supplyRule"))
		addGlobal('trustRule', p.getValue("trustRule"))
		addGlobal('orderSizeRule', p.getValue("orderSizeRule"))
		addGlobal('initialStockRule', p.getValue('initialStockRule'))
		addGlobal('initialStockValue', p.getValue('initialStockValue'))
		addGlobal('desiredStock', p.getValue('desiredStock'))
		addGlobal('agentsPerLevel', p.getValue("agentsPerLevel"))
		addGlobal('maxStep', p.getValue("maxStep"))
		addGlobal('ALPHA', p.getValue("ALPHA"))
		addGlobal('BETA', p.getValue("BETA"))
		addGlobal('THETA', p.getValue("THETA"))
		addGlobal('random', new Random())
	}
}