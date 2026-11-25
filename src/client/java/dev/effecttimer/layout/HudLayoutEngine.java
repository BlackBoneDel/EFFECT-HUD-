package dev.effecttimer.layout;

import dev.effecttimer.config.ConfigValidator;
import dev.effecttimer.config.EffectsConfig;
import dev.effecttimer.config.EffectsVisualStyle;
import dev.effecttimer.config.HudAnchor;
import dev.effecttimer.config.HudOrientation;
import dev.effecttimer.config.PotionHudMode;
import dev.effecttimer.config.PotionMiniDisplayMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Motor de layout del HUD de efectos. {@link #computeEffectsLayout} replica la
 * geometría original del PotionHUD: cajas por fila, ancho unificado en vertical
 * (excepto POTION_HUD MINI, que centra sus filas), escalado sobre el origen del
 * anclaje y clamp a pantalla.
 */
public final class HudLayoutEngine {
	public static final int UNSCALED_LINE_HEIGHT = 9;
	private static final int SNAP_THRESHOLD = 4;

	private HudLayoutEngine() {
	}

	public enum ResizeCorner {
		TOP_LEFT,
		TOP_RIGHT,
		BOTTOM_LEFT,
		BOTTOM_RIGHT
	}

	public record ResizeResult(
			float scale,
			int offsetX,
			int offsetY,
			HudRect bounds
	) {
	}

	public record SnapResult(int x, int y, List<GuideLine> guides) {
	}

	public static HudRect resolveAnchorRect(
			HudAnchor anchor,
			int offsetX,
			int offsetY,
			int scaledWidth,
			int scaledHeight,
			int screenWidth,
			int screenHeight
	) {
		HudAnchor safeAnchor = anchor != null ? anchor : HudAnchor.TOP_LEFT;
		int baseX;
		int baseY;
		switch (safeAnchor) {
			case SIDE_HIGH_LEFT -> {
				baseX = 8;
				baseY = screenHeight / 4 - scaledHeight / 2;
			}
			case SIDE_HIGH_RIGHT -> {
				baseX = screenWidth - scaledWidth - 8;
				baseY = screenHeight / 4 - scaledHeight / 2;
			}
			case SIDE_LOW_LEFT -> {
				baseX = 8;
				baseY = screenHeight * 3 / 4 - scaledHeight / 2;
			}
			case SIDE_LOW_RIGHT -> {
				baseX = screenWidth - scaledWidth - 8;
				baseY = screenHeight * 3 / 4 - scaledHeight / 2;
			}
			case HOTBAR_LEFT -> {
				baseX = screenWidth / 2 - 124 - scaledWidth;
				baseY = screenHeight - scaledHeight - 2;
			}
			case HOTBAR_RIGHT -> {
				baseX = screenWidth / 2 + 122;
				baseY = screenHeight - scaledHeight - 2;
			}
			case CENTER_LEFT -> {
				baseX = 8;
				baseY = (screenHeight - scaledHeight) / 2;
			}
			case CENTER_RIGHT -> {
				baseX = screenWidth - scaledWidth - 8;
				baseY = (screenHeight - scaledHeight) / 2;
			}
			case CENTER_BOTTOM -> {
				baseX = (screenWidth - scaledWidth) / 2;
				baseY = screenHeight - 58 - scaledHeight;
			}
			case TOP_LEFT -> {
				baseX = 6;
				baseY = 6;
			}
			case TOP_RIGHT -> {
				baseX = screenWidth - scaledWidth - 6;
				baseY = 6;
			}
			case BOTTOM_LEFT -> {
				baseX = 6;
				baseY = screenHeight - scaledHeight - 6;
			}
			case BOTTOM_RIGHT -> {
				baseX = screenWidth - scaledWidth - 6;
				baseY = screenHeight - scaledHeight - 6;
			}
			case CUSTOM -> {
				baseX = 0;
				baseY = 0;
			}
			default -> {
				baseX = 6;
				baseY = 6;
			}
		}

		HudRect raw = new HudRect(baseX + offsetX, baseY + offsetY, scaledWidth, scaledHeight);
		return raw.clampToScreen(screenWidth, screenHeight, 2);
	}

	public static EffectsLayoutResult computeEffectsLayout(
			EffectsConfig config,
			List<EffectRowMetric> metrics,
			int screenWidth,
			int screenHeight
	) {
		EffectsConfig safeConfig = config != null ? config : new EffectsConfig();
		float scale = ConfigValidator.clampScale(safeConfig.scale);
		int spacing = ConfigValidator.clampInt(safeConfig.rowSpacing, ConfigValidator.MIN_SPACING, ConfigValidator.MAX_SPACING);
		boolean vertical = safeConfig.orientation != HudOrientation.HORIZONTAL;
		EffectsVisualStyle style = safeConfig.style != null ? safeConfig.style : EffectsVisualStyle.MINECRAFT_COMPACT;
		PotionHudMode potionMode = safeConfig.potionHudMode != null ? safeConfig.potionHudMode : PotionHudMode.NORMAL;
		PotionMiniDisplayMode miniDisplay = safeConfig.potionMiniDisplay != null ? safeConfig.potionMiniDisplay : PotionMiniDisplayMode.TIMER_TEXT;

		if (metrics == null || metrics.isEmpty()) {
			HudRect emptyOrigin = resolveAnchorRect(safeConfig.anchor, safeConfig.offsetX, safeConfig.offsetY, 0, 0, screenWidth, screenHeight);
			return new EffectsLayoutResult(emptyOrigin.x(), emptyOrigin.y(), 0, 0, scale, emptyOrigin, List.of());
		}

		boolean isPotionMini = style == EffectsVisualStyle.POTION_HUD && potionMode == PotionHudMode.MINI;
		boolean isPotionNormal = style == EffectsVisualStyle.POTION_HUD && potionMode == PotionHudMode.NORMAL;

		int iconBox = safeConfig.showIcon
				? (style == EffectsVisualStyle.MINIMAL_TEXT ? 14 : (isPotionMini ? 16 : (isPotionNormal ? 18 : 20)))
				: 0;
		int padX = (style == EffectsVisualStyle.MINIMAL_TEXT || isPotionMini) ? 2 : (isPotionNormal ? 3 : 4);
		int padY = (style == EffectsVisualStyle.MINIMAL_TEXT || isPotionMini) ? 2 : 3;

		record RowSize(int w, int h) {
		}
		List<RowSize> sizes = new ArrayList<>(metrics.size());
		int maxRowW = 0;
		int totalW = 0;
		int totalH = 0;

		for (int i = 0; i < metrics.size(); i++) {
			EffectRowMetric metric = metrics.get(i);
			boolean hasTitle = metric.titleWidth() > 0;
			boolean hasDuration = metric.durationWidth() > 0;

			int rowW;
			int rowH;
			if (isPotionMini) {
				int bottomW = 0;
				int bottomH = 0;
				if (safeConfig.showDuration) {
					if (miniDisplay == PotionMiniDisplayMode.DURATION_BAR) {
						bottomW = 20;
						bottomH = 4;
					} else if (hasDuration) {
						bottomW = (int) Math.ceil(metric.durationWidth() * 0.78f) + 2;
						bottomH = 7;
					}
				}
				int levelW = (!safeConfig.showIcon && metric.miniLevelWidth() > 0)
						? (int) Math.ceil(metric.miniLevelWidth() * 0.78f) + 2
						: 0;
				int topH = iconBox > 0 ? iconBox : (levelW > 0 ? 8 : 0);
				int contentW = Math.max(Math.max(iconBox, levelW), bottomW);
				int gap = (topH > 0 && bottomH > 0) ? 2 : 0;
				rowW = Math.max(22, padX * 2 + contentW);
				rowH = Math.max(16, padY * 2 + topH + gap + bottomH);
			} else if (style == EffectsVisualStyle.MINIMAL_TEXT) {
				int gap = (hasTitle && hasDuration) ? 6 : 0;
				int textW = metric.titleWidth() + gap + metric.durationWidth();
				int textH = (hasTitle || hasDuration) ? UNSCALED_LINE_HEIGHT : 0;
				rowW = padX * 2 + iconBox + (iconBox > 0 && textW > 0 ? 4 : 0) + textW;
				rowH = Math.max(iconBox + padY * 2, textH + padY * 2);
			} else {
				int textW = Math.max(metric.titleWidth(), metric.durationWidth());
				int lines = (hasTitle ? 1 : 0) + (hasDuration ? 1 : 0);
				int textH = lines == 2 ? (UNSCALED_LINE_HEIGHT * 2 + 1) : (lines == 1 ? UNSCALED_LINE_HEIGHT : 0);
				int accentExtra = isPotionNormal ? 2 : 0;
				rowW = padX * 2 + accentExtra + iconBox + (iconBox > 0 && textW > 0 ? 4 : 0) + textW;
				rowH = Math.max(iconBox + padY * 2, textH + padY * 2);
			}

			if (rowW < 18) {
				rowW = 18;
			}
			if (rowH < 14) {
				rowH = 14;
			}
			sizes.add(new RowSize(rowW, rowH));
			maxRowW = Math.max(maxRowW, rowW);
			if (vertical) {
				totalH += rowH + (i > 0 ? spacing : 0);
			} else {
				totalW += rowW + (i > 0 ? spacing : 0);
				totalH = Math.max(totalH, rowH);
			}
		}

		if (vertical) {
			totalW = maxRowW;
		}

		int scaledTotalW = Math.max(1, Math.round(totalW * scale));
		int scaledTotalH = Math.max(1, Math.round(totalH * scale));
		HudRect originRect = resolveAnchorRect(
				safeConfig.anchor,
				safeConfig.offsetX,
				safeConfig.offsetY,
				scaledTotalW,
				scaledTotalH,
				screenWidth,
				screenHeight
		);

		List<EffectRowPlacement> placements = new ArrayList<>(sizes.size());
		int cursorX = 0;
		int cursorY = 0;
		for (int i = 0; i < sizes.size(); i++) {
			RowSize size = sizes.get(i);
			int rowW = (vertical && !isPotionMini) ? maxRowW : size.w();
			int rowH = size.h();
			int drawOffsetX = (vertical && isPotionMini && maxRowW > rowW) ? (maxRowW - rowW) / 2 : 0;
			int sx = originRect.x() + Math.round((cursorX + drawOffsetX) * scale);
			int sy = originRect.y() + Math.round(cursorY * scale);
			int sw = Math.max(1, Math.round(rowW * scale));
			int sh = Math.max(1, Math.round(rowH * scale));
			placements.add(new EffectRowPlacement(
					i,
					cursorX + drawOffsetX,
					cursorY,
					rowW,
					rowH,
					new HudRect(sx, sy, sw, sh)
			));
			if (vertical) {
				cursorY += rowH + spacing;
			} else {
				cursorX += rowW + spacing;
			}
		}

		return new EffectsLayoutResult(originRect.x(), originRect.y(), totalW, totalH, scale, originRect, placements);
	}

	public static ResizeResult computeResizeFromCorner(
			HudRect initialBounds,
			int unscaledWidth,
			int unscaledHeight,
			ResizeCorner corner,
			int pointerX,
			int pointerY,
			int screenWidth,
			int screenHeight
	) {
		HudRect safeBounds = initialBounds != null && !initialBounds.isEmpty()
				? initialBounds
				: new HudRect(0, 0, Math.max(1, unscaledWidth), Math.max(1, unscaledHeight));
		ResizeCorner safeCorner = corner != null ? corner : ResizeCorner.BOTTOM_RIGHT;
		int uW = Math.max(1, unscaledWidth);
		int uH = Math.max(1, unscaledHeight);

		int anchorX;
		int anchorY;
		int desiredW;
		int desiredH;
		switch (safeCorner) {
			case TOP_LEFT -> {
				anchorX = safeBounds.right();
				anchorY = safeBounds.bottom();
				desiredW = anchorX - pointerX;
				desiredH = anchorY - pointerY;
			}
			case TOP_RIGHT -> {
				anchorX = safeBounds.x();
				anchorY = safeBounds.bottom();
				desiredW = pointerX - anchorX;
				desiredH = anchorY - pointerY;
			}
			case BOTTOM_LEFT -> {
				anchorX = safeBounds.right();
				anchorY = safeBounds.y();
				desiredW = anchorX - pointerX;
				desiredH = pointerY - anchorY;
			}
			case BOTTOM_RIGHT -> {
				anchorX = safeBounds.x();
				anchorY = safeBounds.y();
				desiredW = pointerX - anchorX;
				desiredH = pointerY - anchorY;
			}
			default -> {
				anchorX = safeBounds.x();
				anchorY = safeBounds.y();
				desiredW = pointerX - anchorX;
				desiredH = pointerY - anchorY;
			}
		}

		int clampedDesiredW = Math.max(8, desiredW);
		int clampedDesiredH = Math.max(8, desiredH);
		double numerator = (double) clampedDesiredW * uW + (double) clampedDesiredH * uH;
		double denominator = (double) uW * uW + (double) uH * uH;
		float projectedScale = (float) (numerator / Math.max(1.0, denominator));
		float newScale = ConfigValidator.clampScale(projectedScale);

		int newW = Math.max(1, Math.round(uW * newScale));
		int newH = Math.max(1, Math.round(uH * newScale));
		int rawX = (safeCorner == ResizeCorner.TOP_LEFT || safeCorner == ResizeCorner.BOTTOM_LEFT)
				? anchorX - newW
				: anchorX;
		int rawY = (safeCorner == ResizeCorner.TOP_LEFT || safeCorner == ResizeCorner.TOP_RIGHT)
				? anchorY - newH
				: anchorY;

		HudRect clamped = new HudRect(rawX, rawY, newW, newH).clampToScreen(screenWidth, screenHeight, 2);
		return new ResizeResult(newScale, clamped.x(), clamped.y(), clamped);
	}

	public static SnapResult snapPosition(
			int candidateX,
			int candidateY,
			int width,
			int height,
			int screenWidth,
			int screenHeight,
			int gridSize,
			boolean snapToGrid,
			boolean collectGuides,
			List<HudRect> referenceRects
	) {
		int x = candidateX;
		int y = candidateY;
		int safeGrid = ConfigValidator.clampInt(gridSize, ConfigValidator.MIN_GRID_SIZE, ConfigValidator.MAX_GRID_SIZE);

		if (snapToGrid && safeGrid > 1) {
			x = Math.round(x / (float) safeGrid) * safeGrid;
			y = Math.round(y / (float) safeGrid) * safeGrid;
		}

		List<GuideLine> guides = new ArrayList<>();
		int screenCenterX = screenWidth / 2;
		int screenCenterY = screenHeight / 2;
		int boxCenterX = x + width / 2;
		int boxCenterY = y + height / 2;

		boolean snappedX = false;
		boolean snappedY = false;

		if (Math.abs(boxCenterX - screenCenterX) <= SNAP_THRESHOLD) {
			x = screenCenterX - width / 2;
			snappedX = true;
			if (collectGuides) {
				guides.add(new GuideLine(true, screenCenterX));
			}
		}
		if (Math.abs(boxCenterY - screenCenterY) <= SNAP_THRESHOLD) {
			y = screenCenterY - height / 2;
			snappedY = true;
			if (collectGuides) {
				guides.add(new GuideLine(false, screenCenterY));
			}
		}

		if (referenceRects != null) {
			for (HudRect ref : referenceRects) {
				if (ref == null || ref.isEmpty()) {
					continue;
				}
				if (!snappedX) {
					if (Math.abs(x - ref.x()) <= SNAP_THRESHOLD) {
						x = ref.x();
						snappedX = true;
						if (collectGuides) {
							guides.add(new GuideLine(true, ref.x()));
						}
					} else if (Math.abs((x + width) - ref.right()) <= SNAP_THRESHOLD) {
						x = ref.right() - width;
						snappedX = true;
						if (collectGuides) {
							guides.add(new GuideLine(true, ref.right()));
						}
					} else if (Math.abs((x + width / 2) - ref.centerX()) <= SNAP_THRESHOLD) {
						x = ref.centerX() - width / 2;
						snappedX = true;
						if (collectGuides) {
							guides.add(new GuideLine(true, ref.centerX()));
						}
					}
				}
				if (!snappedY) {
					if (Math.abs(y - ref.y()) <= SNAP_THRESHOLD) {
						y = ref.y();
						snappedY = true;
						if (collectGuides) {
							guides.add(new GuideLine(false, ref.y()));
						}
					} else if (Math.abs((y + height) - ref.bottom()) <= SNAP_THRESHOLD) {
						y = ref.bottom() - height;
						snappedY = true;
						if (collectGuides) {
							guides.add(new GuideLine(false, ref.bottom()));
						}
					} else if (Math.abs((y + height / 2) - ref.centerY()) <= SNAP_THRESHOLD) {
						y = ref.centerY() - height / 2;
						snappedY = true;
						if (collectGuides) {
							guides.add(new GuideLine(false, ref.centerY()));
						}
					}
				}
			}
		}

		HudRect clamped = new HudRect(x, y, width, height).clampToScreen(screenWidth, screenHeight, 2);
		return new SnapResult(clamped.x(), clamped.y(), guides);
	}

	public static boolean hasAnyOverlap(List<HudRect> rects) {
		if (rects == null || rects.size() < 2) {
			return false;
		}
		for (int i = 0; i < rects.size(); i++) {
			HudRect a = rects.get(i);
			if (a == null || a.isEmpty()) {
				continue;
			}
			for (int j = i + 1; j < rects.size(); j++) {
				if (a.intersects(rects.get(j))) {
					return true;
				}
			}
		}
		return false;
	}
}
