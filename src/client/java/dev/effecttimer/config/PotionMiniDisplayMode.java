package dev.effecttimer.config;

public enum PotionMiniDisplayMode {
	TIMER_TEXT("effecttimer.effects.mini_display.timer", "effecttimer.tooltip.effects.mini_display.timer"),
	DURATION_BAR("effecttimer.effects.mini_display.duration_bar", "effecttimer.tooltip.effects.mini_display.duration_bar");

	private final String translationKey;
	private final String tooltipKey;

	PotionMiniDisplayMode(String translationKey, String tooltipKey) {
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
