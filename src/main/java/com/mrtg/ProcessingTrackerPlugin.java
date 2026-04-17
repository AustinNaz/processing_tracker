package com.mrtg;

import com.google.gson.reflect.TypeToken;
import com.google.inject.Provides;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import javax.inject.Inject;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
		name = "Processing Tracker"
)
public class ProcessingTrackerPlugin extends Plugin
{
	@Inject
	private ProcessingTrackerConfig config;

	@Inject
	private Client client;

	@Inject
	private net.runelite.client.ui.ClientToolbar clientToolbar;

	@Inject
	private com.google.inject.Injector injector;

	@Inject
	private ConfigManager configManager;

	@Inject
	private com.google.gson.Gson gson;

	private net.runelite.client.ui.NavigationButton navButton;
	private ProcessingTrackerPanel panel;
	private static final String CONFIG_GROUP = "processingtracker";
	private static final String STORED_ITEMS_KEY = "processedItems";

	@Getter
    private final java.util.List<ProcessingItem> processingItems = new java.util.ArrayList<>();
	@Getter
    private ProcessingItem activeItem;

	@Override
	protected void startUp() throws IOException {
		log.info("Processing Tracker started");
		panel = injector.getInstance(ProcessingTrackerPanel.class);

		java.awt.image.BufferedImage icon = javax.imageio.ImageIO.read(
				getClass().getResourceAsStream("/processing_icon.png")
		);

		navButton = net.runelite.client.ui.NavigationButton.builder()
				.tooltip("Processing Tracker")
				.icon(icon)
				.priority(5)
				.panel(panel)
				.build();

		loadProcessingItems();

		if (processingItems.isEmpty())
		{
			createNewProcessingItem();
		}
		else
		{
			panel.refresh();
		}


		clientToolbar.addNavigation(navButton);
	}

	@Override
	protected void shutDown()
	{
		log.info("Processing Tracker stopped");
		if (navButton != null)
		{
			clientToolbar.removeNavigation(navButton);
		}
	}

	@Subscribe
	public void onMenuOpened(MenuOpened event)
	{
		MenuEntry[] entries = event.getMenuEntries();
		if (entries.length == 0)
		{
			return;
		}

		MenuEntry last = entries[entries.length - 1];
		String option = last.getOption();
		String target = stripTags(last.getTarget());

		if (!"Buy-offer".equals(option) && !"Sell-offer".equals(option))
		{
			return;
		}

		MenuEntry parent = client.getMenu().createMenuEntry(-1)
				.setOption("Processing Tracker")
				.setTarget(last.getTarget())
				.setType(MenuAction.RUNELITE);

		Menu subMenu = parent.createSubMenu();

		subMenu.createMenuEntry(-1)
				.setOption("Add to buy")
				.setTarget(last.getTarget())
				.setType(MenuAction.RUNELITE)
				.onClick(e -> handleAddToTracker(last, TradeType.BUY));

		subMenu.createMenuEntry(-1)
				.setOption("Add to sell")
				.setTarget(last.getTarget())
				.setType(MenuAction.RUNELITE)
				.onClick(e -> handleAddToTracker(last, TradeType.SELL));
	}

	private void handleAddToTracker(MenuEntry sourceEntry, TradeType type)
	{
		if (activeItem == null)
		{
			log.info("No active processing item");
			return;
		}

		Widget widget = sourceEntry.getWidget();
		if (widget == null)
		{
			log.info("No widget on source entry");
			return;
		}

		ParsedTradeRow row = parseTradeRow(widget);

		if (row == null)
		{
			log.info("Could not parse trade row");
			return;
		}

		TradeEntry entry = new TradeEntry(
				row.getType(),
				row.getItemName(),
				row.getItemId(),
				row.getQuantity(),
				row.getPriceEach(),
				row.getTotalCoins(),
				row.getGeTax()
		);

		if (activeItem == null)
		{
			createNewProcessingItem();
		}

		activeItem.addTrade(entry);
		saveProcessingItems();

		if (panel != null)
		{
			panel.refresh();
		}
	}

	private ParsedTradeRow parseTradeRow(Widget clickedWidget)
	{
		if (clickedWidget == null)
		{
			return null;
		}

		Widget parent = clickedWidget.getParent();
		if (parent == null)
		{
			log.info("parseTradeRow: parent was null");
			return null;
		}

		Widget[] siblings = parent.getChildren();
		if (siblings == null || siblings.length == 0)
		{
			log.info("parseTradeRow: siblings were null or empty");
			return null;
		}

		int clickedIndex = findWidgetIndex(siblings, clickedWidget);
		if (clickedIndex == -1)
		{
			log.info("parseTradeRow: clicked index not found");
			return null;
		}

		Widget labelWidget = getSibling(siblings, clickedIndex + 2);   // Bought: / Sold:
		Widget nameWidget = getSibling(siblings, clickedIndex + 3);    // Raw marlinx 3,000
		Widget itemWidget = getSibling(siblings, clickedIndex + 4);    // itemId + qty
		Widget priceWidget = getSibling(siblings, clickedIndex + 5);   // coins= ... each

		if (labelWidget == null || nameWidget == null || itemWidget == null || priceWidget == null)
		{
			log.info("parseTradeRow: missing one or more expected widgets");
			return null;
		}

		String labelText = cleanText(labelWidget.getText());
		String nameText = cleanText(nameWidget.getText());
		String priceText = cleanText(priceWidget.getText());

		TradeType type;
		if (labelText.startsWith("Bought"))
		{
			type = TradeType.BUY;
		}
		else if (labelText.startsWith("Sold"))
		{
			type = TradeType.SELL;
		}
		else
		{
			log.info("parseTradeRow: unknown trade label '{}'", labelText);
			return null;
		}

		int itemId = itemWidget.getItemId();
		int quantity = itemWidget.getItemQuantity();

		String itemName = parseItemName(nameText, quantity);

		long totalCoins = parseLeadingCoins(priceText);
		int priceEach = (int) parseTrailingEach(priceText);

		long grossCoins = totalCoins;
		long geTax = 0;

		if (type == TradeType.SELL)
		{
			long[] sellBreakdown = parseSellBreakdown(priceText);
			if (sellBreakdown != null)
			{
				grossCoins = sellBreakdown[0];
				geTax = sellBreakdown[1];
				totalCoins = sellBreakdown[2];
			}
		}

		return new ParsedTradeRow(
				type,
				itemName,
				itemId,
				quantity,
				totalCoins,
				priceEach,
				grossCoins,
				geTax
		);
	}

	private Widget getSibling(Widget[] siblings, int index)
	{
		if (siblings == null || index < 0 || index >= siblings.length)
		{
			return null;
		}
		return siblings[index];
	}

	private String cleanText(String text)
	{
		if (text == null)
		{
			return "";
		}

		return stripTags(text).replace('\u00A0', ' ').trim();
	}

	private String parseItemName(String combinedText, int quantity)
	{
		if (combinedText == null || combinedText.isEmpty())
		{
			return "";
		}

		String qtyWithCommas = String.format("%,d", quantity);
		String suffix = "x " + qtyWithCommas;

		if (combinedText.endsWith(suffix))
		{
			return combinedText.substring(0, combinedText.length() - suffix.length()).trim();
		}

		// fallback if spacing differs slightly
		int xIndex = combinedText.lastIndexOf("x ");
		if (xIndex > 0)
		{
			return combinedText.substring(0, xIndex).trim();
		}

		return combinedText.trim();
	}

	private long parseLeadingCoins(String priceText)
	{
		// examples:
		// "19,830,816 coins= 6,610 each"
		// "17,937,000 coins(18,297,000 - 360,000)= 1,993 each"
		int coinsIndex = priceText.indexOf(" coins");
		if (coinsIndex == -1)
		{
			return 0;
		}

		String numberPart = priceText.substring(0, coinsIndex).trim();
		return parseNumber(numberPart);
	}

	private long parseTrailingEach(String priceText)
	{
		// gets the number before " each"
		int eachIndex = priceText.lastIndexOf(" each");
		if (eachIndex == -1)
		{
			return 0;
		}

		int equalsIndex = priceText.lastIndexOf("= ", eachIndex);
		if (equalsIndex == -1)
		{
			return 0;
		}

		String eachPart = priceText.substring(equalsIndex + 2, eachIndex).trim();
		return parseNumber(eachPart);
	}

	private long[] parseSellBreakdown(String priceText)
	{
		// example:
		// "17,937,000 coins(18,297,000 - 360,000)= 1,993 each"
		int openParen = priceText.indexOf('(');
		int closeParen = priceText.indexOf(')');

		if (openParen == -1 || closeParen == -1 || closeParen <= openParen)
		{
			return null;
		}

		String inner = priceText.substring(openParen + 1, closeParen).trim();
		String[] parts = inner.split(" - ");
		if (parts.length != 2)
		{
			return null;
		}

		long gross = parseNumber(parts[0]);
		long tax = parseNumber(parts[1]);
		long net = parseLeadingCoins(priceText);

		return new long[] { gross, tax, net };
	}

	private long parseNumber(String text)
	{
		if (text == null || text.isEmpty())
		{
			return 0;
		}

		String digitsOnly = text.replaceAll("[^0-9]", "");
		if (digitsOnly.isEmpty())
		{
			return 0;
		}

		try
		{
			return Long.parseLong(digitsOnly);
		}
		catch (NumberFormatException e)
		{
			log.warn("Failed to parse number from '{}'", text, e);
			return 0;
		}
	}

		private int findWidgetIndex(Widget[] siblings, Widget clicked)
	{
		for (int i = 0; i < siblings.length; i++)
		{
			Widget sibling = siblings[i];

			if (sibling == null)
			{
				continue;
			}

//			The only reliable way I'm finding for getting the index is based on the canvasLocation
			Point a = siblings[i].getCanvasLocation();
			Point b = clicked.getCanvasLocation();

			if (a != null && b != null && a.getX() == b.getX() && a.getY() == b.getY())
			{
				return i;
			}
		}

		return -1;
	}

	public void deleteProcessingItem(ProcessingItem item)
	{
		processingItems.remove(item);

		if (activeItem == item)
		{
			activeItem = processingItems.isEmpty() ? null : processingItems.get(0);
		}
		saveProcessingItems();

		if (panel != null)
		{
			panel.refresh();
		}
	}

	public void removeTradeEntry(TradeEntry entry)
	{
		for (ProcessingItem item : processingItems)
		{
			if (item.getTrades().remove(entry))
			{
				break;
			}
		}
		saveProcessingItems();

		if (panel != null)
		{
			panel.refresh();
		}
	}

	public void toggleProcessingItemCollapsed(ProcessingItem item)
	{
		item.setCollapsed(!item.isCollapsed());
		saveProcessingItems();

		if (panel != null)
		{
			panel.refresh();
		}
	}

	public void setActiveItem(ProcessingItem item)
	{
		if (item == null || item == activeItem)
		{
			return;
		}

		activeItem = item;
		saveProcessingItems();

		if (panel != null)
		{
			panel.refresh();
		}
	}

	private void saveProcessingItems()
	{
		String json = gson.toJson(processingItems);
		configManager.setConfiguration(CONFIG_GROUP, STORED_ITEMS_KEY, json);
	}

	private void loadProcessingItems()
	{
		String json = configManager.getConfiguration(CONFIG_GROUP, STORED_ITEMS_KEY);

		if (json == null || json.isEmpty())
		{
			return;
		}

		try
		{
			Type listType = new TypeToken<List<ProcessingItem>>() {}.getType();
			List<ProcessingItem> loadedItems = gson.fromJson(json, listType);

			processingItems.clear();

			if (loadedItems != null)
			{
				processingItems.addAll(loadedItems);
			}

			activeItem = processingItems.isEmpty() ? null : processingItems.get(0);
		}
		catch (Exception e)
		{
			log.warn("Failed to load processing items", e);
		}
	}

	public void renameProcessingItem(ProcessingItem item, String newName)
	{
		if (item == null)
		{
			return;
		}

		String trimmed = newName == null ? "" : newName.trim();
		if (trimmed.isEmpty())
		{
			return;
		}

		item.setName(trimmed);
		saveProcessingItems();

		if (panel != null)
		{
			panel.refresh();
		}
	}

	private String stripTags(String input)
	{
		if (input == null)
		{
			return "";
		}
		return input.replaceAll("<[^>]*>", "");
	}

	private String safe(String value)
	{
		return value == null ? "" : value;
	}

	public void createNewProcessingItem()
	{
		String name = "Processing " + (processingItems.size() + 1);
		ProcessingItem item = new ProcessingItem(name);
		processingItems.add(0, item);
		activeItem = item;

		saveProcessingItems();

		if (panel != null)
		{
			panel.refresh();
		}
	}


    @Provides
	ProcessingTrackerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ProcessingTrackerConfig.class);
	}
}