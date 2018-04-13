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

	def askPeers(ChainLevel self) {}

	def informImages(ChainLevel self, ChainLevel asker) {}
}

class SafeStrategy extends Strategy {
	def decideNextSupplier(ChainLevel self) {
		if (self.trustedInformer) {
			if (self.trustUpstreams[self.supplier.getWho()] < self.confidenceThreshold) {
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

	def acceptNewPreferredSupplier(ChainLevel self, ChainLevel newSupplier) {
		def randomFraction = BigDecimal.valueOf(self.random.nextFloat())
		if (randomFraction >= self.EPSILON1) {
			Image image = this.askPeers(self)
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

	def askPeers(ChainLevel self) {
		def images = [:]
		def reliablePeers = filter({self.trust[it.getWho()] == true}, self.currentLevel)
		for (ChainLevel peer in reliablePeers) {
			images += this.informImages(peer, self)
		}
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
			if (self.supplier.saleMarkup > self.priceThreshold) {
				self.trust[self.trustedInformer.getWho()] = false
			}
			self.trustedInformer = null
		}
		super.applyPreferredRule(self)
	}

	def getPreferredSupplierCandidates(ChainLevel self, subset) {
		return subset.groupBy{it.saleMarkup}.sort()
	}

	def acceptNewPreferredSupplier(ChainLevel self, ChainLevel newSupplier) {
		def randomFraction = BigDecimal.valueOf(self.random.nextFloat())
		if (randomFraction >= self.EPSILON1) {
			Image image = this.askPeers(self)
			if (image && (!newSupplier || image.saleMarkup < newSupplier.saleMarkup)) {
				if (image.saleMarkup < self.supplier.saleMarkup) {
					self.trustedInformer = image.informer
					return image.supplier
				}
			}
		}

		if (newSupplier && newSupplier.saleMarkup < self.supplier.saleMarkup) {
			return newSupplier
		}
	}

	def askPeers(ChainLevel self) {
		def images = [:]
		def reliablePeers = filter({self.trust[it.getWho()] == true}, self.currentLevel)
		for (ChainLevel peer in reliablePeers) {
			images += this.informImages(peer, self)
		}
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