package com.newroze.nokaboom.mixin.client;

import com.newroze.nokaboom.render.NoKABOOMHeldHighlight;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.entity.state.ArmedEntityRenderState;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Highlights held items (swords etc.) carrying an enabled tracked enchantment.
 *
 * <p>In 1.21.11 {@code HeldItemFeatureRenderer.renderItem(...)} positions the
 * hand (arm angle, swing, bow-pull, use poses) and then draws the baked item
 * via {@code ItemRenderState.render(...)}. We redirect exactly that draw call:
 * vanilla renders first, then we submit the glow overlay with the <em>same</em>
 * matrices — so the glow follows every animation frame-perfectly, with no
 * duplicated transform math to drift out of sync.
 *
 * <p>The glow itself is the item's own quads re-submitted with an
 * enchant-colored tint (see {@link NoKABOOMHeldHighlight}): the sword texture
 * shines, no box is drawn.
 */
@Mixin(HeldItemFeatureRenderer.class)
public abstract class HeldItemFeatureRendererMixin {
	@Redirect(
		method = "renderItem(Lnet/minecraft/client/render/entity/state/ArmedEntityRenderState;"
				+ "Lnet/minecraft/client/render/item/ItemRenderState;"
				+ "Lnet/minecraft/item/ItemStack;"
				+ "Lnet/minecraft/util/Arm;"
				+ "Lnet/minecraft/client/util/math/MatrixStack;"
				+ "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/render/item/ItemRenderState;render("
					+ "Lnet/minecraft/client/util/math/MatrixStack;"
					+ "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;III)V"
		)
	)
	private void nokaboom$renderWithGlow(ItemRenderState itemState, MatrixStack matrices,
			OrderedRenderCommandQueue queue, int light, int overlay, int outlineColor,
			ArmedEntityRenderState state, ItemRenderState enclosingItemState, ItemStack stack,
			Arm arm, MatrixStack enclosingMatrices, OrderedRenderCommandQueue enclosingQueue, int enclosingLight) {
		// Vanilla draw first — untouched.
		itemState.render(matrices, queue, light, overlay, outlineColor);
		try {
			Integer rgb = NoKABOOMHeldHighlight.highlightRgb(state, stack);
			if (rgb == null) {
				return;
			}
			NoKABOOMHeldHighlight.renderGlow(matrices, queue, itemState, light, overlay, outlineColor, rgb);
		} catch (Exception ignored) {
			// Rendering must never crash the frame; a missed glow is better than a crash.
		}
	}
}
