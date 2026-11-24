package dev.effecttimer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.effecttimer.config.ConfigManager;
import dev.effecttimer.config.HudConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * configVersion sube a 3. La cadena completa
 * v1 → v2 → v3 y un v2 puntual solo añaden {@code customColorsEnabled=true} y
 * {@code textShadow=true} sin tocar nada más. El guardado sigue siendo atómico
 * con backup `.corrupt-*.bak`.
 */
public final class ConfigMigrationV3Test {

	@Test
	public void currentVersionIsThree() {
		assertEquals(3, HudConfig.CURRENT_CONFIG_VERSION);
	}

	@Test
	public void v1ToV2ToV3ChainKeepsLegacyData(@TempDir Path dir) throws Exception {
		Path file = dir.resolve("effecttimer.json");
		// v1 plana heredada (sin configVersion, posX/posY, blinkOnExpire, textColor)
		Files.writeString(file, """
				{
				  "enabled": true,
				  "posX": 133,
				  "posY": 91,
				  "scale": 1.5,
				  "showIcons": false,
				  "showDuration": true,
				  "blinkOnExpire": true,
				  "textColor": "#123456"
				}
				""");
		ConfigManager mgr = new ConfigManager(file);
		mgr.load();
		HudConfig cfg = mgr.getConfig();
		assertEquals(3, cfg.configVersion, "la cadena debe aterrizar en v3");
		assertEquals(dev.effecttimer.config.HudAnchor.CUSTOM, cfg.effects.anchor, "posX/posY → CUSTOM+offsets");
		assertEquals(133, cfg.effects.offsetX);
		assertEquals(91, cfg.effects.offsetY);
		assertEquals(1.5f, cfg.effects.scale);
		assertTrue(cfg.effects.fadeOnLowDuration, "blinkOnExpire → fadeOnLowDuration");
		assertFalse(cfg.effects.showIcon, "showIcons=false respetado");
		assertTrue(cfg.effects.customColorsEnabled, "v3 añade el global de colores");
		assertTrue(cfg.effects.textShadow, "v3 añade la sombra de texto");
	}

	@Test
	public void v2ToV3OnlyAddsDefaults(@TempDir Path dir) throws Exception {
		Path file = dir.resolve("effecttimer.json");
		Files.writeString(file, """
				{
				  "configVersion": 2,
				  "effects": {
				    "anchor": "SIDE_HIGH_LEFT",
				    "scale": 0.75,
				    "rowSpacing": 6,
				    "orientation": "HORIZONTAL",
				    "customEffectColors": { "minecraft:haste": { "enabled": false, "colorHex": "#0A0B0C" } },
				    "showEffectBackground": true,
				    "showEffectBorder": false
				  },
				  "language": "ENGLISH"
				}
				""");
		ConfigManager mgr = new ConfigManager(file);
		mgr.load();
		HudConfig cfg = mgr.getConfig();
		assertEquals(3, cfg.configVersion);
		assertEquals(dev.effecttimer.config.HudAnchor.SIDE_HIGH_LEFT, cfg.effects.anchor);
		assertEquals(0.75f, cfg.effects.scale);
		assertEquals(6, cfg.effects.rowSpacing);
		assertEquals(dev.effecttimer.config.HudOrientation.HORIZONTAL, cfg.effects.orientation);
		assertEquals("#0A0B0C", cfg.effects.customEffectColors.get("minecraft:haste").colorHex, "colores conservados");
		assertFalse(cfg.effects.customEffectColors.get("minecraft:haste").enabled, "toggle pausado conservado");
		assertTrue(cfg.effects.showEffectBackground);
		assertFalse(cfg.effects.showEffectBorder);
		assertTrue(cfg.effects.customColorsEnabled);
		assertTrue(cfg.effects.textShadow);
		// y re-save atómico deja el config v3 en el archivo
		assertTrue(mgr.applyAndSaveNow(cfg));
		String raw = Files.readString(file);
		assertTrue(raw.contains("\"configVersion\": 3") && raw.contains("\"textShadow\": true"));
	}

	@Test
	public void atomicSaveStillBackupsCorrupt(@TempDir Path dir) throws Exception {
		Path file = dir.resolve("effecttimer.json");
		Files.writeString(file, "{ roto a propósito");
		ConfigManager mgr = new ConfigManager(file);
		mgr.load();
		assertTrue(mgr.consumeRecoveryNotice());
		assertTrue(Files.list(dir).anyMatch(p -> p.getFileName().toString().contains(".corrupt-") && p.getFileName().toString().endsWith(".bak")));
		assertTrue(mgr.getConfig().effects.customColorsEnabled, "defaults restaurados también en v3");
	}
}
