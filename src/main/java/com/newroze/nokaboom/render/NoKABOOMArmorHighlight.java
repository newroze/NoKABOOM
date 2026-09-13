package com.newroze.nokaboom.render;

import com.newroze.nokaboom.NoKABOOM;
import com.newroze.nokaboom.config.NoKABOOMConfig;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.entity.state.ArmorStandEntityRenderState;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

/**
 * Decides <em>what</em> glows and <em>how</em> — the mixin only decides
 * <em>when</em> (right after vanilla renders an armor piece).
 *
 * <p>Look &amp; feel, tuned for PvP readability:
 * <ul>
 *   <li>only the exact piece carrying a tracked enchantment is covered — a helmet
 *       never lights up the chestplate;</li>
 *   <li>a translucent film ({@code textures/highlight.png} tinted per enchantment)
 *       keeps the armor material readable (diamond still looks like diamond);</li>
 *   <li>soft sine pulse instead of a static overlay — visible, but not blinding;</li>
 *   <li>optional fullbright so the mark reads even in the dark.</li>
 * </ul>
 */
public final class NoKABOOMArmorHighlight {
	/** Plain white 1x1 texture; the tint comes from the per-enchantment render color. */
	public static final Identifier TEXTURE = Identifier.of(NoKABOOM.MOD_ID, "textures/highlight.png");

	/**
	 * Draw order offset inside the render command queue.
	 * Vanilla armor submits first; any positive value keeps our shell on top
	 * (depth testing + slight scale-up handle the rest).
	 */
	public static final int QUEUE_ORDER = 1;

	private NoKABOOMArmorHighlight() {
	}

	/**
	 * @param state the entity being rendered (carries the entity type in 1.21+)
	 * @param stack the armor piece vanilla just rendered for one slot
	 * @return {@code true} if the overlay should be drawn for this piece
	 */
	public static boolean shouldHighlight(BipedEntityRenderState state, ItemStack stack) {
		return highlightRgb(state, stack) != null;
	}

	/**
	 * @return the configured RGB for the first enabled tracked enchantment on this
	 *         piece, or {@code null} when nothing should glow. Entity-type filters
	 *         ({@code highlightPlayers} / {@code highlightArmorStands}) apply here,
	 *         so callers only need this one method.
	 */
	public static Integer highlightRgb(BipedEntityRenderState state, ItemStack stack) {
		NoKABOOMConfig config = NoKABOOMConfig.get();
		if (!config.enabled || state == null || stack == null || stack.isEmpty()) {
			return null;
		}
		boolean allowedEntity = (state instanceof PlayerEntityRenderState && config.highlightPlayers)
				|| (state instanceof ArmorStandEntityRenderState && config.highlightArmorStands);
		if (!allowedEntity) {
			return null;
		}
		return NoKABOOMConfig.colorFor(stack);
	}

	/** Current overlay opacity, breathing on a sine wave over wall-clock time. */
	public static int pulseAlpha() {
		NoKABOOMConfig config = NoKABOOMConfig.get();
		if (config.pulseSpeed <= 0.0) {
			return config.maxAlpha;
		}
		double timeSeconds = Util.getMeasuringTimeMs() / 1000.0;
		double wave = 0.5 + 0.5 * Math.sin(timeSeconds * config.pulseSpeed);
		return (int) Math.round(config.minAlpha + (config.maxAlpha - config.minAlpha) * wave);
	}

	/** Packed ARGB render color for this exact stack: pulsing alpha + its enchantment RGB. */
	public static int highlightColor(ItemStack stack) {
		Integer rgb = NoKABOOMConfig.colorFor(stack);
		if (rgb == null) {
			rgb = 0xFF2E2E;
		}
		return (pulseAlpha() << 24) | (rgb & 0xFFFFFF);
	}

	/**
	 * @param vanillaLight the light vanilla used for the armor piece
	 * @return full brightness when {@code fullbright} is on, else vanilla light
	 */
	public static int renderLight(int vanillaLight) {
		return NoKABOOMConfig.get().fullbright ? LightmapTextureManager.MAX_LIGHT_COORDINATE : vanillaLight;
	}
}
