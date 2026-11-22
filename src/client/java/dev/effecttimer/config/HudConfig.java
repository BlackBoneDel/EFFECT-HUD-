package dev.effecttimer.config;

/**
 * Raíz de configuración del Effect Timer (persistida en config/effecttimer.json).
 * Portada de {@code HudConfig} (HUD-MODS) reducida al módulo Effects HUD (único
 * módulo del standalone; los bloques de otros módulos se eliminaron). El campo de esquema pasa a llamarse
 * {@code configVersion} según la spec v1.1:
 * <ul>
 *   <li>1 = formato plano del standalone v1.0.0 (migrado automáticamente),</li>
 *   <li>2 = esquema actual (objeto {@code effects} anidado).</li>
 * </ul>
 */
public final class HudConfig {
	public static final int CURRENT_CONFIG_VERSION = 2;

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
