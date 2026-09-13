package com.newroze.nokaboom.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.newroze.nokaboom.NoKABOOM;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tiny zero-dependency JSON config.
 *
 * <p>Gson ships with Minecraft itself, so no extra libraries (e.g. Cloth Config)
 * are needed for a handful of values. The file lives at
 * {@code config/nokaboom.json} and is created with defaults on first launch.
 * Every value is clamped on load, so a hand-edited broken file can never
 * crash rendering.
 */
public final class NoKABOOMConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String FILE_NAME = "nokaboom.json";

	private static NoKABOOMConfig instance;

	/** Master switch. {@code false} disables the mod without uninstalling it. */
	public boolean enabled = true;

	/** Highlight armor worn by players (the main PvP use-case). */
	public boolean highlightPlayers = true;

	/** Highlight armor displayed on armor stands (kits, previews, bases). */
	public boolean highlightArmorStands = true;

	/** Highlight tint, RGB hex. Alpha is driven by {@link #minAlpha}/{@link #maxAlpha} + pulse. */
	public int highlightRgb = 0xFF2E2E;

	/** Lowest overlay opacity (0-255). */
	public int minAlpha = 0x55;

	/** Highest overlay opacity (0-255). */
	public int maxAlpha = 0xA0;

	/**
	 * Pulse speed in radians per second. The overlay breathes between
	 * {@link #minAlpha} and {@link #maxAlpha}. {@code 0} disables pulsing
	 * and pins the overlay at {@link #maxAlpha}.
	 */
	public double pulseSpeed = 2.5;

	/**
	 * The overlay is rendered slightly larger than the armor piece itself
	 * so the two shells never z-fight. {@code 1.03} ≈ +3%.
	 */
	public float expandScale = 1.03F;

	/**
	 * Render the overlay at full brightness, so enchanted pieces visibly
	 * "glow" even in the dark. When {@code false}, vanilla lighting applies.
	 */
	public boolean fullbright = true;

	/** @return the active (singleton) config, defaults if never loaded. */
	public static NoKABOOMConfig get() {
		if (instance == null) {
			instance = new NoKABOOMConfig();
		}
		return instance;
	}

	/** Loads {@code config/nokaboom.json}, creating it with defaults if missing. */
	public static void load() {
		Path file = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
		if (Files.isRegularFile(file)) {
			try (Reader reader = Files.newBufferedReader(file)) {
				NoKABOOMConfig loaded = GSON.fromJson(reader, NoKABOOMConfig.class);
				instance = loaded != null ? loaded : new NoKABOOMConfig();
			} catch (Exception e) {
				NoKABOOM.LOGGER.warn("[NoKABOOM] Broken config, falling back to defaults: {}", e.toString());
				instance = new NoKABOOMConfig();
			}
		} else {
			instance = new NoKABOOMConfig();
			save();
			return;
		}
		instance.validate();
	}

	/** Writes the current config back to disk. */
	public static void save() {
		Path file = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
		try (Writer writer = Files.newBufferedWriter(file)) {
			GSON.toJson(get(), writer);
		} catch (IOException e) {
			NoKABOOM.LOGGER.warn("[NoKABOOM] Could not save config: {}", e.toString());
		}
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	private void validate() {
		highlightRgb &= 0xFFFFFF;
		minAlpha = clamp(minAlpha, 0, 255);
		maxAlpha = clamp(maxAlpha, 0, 255);
		if (minAlpha > maxAlpha) {
			int swap = minAlpha;
			minAlpha = maxAlpha;
			maxAlpha = swap;
		}
		if (!Double.isFinite(pulseSpeed) || pulseSpeed < 0.0) {
			pulseSpeed = 0.0;
		}
		if (!Float.isFinite(expandScale) || expandScale < 1.0F || expandScale > 1.2F) {
			expandScale = 1.03F;
		}
	}
}
