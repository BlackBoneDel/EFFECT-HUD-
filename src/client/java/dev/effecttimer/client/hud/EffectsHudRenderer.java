package dev.effecttimer.client.hud;

import dev.effecttimer.client.EffectTimerMod;
import dev.effecttimer.config.ConfigValidator;
import dev.effecttimer.config.EffectsFramePolicy;
import dev.effecttimer.config.TextShadowRule;
import dev.effecttimer.config.EffectsConfig;
import dev.effecttimer.config.EffectsVisualStyle;
import dev.effecttimer.config.HudAnchor;
import dev.effecttimer.config.HudConfig;
import dev.effecttimer.config.ModLanguage;
import dev.effecttimer.config.PotionHudMode;
import dev.effecttimer.config.PotionMiniDisplayMode;
import dev.effecttimer.format.DurationFormatter;
import dev.effecttimer.format.EffectSnapshot;
import dev.effecttimer.format.EffectsColorResolver;
import dev.effecttimer.format.RomanNumerals;
import dev.effecttimer.layout.EffectRowMetric;
import dev.effecttimer.layout.EffectRowPlacement;
import dev.effecttimer.layout.EffectsLayoutResult;
import dev.effecttimer.layout.HudLayoutEngine;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import org.joml.Matrix3x2fStack;

public final class EffectsHudRenderer {
	private static final Identifier EFFECT_BACKGROUND_SPRITE = Identifier.withDefaultNamespace("hud/effect_background");
	private static final Identifier EFFECT_BACKGROUND_AMBIENT_SPRITE = Identifier.withDefaultNamespace("hud/effect_background_ambient");

	private EffectsHudRenderer() {
	}

	public static void renderLive(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft client = Minecraft.getInstance();
		HudConfig config = EffectTimerMod.getConfig();
		if (config == null || !VisibilityRules.shouldRenderCustomEffects(config)) {
			return;
		}
		if (!VisibilityRules.shouldRenderLiveHud(client, config.visibility)) {
			return;
		}
		render(graphics, client, config.effects, config.language, false, 1.0f);
	}

	public static EffectsLayoutResult renderForEditor(
			GuiGraphicsExtractor graphics,
			Minecraft client,
			EffectsConfig effectsConfig,
			ModLanguage language,
			float alpha
	) {
		return render(graphics, client, effectsConfig, language, true, alpha);
	}

	public static void renderPreviewInBox(
			GuiGraphicsExtractor graphics,
			Minecraft client,
			EffectsConfig effectsConfig,
			ModLanguage language,
			int boxX,
			int boxY,
			int boxW,
			int boxH,
			float alpha,
			String highlightEffectId
	) {
		if (client == null || effectsConfig == null || boxW <= 4 || boxH <= 4) {
			return;
		}
		EffectsConfig previewConfig = effectsConfig.copy();
		previewConfig.anchor = HudAnchor.TOP_LEFT;
		previewConfig.offsetX = 0;
		previewConfig.offsetY = 0;

		List<LiveDataCollector.EffectRenderEntry> entries = LiveDataCollector.collectEffectEntries(
				client,
				previewConfig,
				language,
				true,
				highlightEffectId
		);
		boolean usingSampleFallback = client == null || client.player == null || client.player.getActiveEffects().isEmpty();
		renderPreviewEntries(graphics, client, previewConfig, language, boxX, boxY, boxW, boxH, alpha, entries, usingSampleFallback);
	}

	/**
	 * Dibuja una lista EXACTA de entradas (reales o ficticias) dentro de la caja
	 * de vista previa. Lo usa el "Modo de Prueba" de la pantalla de configuración:
	 * puede inyectar pociones
	 * ficticias con cuenta regresiva en bucle aunque el jugador tenga efectos
	 * reales.
	 */
	public static void renderPreviewEntries(
			GuiGraphicsExtractor graphics,
			Minecraft client,
			EffectsConfig effectsConfig,
			ModLanguage language,
			int boxX,
			int boxY,
			int boxW,
			int boxH,
			float alpha,
			List<LiveDataCollector.EffectRenderEntry> entries,
			boolean usingSampleFallback
	) {
		if (client == null || effectsConfig == null || entries == null || entries.isEmpty() || boxW <= 4 || boxH <= 4) {
			return;
		}
		EffectsConfig previewConfig = effectsConfig.copy();
		previewConfig.anchor = HudAnchor.TOP_LEFT;
		previewConfig.offsetX = 0;
		previewConfig.offsetY = 0;
		Font font = client.font;
		float tps = LiveDataCollector.currentTicksPerSecond(client);
		List<PreparedRow> rows = prepareRows(previewConfig, entries, tps, usingSampleFallback);
		List<EffectRowMetric> metrics = computeMetrics(font, rows);
		EffectsLayoutResult layout = HudLayoutEngine.computeEffectsLayout(
				previewConfig,
				metrics,
				Math.max(320, boxW),
				Math.max(200, boxH)
		);
		if (layout.rows().isEmpty() || layout.unscaledWidth() <= 0 || layout.unscaledHeight() <= 0) {
			return;
		}

		float maxAvailW = Math.max(12.0f, boxW - 10.0f);
		float maxAvailH = Math.max(12.0f, boxH - 10.0f);
		float fitScale = Math.min(layout.scale(), Math.min(maxAvailW / layout.unscaledWidth(), maxAvailH / layout.unscaledHeight()));
		fitScale = Math.max(0.45f, fitScale);

		int drawW = Math.round(layout.unscaledWidth() * fitScale);
		int drawH = Math.round(layout.unscaledHeight() * fitScale);
		int drawX = boxX + Math.max(0, (boxW - drawW) / 2);
		int drawY = boxY + Math.max(0, (boxH - drawH) / 2);

		EffectsVisualStyle style = previewConfig.style != null ? previewConfig.style : EffectsVisualStyle.MINECRAFT_COMPACT;

		graphics.enableScissor(boxX, boxY, boxX + boxW, boxY + boxH);
		try {
			Matrix3x2fStack pose = graphics.pose();
			pose.pushMatrix();
			try {
				pose.translate(drawX, drawY);
				pose.scale(fitScale, fitScale);
				for (int i = 0; i < layout.rows().size() && i < rows.size(); i++) {
					drawRow(graphics, font, previewConfig, style, rows.get(i), layout.rows().get(i), tps, alpha);
				}
			} finally {
				pose.popMatrix();
			}
		} finally {
			graphics.disableScissor();
		}
	}

	private record PreparedRow(
			LiveDataCollector.EffectRenderEntry entry,
			String title,
			String duration,
			String levelPart,
			float durationFraction
	) {
	}

	private static List<EffectRowMetric> computeMetrics(Font font, List<PreparedRow> rows) {
		List<EffectRowMetric> metrics = new ArrayList<>(rows.size());
		for (PreparedRow row : rows) {
			metrics.add(new EffectRowMetric(
					row.entry().snapshot().id(),
					row.title().isEmpty() ? 0 : font.width(row.title()),
					row.duration().isEmpty() ? 0 : font.width(row.duration()),
					row.levelPart().isEmpty() ? 0 : font.width(row.levelPart())
			));
		}
		return metrics;
	}

	private static List<PreparedRow> prepareRows(
			EffectsConfig config,
			List<LiveDataCollector.EffectRenderEntry> entries,
			float tps,
			boolean previewSample
	) {
		List<PreparedRow> rows = new ArrayList<>(entries.size());
		for (LiveDataCollector.EffectRenderEntry entry : entries) {
			EffectSnapshot snap = entry.snapshot();
			String namePart = config.showName ? snap.safeName() : "";
			String levelPart = config.showLevel
					? RomanNumerals.formatLevel(snap.amplifier(), config.levelFormat, config.hideLevelOne)
					: "";
			String title;
			if (!namePart.isEmpty() && !levelPart.isEmpty()) {
				title = namePart + " " + levelPart;
			} else if (!namePart.isEmpty()) {
				title = namePart;
			} else {
				title = levelPart;
			}
			String duration = config.showDuration
					? DurationFormatter.formatTicks(snap.durationTicks(), tps, snap.infinite(), snap.ticksSuspended())
					: "";
			float durationFraction = LiveDataCollector.remainingDurationFraction(snap, previewSample);
			rows.add(new PreparedRow(entry, title, duration, levelPart, durationFraction));
		}
		return rows;
	}

	private static EffectsLayoutResult render(
			GuiGraphicsExtractor graphics,
			Minecraft client,
			EffectsConfig config,
			ModLanguage language,
			boolean previewFallback,
			float alpha
	) {
		List<LiveDataCollector.EffectRenderEntry> entries = LiveDataCollector.collectEffectEntries(client, config, language, previewFallback);
		if (entries.isEmpty()) {
			return HudLayoutEngine.computeEffectsLayout(config, List.of(), graphics.guiWidth(), graphics.guiHeight());
		}
		boolean usingSampleFallback = previewFallback && (client.player == null || client.player.getActiveEffects().isEmpty());
		Font font = client.font;
		float tps = LiveDataCollector.currentTicksPerSecond(client);
		List<PreparedRow> rows = prepareRows(config, entries, tps, usingSampleFallback);
		List<EffectRowMetric> metrics = computeMetrics(font, rows);
		EffectsLayoutResult layout = HudLayoutEngine.computeEffectsLayout(config, metrics, graphics.guiWidth(), graphics.guiHeight());
		if (layout.rows().isEmpty()) {
			return layout;
		}

		float safeAlpha = Math.max(0.0f, Math.min(1.0f, alpha));
		Matrix3x2fStack pose = graphics.pose();
		pose.pushMatrix();
		try {
			pose.translate(layout.originX(), layout.originY());
			pose.scale(layout.scale(), layout.scale());

			EffectsVisualStyle style = config.style != null ? config.style : EffectsVisualStyle.MINECRAFT_COMPACT;

			for (int i = 0; i < layout.rows().size() && i < rows.size(); i++) {
				EffectRowPlacement placement = layout.rows().get(i);
				PreparedRow row = rows.get(i);
				drawRow(graphics, font, config, style, row, placement, tps, safeAlpha);
			}
		} finally {
			pose.popMatrix();
		}
		return layout;
	}

	private static void drawRow(
			GuiGraphicsExtractor graphics,
			Font font,
			EffectsConfig config,
			EffectsVisualStyle style,
			PreparedRow row,
			EffectRowPlacement placement,
			float tps,
			float alpha
	) {
		EffectSnapshot snap = row.entry().snapshot();
		int x = placement.unscaledX();
		int y = placement.unscaledY();
		int w = placement.unscaledWidth();
		int h = placement.unscaledHeight();

		boolean drawBg = config.showEffectBackground;
		// Sin fondo no hay marco: el borde guardado se conserva pero solo
		// se dibuja con el fondo activado.
		boolean drawBorder = EffectsFramePolicy.drawFrame(config);
		boolean shadow = TextShadowRule.shadow(config);
		float bgOpacity = ConfigValidator.clampInt(
				config.backgroundOpacityPercent,
				ConfigValidator.MIN_OPACITY_PERCENT,
				ConfigValidator.MAX_OPACITY_PERCENT
		) / 100.0f;

		if (style == EffectsVisualStyle.POTION_HUD) {
			drawPotionHudRow(graphics, font, config, row, placement, tps, alpha, drawBg, bgOpacity, drawBorder);
			return;
		}

		int iconBox = config.showIcon ? (style == EffectsVisualStyle.MINIMAL_TEXT ? 14 : 20) : 0;
		int padX = style == EffectsVisualStyle.MINIMAL_TEXT ? 2 : 4;

		switch (style) {
			case MINECRAFT_COMPACT -> {
				if (drawBg && bgOpacity > 0.0f) {
					int bg = ARGB.colorFromFloat(bgOpacity * alpha, 0.07f, 0.09f, 0.12f);
					graphics.fill(x, y, x + w, y + h, bg);
				}
				if (drawBorder) {
					int border = snap.ambient()
							? ARGB.colorFromFloat(0.75f * alpha, 0.35f, 0.65f, 0.85f)
							: ARGB.colorFromFloat(0.68f * alpha, 0.25f, 0.30f, 0.38f);
					graphics.outline(x, y, w, h, border);
				}
			}
			case CLEAN_CARD -> {
				if (drawBg && bgOpacity > 0.0f) {
					int bg = ARGB.colorFromFloat(bgOpacity * alpha, 0.05f, 0.07f, 0.10f);
					graphics.fill(x, y, x + w, y + h, bg);
				}
				if (drawBorder) {
					int border = ARGB.colorFromFloat(0.55f * alpha, 0.22f, 0.27f, 0.35f);
					graphics.outline(x, y, w, h, border);
				}
				// El acento lateral es decorativo (no es marco): se mantiene
				// aunque no haya fondo ni borde .
				int accent = ARGB.multiplyAlpha(EffectsColorResolver.accentColorArgb(snap, config), alpha);
				graphics.fill(x + 1, y + 1, x + 3, y + h - 1, accent);
			}
			case MINIMAL_TEXT -> {
				if (drawBg && bgOpacity > 0.0f) {
					int bg = ARGB.colorFromFloat(bgOpacity * 0.65f * alpha, 0.0f, 0.0f, 0.0f);
					graphics.fill(x, y, x + w, y + h, bg);
				}
				if (drawBorder) {
					int border = ARGB.colorFromFloat(0.55f * alpha, 0.35f, 0.42f, 0.54f);
					graphics.outline(x, y, w, h, border);
				}
			}
			case POTION_HUD -> {
			}
		}

		if (config.showIcon && iconBox > 0) {
			int iconBoxX = x + padX;
			int iconBoxY = y + (h - iconBox) / 2;
			if (style == EffectsVisualStyle.MINECRAFT_COMPACT && drawBg && bgOpacity > 0.0f) {
				Identifier frameSprite = snap.ambient() ? EFFECT_BACKGROUND_AMBIENT_SPRITE : EFFECT_BACKGROUND_SPRITE;
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, frameSprite, iconBoxX, iconBoxY, iconBox, iconBox, ARGB.white(bgOpacity * alpha));
			}
			int spriteSize = style == EffectsVisualStyle.MINIMAL_TEXT ? 12 : 16;
			int spriteX = iconBoxX + (iconBox - spriteSize) / 2;
			int spriteY = iconBoxY + (iconBox - spriteSize) / 2;
			float fade = computeLowDurationFade(config, snap) * alpha;
			drawEffectIcon(graphics, row.entry().holder(), snap, spriteX, spriteY, spriteSize, fade);
		}

		int textX = x + padX + (iconBox > 0 ? iconBox + 4 : 0);
		String title = row.title();
		String duration = row.duration();
		int titleColor = ARGB.multiplyAlpha(EffectsColorResolver.titleColorArgb(snap, config), alpha);
		int durationColor = snap.ticksSuspended()
				? ARGB.color(alpha, 0xFF94A3B8)
				: ARGB.multiplyAlpha(EffectsColorResolver.durationColorArgb(snap, config, tps), alpha);

		if (style == EffectsVisualStyle.MINIMAL_TEXT) {
			int textY = y + Math.max(0, (h - font.lineHeight) / 2);
			int cursorX = textX;
			if (!title.isEmpty()) {
				graphics.text(font, title, cursorX, textY, titleColor, shadow);
				cursorX += font.width(title) + 6;
			}
			if (!duration.isEmpty()) {
				graphics.text(font, duration, cursorX, textY, durationColor, shadow);
			}
		} else {
			int lines = (title.isEmpty() ? 0 : 1) + (duration.isEmpty() ? 0 : 1);
			int textBlockH = lines == 2 ? (font.lineHeight * 2 + 1) : font.lineHeight;
			int textY = y + Math.max(0, (h - textBlockH) / 2);
			if (!title.isEmpty()) {
				graphics.text(font, title, textX, textY, titleColor, shadow);
				textY += font.lineHeight + 1;
			}
			if (!duration.isEmpty()) {
				graphics.text(font, duration, textX, textY, durationColor, shadow);
			}
		}
	}

	private static void drawPotionHudRow(
			GuiGraphicsExtractor graphics,
			Font font,
			EffectsConfig config,
			PreparedRow row,
			EffectRowPlacement placement,
			float tps,
			float alpha,
			boolean drawBg,
			float bgOpacity,
			boolean drawBorder
	) {
		EffectSnapshot snap = row.entry().snapshot();
		int x = placement.unscaledX();
		int y = placement.unscaledY();
		int w = placement.unscaledWidth();
		int h = placement.unscaledHeight();
		boolean shadow = TextShadowRule.shadow(config);
		PotionHudMode mode = config.potionHudMode != null ? config.potionHudMode : PotionHudMode.NORMAL;
		PotionMiniDisplayMode miniDisplay = config.potionMiniDisplay != null ? config.potionMiniDisplay : PotionMiniDisplayMode.TIMER_TEXT;

		if (drawBg && bgOpacity > 0.0f) {
			int bg = ARGB.colorFromFloat(bgOpacity * alpha, 0.06f, 0.08f, 0.12f);
			graphics.fill(x, y, x + w, y + h, bg);
		}
		if (drawBorder) {
			int border = snap.ambient()
					? ARGB.colorFromFloat(0.75f * alpha, 0.35f, 0.65f, 0.85f)
					: ARGB.colorFromFloat(0.58f * alpha, 0.24f, 0.30f, 0.39f);
			graphics.outline(x, y, w, h, border);
		}

		int accentArgb = ARGB.multiplyAlpha(EffectsColorResolver.accentColorArgb(snap, config), alpha);
		int titleColor = ARGB.multiplyAlpha(EffectsColorResolver.titleColorArgb(snap, config), alpha);
		int durationColor = snap.ticksSuspended()
				? ARGB.color(alpha, 0xFF94A3B8)
				: ARGB.multiplyAlpha(EffectsColorResolver.durationColorArgb(snap, config, tps), alpha);

		if (mode == PotionHudMode.MINI) {
			int padY = 2;
			if (config.showIcon) {
				int iconX = x + Math.max(0, (w - 16) / 2);
				int iconY = y + padY;
				float fade = computeLowDurationFade(config, snap) * alpha;
				drawEffectIcon(graphics, row.entry().holder(), snap, iconX, iconY, 16, fade);

				if (config.showLevel && !row.levelPart().isEmpty()) {
					float lvlScale = 0.68f;
					int lvlW = font.width(row.levelPart());
					Matrix3x2fStack pose = graphics.pose();
					pose.pushMatrix();
					try {
						pose.translate(iconX + 16, iconY + 10);
						pose.scale(lvlScale, lvlScale);
						graphics.text(font, row.levelPart(), -lvlW, 0, titleColor, shadow);
					} finally {
						pose.popMatrix();
					}
				}
			} else if (config.showLevel && !row.levelPart().isEmpty()) {
				float lvlScale = 0.78f;
				int lvlW = font.width(row.levelPart());
				Matrix3x2fStack pose = graphics.pose();
				pose.pushMatrix();
				try {
					pose.translate(x + (w / 2.0f), y + padY);
					pose.scale(lvlScale, lvlScale);
					graphics.text(font, row.levelPart(), -Math.round(lvlW / 2.0f), 0, titleColor, shadow);
				} finally {
					pose.popMatrix();
				}
			}

			if (config.showDuration) {
				if (miniDisplay == PotionMiniDisplayMode.DURATION_BAR) {
					int barW = Math.min(20, Math.max(12, w - 4));
					int barX = x + Math.max(0, (w - barW) / 2);
					int barY = y + h - 5;
					drawPotionDurationBar(graphics, barX, barY, barW, 3, snap, row.durationFraction(), accentArgb, durationColor, alpha);
				} else if (!row.duration().isEmpty()) {
					float durScale = 0.78f;
					int durW = font.width(row.duration());
					int durY = y + h - 8;
					Matrix3x2fStack pose = graphics.pose();
					pose.pushMatrix();
					try {
						pose.translate(x + (w / 2.0f), durY);
						pose.scale(durScale, durScale);
						graphics.text(font, row.duration(), -Math.round(durW / 2.0f), 0, durationColor, shadow);
					} finally {
						pose.popMatrix();
					}
				}
			}
			return;
		}

		// PotionHUD NORMAL mode: sleek compact row with optional thin left accent bar
		int padX = 3;
		if (drawBg || drawBorder) {
			graphics.fill(x + 1, y + 1, x + 3, y + h - 1, accentArgb);
		}
		int iconBox = config.showIcon ? 18 : 0;
		if (iconBox > 0) {
			int iconBoxX = x + padX + 2;
			int iconBoxY = y + (h - iconBox) / 2;
			int spriteX = iconBoxX + (iconBox - 16) / 2;
			int spriteY = iconBoxY + (iconBox - 16) / 2;
			float fade = computeLowDurationFade(config, snap) * alpha;
			drawEffectIcon(graphics, row.entry().holder(), snap, spriteX, spriteY, 16, fade);
		}

		int textX = x + padX + 2 + (iconBox > 0 ? iconBox + 4 : 0);
		String title = row.title();
		String duration = row.duration();
		int lines = (title.isEmpty() ? 0 : 1) + (duration.isEmpty() ? 0 : 1);
		int textBlockH = lines == 2 ? (font.lineHeight * 2 + 1) : font.lineHeight;
		int textY = y + Math.max(0, (h - textBlockH) / 2);
		if (!title.isEmpty()) {
			graphics.text(font, title, textX, textY, titleColor, shadow);
			textY += font.lineHeight + 1;
		}
		if (!duration.isEmpty()) {
			graphics.text(font, duration, textX, textY, durationColor, shadow);
		}
	}

	private static void drawPotionDurationBar(
			GuiGraphicsExtractor graphics,
			int x,
			int y,
			int w,
			int h,
			EffectSnapshot snap,
			float durationFraction,
			int accentArgb,
			int durationColorArgb,
			float alpha
	) {
		graphics.fill(x, y, x + w, y + h, ARGB.color(0.85f * alpha, 0xFF0D1117));
		int innerW = Math.max(0, w - 2);
		if (innerW <= 0) {
			return;
		}
		if (snap.ticksSuspended()) {
			// Suspended / frozen ticks: show paused bar with center pause notch instead of claiming live countdown
			int pausedFillW = Math.max(1, Math.min(innerW, Math.round(innerW * durationFraction)));
			int pausedColor = ARGB.color(alpha, 0xFF7C93A8);
			graphics.fill(x + 1, y + 1, x + 1 + pausedFillW, y + h - 1, pausedColor);
			int mid = x + w / 2;
			if (pausedFillW >= 5) {
				graphics.fill(mid, y + 1, mid + 1, y + h - 1, ARGB.color(alpha, 0xFF0D1117));
			}
			return;
		}
		if (snap.infinite() || snap.durationTicks() < 0) {
			graphics.fill(x + 1, y + 1, x + 1 + innerW, y + h - 1, ARGB.color(alpha, 0xFF7CE8FF));
			return;
		}
		int fillW = Math.max(0, Math.min(innerW, Math.round(innerW * durationFraction)));
		int fillColor = durationColorArgb != 0 ? durationColorArgb : accentArgb;
		if (fillW > 0) {
			graphics.fill(x + 1, y + 1, x + 1 + fillW, y + h - 1, fillColor);
		}
	}

	private static void drawEffectIcon(
			GuiGraphicsExtractor graphics,
			Holder<MobEffect> holder,
			EffectSnapshot snap,
			int x,
			int y,
			int size,
			float alpha
	) {
		if (holder != null && holder.unwrapKey().isPresent()) {
			Identifier sprite = Hud.getMobEffectSprite(holder);
			if (!MissingTextureAtlasSprite.getLocation().equals(sprite)) {
				// Never tint registered mob effect sprites; keep original texture colors intact.
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, size, size, EffectsColorResolver.iconTintArgb(alpha));
				return;
			}
		}
		int fill = ARGB.color(alpha, ARGB.opaque(EffectsColorResolver.resolveNativeDisplayRgb(snap.colorRgb())));
		int border = ARGB.color(alpha, 0xFFFFFFFF);
		graphics.fill(x + 1, y + 1, x + size - 1, y + size - 1, fill);
		graphics.outline(x + 1, y + 1, size - 2, size - 2, border);
	}

	private static float computeLowDurationFade(EffectsConfig config, EffectSnapshot snap) {
		if (!config.fadeOnLowDuration || snap.ambient() || snap.infinite() || snap.durationTicks() > 200) {
			return 1.0f;
		}
		int duration = Math.max(0, snap.durationTicks());
		int factor = 10 - duration / 20;
		float f = Mth.clamp(duration / 10.0F / 5.0F * 0.5F, 0.0F, 0.5F)
				+ Mth.cos(duration * (float) Math.PI / 5.0F) * Mth.clamp(factor / 10.0F * 0.25F, 0.0F, 0.25F);
		return Mth.clamp(f, 0.25f, 1.0F);
	}
}
