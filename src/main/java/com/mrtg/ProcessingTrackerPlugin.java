package com.mrtg;

import com.google.inject.Provides;
import java.util.function.Consumer;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Point;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.api.Client;

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

	@Override
	protected void startUp()
	{
		log.info("Processing Tracker started");
	}

	@Override
	protected void shutDown()
	{
		log.info("Processing Tracker stopped");
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

		client.getMenu().createMenuEntry(-1)
				.setOption("Add to processing tracker")
				.setTarget(last.getTarget())
				.setType(MenuAction.RUNELITE)
				.onClick(e ->
				{
//					Can refactor this and take out the function
					Widget widget = last.getWidget();
					if (widget != null)
					{
						log.info("Clicked above");
						logWidget(widget, "clicked");
					}

					handleCustomMenuClick(last);
				});
	}

	private void handleCustomMenuClick(MenuEntry entry)
	{
		log.info("Custom menu clicked");
		log.info("Option: {}", entry.getOption());
		log.info("Target: {}", stripTags(entry.getTarget()));

		Widget widget = entry.getWidget();
		if (widget == null)
		{
			log.info("Widget was null");
			return;
		}

		log.info("Clicked below");
		logWidget(widget, "clicked");

		Widget parent = widget.getParent();
		if (parent == null)
		{
			log.info("Parent: null");
			return;
		}

		logWidget(parent, "parent");

		Widget[] siblings = parent.getChildren();
		if (siblings == null || siblings.length == 0)
		{
			log.info("Parent children: null or empty");
			return;
		}

		int clickedIndex = findWidgetIndex(siblings, widget);

		log.info("Clicked sibling index: {}", clickedIndex);

		if (clickedIndex == -1)
		{
			log.info("Clicked widget not found in parent's children, dumping all siblings");
			for (int i = 0; i < siblings.length; i++)
			{
				Widget sibling = siblings[i];
				if (sibling == null)
				{
					continue;
				}

				logWidget(sibling, "allSibling[" + i + "]");
			}
			return;
		}

		for (int offset = 1; offset <= 5; offset++)
		{
			int siblingIndex = clickedIndex + offset;
			if (siblingIndex >= siblings.length)
			{
				break;
			}

			Widget sibling = siblings[siblingIndex];
			if (sibling == null)
			{
				continue;
			}

			logWidget(sibling, "sibling[" + siblingIndex + "]");
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

			log.info("Checked Widget:   {}", sibling.getCanvasLocation());

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

	private void logWidget(Widget widget, String label)
	{
		log.info(
				"{}: id={} type={} itemId={} qty={} text='{}' name='{}' canvasLoc='{}'",
				label,
				widget.getId(),
				widget.getType(),
				widget.getItemId(),
				widget.getItemQuantity(),
				safe(stripTags(widget.getText())),
				safe(stripTags(widget.getName())),
				widget.getCanvasLocation()
		);
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

	@Provides
	ProcessingTrackerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ProcessingTrackerConfig.class);
	}
}