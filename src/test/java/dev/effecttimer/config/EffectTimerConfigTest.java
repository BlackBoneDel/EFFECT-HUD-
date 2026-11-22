package dev.effecttimer.config;

import dev.effecttimer.client.config.ColorHex;
import dev.effecttimer.client.config.EffectTimerConfig;
import dev.effecttimer.client.config.Orientation;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Persistencia JSON del Effect Timer: guarda SOLO los parámetros del módulo,
 * normaliza valores, y recupera defaults con backup si el archivo está corrupto.
 */
public final class EffectTimerConfigTest {
	@Test
	public void roundTripsOnlyTheDeclaredFields(@TempDir Path dir) throws Exception {
		Path file = dir.resolve("effecttimer.json");
		EffectTimerConfig cfg = new EffectTimerConfig();
		cfg.enabled = false;
		cfg.posX = 120;
		cfg.posY = 240;
		cfg.scale = 1.35F;
		cfg.orientation = Orientation.HORIZONTAL;
		cfg.showIcons = false;
		cfg.showDuration = true;
		cfg.showName = false;
		cfg.textColor = "#38BDF8";
		cfg.blinkOnExpire = false;
		assertTrue(cfg.save(file));

		String json = Files.readString(file);
		for (String forbidden : new String[] {"armor", "fps", "cps", "keystroke", "compass", "ping", "anchor", "style", "presetColor"}) {
			assertFalse(json.toLowerCase().contains(forbidden), "config JSON must not reference " + forbidden);
		}

		EffectTimerConfig loaded = EffectTimerConfig.load(file);
		assertFalse(loaded.enabled);
		assertEquals(120, loaded.posX);
		assertEquals(240, loaded.posY);
		assertEquals(1.35F, loaded.scale);
		assertEquals(Orientation.HORIZONTAL, loaded.orientation);
		assertFalse(loaded.showIcons);
		assertTrue(loaded.showDuration);
		assertFalse(loaded.showName);
		assertEquals("#38BDF8", loaded.textColor);
		assertFalse(loaded.blinkOnExpire);
	}

	@Test
	public void clampsAndNormalizesOnLoad(@TempDir Path dir) throws Exception {
		Path file = dir.resolve("effecttimer.json");
		Files.writeString(file, """
				{
				  "enabled": true,
				  "posX": -50,
				  "posY": -7,
				  "scale": 99.0,
				  "orientation": "VERTICAL",
				  "showIcons": true,
				  "showDuration": true,
				  "showName": true,
				  "textColor": "ff5555",
				  "blinkOnExpire": true
				}
				""");
		EffectTimerConfig loaded = EffectTimerConfig.load(file);
		assertEquals(0, loaded.posX);
		assertEquals(0, loaded.posY);
		assertEquals(EffectTimerConfig.MAX_SCALE, loaded.scale);
		assertEquals("#FF5555", loaded.textColor);
		assertEquals(0xFFFF5555, loaded.textColorArgb());
	}

	@Test
	public void corruptFileFallsBackToDefaultsWithBackup(@TempDir Path dir) throws Exception {
		Path file = dir.resolve("effecttimer.json");
		Files.writeString(file, "{ this is not json !!");
		EffectTimerConfig loaded = EffectTimerConfig.load(file);
		EffectTimerConfig defaults = EffectTimerConfig.createDefault();
		assertTrue(loaded.enabled);
		assertEquals(4, loaded.posX);
		assertEquals(defaults.textColor, loaded.textColor);
		try (var files = Files.list(dir)) {
			assertTrue(files.anyMatch(p -> p.getFileName().toString().contains(".corrupt-") && p.toString().endsWith(".bak")),
					"corrupt config should be preserved as a .corrupt-*.bak file");
		}
	}

	@Test
	public void missingFileWritesDefaults(@TempDir Path dir) throws Exception {
		Path file = dir.resolve("nested/effecttimer.json");
		EffectTimerConfig loaded = EffectTimerConfig.load(file);
		assertTrue(Files.exists(file));
		assertTrue(loaded.enabled);
	}

	@Test
	public void hexValidationMatchesOriginalRules() {
		assertEquals("#FF5555", ColorHex.normalizeHex("ff5555").orElseThrow());
		assertEquals("#FF5555", ColorHex.normalizeHex("#FF5555").orElseThrow());
		assertTrue(ColorHex.normalizeHex("#FFF").isEmpty());
		assertTrue(ColorHex.normalizeHex("zzzzzz").isEmpty());
		assertEquals(0x38BDF8, ColorHex.parseRgb("#38bdf8").orElseThrow());
		assertEquals("#FFFFFF", ColorHex.normalizeOrDefault(null, "#FFFFFF"));
		// La paleta es cíclica, como en el mod original.
		String first = ColorHex.TEXT_PALETTE.get(0);
		String last = ColorHex.TEXT_PALETTE.get(ColorHex.TEXT_PALETTE.size() - 1);
		assertEquals(ColorHex.TEXT_PALETTE.get(1), ColorHex.nextPaletteColor(first, ColorHex.TEXT_PALETTE));
		assertEquals(first, ColorHex.nextPaletteColor(last, ColorHex.TEXT_PALETTE));
	}
}
