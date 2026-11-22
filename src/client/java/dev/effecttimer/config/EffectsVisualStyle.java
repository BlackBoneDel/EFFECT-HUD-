package dev.effecttimer.config;

public enum EffectsVisualStyle {
	MINECRAFT_COMPACT("effecttimer.effects.style.minecraft_compact", "effecttimer.tooltip.effects.style.minecraft_compact"),
	CLEAN_CARD("effecttimer.effects.style.clean_card", "effecttimer.tooltip.effects.style.clean_card"),
	MINIMAL_TEXT("effecttimer.effects.style.minimal_text", "effecttimer.tooltip.effects.style.minimal_text"),
	POTION_HUD("effecttimer.effects.style.potion_hud", "effecttimer.tooltip.effects.style.potion_hud");

	private final String translationKey;
	private final String tooltipKey;

	EffectsVisualStyle(String translationKey, String tooltipKey) {
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
