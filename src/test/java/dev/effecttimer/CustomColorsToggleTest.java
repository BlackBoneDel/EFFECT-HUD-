package dev.effecttimer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.effecttimer.config.ConfigManager;
import dev.effecttimer.config.EffectColorOverride;
import dev.effecttimer.config.EffectsConfig;
import dev.effecttimer.config.HudConfig;
import dev.effecttimer.config.PresetCatalog;
import dev.effecttimer.config.PresetId;
import dev.effecttimer.format.EffectCategoryKind;
import dev.effecttimer.format.EffectSnapshot;
import dev.effecttimer.format.EffectsColorResolver;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Bloque 5 (v1.2): interruptor global {@code customColorsEnabled} (default ON)
 * y toggle por efecto. Con el global en OFF ningún override se aplica (nombre,
 * temporizador y acento vuelven a la cadena normal), pero TODOS los hex y los
 * toggles individuales quedan guardados y vuelven al reactivar.
 */
public final class CustomColorsToggleTest {

	private static EffectSnapshot snap() {
		return new EffectSnapshot("minecraft:speed", "Speed", EffectCategoryKind.BENEFICIAL, 0, 200,
				false, false, true, true, 0x7CDFCF);
	}

	private static EffectsConfig withOverride() {
		EffectsConfig cfg = new EffectsConfig();
		cfg.customEffectColors.put("minecraft:speed", new EffectColorOverride(true, "#00FF00"));
		return cfg;
	}

	@Test
	public void globalOffSuppressesEveryOverrideButKeepsData() {
		EffectsConfig cfg = withOverride();
		assertTrue(cfg.customColorsEnabled, "default ON");
		OptionalInt on = EffectsColorResolver.resolveCustomRgb("minecraft:speed", cfg);
		assertEquals(0x00FF00, on.getAsInt());

		cfg.customColorsEnabled = false;
		assertTrue(EffectsColorResolver.resolveCustomRgb("minecraft:speed", cfg).isEmpty(), "global OFF anula el override");
		assertEquals(EffectsColorResolver.resolveNativeDisplayRgb(0x7CDFCF),
				EffectsColorResolver.resolveEffectiveRgb(snap(), cfg), "cae al automático vívido");
		// temporizador y acento también quedan anulados
		assertFalse(EffectsColorResolver.durationColorArgb(snap(), cfg, 20f) == (0xFF000000 | 0x00FF00));
		assertFalse(EffectsColorResolver.accentColorArgb(snap(), cfg) == (0xFF000000 | 0x00FF00));
		// datos intactos: hex y enabled por efecto sin tocar
		EffectColorOverride ov = cfg.customEffectColors.get("minecraft:speed");
		assertTrue(ov.enabled);
		assertEquals("#00FF00", ov.colorHex);

		cfg.customColorsEnabled = true;
		assertEquals(0x00FF00, EffectsColorResolver.resolveCustomRgb("minecraft:speed", cfg).getAsInt(),
				"global ON restaura cada color tal cual");
	}

	@Test
	public void perEffectTogglePausesSingleOverride() {
		EffectsConfig cfg = withOverride();
		cfg.customEffectColors.get("minecraft:speed").enabled = false;
		assertTrue(EffectsColorResolver.resolveCustomRgb("minecraft:speed", cfg).isEmpty(), "override pausado");
		assertEquals("#00FF00", cfg.customEffectColors.get("minecraft:speed").colorHex, "el hex se conserva");
		cfg.customEffectColors.get("minecraft:speed").enabled = true;
		assertEquals(0x00FF00, EffectsColorResolver.resolveCustomRgb("minecraft:speed", cfg).getAsInt());
	}

	@Test
	public void priorityChainOverrideThenTimeThenVivid() {
		// 1. override activo (con global ON) gana incluso con colorByRemaining
		EffectsConfig cfg = withOverride();
		cfg.colorDurationByRemaining = true;
		assertEquals(0xFF000000 | 0x00FF00, EffectsColorResolver.durationColorArgb(snap(), cfg, 20f));

		// 2. sin override → color por tiempo restante (10 s → rojo vívido)
		cfg.customColorsEnabled = false;
		assertEquals(0xFFFF3B3B, EffectsColorResolver.durationColorArgb(snap(), cfg, 20f));

		// 3. sin tiempo-color → automático vívido
		cfg.colorDurationByRemaining = false;
		assertEquals(0xFF000000 | EffectsColorResolver.resolveNativeDisplayRgb(0x7CDFCF),
				EffectsColorResolver.durationColorArgb(snap(), cfg, 20f));
	}

	@Test
	public void globalFlagRoundTripsAndResetTurnsItBackOn(@TempDir Path dir) throws Exception {
		Path file = dir.resolve("effecttimer.json");
		ConfigManager mgr = new ConfigManager(file);
		mgr.load();
		HudConfig cfg = mgr.getConfig().copy();
		cfg.effects.customColorsEnabled = false;
		cfg.effects.customEffectColors.put("minecraft:luck", new EffectColorOverride(true, "#123456"));
		assertTrue(mgr.applyAndSaveNow(cfg));
		ConfigManager again = new ConfigManager(file);
		again.load();
		assertFalse(again.getConfig().effects.customColorsEnabled);
		assertTrue(again.getConfig().effects.customEffectColors.get("minecraft:luck").enabled,
				"los datos por efecto sobreviven al global OFF");

		// "Reset" de la pantalla = new EffectsConfig() preservando colores → ON
		EffectsConfig reset = new EffectsConfig();
		assertTrue(reset.customColorsEnabled, "Reset pone customColorsEnabled=true");

		// los preajustes NO cambian el global ni los overrides
		HudConfig withPreset = again.getConfig().copy();
		withPreset.effects.customColorsEnabled = false;
		PresetCatalog.applyPreset(withPreset, PresetId.EXPANDED_READOUT);
		assertFalse(withPreset.effects.customColorsEnabled, "el preajuste debe conservar el interruptor");
		assertTrue(withPreset.effects.customEffectColors.containsKey("minecraft:luck"), "conserva overrides");
	}

	@Test
	public void migrationFromV2AddsGlobalFlagTrue(@TempDir Path dir) throws Exception {
		Path file = dir.resolve("effecttimer.json");
		Files.writeString(file, """
				{
				  "configVersion": 2,
				  "effects": {
				    "scale": 1.2,
				    "customEffectColors": { "minecraft:speed": { "enabled": true, "colorHex": "#ABCDEF" } }
				  }
				}
				""");
		ConfigManager mgr = new ConfigManager(file);
		mgr.load();
		assertTrue(mgr.getConfig().effects.customColorsEnabled);
		assertEquals("#ABCDEF", mgr.getConfig().effects.customEffectColors.get("minecraft:speed").colorHex);
		assertEquals(3, mgr.getConfig().configVersion);
	}

	@Test
	public void listRowVisualsAndGlobalControls() throws Exception {
		// UI de bloque 5: casilla por fila (hit-test), muestra atenuada + tachado
		// cuando el override esta pausado o el global esta OFF, y boton global
		// TAMBIEN en «Configurar efectos» (spec 5.a).
		Path screen = Path.of("src/client/java/dev/effecttimer/client/screen/EffectColorsScreen.java");
		if (!java.nio.file.Files.exists(screen)) {
			screen = Path.of("..", "src/client/java/dev/effecttimer/client/screen/EffectColorsScreen.java");
		}
		String src = java.nio.file.Files.readString(screen);
		assertTrue(src.contains("ARGB.multiplyAlpha"), "muestra atenuada con global OFF");
		assertTrue(src.contains("graphics.fill(sx, sy + 4, sx + 9, sy + 5"), "linea de tachado en fila pausada");
		assertTrue(src.contains("rowToggleRect("), "hit-test de la casilla por fila");
		assertTrue(src.contains("customColorsEnabled"), "el global vive en la pantalla");
		Path cfg = Path.of("src/client/java/dev/effecttimer/client/screen/EffectsConfigScreen.java");
		if (!java.nio.file.Files.exists(cfg)) {
			cfg = Path.of("..", "src/client/java/dev/effecttimer/client/screen/EffectsConfigScreen.java");
		}
		assertTrue(java.nio.file.Files.readString(cfg).contains("effecttimer.effects.custom_colors"),
				"el toggle global debe existir tambien en Configurar efectos");
	}

	@Test
	public void rowToggleGeometryIsPureAndStable() {
		int[] rect = dev.effecttimer.client.screen.EffectColorsScreen.rowToggleRect(100, 200, 40);
		assertEquals(100 + 200 - 13 - 14, rect[0]);
		assertEquals(40 + (22 - 11) / 2, rect[1]);
		assertEquals(11, rect[2]);
		assertEquals(11, rect[3]);
	}
}
