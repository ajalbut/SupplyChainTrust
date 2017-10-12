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

	def decideNextSupplier(ChainLevel self) {}

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
		if (self.supplier) {
			def randomFraction = self.random.nextInt(1000001)/1000000
			if (randomFraction <= self.trustUpstreams[self.supplier.getWho()]) {
				return
			}
		}

		self.supplier = null
		def candidates = self.upstreamLevel.clone()
		def weights = self.trustUpstreams.clone()
		while (!self.supplier & candidates.size() != 0) {
			ChainLevel supplier = this.chooseRandomSupplier(self, candidates, weights)
			if (supplier.strategy.acceptClient(supplier)) {
				self.supplier = supplier
			} else {
				candidates.remove(supplier)
				weights.remove(supplier.getWho())
			}
		}
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
		self.supplier = null
		def candidates = self.upstreamLevel.clone().groupBy{it.saleMarkup}.sort()
		while (!self.supplier & candidates.size() > 0) {
			def candidateGroup = candidates.take(1).values().flatten()
			candidates = candidates.drop(1)
			while (!self.supplier & candidateGroup.size() > 0) {
				ChainLevel supplier = this.chooseRandomSupplier(self, candidateGroup, [:])
				if (supplier.strategy.acceptClient(supplier)) {
					self.supplier = supplier
				} else {
					candidateGroup.remove(supplier)
				}
			}
		}
	}

	def calculateSaleMarkup(ChainLevel self) {
		def clientCount = filter({ self == it.supplier }, self.downstreamLevel).size()
		self.saleMarkup = self.minMarkup + (self.maxMarkup - self.minMarkup) * clientCount / self.agentsPerLevel
	}

	def acceptClient(ChainLevel self) {
		return true
	}
}