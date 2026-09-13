package com.newroze.nokaboom.render;

import com.newroze.nokaboom.config.NoKABOOMConfig;
import com.newroze.nokaboom.mixin.client.ItemLayerAccessor;
import com.newroze.nokaboom.mixin.client.ItemRenderStateAccessor;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.state.ArmedEntityRenderState;
import net.minecraft.client.render.entity.state.ArmorStandEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.json.Transformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Highlight for held items (swords etc.): decides <em>what</em> glows and draws the glow.
 *
 * <p>How it looks: instead of a box around the weapon, the weapon's own texture
 * glows in the tracked enchantment's color. Technically it is a second draw of
 * the exact same baked item quads (same geometry, same item texture, same
 * per-layer transforms), tinted with the enchantment color at pulsing opacity
 * and rendered at full brightness. No custom shader, one extra translucent
 * draw call per layer (usually one) — effectively zero FPS cost.
 *
 * <p>Every tintable item layer is overlaid (solid, cutout and translucent):
 * skipping opaque layers would mean swords never glow, since held weapons
 * almost never use a translucent pipeline. The copy is scaled slightly up
 * ({@link NoKABOOMConfig#heldGlowScale}) so it reads as a halo instead of
 * hiding the real item, with pulsing opacity from
 * {@link NoKABOOMArmorHighlight#pulseAlpha()}. Special models
 * (trident, shield, …) have no tintable quads and are skipped.
 */
public final class NoKABOOMHeldHighlight {
	private NoKABOOMHeldHighlight() {
	}

	/**
	 * @return configured RGB for the first enabled tracked enchantment on this held stack,
	 *         or {@code null} when nothing should glow.
	 */
	public static Integer highlightRgb(ArmedEntityRenderState state, ItemStack stack) {
		NoKABOOMConfig config = NoKABOOMConfig.get();
		if (!config.enabled || !config.highlightHeldItems || state == null || stack == null || stack.isEmpty()) {
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
	 * Draws the glow overlay. Must be called with the same matrices vanilla used
	 * for the real item (so swing / bow-pull / use poses match exactly).
	 *
	 * @param vanillaLight light vanilla used for the real item
	 * @param overlay      overlay vanilla used (pass-through)
	 * @param outlineColor outline color vanilla used (pass-through)
	 * @param rgb          enchantment color (alpha comes from the pulse)
	 */
	public static void renderGlow(MatrixStack matrices, OrderedRenderCommandQueue queue,
			ItemRenderState itemState, int vanillaLight, int overlay, int outlineColor, int rgb) {
		if (matrices == null || queue == null || itemState == null) {
			return;
		}
		NoKABOOMConfig config = NoKABOOMConfig.get();
		int light = config.fullbright ? LightmapTextureManager.MAX_LIGHT_COORDINATE : vanillaLight;
		int argb = (NoKABOOMArmorHighlight.pulseAlpha() << 24) | (rgb & 0xFFFFFF);
		float scale = config.heldGlowScale;
		if (!Float.isFinite(scale) || scale < 1.0F || scale > 1.15F) {
			scale = 1.04F;
		}

		ItemRenderState.LayerRenderState[] layers;
		int layerCount;
		ItemDisplayContext displayContext;
		try {
			ItemRenderStateAccessor acc = (ItemRenderStateAccessor) itemState;
			layers = acc.nokaboom$getLayers();
			layerCount = acc.nokaboom$getLayerCount();
			displayContext = acc.nokaboom$getDisplayContext();
		} catch (Exception ignored) {
			return;
		}
		if (layers == null || displayContext == null || layerCount <= 0) {
			return;
		}
		boolean leftHand = displayContext.isLeftHand();
		int[] glowTint = new int[]{argb};

		matrices.push();
		try {
			// Slightly larger than the real item: no z-fighting, reads as a halo.
			matrices.scale(scale, scale, scale);
			int end = Math.min(layerCount, layers.length);
			for (int i = 0; i < end; i++) {
				ItemRenderState.LayerRenderState layer = layers[i];
				if (layer == null) {
					continue;
				}
				RenderLayer renderLayer;
				Transformation transform;
				try {
					ItemLayerAccessor la = (ItemLayerAccessor) (Object) layer;
					if (la.nokaboom$getSpecialModel() != null) {
						continue;
					}
					renderLayer = la.nokaboom$getRenderLayer();
					transform = la.nokaboom$getTransform();
				} catch (Exception ignored) {
					continue;
				}
				if (renderLayer == null) {
					continue;
				}
				List<BakedQuad> quads;
				try {
					quads = layer.getQuads();
				} catch (Exception ignored) {
					continue;
				}
				if (quads == null || quads.isEmpty()) {
					continue;
				}
				// Same quads, but every vertex forced to tint slot 0 = our glow color.
				// The texture stays the item texture, so the blade keeps its shape
				// and details and just shines in the enchantment color.
				List<BakedQuad> tinted = new ArrayList<>(quads.size());
				try {
					for (BakedQuad q : quads) {
						if (q == null) {
							continue;
						}
						tinted.add(new BakedQuad(q.position0(), q.position1(), q.position2(), q.position3(),
								q.packedUV0(), q.packedUV1(), q.packedUV2(), q.packedUV3(),
								0, q.face(), q.sprite(), q.shade(), q.lightEmission()));
					}
				} catch (Exception ignored) {
					continue;
				}
				if (tinted.isEmpty()) {
					continue;
				}
				matrices.push();
				try {
					if (transform != null) {
						transform.apply(leftHand, matrices.peek());
					}
					queue.submitItem(matrices, displayContext, light,
							overlay == 0 ? OverlayTexture.DEFAULT_UV : overlay,
							outlineColor, glowTint, tinted, renderLayer,
							ItemRenderState.Glint.NONE);
				} catch (Exception ignored) {
					// A missed glow is better than a crashed frame.
				} finally {
					matrices.pop();
				}
			}
		} catch (Exception ignored) {
		} finally {
			matrices.pop();
		}
	}
}
