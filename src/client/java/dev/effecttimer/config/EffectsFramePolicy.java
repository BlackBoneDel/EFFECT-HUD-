package dev.effecttimer.config;

/**
 * Regla de marco v1.2 («sin fondo = sin marco»): sustituye a la antigua
 * "borde independiente del fondo". El marco (outline de fila, bordes de las
 * fichas PotionHUD normal/mini y cualquier adorno de encuadre) solo se dibuja
 * si AMBOS interruptores están activados. El valor de {@code showEffectBorder}
 * se conserva guardado aunque el fondo esté apagado: al reactivar el fondo el
 * borde reaparece. El acento lateral de CLEAN_CARD es decorativo y NO es
 * marco: se dibuja siempre.
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
