package dev.effecttimer.layout;

public record EffectRowPlacement(
		int index,
		int unscaledX,
		int unscaledY,
		int unscaledWidth,
		int unscaledHeight,
		HudRect screenBounds
) {
}
