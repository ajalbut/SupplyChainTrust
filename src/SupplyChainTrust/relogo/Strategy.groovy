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

	def applyPreferredRule(ChainLevel self) {
		def set = self.upstreamLevel.clone()
		Collections.shuffle(set)
		def subset = set.take(self.candidatesPerStep)
		def candidates = self.strategy.getPreferredSupplierCandidates(self, subset)
		ChainLevel candidate = candidates.take(1).values().flatten()[0]
		ChainLevel newSupplier = self.strategy.acceptNewPreferredSupplier(self, candidate)
		if (newSupplier) {
			self.supplier = newSupplier
		} else {
			def randomFraction = BigDecimal.valueOf(self.random.nextFloat())
			if (randomFraction >= self.EPSILON2) {
				self.supplier = this.chooseRandomSupplier(self, self.upstreamLevel)
			}
		}
	}

	def getPreferredSupplierCandidates(ChainLevel self) {}

	def acceptNewPreferredSupplier(ChainLevel self, ChainLevel newSupplier) {}

	def chooseRandomSupplier(ChainLevel self, candidates) {
		return candidates[self.random.nextInt(candidates.size())]
	}
}

class SafeStrategy extends Strategy {
	def decideNextSupplier(ChainLevel self) {
		super.applyPreferredRule(self)
	}

	def getPreferredSupplierCandidates(ChainLevel self, subset) {
		subset = filter({self.trustUpstreams.containsKey(it.getWho())}, subset)
		return subset.groupBy{self.trustUpstreams[it.getWho()]}.sort{a,b -> b.key <=> a.key}
	}

	def acceptNewPreferredSupplier(ChainLevel self, ChainLevel newSupplier) {
		def randomFraction = BigDecimal.valueOf(self.random.nextFloat())
		if (randomFraction >= self.EPSILON1) {
			Image image = self.askPeers()
			if (image && (!newSupplier || image.confidence > self.trustUpstreams[newSupplier.getWho()])) {
				if (image.confidence > self.trustUpstreams[self.supplier.getWho()]) {
					self.trustedInformer = image.informer
					return image.supplier
				}
			}
		}

		if (newSupplier && self.trustUpstreams[newSupplier.getWho()] > self.trustUpstreams[self.supplier.getWho()]) {
			return newSupplier
		}
	}

	def calculateSaleMarkup(ChainLevel self) {
		self.saleMarkup = self.minMarkup
	}
}

class RiskyStrategy extends Strategy {
	def decideNextSupplier(ChainLevel self) {
		super.applyPreferredRule(self)
	}

	def getPreferredSupplierCandidates(ChainLevel self, subset) {
		return subset.groupBy{it.saleMarkup}.sort()
	}

	def acceptNewPreferredSupplier(ChainLevel self, ChainLevel newSupplier) {
		if (newSupplier.saleMarkup < self.supplier.saleMarkup) {
			return newSupplier
		}
	}

	def calculateSaleMarkup(ChainLevel self) {
		self.saleMarkup = self.maxMarkup
	}
}

class RandomStrategy extends Strategy {
	def decideNextSupplier(ChainLevel self) {
		self.supplier = this.chooseRandomSupplier(self, self.upstreamLevel)
	}

	def calculateSaleMarkup(ChainLevel self) {
		self.saleMarkup = self.minMarkup + (self.maxMarkup - self.minMarkup) * BigDecimal.valueOf(self.random.nextFloat())
	}
}