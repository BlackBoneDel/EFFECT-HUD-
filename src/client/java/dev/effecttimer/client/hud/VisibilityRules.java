package dev.effecttimer.client.hud;

import dev.effecttimer.client.screen.AbstractCustomizableScreen;
import dev.effecttimer.client.screen.HudEditorScreen;
import dev.effecttimer.config.HudConfig;
import dev.effecttimer.config.VanillaEffectsMode;
import dev.effecttimer.config.VisibilityConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

public final class VisibilityRules {
	private VisibilityRules() {
	}

	public static boolean shouldRenderLiveHud(Minecraft client, VisibilityConfig config) {
		if (client == null || config == null) {
			return false;
		}
		// Desde 26.2 el estado "ocultar HUD" vive en el Hud, no en las Options.
		if (client.gui.hud.isHidden()) {
			return false;
		}
		if (config.requireActiveWorld && (client.player == null || client.level == null)) {
			return false;
		}
		Screen currentScreen = client.gui.screen();
		if (currentScreen instanceof HudEditorScreen || currentScreen instanceof AbstractCustomizableScreen) {
			return false;
		}
		if (config.hideInInventory && currentScreen instanceof AbstractContainerScreen<?>) {
			return false;
		}
		if (config.hideInChat && currentScreen instanceof ChatScreen) {
			return false;
		}
		if (config.hideInPauseScreen && currentScreen instanceof PauseScreen) {
			return false;
		}
		return true;
	}

	public static boolean shouldRenderVanillaEffects(HudConfig config) {
		if (config == null || config.effects == null || !config.effects.enabled) {
			// Disabling our Effects HUD always leaves the vanilla HUD intact.
			return true;
		}
		return config.effects.vanillaEffectsMode != VanillaEffectsMode.CUSTOM_ONLY;
	}

	public static boolean shouldRenderCustomEffects(HudConfig config) {
		if (config == null || config.effects == null || !config.effects.enabled) {
			return false;
		}
		return config.effects.vanillaEffectsMode != VanillaEffectsMode.VANILLA_ONLY;
	}
}
