package com.mrtg;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ProcessingItem
{
    private final String name;
    private final List<TradeEntry> trades = new ArrayList<>();

    public ProcessingItem(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public List<TradeEntry> getTrades()
    {
        return trades;
    }

    public void addTrade(TradeEntry trade)
    {
        trades.add(trade);
    }

    public List<TradeEntry> getTradesByType(TradeType type)
    {
        return trades.stream()
                .filter(t -> t.getType() == type)
                .collect(Collectors.toList());
    }

    public long calculateProfit()
    {
        long bought = trades.stream()
                .filter(t -> t.getType() == TradeType.BUY)
                .mapToLong(TradeEntry::getTotalCoins)
                .sum();

        long sold = trades.stream()
                .filter(t -> t.getType() == TradeType.SELL)
                .mapToLong(TradeEntry::getTotalCoins)
                .sum();

        return sold - bought;
    }
}