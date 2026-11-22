package dev.effecttimer.config;

public enum HudOrientation {
	HORIZONTAL("effecttimer.orientation.horizontal", "effecttimer.tooltip.orientation.horizontal"),
	VERTICAL("effecttimer.orientation.vertical", "effecttimer.tooltip.orientation.vertical");

	private final String translationKey;
	private final String tooltipKey;

	HudOrientation(String translationKey, String tooltipKey) {
		this.translationKey = translationKey;
		this.tooltipKey = tooltipKey;
	}

	public String translationKey() {
		return translationKey;
	}

	public String tooltipKey() {
		return tooltipKey;
	}
}
