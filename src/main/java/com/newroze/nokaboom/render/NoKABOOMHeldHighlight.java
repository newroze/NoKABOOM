package com.newroze.nokaboom.render;

import com.newroze.nokaboom.NoKABOOM;
import com.newroze.nokaboom.config.NoKABOOMConfig;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.state.ArmedEntityRenderState;
import net.minecraft.client.render.entity.state.ArmorStandEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

/**
 * Highlight for held items (swords etc.): decides <em>what</em> glows and draws the glow box.
 *
 * <p>Unlike armor (which re-uses the vanilla {@code BipedEntityModel}), a held sword is a baked
 * item model, so there is no entity model to re-tint. Instead the mixin re-applies the vanilla
 * hand transform and submits a small translucent pulsing cube around the item via
 * {@code OrderedRenderCommandQueue.submitCustom}. The cube uses the same white 1x1 texture as
 * armor, tinted with the tracked enchantment's custom color.
 */
public final class NoKABOOMHeldHighlight {
	/** Same white 1x1 film as armor; tint comes from the vertex color. */
	public static final Identifier TEXTURE = Identifier.of(NoKABOOM.MOD_ID, "textures/highlight.png");

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

	/** Packed ARGB render color for this held stack: pulsing alpha + its enchantment RGB. */
	public static int highlightColor(ItemStack stack) {
		Integer rgb = NoKABOOMConfig.colorFor(stack);
		if (rgb == null) {
			rgb = 0xFF7A1A;
		}
		return (NoKABOOMArmorHighlight.pulseAlpha() << 24) | (rgb & 0xFFFFFF);
	}

	/**
	 * @param vanillaLight the light vanilla used for the held item
	 * @return full brightness when {@code fullbright} is on, else vanilla light
	 */
	public static int renderLight(int vanillaLight) {
		return NoKABOOMConfig.get().fullbright ? LightmapTextureManager.MAX_LIGHT_COORDINATE : vanillaLight;
	}

	/**
	 * Draws a unit cube centered at the current matrix origin (caller scales it to
	 * {@link NoKABOOMConfig#heldBoxSize}). Each face is tinted with {@code argb}.
	 */
	public static void drawBox(MatrixStack.Entry entry, VertexConsumer consumer, int argb, int light) {
		float h = 0.5F;
		// +Y
		quad(entry, consumer, -h, h, -h, h, h, -h, h, h, h, -h, h, h, 0, 1, 0, argb, light);
		// -Y
		quad(entry, consumer, -h, -h, h, h, -h, h, h, -h, -h, -h, -h, -h, 0, -1, 0, argb, light);
		// +X
		quad(entry, consumer, h, -h, -h, h, -h, h, h, h, h, h, h, -h, 1, 0, 0, argb, light);
		// -X
		quad(entry, consumer, -h, -h, h, -h, -h, -h, -h, h, h, -h, h, h, -1, 0, 0, argb, light);
		// +Z
		quad(entry, consumer, -h, -h, h, h, -h, h, h, h, h, -h, h, h, 0, 0, 1, argb, light);
		// -Z
		quad(entry, consumer, h, -h, -h, -h, -h, -h, -h, h, -h, h, h, -h, 0, 0, -1, argb, light);
	}

	private static void quad(MatrixStack.Entry entry, VertexConsumer consumer,
			float x1, float y1, float z1,
			float x2, float y2, float z2,
			float x3, float y3, float z3,
			float x4, float y4, float z4,
			float nx, float ny, float nz,
			int argb, int light) {
		vertex(entry, consumer, x1, y1, z1, nx, ny, nz, argb, light);
		vertex(entry, consumer, x2, y2, z2, nx, ny, nz, argb, light);
		vertex(entry, consumer, x3, y3, z3, nx, ny, nz, argb, light);
		vertex(entry, consumer, x4, y4, z4, nx, ny, nz, argb, light);
	}

	private static void vertex(MatrixStack.Entry entry, VertexConsumer consumer,
			float x, float y, float z, float nx, float ny, float nz, int argb, int light) {
		consumer.vertex(entry, x, y, z)
				.color(argb)
				.texture(0.0F, 0.0F)
				.overlay(OverlayTexture.DEFAULT_UV)
				.light(light)
				.normal(entry, nx, ny, nz);
	}
}
