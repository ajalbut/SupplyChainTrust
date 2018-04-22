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
		def recommendedSupplier = self.strategy.acceptRecommendedSupplier(self)
		if (recommendedSupplier) {
			self.supplier = recommendedSupplier
			return
		}

		def set = self.upstreamLevel.clone()
		Collections.shuffle(set)
		def subset = set.take(self.candidatesPerStep)
		def candidates = self.strategy.getPreferredSupplierCandidates(self, subset)
		ChainLevel candidate = candidates.take(1).values().flatten()[0]
		ChainLevel exploredSupplier = self.strategy.acceptExploredSupplier(self, candidate)
		if (exploredSupplier) {
			self.supplier = exploredSupplier
		}
	}

	def getPreferredSupplierCandidates(ChainLevel self) {}

	def acceptRecommendedSupplier(ChainLevel self) {}

	def acceptExploredSupplier(ChainLevel self, ChainLevel newSupplier) {}

	def chooseRandomSupplier(ChainLevel self, candidates) {
		return candidates[self.random.nextInt(candidates.size())]
	}

	def askPeers(ChainLevel self) {}

	def informImages(ChainLevel self, ChainLevel asker) {}
}

class SafeStrategy extends Strategy {
	def decideNextSupplier(ChainLevel self) {
		if (self.trustedInformer) {
			if (self.trustUpstreams.containsKey(self.supplier.getWho()) && self.trustUpstreams[self.supplier.getWho()] < self.confidenceThreshold) {
				self.trust[self.trustedInformer.getWho()] = false
			}
			self.trustedInformer = null
		}
		super.applyPreferredRule(self)
	}

	def getPreferredSupplierCandidates(ChainLevel self, subset) {
		subset = filter({self.trustUpstreams.containsKey(it.getWho())}, subset)
		return subset.groupBy{self.trustUpstreams[it.getWho()]}.sort{a,b -> b.key <=> a.key}
	}

	def acceptRecommendedSupplier(ChainLevel self) {
		def randomFraction = BigDecimal.valueOf(self.random.nextFloat())
		if (randomFraction >= self.EPSILON1) {
			Image image = this.askPeers(self)
			if (image && image.confidence > self.trustUpstreams[self.supplier.getWho()]) {
				self.trustedInformer = image.informer
				return image.supplier
			}
		}
	}

	def acceptExploredSupplier(ChainLevel self, ChainLevel newSupplier) {
		if (newSupplier && self.trustUpstreams[newSupplier.getWho()] > self.trustUpstreams[self.supplier.getWho()]) {
			return newSupplier
		}
	}

	def askPeers(ChainLevel self) {
		def reliablePeers = filter({self.trust[it.getWho()] == true}, self.currentLevel)
		ChainLevel peer = reliablePeers[self.random.nextInt(reliablePeers.size())]
		def images = this.informImages(peer, self)
		def bestImages = images.values().groupBy{it.confidence}.sort{-it.key}.values()[0]
		if (bestImages) {
			return bestImages[self.random.nextInt(bestImages.size())]
		}
	}

	def informImages(ChainLevel self, ChainLevel asker) {
		if (self.trust[asker.getWho()] == true) {
			return self.images.findAll{it.value.confidence && it.value.confidence > self.confidenceThreshold}
		} else {
			return [:]
		}
	}

	def calculateSaleMarkup(ChainLevel self) {
		self.saleMarkup = self.minMarkup + (BigDecimal.valueOf(self.random.nextFloat()) * (1.0 - self.priceOffset)) * (self.maxMarkup - self.minMarkup)
	}
}

class RiskyStrategy extends Strategy {
	def decideNextSupplier(ChainLevel self) {
		if (self.trustedInformer) {
			if (self.supplier.saleMarkup > self.supplier.minMarkup + self.priceThreshold * (self.supplier.maxMarkup - self.supplier.minMarkup)) {
				self.trust[self.trustedInformer.getWho()] = false
			}
			self.trustedInformer = null
		}
		super.applyPreferredRule(self)
	}

	def getPreferredSupplierCandidates(ChainLevel self, subset) {
		return subset.groupBy{it.saleMarkup}.sort()
	}

	def acceptRecommendedSupplier(ChainLevel self) {
		def randomFraction = BigDecimal.valueOf(self.random.nextFloat())
		if (randomFraction >= self.EPSILON1) {
			Image image = this.askPeers(self)
			if (image && image.saleMarkup < self.supplier.saleMarkup) {
				self.trustedInformer = image.informer
				return image.supplier
			}
		}
	}

	def acceptExploredSupplier(ChainLevel self, ChainLevel newSupplier) {
		if (newSupplier && newSupplier.saleMarkup < self.supplier.saleMarkup) {
			return newSupplier
		}
	}

	def askPeers(ChainLevel self) {
		def reliablePeers = filter({self.trust[it.getWho()] == true}, self.currentLevel)
		ChainLevel peer = reliablePeers[self.random.nextInt(reliablePeers.size())]
		def images = this.informImages(peer, self)
		def bestImages = images.values().groupBy{it.saleMarkup}.sort{it.key}.values()[0]
		if (bestImages) {
			return bestImages[self.random.nextInt(bestImages.size())]
		}
	}

	def informImages(ChainLevel self, ChainLevel asker) {
		if (self.trust[asker.getWho()] == true) {
			return self.images.findAll{it.value.saleMarkup && it.value.saleMarkup < self.supplier.minMarkup + self.priceThreshold * (self.supplier.maxMarkup - self.supplier.minMarkup)}
		} else {
			return [:]
		}
	}

	def calculateSaleMarkup(ChainLevel self) {
		self.saleMarkup = self.minMarkup + (self.priceOffset + BigDecimal.valueOf(self.random.nextFloat()) * (1.0 - self.priceOffset)) * (self.maxMarkup - self.minMarkup)
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