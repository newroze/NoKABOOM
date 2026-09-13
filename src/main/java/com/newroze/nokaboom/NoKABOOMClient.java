package com.newroze.nokaboom;

import com.newroze.nokaboom.config.NoKABOOMConfig;
import net.fabricmc.api.ClientModInitializer;

/**
 * Client entrypoint (see {@code fabric.mod.json}).
 *
 * <p>Loads the JSON config. Everything else is driven by mixins at render time
 * (armor + held-item highlights) and on client tick (menu hotkey), so there is
 * nothing to register here — no blocks, items, packets or Fabric API callbacks.
 */
public final class NoKABOOMClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		NoKABOOMConfig.load();
		NoKABOOM.LOGGER.info("[NoKABOOM] initialized — tracked enchants glow with custom colors. Press H or use pause menu button.");
	}
}
