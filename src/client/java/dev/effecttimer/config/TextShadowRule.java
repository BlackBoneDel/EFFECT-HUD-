package dev.effecttimer.config;

/**
 * Regla del flag de sombra de texto. La capa de dibujo la usa para
 * cada {@code GuiGraphics.drawString}; así es testeable sin cliente.
 */
public final class TextShadowRule {
	private TextShadowRule() {
	}

	public static boolean shadow(boolean textShadow) {
		return textShadow;
	}

	public static boolean shadow(EffectsConfig config) {
		return config != null && config.textShadow;
	}
}
