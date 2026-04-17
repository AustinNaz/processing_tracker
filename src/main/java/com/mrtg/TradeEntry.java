package com.mrtg;

public class TradeEntry
{
    private final TradeType type;
    private final String itemName;
    private final int itemId;
    private final int quantity;
    private final int priceEach;
    private final long totalCoins;
    private final long geTax;

    public TradeEntry(
            TradeType type,
            String itemName,
            int itemId,
            int quantity,
            int priceEach,
            long totalCoins,
            long geTax
    )
    {
        this.type = type;
        this.itemName = itemName;
        this.itemId = itemId;
        this.quantity = quantity;
        this.priceEach = priceEach;
        this.totalCoins = totalCoins;
        this.geTax = geTax;
    }

    public TradeType getType()
    {
        return type;
    }

    public String getItemName()
    {
        return itemName;
    }

    public int getItemId()
    {
        return itemId;
    }

    public int getQuantity()
    {
        return quantity;
    }

    public int getPriceEach()
    {
        return priceEach;
    }

    public long getTotalCoins()
    {
        return totalCoins;
    }

    public long getGeTax()
    {
        return geTax;
    }
}