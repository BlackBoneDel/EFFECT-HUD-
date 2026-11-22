package dev.effecttimer.config;

public enum VanillaEffectsMode {
	SHOW_BOTH("effecttimer.effects.vanilla_mode.show_both", "effecttimer.tooltip.effects.vanilla_mode.show_both"),
	CUSTOM_ONLY("effecttimer.effects.vanilla_mode.custom_only", "effecttimer.tooltip.effects.vanilla_mode.custom_only"),
	VANILLA_ONLY("effecttimer.effects.vanilla_mode.vanilla_only", "effecttimer.tooltip.effects.vanilla_mode.vanilla_only");

	private final String translationKey;
	private final String tooltipKey;

	VanillaEffectsMode(String translationKey, String tooltipKey) {
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
