package com.mycelium.wapi.wallet.bch.coins

import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.SoftDustPolicy

class BchMain private constructor() : BchCoin() {
    //TODO: create init
    init {
        id = "bitcoin_cash.test"
        name = "Bitcoin Cash Test"
        symbol = "BCH"
        uriScheme = "bitcoin"
        addressHeader = 0
        p2shHeader = 5
        acceptableAddressCodes = intArrayOf(addressHeader, p2shHeader)
        spendableCoinbaseDepth = 100
        dumpedPrivateKeyHeader = 128
        bip44Index = 0
        unitExponent = 8
        feeValue = value(12000)
        minNonDust = value(5460)
        softDustLimit = value(1000000) // 0.01 BTC
        softDustPolicy = SoftDustPolicy.AT_LEAST_BASE_FEE_IF_SOFT_DUST_TXO_PRESENT
        signedMessageHeader = toBytes("Bitcoin Cash Signed Message:\n")
    }

    companion object {
        private val instance = BchMain()
        @Synchronized
        fun get(): CryptoCurrency {
            return instance
        }
    }
}