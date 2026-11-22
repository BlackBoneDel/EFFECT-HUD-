package dev.effecttimer.client.config;

/**
 * Orientación de la lista de efectos del HUD.
 * Extraído del enum {@code HudOrientation} del módulo original (customizablehud),
 * reducido a las dos opciones estrictamente necesarias para el Effect Timer.
 */
public enum Orientation {
	VERTICAL("effecttimer.orientation.vertical"),
	HORIZONTAL("effecttimer.orientation.horizontal");

	private final String translationKey;

	Orientation(String translationKey) {
		this.translationKey = translationKey;
	}

	public String translationKey() {
		return translationKey;
	}
}
