package com.mrtg;

import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.Border;

import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

public class ProcessingTrackerPanel extends PluginPanel
{
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getNumberInstance(Locale.US);

    private final ProcessingTrackerPlugin plugin;
    private final ItemManager itemManager;

    private final JPanel processedItemsContainer = new JPanel();

    @Inject
    public ProcessingTrackerPanel(ProcessingTrackerPlugin plugin, ItemManager itemManager)
    {
        super(false);
        this.plugin = plugin;
        this.itemManager = itemManager;

        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(ColorScheme.DARK_GRAY_COLOR);
        root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JButton newProcessedButton = new JButton("+ New Processed Trade");
        newProcessedButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        newProcessedButton.addActionListener(e ->
        {
            plugin.createNewProcessingItem();
            refresh();
        });

        JPanel topRow = new JPanel();
        topRow.setLayout(new BoxLayout(topRow, BoxLayout.X_AXIS));
        topRow.setOpaque(false);
        topRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        topRow.add(newProcessedButton);
        topRow.add(Box.createRigidArea(new Dimension(4, 0)));
        topRow.add(createHelpButton());

        root.add(topRow);
        root.add(Box.createRigidArea(new Dimension(0, 8)));

        processedItemsContainer.setLayout(new BoxLayout(processedItemsContainer, BoxLayout.Y_AXIS));
        processedItemsContainer.setBackground(ColorScheme.DARK_GRAY_COLOR);
        processedItemsContainer.setAlignmentX(Component.LEFT_ALIGNMENT);

        root.add(processedItemsContainer);

        JScrollPane scrollPane = new JScrollPane(root);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(ColorScheme.DARK_GRAY_COLOR);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        add(scrollPane, BorderLayout.CENTER);

        refresh();
    }

    public void refresh()
    {
        processedItemsContainer.removeAll();

        List<ProcessingItem> items = plugin.getProcessingItems();

        if (items.isEmpty())
        {
            JLabel empty = new JLabel("No processed items yet");
            empty.setForeground(Color.LIGHT_GRAY);
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            processedItemsContainer.add(empty);
        }
        else
        {
            for (ProcessingItem item : items)
            {
                processedItemsContainer.add(createProcessedCard(item));
                processedItemsContainer.add(Box.createRigidArea(new Dimension(0, 8)));
            }
        }

        revalidate();
        repaint();
    }

    private JPanel createProcessedCard(ProcessingItem item)
    {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setOpaque(false);

        JPanel headerSection = createInnerSection();
        headerSection.setLayout(new BorderLayout());

        JTextField nameField = new JTextField(item.getName());
        nameField.setForeground(Color.WHITE);
        nameField.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        nameField.setCaretColor(Color.WHITE);
        nameField.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        nameField.setOpaque(true);
        nameField.setToolTipText("Rename processed item");

        nameField.addActionListener(e ->
                plugin.renameProcessingItem(item, nameField.getText())
        );

        nameField.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusLost(FocusEvent e)
            {
                plugin.renameProcessingItem(item, nameField.getText());
            }
        });

        nameField.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                plugin.setActiveItem(item);
            }
        });

        JButton collapseButton = new JButton(item.isCollapsed() ? "+" : "−");
        collapseButton.setFocusable(false);
        collapseButton.setMargin(new Insets(0, 4, 0, 4));
        collapseButton.setPreferredSize(new Dimension(24, 20));
        collapseButton.setMinimumSize(new Dimension(24, 20));
        collapseButton.setMaximumSize(new Dimension(24, 20));
        collapseButton.setBorderPainted(false);
        collapseButton.setContentAreaFilled(false);
        collapseButton.setForeground(Color.LIGHT_GRAY);
        collapseButton.setToolTipText(item.isCollapsed() ? "Expand" : "Minimize");
        collapseButton.addActionListener(e -> plugin.toggleProcessingItemCollapsed(item));

        boolean isActive = item == plugin.getActiveItem();

        headerSection.add(nameField, BorderLayout.CENTER);
        headerSection.add(collapseButton, BorderLayout.EAST);

        headerSection.setMaximumSize(new Dimension(Integer.MAX_VALUE, headerSection.getPreferredSize().height));

        headerSection.setBorder(
                createHeaderBorder(isActive)
        );

        headerSection.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                plugin.setActiveItem(item);
            }
        });

        card.add(headerSection);

        if (!item.isCollapsed())
        {
            JPanel boughtSection = createInnerSection();
            boughtSection.add(createSectionTitle("Bought"));
            boughtSection.add(Box.createRigidArea(new Dimension(0, 6)));
            boughtSection.add(createTradeListPanel(item.getTradesByType(TradeType.BUY)));

            JPanel soldSection = createInnerSection();
            soldSection.add(createSectionTitle("Sold"));
            soldSection.add(Box.createRigidArea(new Dimension(0, 6)));
            soldSection.add(createTradeListPanel(item.getTradesByType(TradeType.SELL)));

            JPanel profitSection = createProfitSection(item);

            boughtSection.setMaximumSize(new Dimension(Integer.MAX_VALUE, boughtSection.getPreferredSize().height));
            soldSection.setMaximumSize(new Dimension(Integer.MAX_VALUE, soldSection.getPreferredSize().height));
            profitSection.setMaximumSize(new Dimension(Integer.MAX_VALUE, profitSection.getPreferredSize().height));

            card.add(Box.createRigidArea(new Dimension(0, 2)));
            card.add(boughtSection);
            card.add(Box.createRigidArea(new Dimension(0, 2)));
            card.add(soldSection);
            card.add(Box.createRigidArea(new Dimension(0, 2)));
            card.add(profitSection);
        }

        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, card.getPreferredSize().height));

        return card;
    }

    private JPanel createProfitSection(ProcessingItem item)
    {
        JPanel section = new JPanel(new BorderLayout());
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        section.setOpaque(true);
        section.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        section.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        long profit = item.calculateProfit();

        JLabel profitLabel = new JLabel("Profit: " + formatNumber(profit) + " gp");
        profitLabel.setForeground(profit >= 0 ? new Color(0, 200, 83) : new Color(255, 82, 82));

        JButton deleteButton = new JButton("x");
        deleteButton.setFocusable(false);
        deleteButton.setMargin(new Insets(2, 8, 2, 8));
        deleteButton.setToolTipText("Delete " + item.getName());
        deleteButton.addActionListener(e -> plugin.deleteProcessingItem(item));

        section.add(profitLabel, BorderLayout.WEST);
        section.add(deleteButton, BorderLayout.EAST);

        return section;
    }

    private JPanel createInnerSection()
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setOpaque(true);
        panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        return panel;
    }

    private JLabel createSectionTitle(String text)
    {
        JLabel label = new JLabel(text);
        label.setForeground(Color.WHITE);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JPanel createTradeListPanel(List<TradeEntry> entries)
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        if (entries.isEmpty())
        {
            JLabel empty = new JLabel("None");
            empty.setForeground(Color.LIGHT_GRAY);
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(empty);
            return panel;
        }

        for (TradeEntry entry : entries)
        {
            panel.add(createTradeRow(entry));
            panel.add(Box.createRigidArea(new Dimension(0, 6)));
        }

        return panel;
    }

    private JPanel createTradeRow(TradeEntry entry)
    {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(true);
        row.setBackground(ColorScheme.DARK_GRAY_COLOR);
        row.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;

        JLabel iconLabel = new JLabel();
        iconLabel.setPreferredSize(new Dimension(40, 40));
        iconLabel.setMinimumSize(new Dimension(40, 40));
        iconLabel.setMaximumSize(new Dimension(40, 40));

        itemManager.getImage(
                entry.getItemId(),
                entry.getQuantity(),
                entry.getQuantity() > 1
        ).addTo(iconLabel);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        gbc.weightx = 0;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(0, 0, 0, 6);

        row.add(iconLabel, gbc);

        JButton deleteButton = new JButton("×");
        deleteButton.setFocusable(false);
        deleteButton.setToolTipText("Remove trade");
        deleteButton.setMargin(new Insets(2, 6, 2, 6));
        deleteButton.setPreferredSize(new Dimension(24, 16));
        deleteButton.setMinimumSize(new Dimension(24, 16));
        deleteButton.setMaximumSize(new Dimension(24, 16));
        deleteButton.addActionListener(e -> plugin.removeTradeEntry(entry));

        deleteButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                deleteButton.setBackground(Color.RED);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                deleteButton.setBackground(Color.DARK_GRAY);
            }
        });

        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.gridheight = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(0, 6, 0, 0);
        row.add(deleteButton, gbc);

        JLabel nameLabel = new JLabel(ellipsize(entry.getItemName(), 20));
        nameLabel.setForeground(Color.WHITE);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 0, 0);
        row.add(nameLabel, gbc);

        String details =
                "x " + formatNumber(entry.getQuantity()) +
                        " @ " + formatNumber(entry.getPriceEach()) + " gp";

        String total =
                "Total: " + formatNumber(entry.getTotalCoins()) + " gp";

        if (entry.getType() == TradeType.SELL && entry.getGeTax() > 0)
        {
            total += " (Tax: " + formatNumber(entry.getGeTax()) + ")";
        }

        JPanel detailsPanel = new JPanel();
        detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));
        detailsPanel.setOpaque(false);
        detailsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel detailsLabel = new JLabel(htmlWrap(details, 100));
        detailsLabel.setForeground(Color.LIGHT_GRAY);

        JLabel totalLabel = new JLabel(htmlWrap(total, 110));
        totalLabel.setForeground(Color.LIGHT_GRAY);

        detailsPanel.add(detailsLabel);
        detailsPanel.add(Box.createRigidArea(new Dimension(0, 2)));
        detailsPanel.add(totalLabel);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2, 0, 0, 0);
        row.add(detailsPanel, gbc);

        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        return row;
    }

    private Border createHeaderBorder(boolean isActive)
    {
        Border line;

        if (isActive)
        {
            line = BorderFactory.createMatteBorder(
                    0, 0, 3, 0,
                    ColorScheme.PROGRESS_COMPLETE_COLOR
            );
        }
        else
        {
            line = BorderFactory.createMatteBorder(
                    0, 0, 1, 0,
                    ColorScheme.DARK_GRAY_COLOR
            );
        }

        Border padding = BorderFactory.createEmptyBorder(
                6, 8, 6, 8
        );

        return BorderFactory.createCompoundBorder(
                line,
                padding
        );
    }

    private JButton createHelpButton()
    {
        JButton helpButton = new JButton("?");

        helpButton.setFocusable(false);

        helpButton.setMargin(new Insets(0, 6, 0, 6));

        helpButton.setPreferredSize(new Dimension(24, 24));
        helpButton.setMinimumSize(new Dimension(24, 24));
        helpButton.setMaximumSize(new Dimension(24, 24));

        helpButton.setBorderPainted(false);
        helpButton.setContentAreaFilled(false);

        helpButton.setForeground(Color.LIGHT_GRAY);

        helpButton.setToolTipText(buildHelpTooltip());

        return helpButton;
    }

    private String buildHelpTooltip()
    {
        return "<html>"
                + "<b>Processing Tracker</b><br><br>"
                + "1. Click <b>New</b> to create a processing item.<br>"
                + "2. Right-click a GE history entry.<br>"
                + "3. Choose <b>Add to Buy</b> or <b>Add to Sell</b>.<br>"
                + "4. Profit is calculated automatically.<br><br>"
                + "<i>Tip:</i> Click a header to set it active."
                + "</html>";
    }

    private String formatNumber(long value)
    {
        return NUMBER_FORMAT.format(value);
    }

    private String ellipsize(String text, int maxLength)
    {
        if (text == null)
        {
            return "";
        }

        if (text.length() <= maxLength)
        {
            return text;
        }

        return text.substring(0, Math.max(0, maxLength - 1)) + "…";
    }

    private String htmlWrap(String text, int width)
    {
        if (text == null)
        {
            return "<html></html>";
        }

        return "<html><body style='width:" + width + "px'>" + escapeHtml(text) + "</body></html>";
    }

    private String escapeHtml(String text)
    {
        if (text == null)
        {
            return "";
        }

        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}