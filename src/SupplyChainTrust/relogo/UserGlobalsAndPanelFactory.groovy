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
		addGlobal('agentsPerLevel', p.getValue("agentsPerLevel"))
		addGlobal('maxStep', p.getValue("maxStep"))
		addGlobal('random', new Random())
	}
}