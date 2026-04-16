package com.mrtg;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("Processing Tracker")
public interface ProcessingTrackerConfig extends Config
{
	@ConfigItem(
		keyName = "greeting",
		name = "Track Processes NSEWR",
		description = "The message to show to the user when they login"
	)
	default String greeting()
	{
		return "Goodbye";
	}
}
