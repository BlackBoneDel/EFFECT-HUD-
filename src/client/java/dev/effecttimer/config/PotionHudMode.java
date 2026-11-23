package dev.effecttimer.config;

public enum PotionHudMode {
	NORMAL("effecttimer.effects.potion_mode.normal", "effecttimer.tooltip.effects.potion_mode.normal"),
	MINI("effecttimer.effects.potion_mode.mini", "effecttimer.tooltip.effects.potion_mode.mini");

	private final String translationKey;
	private final String tooltipKey;

	PotionHudMode(String translationKey, String tooltipKey) {
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
