package dev.effecttimer.config;

public enum HudAnchor {
	CENTER_LEFT("effecttimer.anchor.center_left", "effecttimer.tooltip.anchor.center_left"),
	CENTER_RIGHT("effecttimer.anchor.center_right", "effecttimer.tooltip.anchor.center_right"),
	CENTER_BOTTOM("effecttimer.anchor.center_bottom", "effecttimer.tooltip.anchor.center_bottom"),
	TOP_LEFT("effecttimer.anchor.top_left", "effecttimer.tooltip.anchor.top_left"),
	TOP_RIGHT("effecttimer.anchor.top_right", "effecttimer.tooltip.anchor.top_right"),
	BOTTOM_LEFT("effecttimer.anchor.bottom_left", "effecttimer.tooltip.anchor.bottom_left"),
	BOTTOM_RIGHT("effecttimer.anchor.bottom_right", "effecttimer.tooltip.anchor.bottom_right"),
	CUSTOM("effecttimer.anchor.custom", "effecttimer.tooltip.anchor.custom");

	private final String translationKey;
	private final String tooltipKey;

	HudAnchor(String translationKey, String tooltipKey) {
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
