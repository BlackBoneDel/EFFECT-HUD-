package dev.effecttimer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.effecttimer.client.screen.AnchorQuickPicker;
import dev.effecttimer.config.ConfigManager;
import dev.effecttimer.config.EffectsConfig;
import dev.effecttimer.config.HudAnchor;
import dev.effecttimer.config.HudConfig;
import dev.effecttimer.config.HudOrientation;
import dev.effecttimer.layout.EffectRowMetric;
import dev.effecttimer.layout.HudLayoutEngine;
import dev.effecttimer.layout.HudRect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * los anclajes laterales alto/medio/bajo (25 % / 50 % / 75 %
 * de altura) en ambos lados, margen horizontal idéntico al de CENTER_*, clamp
 * con cualquier escala y orientación, selector rápido 3x2 con reset de offsets
 * y round-trip JSON de los nombres nuevos.
 */
public final class SideAnchorsTest {

	@Test
	public void catalogIsThirteenPresetsPlusCustom() {
		assertEquals(14, HudAnchor.values().length, "13 predefinidos + CUSTOM");
		for (String name : new String[] {"SIDE_HIGH_LEFT", "SIDE_HIGH_RIGHT", "SIDE_LOW_LEFT", "SIDE_LOW_RIGHT",
				"CENTER_LEFT", "CENTER_RIGHT", "HOTBAR_LEFT", "HOTBAR_RIGHT", "TOP_LEFT", "TOP_RIGHT",
				"BOTTOM_LEFT", "BOTTOM_RIGHT", "CENTER_BOTTOM", "CUSTOM"}) {
			assertTrue(HudAnchor.valueOf(name) != null, name);
		}
	}

	private static void assertCenter(HudRect r, double expectedFraction, int screenHeight, String label) {
		double center = r.y() + r.height() / 2.0;
		double tolerance = Math.max(1.5, screenHeight * 0.005);
		double delta = Math.abs(center - screenHeight * expectedFraction);
		assertTrue(delta <= tolerance, label + ": centerY=" + center + " esperado ~" + screenHeight * expectedFraction);
	}

	@Test
	public void sideAnchorsLandOnTwentyFiveFiftySeventyFive() {
		int[][] resolutions = { {427, 240}, {854, 480}, {1920, 1080} };
		double[] scales = {0.5, 1.0, 2.5};
		for (int[] res : resolutions) {
			for (double sc : scales) {
				int w = (int) Math.round(60 * sc);
				int h = (int) Math.round(20 * sc);
				assertCenter(HudLayoutEngine.resolveAnchorRect(HudAnchor.SIDE_HIGH_LEFT, 0, 0, w, h, res[0], res[1]), 0.25, res[1], "high-L");
				assertCenter(HudLayoutEngine.resolveAnchorRect(HudAnchor.SIDE_LOW_RIGHT, 0, 0, w, h, res[0], res[1]), 0.75, res[1], "low-R");
				assertCenter(HudLayoutEngine.resolveAnchorRect(HudAnchor.CENTER_LEFT, 0, 0, w, h, res[0], res[1]), 0.50, res[1], "mid-L");
				assertCenter(HudLayoutEngine.resolveAnchorRect(HudAnchor.CENTER_RIGHT, 0, 0, w, h, res[0], res[1]), 0.50, res[1], "mid-R");
			}
		}
	}

	@Test
	public void horizontalMarginMatchesCenterAnchorsAndClamps() {
		for (double sc : new double[] {0.5, 1.0, 2.5}) {
			int w = (int) Math.round(60 * sc);
			int h = (int) Math.round(20 * sc);
			HudRect hl = HudLayoutEngine.resolveAnchorRect(HudAnchor.SIDE_HIGH_LEFT, 0, 0, w, h, 854, 480);
			HudRect cl = HudLayoutEngine.resolveAnchorRect(HudAnchor.CENTER_LEFT, 0, 0, w, h, 854, 480);
			assertEquals(cl.x(), hl.x(), "margen izquierdo debe coincidir con CENTER_LEFT");
			HudRect hr = HudLayoutEngine.resolveAnchorRect(HudAnchor.SIDE_LOW_RIGHT, 0, 0, w, h, 854, 480);
			HudRect cr = HudLayoutEngine.resolveAnchorRect(HudAnchor.CENTER_RIGHT, 0, 0, w, h, 854, 480);
			assertEquals(cr.x(), hr.x(), "margen derecho debe coincidir con CENTER_RIGHT (8 px)");
			// módulo mayor que la pantalla (900×600 en 300×200): el motor, igual
			// que en los CENTER_*, clamp del ORIGEN a pantalla (no
			// reescala); el recorte del sobrante lo hace el borde de la GUI.
			HudRect huge = HudLayoutEngine.resolveAnchorRect(HudAnchor.SIDE_LOW_LEFT, 5000, -5000, 900, 600, 300, 200);
			assertTrue(huge.x() >= 0 && huge.y() >= 0 && huge.x() < 300 && huge.y() < 200, huge.toString());
			// y con un modulo que CABE, el clamp mantiene todo dentro de pantalla
			HudRect fits = HudLayoutEngine.resolveAnchorRect(HudAnchor.SIDE_LOW_RIGHT, 9999, -9999, 120, 30, 300, 200);
			assertTrue(fits.x() >= 0 && fits.right() <= 300 && fits.y() >= 0 && fits.bottom() <= 200, fits.toString());
		}
	}

	@Test
	public void layoutClampsSideAnchorsBothOrientations() {
		for (HudOrientation orientation : HudOrientation.values()) {
			EffectsConfig cfg = new EffectsConfig();
			cfg.anchor = HudAnchor.SIDE_HIGH_RIGHT;
			cfg.orientation = orientation;
			// escala cabible: el modulo completo queda dentro de pantalla
			cfg.scale = 0.5f;
			List<EffectRowMetric> metrics = List.of(
					new EffectRowMetric("a", 300, 20, 0),
					new EffectRowMetric("b", 280, 20, 0));
			HudRect b = HudLayoutEngine.computeEffectsLayout(cfg, metrics, 427, 240).bounds();
			assertTrue(b.x() >= 0 && b.y() >= 0 && b.right() <= 427 && b.bottom() <= 240,
					orientation + ": " + b);
			// escala 2.5 con filas de 300 px = modulo de 750 px: supera la pantalla;
			// la politica (igual que CENTER_*) es clamp del ORIGEN (el
			// exceso solo puede recortarse por el borde; no hay reescala).
			cfg.scale = 2.5f;
			HudRect big = HudLayoutEngine.computeEffectsLayout(cfg, metrics, 427, 240).bounds();
			assertTrue(big.x() >= 0 && big.y() >= 0 && big.y() < 240,
					orientation + " oversize: el origen debe seguir en pantalla: " + big);
		}
	}

	@Test
	public void quickPickerMappingAndOffsetReset() {
		assertEquals(6, AnchorQuickPicker.GRID.length);
		assertEquals(HudAnchor.SIDE_HIGH_LEFT, AnchorQuickPicker.anchorAt(0));
		assertEquals(HudAnchor.SIDE_HIGH_RIGHT, AnchorQuickPicker.anchorAt(1));
		assertEquals(HudAnchor.CENTER_LEFT, AnchorQuickPicker.anchorAt(2));
		assertEquals(HudAnchor.CENTER_RIGHT, AnchorQuickPicker.anchorAt(3));
		assertEquals(HudAnchor.SIDE_LOW_LEFT, AnchorQuickPicker.anchorAt(4));
		assertEquals(HudAnchor.SIDE_LOW_RIGHT, AnchorQuickPicker.anchorAt(5));
		assertEquals(null, AnchorQuickPicker.anchorAt(6));
		assertEquals(-1, AnchorQuickPicker.indexOf(HudAnchor.CUSTOM));
		EffectsConfig effects = new EffectsConfig();
		effects.anchor = HudAnchor.CUSTOM;
		effects.offsetX = 44;
		effects.offsetY = -12;
		AnchorQuickPicker.applyFixedAnchor(effects, HudAnchor.SIDE_LOW_LEFT);
		assertEquals(HudAnchor.SIDE_LOW_LEFT, effects.anchor);
		assertEquals(0, effects.offsetX, "los offsets deben resetearse al salir de CUSTOM");
		assertEquals(0, effects.offsetY);
	}

	@Test
	public void newAnchorsRoundTripAndV2MigrationKeepsOldOnes(@TempDir Path dir) throws Exception {
		Path file = dir.resolve("effecttimer.json");
		ConfigManager mgr = new ConfigManager(file);
		mgr.load();
		HudConfig edited = mgr.getConfig().copy();
		edited.effects.anchor = HudAnchor.SIDE_LOW_RIGHT;
		edited.effects.offsetX = 7;
		edited.effects.offsetY = -4;
		assertTrue(mgr.applyAndSaveNow(edited));
		String raw = Files.readString(file);
		assertTrue(raw.contains("SIDE_LOW_RIGHT"), "el anclaje nuevo debe serializarse por nombre");
		ConfigManager again = new ConfigManager(file);
		again.load();
		assertEquals(HudAnchor.SIDE_LOW_RIGHT, again.getConfig().effects.anchor);
		assertEquals(7, again.getConfig().effects.offsetX);

		// config v2 con un anclaje antiguo: la migración a v3 lo deja intacto
		Files.writeString(file, """
				{
				  "configVersion": 2,
				  "effects": { "anchor": "CENTER_LEFT", "scale": 1.4, "rowSpacing": 3, "showEffectBorder": false },
				  "language": "SPANISH"
				}
				""");
		ConfigManager migrated = new ConfigManager(file);
		migrated.load();
		assertEquals(HudAnchor.CENTER_LEFT, migrated.getConfig().effects.anchor, "v2->v3 no debe tocar anclajes");
		assertEquals(1.4f, migrated.getConfig().effects.scale);
		assertTrue(migrated.getConfig().effects.customColorsEnabled, "v3 añade customColorsEnabled=true");
		assertTrue(migrated.getConfig().effects.textShadow, "v3 añade textShadow=true");
		assertEquals(3, migrated.getConfig().configVersion);
	}
}
