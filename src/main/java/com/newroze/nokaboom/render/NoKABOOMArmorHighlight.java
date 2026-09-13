package com.newroze.nokaboom.render;

import com.newroze.nokaboom.config.NoKABOOMConfig;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.entity.state.ArmorStandEntityRenderState;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Util;

/**
 * Decides <em>what</em> glows and <em>how</em> — the mixins only decide
 * <em>when</em>.
 *
 * <p>Look &amp; feel, tuned for PvP readability: only the exact piece carrying
 * a tracked enchantment is tinted, and the tint multiplies the armor's own
 * texture (exactly how vanilla dyes leather armor) instead of covering it
 * with a flat colored shell — diamond still looks like diamond, just red.
 * Same idea as the held-item glow: no boxes, no custom shaders, just the
 * item's own pixels in the enchantment's color.
 *
 * <p>Technically the armor is drawn twice when highlighted: vanilla first,
 * then the same render with the dye color swapped for the enchantment color
 * (see {@code ArmorFeatureRendererMixin} + {@code EquipmentRendererMixin}).
 * One extra opaque draw call per highlighted piece — effectively zero FPS cost.
 */
public final class NoKABOOMArmorHighlight {
	/**
	 * Enchant tint (opaque ARGB) for the armor pass currently being rendered,
	 * or {@code null} outside of it. Set by {@code ArmorFeatureRendererMixin}
	 * around the second render, read by {@code EquipmentRendererMixin}.
	 * Render-thread only.
	 */
	public static final ThreadLocal<Integer> TINT_OVERRIDE = new ThreadLocal<>();

	private NoKABOOMArmorHighlight() {
	}

	/**
	 * @param state the entity being rendered (carries the entity type in 1.21+)
	 * @param stack the armor piece vanilla just rendered for one slot
	 * @return {@code true} if the tint should be drawn for this piece
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

	/**
	 * Scale of the tint shell (the enchant-colored copy of the armor drawn
	 * slightly larger so it never z-fights the real piece underneath).
	 * Clamped here too (not just on config load) so a broken runtime value
	 * can never poison the matrix.
	 */
	public static float tintScale() {
		float scale = NoKABOOMConfig.get().expandScale;
		if (!Float.isFinite(scale) || scale < 1.0F || scale > 1.2F) {
			scale = 1.03F;
		}
		return scale;
	}

	/** Current overlay opacity for the held-item glow, breathing on a sine wave over wall-clock time. */
	public static int pulseAlpha() {
		NoKABOOMConfig config = NoKABOOMConfig.get();
		if (config.pulseSpeed <= 0.0) {
			return config.maxAlpha;
		}
		double timeSeconds = Util.getMeasuringTimeMs() / 1000.0;
		double wave = 0.5 + 0.5 * Math.sin(timeSeconds * config.pulseSpeed);
		return (int) Math.round(config.minAlpha + (config.maxAlpha - config.minAlpha) * wave);
	}

	/**
	 * @param vanillaLight the light vanilla used for the piece
	 * @return full brightness when {@code fullbright} is on, else vanilla light
	 */
	public static int renderLight(int vanillaLight) {
		return NoKABOOMConfig.get().fullbright ? LightmapTextureManager.MAX_LIGHT_COORDINATE : vanillaLight;
	}
}
