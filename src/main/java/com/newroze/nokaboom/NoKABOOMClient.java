package com.newroze.nokaboom;

import com.newroze.nokaboom.config.NoKABOOMConfig;
import net.fabricmc.api.ClientModInitializer;

/**
 * Client entrypoint (see {@code fabric.mod.json}).
 *
 * <p>Loads the JSON config. Everything else is driven by the
 * {@code ArmorFeatureRenderer} mixin at render time, so there is
 * nothing to register here — no blocks, items, packets or tick handlers.
 */
public final class NoKABOOMClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		NoKABOOMConfig.load();
		NoKABOOM.LOGGER.info("[NoKABOOM] initialized — Blast Protection armor will glow red.");
	}
}
