package SupplyChainTrust.relogo

import repast.simphony.relogo.factories.AbstractReLogoGlobalsAndPanelFactory

public class UserGlobalsAndPanelFactory extends AbstractReLogoGlobalsAndPanelFactory{
	public void addGlobalsAndPanelComponents(){
		addStateChangeButtonWL("toggleTrustVisibility","Toggle Trust visibility")
		
		addMonitor("getGlobalUtility", 1)
	}
}