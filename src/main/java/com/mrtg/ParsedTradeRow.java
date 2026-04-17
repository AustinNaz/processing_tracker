package com.mrtg;

public class ParsedTradeRow
{
    private final TradeType type;
    private final String itemName;
    private final int itemId;
    private final int quantity;
    private final long totalCoins;
    private final int priceEach;
    private final long grossCoins;
    private final long geTax;

    public ParsedTradeRow(
            TradeType type,
            String itemName,
            int itemId,
            int quantity,
            long totalCoins,
            int priceEach,
            long grossCoins,
            long geTax
    )
    {
        this.type = type;
        this.itemName = itemName;
        this.itemId = itemId;
        this.quantity = quantity;
        this.totalCoins = totalCoins;
        this.priceEach = priceEach;
        this.grossCoins = grossCoins;
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

    public long getTotalCoins()
    {
        return totalCoins;
    }

    public int getPriceEach()
    {
        return priceEach;
    }

    public long getGrossCoins()
    {
        return grossCoins;
    }

    public long getGeTax()
    {
        return geTax;
    }

    @Override
    public String toString()
    {
        return "ParsedTradeRow{" +
                "type=" + type +
                ", itemName='" + itemName + '\'' +
                ", itemId=" + itemId +
                ", quantity=" + quantity +
                ", totalCoins=" + totalCoins +
                ", priceEach=" + priceEach +
                ", grossCoins=" + grossCoins +
                ", geTax=" + geTax +
                '}';
    }
}