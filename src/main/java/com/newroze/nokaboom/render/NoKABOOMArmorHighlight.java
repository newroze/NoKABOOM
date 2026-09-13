package com.newroze.nokaboom.render;

import com.newroze.nokaboom.NoKABOOM;
import com.newroze.nokaboom.config.NoKABOOMConfig;
import com.newroze.nokaboom.util.BlastProtectionChecker;
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
 *   <li>only the exact piece carrying Blast Protection is covered — a helmet
 *       never lights up the chestplate;</li>
 *   <li>a translucent red film ({@code textures/highlight.png} tinted red)
 *       keeps the armor material readable (diamond still looks like diamond);</li>
 *   <li>soft sine pulse instead of a static overlay — visible, but not blinding;</li>
 *   <li>optional fullbright so the mark reads even in the dark.</li>
 * </ul>
 */
public final class NoKABOOMArmorHighlight {
	/** Plain white 1x1 texture; the red tint comes from the render color. */
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
	 * @return {@code true} if the red overlay should be drawn for this piece
	 */
	public static boolean shouldHighlight(BipedEntityRenderState state, ItemStack stack) {
		NoKABOOMConfig config = NoKABOOMConfig.get();
		if (!config.enabled || state == null || stack == null || stack.isEmpty()) {
			return false;
		}
		boolean allowedEntity = (state instanceof PlayerEntityRenderState && config.highlightPlayers)
				|| (state instanceof ArmorStandEntityRenderState && config.highlightArmorStands);
		if (!allowedEntity) {
			return false;
		}
		return BlastProtectionChecker.hasBlastProtection(stack);
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

	/** Packed ARGB render color: pulsing alpha + configured RGB. */
	public static int highlightColor() {
		NoKABOOMConfig config = NoKABOOMConfig.get();
		return (pulseAlpha() << 24) | (config.highlightRgb & 0xFFFFFF);
	}

	/**
	 * @param vanillaLight the light vanilla used for the armor piece
	 * @return full brightness when {@code fullbright} is on, else vanilla light
	 */
	public static int renderLight(int vanillaLight) {
		return NoKABOOMConfig.get().fullbright ? LightmapTextureManager.MAX_LIGHT_COORDINATE : vanillaLight;
	}
}
