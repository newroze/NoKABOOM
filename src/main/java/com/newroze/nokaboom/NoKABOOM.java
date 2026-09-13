package com.newroze.nokaboom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared constants for NoKABOOM.
 *
 * <p>The mod is client-only: all logic lives behind {@link NoKABOOMClient}
 * and the render mixin. This class only holds the mod id and logger so
 * every other class references a single source of truth.
 */
public final class NoKABOOM {
	/** Mod id. Must match {@code fabric.mod.json} and the mixin package. */
	public static final String MOD_ID = "nokaboom";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private NoKABOOM() {
	}
}
