package com.mrtg;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class ProcessingItem
{
    @Getter
    @Setter
    private String name;
    private final List<TradeEntry> trades = new ArrayList<>();

    public ProcessingItem(String name)
    {
        this.name = name;
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

    @Setter
    private boolean collapsed = false;
}