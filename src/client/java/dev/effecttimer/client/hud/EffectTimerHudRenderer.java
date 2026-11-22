package dev.effecttimer.client.hud;

import dev.effecttimer.client.EffectTimerMod;
import dev.effecttimer.client.config.EffectTimerConfig;
import dev.effecttimer.client.config.Orientation;
import dev.effecttimer.client.format.DurationFormatter;
import dev.effecttimer.client.format.LevelNames;
import dev.effecttimer.client.screen.EffectTimerConfigScreen;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import org.joml.Matrix3x2fStack;

/**
 * Renderizador del Effect Timer, extraído de {@code EffectsHudRenderer} +
 * {@code LiveDataCollector} + {@code EffectOrderer} + {@code EffectsColorResolver}
 * del módulo "Effects HUD" (customizablehud) y aislado del resto del mod.
 *
 * <p>Conserva las técnicas originales: íconos vía {@code Gui.getMobEffectSprite} +
 * {@code blitSprite} sin teñir, parpadeo de expiración con la fórmula de fade del
 * original (umbral de 10 s), escala vía {@code Matrix3x2fStack} y anclaje tras
 * {@code VanillaHudElements.STATUS_EFFECTS}. Se eliminaron los estilos PotionHUD,
 * fondos/bordes configurables, colores por efecto y presets, por no pertenecer a
 * los parámetros del Effect Timer.</p>
 */
public final class EffectTimerHudRenderer {
	private static final int ROW_HEIGHT = 20;
	private static final int ROW_SPACING = 2;
	private static final int ICON_BOX = 20;
	private static final int ICON_SIZE = 16;
	private static final int PAD_X = 4;
	private static final int TEXT_GAP = 4;
	/** Umbral de parpadeo: 10 segundos (200 ticks), igual que el fade original. */
	private static final int BLINK_THRESHOLD_TICKS = 200;
	/** Color de relleno para efectos sin sprite registrado (fallback del resolver original). */
	private static final int UNREADABLE_FALLBACK_RGB = 0xE6EDF3;

	/** Una fila lista para dibujar (modelo reducido del EffectSnapshot original). */
	public record Row(
			Holder<MobEffect> holder,
			String name,
			int amplifier,
			int durationTicks,
			boolean infinite,
			int nativeColorRgb
	) {
	}

	private EffectTimerHudRenderer() {
	}

	/**
	 * Hook registrado tras el HUD vanilla de efectos. Optimización del mod
	 * original (Fase 5): si no hay nada que dibujar, devuelve inmediatamente
	 * tras unas pocas comprobaciones de campos → coste ~0%.
	 */
	public static void renderLive(GuiGraphics graphics, DeltaTracker deltaTracker) {
		EffectTimerConfig config = EffectTimerMod.getConfig();
		if (config == null || !config.enabled) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (client == null || client.player == null || client.options == null || client.options.hideGui) {
			return;
		}
		// La pantalla de configuración dibuja su propia copia; evita doble pasada.
		if (client.screen instanceof EffectTimerConfigScreen) {
			return;
		}
		Collection<MobEffectInstance> activeEffects = client.player.getActiveEffects();
		if (activeEffects.isEmpty()) {
			return;
		}
		List<Row> rows = collectLiveRows(activeEffects);
		if (rows.isEmpty()) {
			return;
		}
		drawRows(graphics, config, client.font, rows, currentTicksPerSecond(client));
	}

	/** Recoge los StatusEffects reales del jugador (orden heredado: benéficos → neutros → perjudiciales, por nombre). */
	public static List<Row> collectLiveRows(Minecraft client) {
		if (client == null || client.player == null) {
			return List.of();
		}
		return collectLiveRows(client.player.getActiveEffects());
	}

	private static List<Row> collectLiveRows(Collection<MobEffectInstance> instances) {
		List<Row> rows = new ArrayList<>(instances.size());
		for (MobEffectInstance instance : instances) {
			if (instance == null || instance.getEffect() == null) {
				continue;
			}
			Holder<MobEffect> holder = instance.getEffect();
			MobEffect effect = holder.value();
			if (effect == null) {
				continue;
			}
			rows.add(new Row(
					holder,
					Component.translatable(effect.getDescriptionId()).getString(),
					instance.getAmplifier(),
					instance.getDuration(),
					instance.isInfiniteDuration(),
					effect.getColor()
			));
		}
		sortByCategoryThenName(rows);
		return rows;
	}

	/**
	 * Pociones ficticias del modo de prueba (Fase 4): "Velocidad II - 02:15",
	 * "Fuerza - 00:45" y "Regeneración - 00:10" contando hacia atrás en tiempo
	 * real y reapareciendo en bucle para calibrar tamaño, color, parpadeo y
	 * posición sin beber pociones reales.
	 */
	public static List<Row> createPreviewRows(long elapsedTicks) {
		PreviewSpec[] specs = {
				new PreviewSpec("minecraft:speed", 1, 2700),         // 02:15
				new PreviewSpec("minecraft:strength", 0, 900),       // 00:45
				new PreviewSpec("minecraft:regeneration", 0, 200)    // 00:10 → zona de parpadeo
		};
		List<Row> rows = new ArrayList<>(specs.length);
		for (PreviewSpec spec : specs) {
			long cycle = spec.baseTicks() + 20L; // 1 s de pausa antes de reaparecer (demo de entrada/salida)
			long remaining = spec.baseTicks() - elapsedTicks % cycle;
			if (remaining <= 0L) {
				continue;
			}
			Holder<MobEffect> holder = null;
			String name = spec.effectId();
			int color = 0;
			Identifier parsedId = Identifier.tryParse(spec.effectId());
			if (parsedId != null) {
				Optional<Holder.Reference<MobEffect>> holderOpt = BuiltInRegistries.MOB_EFFECT.get(parsedId);
				if (holderOpt.isPresent()) {
					holder = holderOpt.get();
					MobEffect effect = holder.value();
					name = Component.translatable(effect.getDescriptionId()).getString();
					color = effect.getColor();
				}
			}
			rows.add(new Row(holder, name, spec.amplifier(), (int) remaining, false, color));
		}
		return rows;
	}

	private record PreviewSpec(String effectId, int amplifier, int baseTicks) {
	}

	private static void sortByCategoryThenName(List<Row> rows) {
		rows.sort(Comparator
				.comparingInt(EffectTimerHudRenderer::categoryPriority)
				.thenComparing(row -> row.name() == null ? "" : row.name(), String.CASE_INSENSITIVE_ORDER));
	}

	private static int categoryPriority(Row row) {
		if (row.holder() == null) {
			return 1;
		}
		MobEffect effect = row.holder().value();
		MobEffectCategory category = effect == null ? null : effect.getCategory();
		if (category == MobEffectCategory.BENEFICIAL) {
			return 0;
		}
		if (category == MobEffectCategory.HARMFUL) {
			return 2;
		}
		return 1;
	}

	/** Ticks/segundos reales del mundo (el original ajustaba el formato al tickrate). */
	public static float currentTicksPerSecond(Minecraft client) {
		if (client != null && client.level != null) {
			return Math.max(1.0F, client.level.tickRateManager().tickrate());
		}
		return 20.0F;
	}

	/**
	 * Dibuja las filas en la posición/escala configuradas. Réplica de la pasada
	 * de dibujo del renderizador original (translate + scale sobre el pose
	 * stack), con el origen recortado contra los bordes de pantalla.
	 */
	public static void drawRows(GuiGraphics graphics, EffectTimerConfig config, Font font, List<Row> rows, float ticksPerSecond) {
		if (graphics == null || config == null || font == null || rows == null || rows.isEmpty()) {
			return;
		}
		if (!config.showIcons && !config.showDuration && !config.showName) {
			return; // Nada visible que dibujar.
		}

		Orientation orientation = config.orientation != null ? config.orientation : Orientation.VERTICAL;
		List<String> labels = new ArrayList<>(rows.size());
		List<Integer> rowWidths = new ArrayList<>(rows.size());
		int totalW = 0;
		for (Row row : rows) {
			String label = composeLabel(config, row, ticksPerSecond);
			int labelW = label.isEmpty() ? 0 : font.width(label);
			int rowW = labelW + (config.showIcons ? ICON_BOX + TEXT_GAP : 0) + PAD_X * 2;
			labels.add(label);
			rowWidths.add(rowW);
			totalW = orientation == Orientation.VERTICAL ? Math.max(totalW, rowW) : totalW + rowW;
		}
		int totalH;
		if (orientation == Orientation.HORIZONTAL) {
			totalW += ROW_SPACING * (rows.size() - 1);
			totalH = ROW_HEIGHT;
		} else {
			totalH = rows.size() * ROW_HEIGHT + ROW_SPACING * (rows.size() - 1);
		}
		if (totalW <= 0 || totalH <= 0) {
			return;
		}

		float scale = Mth.clamp(
				Float.isFinite(config.scale) ? config.scale : 1.0F,
				EffectTimerConfig.MIN_SCALE,
				EffectTimerConfig.MAX_SCALE
		);
		int scaledW = Math.round(totalW * scale);
		int scaledH = Math.round(totalH * scale);
		int originX = Mth.clamp(config.posX, 0, Math.max(0, graphics.guiWidth() - scaledW - 2));
		int originY = Mth.clamp(config.posY, 0, Math.max(0, graphics.guiHeight() - scaledH - 2));

		Matrix3x2fStack pose = graphics.pose();
		pose.pushMatrix();
		try {
			pose.translate(originX, originY);
			pose.scale(scale, scale);

			int cursor = 0;
			for (int i = 0; i < rows.size(); i++) {
				Row row = rows.get(i);
				String label = labels.get(i);
				int rowW = rowWidths.get(i);
				int x = orientation == Orientation.HORIZONTAL ? cursor : 0;
				int y = orientation == Orientation.VERTICAL ? cursor : 0;
				drawRow(graphics, font, config, row, label, x, y);
				cursor += (orientation == Orientation.HORIZONTAL ? rowW : ROW_HEIGHT) + ROW_SPACING;
			}
		} finally {
			pose.popMatrix();
		}
	}

	private static String composeLabel(EffectTimerConfig config, Row row, float ticksPerSecond) {
		StringBuilder sb = new StringBuilder();
		if (config.showName) {
			String name = row.name() == null || row.name().isBlank() ? "Unknown" : row.name();
			sb.append(LevelNames.withLevel(name, row.amplifier()));
		}
		if (config.showDuration) {
			String duration = DurationFormatter.formatTicks(row.durationTicks(), ticksPerSecond, row.infinite());
			if (!sb.isEmpty()) {
				sb.append(" - ");
			}
			sb.append(duration);
		}
		return sb.toString();
	}

	private static void drawRow(GuiGraphics graphics, Font font, EffectTimerConfig config, Row row, String label, int x, int y) {
		float blink = computeExpireBlink(config, row);

		int textX = x + PAD_X;
		if (config.showIcons) {
			int iconBoxX = x + PAD_X;
			int iconBoxY = y + (ROW_HEIGHT - ICON_BOX) / 2;
			int spriteX = iconBoxX + (ICON_BOX - ICON_SIZE) / 2;
			int spriteY = iconBoxY + (ICON_BOX - ICON_SIZE) / 2;
			drawEffectIcon(graphics, row, spriteX, spriteY, ICON_SIZE, blink);
			textX += ICON_BOX + TEXT_GAP;
		}

		if (!label.isEmpty()) {
			int textColor = ARGB.multiplyAlpha(config.textColorArgb(), blink);
			int textY = y + Math.max(0, (ROW_HEIGHT - font.lineHeight) / 2);
			graphics.drawString(font, label, textX, textY, textColor, true);
		}
	}

	/**
	 * Parpadeo al expirar (menos de 10 s): fórmula de fade del {@code
	 * computeLowDurationFade} original del mod (conservada tal cual, Fase 3.1.9).
	 */
	private static float computeExpireBlink(EffectTimerConfig config, Row row) {
		if (!config.blinkOnExpire || row.infinite() || row.durationTicks() > BLINK_THRESHOLD_TICKS) {
			return 1.0F;
		}
		int duration = Math.max(0, row.durationTicks());
		int factor = 10 - duration / 20;
		float f = Mth.clamp(duration / 10.0F / 5.0F * 0.5F, 0.0F, 0.5F)
				+ Mth.cos(duration * (float) Math.PI / 5.0F) * Mth.clamp(factor / 10.0F * 0.25F, 0.0F, 0.25F);
		return Mth.clamp(f, 0.25F, 1.0F);
	}

	/** Ícono real del efecto vía atlas de sprites; nunca se tiñe (idéntico al original). */
	private static void drawEffectIcon(GuiGraphics graphics, Row row, int x, int y, int size, float alpha) {
		Holder<MobEffect> holder = row.holder();
		if (holder != null && holder.unwrapKey().isPresent()) {
			Identifier sprite = Gui.getMobEffectSprite(holder);
			if (!MissingTextureAtlasSprite.getLocation().equals(sprite)) {
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, size, size, ARGB.white(alpha));
				return;
			}
		}
		int fill = ARGB.color(alpha, ARGB.opaque(resolveNativeDisplayRgb(row.nativeColorRgb())));
		int border = ARGB.color(alpha, 0xFFFFFFFF);
		graphics.fill(x + 1, y + 1, x + size - 1, y + size - 1, fill);
		graphics.renderOutline(x + 1, y + 1, size - 2, size - 2, border);
	}

	/**
	 * Garantiza legibilidad del color nativo (recortado de {@code
	 * EffectsColorResolver.resolveNativeDisplayRgb}, usado por el fallback del
	 * ícono del original).
	 */
	private static int resolveNativeDisplayRgb(int rawColorRgb) {
		int rgb = rawColorRgb & 0xFFFFFF;
		if (rgb == 0) {
			return UNREADABLE_FALLBACK_RGB;
		}
		int r = (rgb >> 16) & 0xFF;
		int g = (rgb >> 8) & 0xFF;
		int b = rgb & 0xFF;
		float lum = (0.2126F * r + 0.7152F * g + 0.0722F * b) / 255.0F;
		if (lum >= 0.34F) {
			return rgb;
		}
		float blend = Mth.clamp((0.44F - lum) / Math.max(0.05F, 1.0F - lum), 0.0F, 0.75F);
		int nr = Math.min(255, Math.round(r + (255 - r) * blend));
		int ng = Math.min(255, Math.round(g + (255 - g) * blend));
		int nb = Math.min(255, Math.round(b + (255 - b) * blend));
		return (nr << 16) | (ng << 8) | nb;
	}
}
