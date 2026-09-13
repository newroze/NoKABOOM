package com.newroze.nokaboom.mixin.client;

import com.newroze.nokaboom.config.NoKABOOMConfig;
import com.newroze.nokaboom.render.NoKABOOMArmorHighlight;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks vanilla armor rendering to paint the per-enchantment highlight film.
 *
 * <p>In 1.21.11 {@code ArmorFeatureRenderer} renders every slot through the private
 * {@code renderArmor(matrices, queue, stack, slot, light, state)} helper, which already
 * receives the exact {@link ItemStack} worn in that slot plus the
 * {@link BipedEntityRenderState} (player vs armor stand). Injecting at {@code TAIL}
 * keeps vanilla behavior untouched and only adds a slightly larger translucent shell
 * on top when that exact piece carries an enabled tracked enchantment.
 */
@Mixin(ArmorFeatureRenderer.class)
public abstract class ArmorFeatureRendererMixin {
	/**
	 * Vanilla's per-slot model lookup (inner model for leggings, outer otherwise).
	 * The target method is private, so the shadow uses a dummy body instead of
	 * {@code abstract} ({@code private abstract} is illegal in Java).
	 */
	@Shadow
	private BipedEntityModel<?> getModel(BipedEntityRenderState state, EquipmentSlot slot) {
		throw new AssertionError("Mixin shadow");
	}

	/**
	 * Runs after vanilla finished one armor piece. Draws the highlight shell only
	 * when that exact piece carries an enabled tracked enchantment.
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	@Inject(
		method = "renderArmor(Lnet/minecraft/client/util/math/MatrixStack;"
				+ "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;"
				+ "Lnet/minecraft/item/ItemStack;"
				+ "Lnet/minecraft/entity/EquipmentSlot;I"
				+ "Lnet/minecraft/client/render/entity/state/BipedEntityRenderState;)V",
		at = @At("TAIL")
	)
	private void nokaboom$highlightBlastProtection(MatrixStack matrices, OrderedRenderCommandQueue queue,
			ItemStack stack, EquipmentSlot slot, int light, BipedEntityRenderState state, CallbackInfo ci) {
		if (NoKABOOMArmorHighlight.highlightRgb(state, stack) == null) {
			return;
		}
		BipedEntityModel<?> armorModel;
		try {
			armorModel = getModel(state, slot);
		} catch (Exception ignored) {
			return;
		}
		if (armorModel == null) {
			return;
		}

		// Slightly inflate the shell so it never z-fights the armor underneath.
		float scale = NoKABOOMConfig.get().expandScale;
		matrices.push();
		try {
			matrices.scale(scale, scale, scale);
			queue.getBatchingQueue(NoKABOOMArmorHighlight.QUEUE_ORDER).submitModel(
					(Model) armorModel,
					state,
					matrices,
					RenderLayers.entityTranslucent(NoKABOOMArmorHighlight.TEXTURE),
					NoKABOOMArmorHighlight.renderLight(light),
					OverlayTexture.DEFAULT_UV,
					NoKABOOMArmorHighlight.highlightColor(stack),
					null,
					state.outlineColor,
					null);
		} catch (Exception ignored) {
			// Rendering must never crash the frame; a missed glow is better than a crash.
		} finally {
			matrices.pop();
		}
	}
}
