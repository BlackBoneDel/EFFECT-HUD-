package dev.effecttimer.config;

public enum HudAnchor {
	// Anclajes junto a la barra rápida vanilla (izquierda/derecha del inventario de
	// 9 huecos centrado). No tienen ninguna relación con armadura: solo usan la
	// geometría fija de la hotbar (182 px centrados).
	HOTBAR_LEFT("effecttimer.anchor.hotbar_left", "effecttimer.tooltip.anchor.hotbar_left"),
	HOTBAR_RIGHT("effecttimer.anchor.hotbar_right", "effecttimer.tooltip.anchor.hotbar_right"),
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
