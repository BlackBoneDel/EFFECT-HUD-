package dev.effecttimer.config;

/**
 * Sin fondo no hay marco. El marco (outline de fila, bordes de las fichas PotionHUD
 * y cualquier encuadre) solo se dibuja si AMBOS interruptores están activados.
 * {@code showEffectBorder} se conserva aunque el fondo esté apagado. El acento
 * lateral de CLEAN_CARD es decorativo y no es marco: se dibuja siempre.
 */
public final class EffectsFramePolicy {
	private EffectsFramePolicy() {
	}

	public static boolean drawFrame(boolean showEffectBackground, boolean showEffectBorder) {
		return showEffectBackground && showEffectBorder;
	}

	/** El control de borde de la UI solo está habilitado con el fondo activo. */
	public static boolean borderControlEnabled(boolean showEffectBackground) {
		return showEffectBackground;
	}

	public static boolean drawFrame(EffectsConfig config) {
		if (config == null) {
			return false;
		}
		return drawFrame(config.showEffectBackground, config.showEffectBorder);
	}
}
