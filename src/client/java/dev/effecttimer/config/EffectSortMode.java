package dev.effecttimer.config;

public enum EffectSortMode {
	CATEGORY_THEN_NAME("effecttimer.effects.sort.category_then_name", "effecttimer.tooltip.effects.sort.category_then_name"),
	DURATION_ASC("effecttimer.effects.sort.duration_asc", "effecttimer.tooltip.effects.sort.duration_asc"),
	DURATION_DESC("effecttimer.effects.sort.duration_desc", "effecttimer.tooltip.effects.sort.duration_desc"),
	NAME_ASC("effecttimer.effects.sort.name_asc", "effecttimer.tooltip.effects.sort.name_asc"),
	NONE("effecttimer.effects.sort.none", "effecttimer.tooltip.effects.sort.none");

	private final String translationKey;
	private final String tooltipKey;

	EffectSortMode(String translationKey, String tooltipKey) {
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
