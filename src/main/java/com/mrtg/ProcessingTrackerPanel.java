package com.mrtg;

import java.awt.BorderLayout;
//import java.awt.Box;
import java.awt.Component;
import java.awt.Dimension;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import javax.inject.Inject;
import javax.swing.*;

import net.runelite.client.ui.PluginPanel;

public class ProcessingTrackerPanel extends PluginPanel
{
    private final ProcessingTrackerPlugin plugin;

    private final JLabel activeItemLabel = new JLabel("No active item");
    private final JPanel boughtListPanel = new JPanel();
    private final JPanel soldListPanel = new JPanel();
    private final JLabel profitLabel = new JLabel("Profit: 0 gp");

    @Inject
    public ProcessingTrackerPanel(ProcessingTrackerPlugin plugin)
    {
        super(false);
        this.plugin = plugin;

        setLayout(new BorderLayout());

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton newItemButton = new JButton("+ New item");
        newItemButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        newItemButton.addActionListener(e ->
        {
            plugin.createNewProcessingItem();
            refresh();
        });

        activeItemLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        content.add(newItemButton);
        content.add(Box.createRigidArea(new Dimension(0, 8)));
        content.add(activeItemLabel);
        content.add(Box.createRigidArea(new Dimension(0, 12)));

        content.add(createSectionTitle("Bought"));
        setupListPanel(boughtListPanel);
        content.add(boughtListPanel);

        content.add(Box.createRigidArea(new Dimension(0, 12)));

        content.add(createSectionTitle("Sold"));
        setupListPanel(soldListPanel);
        content.add(soldListPanel);

        content.add(Box.createVerticalGlue());
        content.add(Box.createRigidArea(new Dimension(0, 12)));

        profitLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(profitLabel);

        add(content, BorderLayout.NORTH);
    }

    public void refresh()
    {
        ProcessingItem activeItem = plugin.getActiveItem();

        if (activeItem == null)
        {
            activeItemLabel.setText("No active item");
            rebuildTradeList(boughtListPanel, List.of());
            rebuildTradeList(soldListPanel, List.of());
            profitLabel.setText("Profit: 0 gp");
            revalidate();
            repaint();
            return;
        }

        activeItemLabel.setText("Active: " + activeItem.getName());

        rebuildTradeList(
                boughtListPanel,
                activeItem.getTradesByType(TradeType.BUY)
        );

        rebuildTradeList(
                soldListPanel,
                activeItem.getTradesByType(TradeType.SELL)
        );

        profitLabel.setText("Profit: " + formatNumber(activeItem.calculateProfit()) + " gp");

        revalidate();
        repaint();
    }

    private JLabel createSectionTitle(String text)
    {
        JLabel label = new JLabel(text);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
        return label;
    }

    private void setupListPanel(JPanel panel)
    {
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(panel.getForeground()),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)
        ));
    }

    private void rebuildTradeList(JPanel panel, List<TradeEntry> entries)
    {
        panel.removeAll();

        if (entries.isEmpty())
        {
            JLabel empty = new JLabel("None");
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(empty);
            return;
        }

        for (TradeEntry entry : entries)
        {
            JPanel row = new JPanel();
            row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

            JLabel line1 = new JLabel(entry.getItemName());
            JLabel line2 = new JLabel(
                    "Qty: " + formatNumber(entry.getQuantity())
                            + " @ " + formatNumber(entry.getPriceEach()) + " gp"
            );
            JLabel line3 = new JLabel(
                    "Total: " + formatNumber(entry.getTotalCoins()) + " gp"
            );

            line1.setAlignmentX(Component.LEFT_ALIGNMENT);
            line2.setAlignmentX(Component.LEFT_ALIGNMENT);
            line3.setAlignmentX(Component.LEFT_ALIGNMENT);

            row.add(line1);
            row.add(line2);
            row.add(line3);

            panel.add(row);
        }
    }

    private String formatNumber(long value)
    {
        return NumberFormat.getNumberInstance(Locale.US).format(value);
    }
}