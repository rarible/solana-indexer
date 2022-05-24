package com.rarible.protocol.solana.nft.listener.service.auction.house

class AuctionHouseNotReadyException(override val message: String) : Exception(message)