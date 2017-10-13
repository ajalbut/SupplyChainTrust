package SupplyChainTrust.relogo
import static repast.simphony.relogo.Utility.*;
import static repast.simphony.relogo.UtilityG.*;

import java.awt.geom.FlatteningPathIterator
import java.util.LinkedHashMap.LinkedValues
import java.util.regex.Pattern.First

abstract class Strategy {
	def name
	def color
	def desiredStock

	def calculateSaleMarkup(ChainLevel self) {}

	def decideNextSupplier(ChainLevel self, rule) {
		def newSupplier
		def candidates = self.upstreamLevel.clone()
		while (!newSupplier & candidates.size() != 0) {
			ChainLevel chosen = this.chooseRandomSupplier(self, candidates, [:])
			if (chosen.strategy.acceptClient(chosen)) {
				newSupplier = chosen
			} else {
				candidates.remove(chosen)
			}
		}

		if (!!newSupplier & !!self.supplier) {
			if (this."$rule"(self, newSupplier)) {
				self.supplier = newSupplier
			} else {
				def randomFraction = self.random.nextInt(1000001)/1000000
				if (randomFraction >= self.EPSILON) {
					self.supplier = newSupplier
				}
			}
		} else if (newSupplier) {
			self.supplier = newSupplier
		}
	}

	def preferredTrustRule(ChainLevel self, ChainLevel newSupplier) {
		return self.trustUpstreams[newSupplier] > self.trustUpstreams[self.supplier]
	}

	def preferredPriceRule(ChainLevel self, ChainLevel newSupplier) {
		return newSupplier.saleMarkup < self.supplier.saleMarkup
	}

	def acceptClient(ChainLevel self) {}

	def chooseRandomSupplier(ChainLevel self, candidates, weights) {
		def trustSum = 0.0
		if (weights.values()) {
			trustSum = weights.values().sum()
		}
		if (trustSum) {
			def trustPartial = 0.0
			def randomTrustSumFraction = self.random.nextInt(1000001)/1000000 *	trustSum
			for (ChainLevel upstream in candidates) {
				trustPartial += weights[upstream.getWho()]
				if (randomTrustSumFraction <= trustPartial) {
					return upstream
				}
			}
		} else {
			return candidates[self.random.nextInt(candidates.size())]
		}
	}
}

class SafeStrategy extends Strategy {

	def decideNextSupplier(ChainLevel self) {
		super.decideNextSupplier(self, 'preferredTrustRule')
	}

	def calculateSaleMarkup(ChainLevel self) {
		self.saleMarkup = self.minMarkup
	}

	def acceptClient(ChainLevel self) {
		return (self.getEffectiveStock() >= 0)
	}
}

class RiskyStrategy extends Strategy {

	def decideNextSupplier(ChainLevel self) {
		super.decideNextSupplier(self, 'preferredPriceRule')
	}

	def calculateSaleMarkup(ChainLevel self) {
		def clientCount = filter({ self == it.supplier }, self.downstreamLevel).size()
		self.saleMarkup = self.minMarkup + (self.maxMarkup - self.minMarkup) * clientCount / self.agentsPerLevel
	}

	def acceptClient(ChainLevel self) {
		return true
	}
}