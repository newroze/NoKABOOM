package com.newroze.nokaboom.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Hotkey handling without Fabric API: vanilla {@link net.minecraft.client.option.KeyBinding}
 * registration needs the Fabric keybinding helper, so instead we poll the raw key state
 * on the client tick (see {@code MinecraftClientMixin}) with edge detection.
 *
 * <p>Default key: {@code H}. Opens the config when no screen is open, closes it (back to
 * parent) when our screen is open and the user is not typing in a text field — so chatting
 * or typing in an anvil never triggers the menu.
 */
public final class NoKABOOMKeybinds {
	public static final int OPEN_MENU_KEY = GLFW.GLFW_KEY_H;

	private static boolean prevDown;

	private NoKABOOMKeybinds() {
	}

	public static void poll(MinecraftClient client) {
		if (client == null) {
			return;
		}
		boolean down = false;
		try {
			down = InputUtil.isKeyPressed(client.getWindow(), OPEN_MENU_KEY);
		} catch (Exception ignored) {
			down = false;
		}
		try {
			if (down && !prevDown) {
				if (client.currentScreen instanceof NoKABOOMConfigScreen screen) {
					if (!screen.isTyping()) {
						client.setScreen(screen.getParent());
					}
				} else if (client.currentScreen == null && client.player != null) {
					client.setScreen(new NoKABOOMConfigScreen(null));
				}
			}
		} finally {
			prevDown = down;
		}
	}
}
