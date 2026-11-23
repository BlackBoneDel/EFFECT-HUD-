package dev.effecttimer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.effecttimer.config.ColorHexUtils;
import dev.effecttimer.config.ConfigManager;
import dev.effecttimer.config.ConfigMigrator;
import dev.effecttimer.config.ConfigValidator;
import dev.effecttimer.config.EffectColorOverride;
import dev.effecttimer.config.EffectSortMode;
import dev.effecttimer.config.EffectsConfig;
import dev.effecttimer.config.EffectsVisualStyle;
import dev.effecttimer.config.HudAnchor;
import dev.effecttimer.config.HudConfig;
import dev.effecttimer.config.HudOrientation;
import dev.effecttimer.config.LevelFormat;
import dev.effecttimer.config.ModLanguage;
import dev.effecttimer.config.PotionHudMode;
import dev.effecttimer.config.PotionMiniDisplayMode;
import dev.effecttimer.config.PresetCatalog;
import dev.effecttimer.config.PresetId;
import dev.effecttimer.config.VanillaEffectsMode;
import dev.effecttimer.config.VisibilityConfig;
import dev.effecttimer.format.DurationFormatter;
import dev.effecttimer.format.EffectCategoryKind;
import dev.effecttimer.format.EffectDurationTracker;
import dev.effecttimer.format.EffectOrderer;
import dev.effecttimer.format.EffectSnapshot;
import dev.effecttimer.format.EffectsColorResolver;
import dev.effecttimer.format.RomanNumerals;
import dev.effecttimer.layout.EffectRowMetric;
import dev.effecttimer.layout.EffectsLayoutResult;
import dev.effecttimer.layout.HudLayoutEngine;
import dev.effecttimer.layout.HudRect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Batería v1.1.0–v1.2.0 sobre la lógica portada de HUD-MODS (paquetes config/format/
 * layout). Cubre los puntos de la spec que pueden validarse sin cliente de
 * Minecraft; los flujos de pantalla (vista previa, Modo de Prueba, editor
 * visual) se verifican en juego y quedan documentados en docs/VERIFICATION.md.
 */
public final class EffectTimerBatteryTest {

	// ---------- §1 valores por defecto ----------
	@Test
	public void defaultsMatchSpec() {
		EffectsConfig d = new EffectsConfig();
		assertTrue(d.enabled);
		assertEquals(HudAnchor.CENTER_RIGHT, d.anchor);
		assertEquals(HudOrientation.VERTICAL, d.orientation);
		assertEquals(EffectsVisualStyle.MINECRAFT_COMPACT, d.style);
		assertEquals(1.0f, d.scale);
		assertEquals(2, d.rowSpacing);
		assertFalse(d.showEffectBackground);
		assertEquals(65, d.backgroundOpacityPercent);
		assertTrue(d.showEffectBorder);
		assertTrue(d.showIcon);
		assertTrue(d.showName);
		assertTrue(d.showDuration);
		assertTrue(d.showLevel);
		assertTrue(d.hideLevelOne);
		assertEquals(LevelFormat.ROMAN, d.levelFormat);
		assertTrue(d.showEffectsWithoutParticles);
		assertTrue(d.groupByEffectCategory);
		assertEquals(EffectSortMode.CATEGORY_THEN_NAME, d.sortMode);
		assertEquals(VanillaEffectsMode.SHOW_BOTH, d.vanillaEffectsMode);
		assertTrue(d.fadeOnLowDuration);
		assertFalse(d.colorDurationByRemaining);
		assertEquals(PotionHudMode.NORMAL, d.potionHudMode);
		assertEquals(PotionMiniDisplayMode.TIMER_TEXT, d.potionMiniDisplay);
		assertTrue(d.customEffectColors.isEmpty());
		assertTrue(d.customColorsEnabled, "v1.2: global de colores por defecto ON");
		assertTrue(d.textShadow, "v1.2: sombra de texto por defecto ON");
		assertEquals(3, HudConfig.CURRENT_CONFIG_VERSION);
		assertEquals(ModLanguage.ENGLISH, new HudConfig().language);
		VisibilityConfig vis = new VisibilityConfig();
		assertTrue(vis.hideInInventory);
		assertFalse(vis.hideInChat);
		assertTrue(vis.hideInPauseScreen);
		assertTrue(vis.requireActiveWorld);
	}

	// ---------- §2/§3 clamps ----------
	@Test
	public void validatorClamps() {
		HudConfig bad = new HudConfig();
		bad.effects.scale = 0.2f;
		bad.effects.rowSpacing = -5;
		bad.effects.backgroundOpacityPercent = 110;
		bad.editorGridSize = 99;
		bad.effects.offsetX = 99999;
		ConfigValidator.ValidationResult vr = ConfigValidator.validateAndSanitize(bad);
		assertTrue(vr.modified());
		assertEquals(0.5f, vr.config().effects.scale);
		assertEquals(0, vr.config().effects.rowSpacing);
		assertEquals(100, vr.config().effects.backgroundOpacityPercent);
		assertEquals(32, vr.config().editorGridSize);
		assertEquals(4000, vr.config().effects.offsetX);

		HudConfig big = new HudConfig();
		big.effects.scale = 3.4f;
		big.effects.rowSpacing = 99;
		big.effects.backgroundOpacityPercent = -10;
		ConfigValidator.ValidationResult vr2 = ConfigValidator.validateAndSanitize(big);
		assertEquals(2.5f, vr2.config().effects.scale);
		assertEquals(16, vr2.config().effects.rowSpacing);
		assertEquals(0, vr2.config().effects.backgroundOpacityPercent);
		assertEquals(1.23f, ConfigValidator.clampScale(1.234f));
	}

	@Test
	public void nullAnchorFallsBackToCenterRight() {
		HudConfig c = new HudConfig();
		c.effects.anchor = null;
		assertEquals(HudAnchor.CENTER_RIGHT, ConfigValidator.validateAndSanitize(c).config().effects.anchor);
	}

	// ---------- §30/§31 ids y hex ----------
	@Test
	public void effectIdNormalization() {
		assertEquals("minecraft:speed", ConfigValidator.normalizeEffectId("speed").orElse(""));
		assertEquals("minecraft:regeneration", ConfigValidator.normalizeEffectId("minecraft:regeneration").orElse(""));
		assertEquals("somemod:aura", ConfigValidator.normalizeEffectId("somemod:aura").orElse(""));
		assertTrue(ConfigValidator.normalizeEffectId("").isEmpty());
		assertTrue(ConfigValidator.normalizeEffectId("Bad ID").isEmpty());
		assertEquals("minecraft:speed", ConfigValidator.normalizeEffectId("MINECRAFT:SPEED").orElse(""));
	}

	@Test
	public void paletteHasExactlyTenSpecColors() {
		assertEquals(10, ColorHexUtils.EFFECT_PALETTE.size());
		assertEquals("#FF66B2", ColorHexUtils.EFFECT_PALETTE.get(0).hex());
		assertEquals("#E879F9", ColorHexUtils.EFFECT_PALETTE.get(1).hex());
		assertEquals("#FF5555", ColorHexUtils.EFFECT_PALETTE.get(2).hex());
		assertEquals("#FFA94D", ColorHexUtils.EFFECT_PALETTE.get(3).hex());
		assertEquals("#FFE066", ColorHexUtils.EFFECT_PALETTE.get(4).hex());
		assertEquals("#6EEB83", ColorHexUtils.EFFECT_PALETTE.get(5).hex());
		assertEquals("#38BDF8", ColorHexUtils.EFFECT_PALETTE.get(6).hex());
		assertEquals("#60A5FA", ColorHexUtils.EFFECT_PALETTE.get(7).hex());
		assertEquals("#A78BFA", ColorHexUtils.EFFECT_PALETTE.get(8).hex());
		assertEquals("#F8FAFC", ColorHexUtils.EFFECT_PALETTE.get(9).hex());
	}

	@Test
	public void hexValidation() {
		assertEquals("#FF66B2", ColorHexUtils.normalizeHex("  ff66b2 ").orElse(""));
		assertTrue(ColorHexUtils.normalizeHex("#12345").isEmpty());
		assertEquals(0xFF66B2, ColorHexUtils.parseRgb("#ff66b2").orElse(0));
		assertEquals(0xABCDEF, ColorHexUtils.parseRgb("abcdef").orElse(0));
		assertEquals("#E6EDF3", ColorHexUtils.normalizeOrDefault("zzz", "#E6EDF3"));
		assertEquals("#E6EDF3", new EffectColorOverride().colorHex);
	}

	// ---------- §38 migración v1.0.0 ----------
	@Test
	public void migratesLegacyStandaloneKeys() {
		String legacy = "{\"enabled\":false,\"posX\":100,\"posY\":50,\"scale\":1.5,"
				+ "\"orientation\":\"HORIZONTAL\",\"showIcons\":false,\"showDuration\":true,"
				+ "\"showName\":false,\"blinkOnExpire\":false,\"textColor\":\"#112233\"}";
		JsonObject root = JsonParser.parseString(legacy).getAsJsonObject();
		HudConfig target = new HudConfig();
		assertTrue(ConfigMigrator.migrate(root, target));
		assertFalse(target.effects.enabled);
		assertEquals(1.5f, target.effects.scale);
		assertEquals(HudOrientation.HORIZONTAL, target.effects.orientation);
		assertFalse(target.effects.showIcon);
		assertTrue(target.effects.showDuration);
		assertFalse(target.effects.showName);
		assertFalse(target.effects.fadeOnLowDuration);
		assertEquals(HudAnchor.CUSTOM, target.effects.anchor);
		assertEquals(100, target.effects.offsetX);
		assertEquals(50, target.effects.offsetY);
		assertEquals(3, target.configVersion);

		HudConfig modern = new HudConfig();
		assertFalse(ConfigMigrator.migrate(JsonParser.parseString("{\"configVersion\":3}").getAsJsonObject(), modern));
	}

	// ---------- §35 presets ----------
	@Test
	public void presetsApplyAndPreserveColors() {
		HudConfig withColors = new HudConfig();
		EffectColorOverride ovr = new EffectColorOverride(true, "#FF5555");
		withColors.effects.customEffectColors.put("minecraft:regeneration", ovr);
		PresetCatalog.applyPreset(withColors, PresetId.COMPACT_TEXT);
		assertEquals(EffectsVisualStyle.MINIMAL_TEXT, withColors.effects.style);
		assertEquals(0.9f, withColors.effects.scale);
		assertTrue(withColors.effects.customEffectColors.containsKey("minecraft:regeneration"));

		HudConfig expanded = new HudConfig();
		PresetCatalog.applyPreset(expanded, PresetId.EXPANDED_READOUT);
		assertTrue(expanded.effects.colorDurationByRemaining);
		assertTrue(expanded.effects.showEffectBorder);
		assertFalse(expanded.effects.showEffectBackground);

		HudConfig vanilla = new HudConfig();
		vanilla.effects.scale = 2.0f;
		PresetCatalog.applyPreset(vanilla, PresetId.VANILLA_COMPACT);
		assertEquals(1.0f, vanilla.effects.scale);
	}

	// ---------- §37/§38 persistencia ----------
	@Test
	public void configRoundTripAtomicAndCorruptBackup(@TempDir Path dir) {
		Path file = dir.resolve("effecttimer.json");
		ConfigManager mgr = new ConfigManager(file);
		mgr.load();
		HudConfig edited = mgr.getConfig().copy();
		edited.effects.scale = 1.37f;
		edited.effects.anchor = HudAnchor.TOP_RIGHT;
		edited.language = ModLanguage.SPANISH;
		assertTrue(mgr.applyAndSaveNow(edited));
		ConfigManager mgr2 = new ConfigManager(file);
		mgr2.load();
		assertEquals(1.37f, mgr2.getConfig().effects.scale);
		assertEquals(HudAnchor.TOP_RIGHT, mgr2.getConfig().effects.anchor);
		assertEquals(ModLanguage.SPANISH, mgr2.getConfig().language);
		assertFalse(mgr2.consumeRecoveryNotice());

		ConfigManager mgr3 = new ConfigManager(file);
		mgr3.load();
		assertFalse(mgr3.consumeRecoveryNotice());

		try {
			Files.writeString(file, "{ this is not json");
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
		ConfigManager mgr4 = new ConfigManager(file);
		mgr4.load();
		assertTrue(mgr4.consumeRecoveryNotice());
		try (var stream = Files.list(dir)) {
			assertTrue(stream.anyMatch(p -> {
				String n = p.getFileName().toString();
				return n.contains(".corrupt-") && n.endsWith(".bak");
			}));
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
		assertEquals(1.0f, mgr4.getConfig().effects.scale);
	}

	// ---------- §16/§17 formato y colores ----------
	@Test
	public void durationFormatting() {
		assertEquals("\u221e", DurationFormatter.formatTicks(100, 20f, true));
		assertEquals("0:42", DurationFormatter.formatTicks(840, 20f, false));
		assertEquals("1:00:05", DurationFormatter.formatTicks(72100, 20f, false));
		assertEquals("~0:42", DurationFormatter.formatTicks(840, 20f, false, true));
		assertEquals("0:42", DurationFormatter.formatTicks(840, 0.01f, false));
	}

	@Test
	public void durationColorThresholds() {
		assertEquals(0xFFD8DEE9, DurationFormatter.durationColorArgb(100, 20f, false, false));
		assertEquals(0xFFFF3B3B, DurationFormatter.durationColorArgb(200, 20f, false, true), "v1.2 rojo vívido");
		assertEquals(0xFFFFE600, DurationFormatter.durationColorArgb(600, 20f, false, true), "v1.2 amarillo vívido");
		assertEquals(0xFF00E5FF, DurationFormatter.durationColorArgb(-1, 20f, true, true), "v1.2 cian vívido");
		assertEquals(0xFFFFFFFF, DurationFormatter.durationColorArgb(6000, 20f, false, true), "v1.2 neutro blanco");
	}

	@Test
	public void romanNumeralsAndLevel() {
		assertEquals("I", RomanNumerals.format(1));
		assertEquals("IV", RomanNumerals.format(4));
		assertEquals("IX", RomanNumerals.format(9));
		assertEquals("XXXIX", RomanNumerals.format(39));
		assertEquals("II", RomanNumerals.formatLevel(1, LevelFormat.ROMAN, true));
		assertTrue(RomanNumerals.formatLevel(0, LevelFormat.ROMAN, true).isEmpty());
		assertEquals("2", RomanNumerals.formatLevel(1, LevelFormat.ARABIC, true));
	}

	// ---------- §20/§21 tracker ----------
	@Test
	public void observedMaxAndPurge() {
		EffectDurationTracker tracker = new EffectDurationTracker();
		EffectSnapshot s1 = snap("minecraft:speed", "Speed", EffectCategoryKind.BENEFICIAL, 1, 300, 0xFF00FF00);
		EffectSnapshot s1b = snap("minecraft:speed", "Speed", EffectCategoryKind.BENEFICIAL, 1, 200, 0xFF00FF00);
		tracker.syncAndObserve(List.of(s1));
		assertEquals(300, tracker.observedPeakTicks(s1b));
		tracker.syncAndObserve(List.of(s1b));
		assertEquals(300, tracker.observedPeakTicks(s1b));
		EffectSnapshot gone = snap("minecraft:jump_boost", "Jump", EffectCategoryKind.BENEFICIAL, 0, 100, 0xFF00FF00);
		tracker.syncAndObserve(List.of(gone));
		assertEquals(200, tracker.observedPeakTicks(s1b));
		EffectSnapshot inf = new EffectSnapshot("minecraft:night_vision", "NV", EffectCategoryKind.BENEFICIAL, 0, -1, true, false, true, true, 0xFF00FF00);
		tracker.syncAndObserve(List.of(inf));
		assertEquals(1.0f, tracker.remainingFraction(inf));
	}

	// ---------- §18 filtros y orden ----------
	@Test
	public void sortModesAndFilters() {
		EffectsConfig ord = new EffectsConfig();
		ord.sortMode = EffectSortMode.NAME_ASC;
		ord.groupByEffectCategory = false;
		EffectSnapshot b = snap("minecraft:blindness", "Blindness", EffectCategoryKind.HARMFUL, 0, 100, 0);
		EffectSnapshot s = snap("minecraft:speed", "Speed", EffectCategoryKind.BENEFICIAL, 0, 100, 0);
		assertEquals("minecraft:blindness", EffectOrderer.prepare(new ArrayList<>(List.of(s, b)), ord).get(0).id());

		ord.sortMode = EffectSortMode.DURATION_ASC;
		EffectSnapshot long1 = snap("minecraft:x", "Long", EffectCategoryKind.NEUTRAL, 0, 900, 0);
		EffectSnapshot short1 = snap("minecraft:y", "Short", EffectCategoryKind.NEUTRAL, 0, 50, 0);
		assertEquals("minecraft:y", EffectOrderer.prepare(new ArrayList<>(List.of(long1, short1)), ord).get(0).id());

		ord.sortMode = EffectSortMode.CATEGORY_THEN_NAME;
		ord.groupByEffectCategory = true;
		assertEquals("minecraft:speed", EffectOrderer.prepare(new ArrayList<>(List.of(b, s)), ord).get(0).id());

		ord.showEffectsWithoutParticles = false;
		EffectSnapshot noParticles = new EffectSnapshot("minecraft:z", "Z", EffectCategoryKind.NEUTRAL, 0, 10, false, false, false, false, 0);
		assertEquals(1, EffectOrderer.prepare(new ArrayList<>(List.of(s, noParticles)), ord).size());
	}

	// ---------- §27/§28 color override > automático > nativo ----------
	@Test
	public void colorOverridePriority() {
		EffectsConfig col = new EffectsConfig();
		col.colorDurationByRemaining = true;
		col.customEffectColors.put("minecraft:speed", new EffectColorOverride(true, "#00FF00"));
		assertEquals(0xFF00FF00, EffectsColorResolver.durationColorArgb(
				snap("minecraft:speed", "Speed", EffectCategoryKind.BENEFICIAL, 1, 200, 0xFF0000), col, 20f));
		assertEquals(0xFFFF3B3B, EffectsColorResolver.durationColorArgb(
				snap("minecraft:haste", "H", EffectCategoryKind.BENEFICIAL, 0, 100, 0x99AABB), col, 20f), "v1.2 rojo vívido");
		col.customEffectColors.put("minecraft:slowness", new EffectColorOverride(false, "#00FF00"));
		assertEquals(0xFFFF3B3B, EffectsColorResolver.durationColorArgb(
				snap("minecraft:slowness", "S", EffectCategoryKind.HARMFUL, 0, 50, 0), col, 20f));
	}

	// ---------- §12/§13/§25/§26 motor de layout ----------
	@Test
	public void layoutEngineGeometry() {
		EffectsConfig lay = new EffectsConfig();
		lay.anchor = HudAnchor.TOP_LEFT;
		lay.rowSpacing = 2;
		List<EffectRowMetric> metrics = List.of(new EffectRowMetric("a", 40, 20, 0), new EffectRowMetric("b", 30, 18, 0));
		EffectsLayoutResult vert = HudLayoutEngine.computeEffectsLayout(lay, metrics, 400, 300);
		assertEquals(2, vert.rows().size());
		assertTrue(vert.rows().get(1).unscaledY() > vert.rows().get(0).unscaledY());

		lay.orientation = HudOrientation.HORIZONTAL;
		EffectsLayoutResult hor = HudLayoutEngine.computeEffectsLayout(lay, metrics, 400, 300);
		assertTrue(hor.rows().get(1).unscaledX() > hor.rows().get(0).unscaledX());

		lay.anchor = HudAnchor.CUSTOM;
		lay.offsetX = -10000;
		lay.offsetY = 10;
		assertTrue(HudLayoutEngine.computeEffectsLayout(lay, metrics, 400, 300).bounds().x() >= 2);
		assertTrue(HudLayoutEngine.computeEffectsLayout(lay, List.of(), 400, 300).rows().isEmpty());
	}

	@Test
	public void snapResizeAndBounds() {
		HudLayoutEngine.SnapResult grid = HudLayoutEngine.snapPosition(101, 102, 20, 10, 400, 300, 4, true, false, List.of());
		assertEquals(0, grid.x() % 4);
		assertEquals(0, grid.y() % 4);
		HudLayoutEngine.SnapResult center = HudLayoutEngine.snapPosition(191, 102, 20, 10, 400, 300, 4, true, true, List.of());
		assertEquals(190, center.x());
		assertFalse(center.guides().isEmpty());

		HudLayoutEngine.ResizeResult grow = HudLayoutEngine.computeResizeFromCorner(
				new HudRect(10, 10, 50, 30), 50, 30, HudLayoutEngine.ResizeCorner.BOTTOM_RIGHT, 110, 70, 400, 300);
		assertTrue(grow.scale() > 1.0f);
		HudLayoutEngine.ResizeResult shrink = HudLayoutEngine.computeResizeFromCorner(
				new HudRect(10, 10, 200, 100), 200, 100, HudLayoutEngine.ResizeCorner.BOTTOM_RIGHT, 12, 12, 400, 300);
		assertEquals(0.5f, shrink.scale());

		assertTrue(HudLayoutEngine.hasAnyOverlap(List.of(new HudRect(0, 0, 10, 10), new HudRect(5, 5, 10, 10))));
		assertFalse(HudLayoutEngine.hasAnyOverlap(List.of(new HudRect(0, 0, 10, 10), new HudRect(20, 20, 10, 10))));
		assertEquals(14, HudAnchor.values().length, "13 predefinidos + CUSTOM (v1.2)");
	}

	@Test
	public void hudRectHelpers() {
		HudRect clamped = new HudRect(-50, -50, 20, 20).clampToScreen(400, 300, 2);
		assertTrue(clamped.x() >= 2 && clamped.y() >= 2);
		assertTrue(HudRect.EMPTY.isEmpty());
		assertTrue(new HudRect(0, 0, 10, 10).contains(5, 5));
		assertFalse(new HudRect(0, 0, 10, 10).contains(15, 15));
	}

	@Test
	public void deepCopyIndependence() {
		HudConfig src = new HudConfig();
		src.effects.customEffectColors.put("minecraft:luck", new EffectColorOverride(true, "#00FF00"));
		HudConfig cp = src.copy();
		cp.effects.customEffectColors.get("minecraft:luck").colorHex = "#0000FF";
		assertEquals("#00FF00", src.effects.customEffectColors.get("minecraft:luck").colorHex);
		cp.effects.scale = 2.0f;
		assertEquals(1.0f, src.effects.scale);
	}

	// ---------- §36 i18n ----------
	@Test
	public void languageFilesParityAndSpecTexts() throws Exception {
		Path base = Path.of("src/main/resources/assets/effecttimer/lang");
		if (!Files.isDirectory(base)) {
			base = Path.of("../src/main/resources/assets/effecttimer/lang");
		}
		JsonObject en = JsonParser.parseString(Files.readString(base.resolve("en_us.json"))).getAsJsonObject();
		JsonObject es = JsonParser.parseString(Files.readString(base.resolve("es_es.json"))).getAsJsonObject();
		for (String k : en.keySet()) {
			assertTrue(es.has(k), "falta clave ES: " + k);
		}
		for (String k : es.keySet()) {
			assertTrue(en.has(k), "falta clave EN: " + k);
		}
		assertTrue(en.get("effecttimer.tooltip.effects.fade_low_on").getAsString().contains("10 seconds"));
		assertTrue(es.get("effecttimer.tooltip.effects.fade_low_on").getAsString().contains("10 segundos"));
		assertTrue(en.get("effecttimer.tooltip.effects.color_duration_on").getAsString().contains("\u226410s"));
		assertTrue(es.get("effecttimer.tooltip.effects.color_duration_on").getAsString().contains("\u226410 s"));
		assertNotNull(en.get("key.category.effecttimer.general"));
	}

	private static EffectSnapshot snap(String id, String name, EffectCategoryKind cat, int amp, int ticks, int color) {
		return new EffectSnapshot(id, name, cat, amp, ticks, false, false, true, true, color);
	}
}
