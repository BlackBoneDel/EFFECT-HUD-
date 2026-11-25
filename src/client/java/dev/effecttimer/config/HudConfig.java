package dev.effecttimer.config;

/**
 * Raíz de la configuración guardada en config/effecttimer.json.
 *
 * <p>{@code configVersion}: 1 = formato plano heredado (migrado automáticamente),
 * 2 = objeto {@code effects} anidado, 3 = esquema actual.</p>
 */
public final class HudConfig {
	public static final int CURRENT_CONFIG_VERSION = 3;

	public int configVersion = CURRENT_CONFIG_VERSION;
	public ModLanguage language = ModLanguage.ENGLISH;
	public EffectsConfig effects = new EffectsConfig();
	public VisibilityConfig visibility = new VisibilityConfig();
	public boolean editorSnapToGrid = true;
	public int editorGridSize = 4;
	public boolean editorShowGuides = true;

	public static HudConfig createDefault() {
		return new HudConfig();
	}

	public HudConfig copy() {
		HudConfig copy = new HudConfig();
		copy.configVersion = configVersion;
		copy.language = language != null ? language : ModLanguage.ENGLISH;
		copy.effects = effects != null ? effects.copy() : new EffectsConfig();
		copy.visibility = visibility != null ? visibility.copy() : new VisibilityConfig();
		copy.editorSnapToGrid = editorSnapToGrid;
		copy.editorGridSize = editorGridSize;
		copy.editorShowGuides = editorShowGuides;
		return copy;
	}
}
