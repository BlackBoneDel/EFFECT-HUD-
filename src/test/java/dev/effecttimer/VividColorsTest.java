package dev.effecttimer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.effecttimer.format.EffectsColorResolver;
import org.junit.jupiter.api.Test;

/**
 * color automático vívido. Tono conservado (±3°) en cromáticos,
 * S ≥ 0.70 (donde la legibilidad lo permite), V ≥ 0.95; casi-grises → V ≥ 0.92
 * sin inventarles tono; luminancia relativa ≥ 0.35; idempotencia; nunca baja el
 * valor de entrada; los overrides del usuario no se tocan.
 */
public final class VividColorsTest {

	private static float[] hsv(int rgb) {
		return EffectsColorResolver.rgbToHsv(rgb);
	}

	private static float hueDelta(float a, float b) {
		float d = Math.abs(a - b);
		return Math.min(d, 1.0f - d) * 360.0f;
	}

	private static void assertChromatic(int input) {
		int out = EffectsColorResolver.vividize(input);
		float[] a = hsv(input);
		float[] b = hsv(out);
		assertTrue(hueDelta(a[0], b[0]) <= 3.5f, "tono desplazado para " + Integer.toHexString(input) + ": " + hueDelta(a[0], b[0]));
		assertTrue(b[2] >= 0.95f - 0.005f || b[1] < 0.12f, "V alta para " + Integer.toHexString(input) + " fue " + b[2]);
		float lumIn = EffectsColorResolver.luminance(input);
		assertTrue(EffectsColorResolver.luminance(out) >= Math.max(EffectsColorResolver.VIVID_MIN_LUMINANCE, lumIn) - 0.015f,
				"legibilidad (>= max(0.35, lum entrada)) falló en " + Integer.toHexString(input));
		int outV = Math.max(Math.max((out >> 16) & 0xFF, (out >> 8) & 0xFF), out & 0xFF);
		int inV = Math.max(Math.max((input >> 16) & 0xFF, (input >> 8) & 0xFF), input & 0xFF);
		assertTrue(outV >= inV, "el valor nunca debe bajar (" + Integer.toHexString(input) + " -> " + Integer.toHexString(out) + ")");
		assertEquals(out, EffectsColorResolver.vividize(out), "idempotencia rota en " + Integer.toHexString(input));
	}

	@Test
	public void chromaticColorsStayVividAndStable() {
		for (int input : new int[] {0xFF0000, 0x00FF00, 0xFFE600, 0x8A2F2F, 0x3E7C31, 0x7CDFCF, 0xB05050}) {
			assertChromatic(input);
			int out = EffectsColorResolver.vividize(input);
			float[] b = hsv(out);
			assertTrue(b[1] >= 0.70f - 0.02f, "S >= 0.70 para " + Integer.toHexString(input) + " (" + b[1] + ")");
		}
	}

	@Test
	public void nearGraysLightenWithoutInventingHue() {
		for (int input : new int[] {0x777777, 0x333333, 0x010101, 0xBFBFBF}) {
			int out = EffectsColorResolver.vividize(input);
			float[] b = hsv(out);
			assertTrue(b[1] <= 0.12f + 0.02f, "un casi-gris no debe saturarse: " + Integer.toHexString(input) + " S=" + b[1]);
			assertTrue(b[2] >= 0.92f - 0.005f, "V >= 0.92 para " + Integer.toHexString(input) + " V=" + b[2]);
			assertEquals(out, EffectsColorResolver.vividize(out), "idempotencia grises " + Integer.toHexString(input));
		}
	}

	@Test
	public void readabilityFloorBeatsSaturationFloorOnDarkHues() {
		// Azul puro: S >= 0.70 y luminancia >= 0.35 son incompatibles en azul;
		// la legibilidad manda (prioridad documentada en la clase).
		int blue = 0x0000FF;
		int out = EffectsColorResolver.vividize(blue);
		float[] b = hsv(out);
		assertTrue(EffectsColorResolver.luminance(out) >= EffectsColorResolver.VIVID_MIN_LUMINANCE - 0.01f);
		assertTrue(b[0] > 0.5f && b[0] < 0.83f, "el azul sigue siendo azulado: " + b[0]);
		assertEquals(out, EffectsColorResolver.vividize(out), "idempotencia azul");
	}

	@Test
	public void neverLessReadableThanInputAcrossBroadSpectrum() {
		for (int c : new int[] {0x99AABB, 0x7CDFCF, 0xB05050, 0x4A6D98, 0x5A6C81, 0x98D98C, 0x6A0DAD, 0x333333}) {
			int out = EffectsColorResolver.vividize(c);
			assertTrue(EffectsColorResolver.luminance(out) >= EffectsColorResolver.luminance(c) - 0.015f,
					"nunca menos legible que la entrada: " + Integer.toHexString(c));
			assertEquals(out, EffectsColorResolver.vividize(out), "idempotencia amplia " + Integer.toHexString(c));
		}
	}

	@Test
	public void whiteAndPureInputsArePreserved() {
		assertEquals(0xFFFFFF, EffectsColorResolver.vividize(0xFFFFFF), "el blanco queda blanco");
		assertEquals(0x00FF00, EffectsColorResolver.vividize(0x00FF00), "verde puro ya cumple y no se toca");
	}

	@Test
	public void overridesNeverVividized() {
		// el hex personalizado del usuario se devuelve EXACTO (no vívido)
		dev.effecttimer.config.EffectsConfig cfg = new dev.effecttimer.config.EffectsConfig();
		cfg.customEffectColors.put("minecraft:slowness",
				new dev.effecttimer.config.EffectColorOverride(true, "#5A6C81"));
		assertTrue(EffectsColorResolver.resolveCustomRgb("minecraft:slowness", cfg).getAsInt() == 0x5A6C81,
				"el override gana tal cual, sin pasar por vividize");
	}

	@Test
	public void paletteOfTenColorsUnchanged() {
		// la paleta del selector NO se toca
		assertEquals(10, dev.effecttimer.config.ColorHexUtils.EFFECT_PALETTE.size());
	}
}
