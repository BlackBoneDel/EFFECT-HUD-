package dev.effecttimer.config;

public enum LevelFormat {
	ROMAN("effecttimer.effects.level_format.roman", "effecttimer.tooltip.effects.level_format.roman"),
	ARABIC("effecttimer.effects.level_format.arabic", "effecttimer.tooltip.effects.level_format.arabic");

	private final String translationKey;
	private final String tooltipKey;

	LevelFormat(String translationKey, String tooltipKey) {
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
