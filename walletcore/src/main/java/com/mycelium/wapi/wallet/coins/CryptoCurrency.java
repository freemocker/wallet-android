package com.mycelium.wapi.wallet.coins;

import com.google.common.base.Charsets;
import com.mycelium.wapi.wallet.GenericAddress;

import java.math.BigInteger;

import static com.google.common.base.Preconditions.checkNotNull;

public class CryptoCurrency extends AbstractAsset {
    private static final long serialVersionUID = 1L;

    protected String id;
    protected Integer unitExponent;
    protected Integer friendlyDigits;
    protected boolean isUtxosBased = true;

    public CryptoCurrency(String id, String name, String symbol, Integer unitExponent, Integer friendlyDigits, boolean isUtxosBased) {
        this.id = id;
        this.name = name;
        this.symbol = symbol;
        this.unitExponent = unitExponent;
        this.friendlyDigits = friendlyDigits;
        this.isUtxosBased = isUtxosBased;
    }

    @Override
    public String getName() {
        return checkNotNull(name, "A coin failed to set a name");
    }

    @Override
    public String getSymbol() {
        return checkNotNull(symbol, "A coin failed to set a symbol");
    }

    @Override
    public int getUnitExponent() {
        return checkNotNull(unitExponent, "A coin failed to set a unit exponent");
    }

    @Override
    public int getFriendlyDigits() {
        return friendlyDigits;
    }

    protected static byte[] toBytes(String str) {
        return str.getBytes(Charsets.UTF_8);
    }

    @Override
    public Value oneCoin() {
        if (oneCoin == null) {
            BigInteger units = BigInteger.TEN.pow(getUnitExponent());
            oneCoin = Value.valueOf(this, units.longValue());
        }
        return oneCoin;
    }

    @Override
    public Value value(String string) {
        return Value.parse(this, string);
    }

    @Override
    public GenericAddress parseAddress(String address) {
        return null;
    }

    @Override
    public Value value(long units) {
        return Value.valueOf(this, units);
    }

    @Override
    public String toString() {
        return "Coin{" +
                "name='" + name + '\'' +
                ", symbol='" + symbol + '\'' +
                '}';
    }

    public String getId() {
        return id;
    }

    public boolean isUtxosBased() {
        return isUtxosBased;
    }
}
