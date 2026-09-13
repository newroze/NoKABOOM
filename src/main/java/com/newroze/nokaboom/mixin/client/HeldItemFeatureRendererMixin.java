package com.newroze.nokaboom.mixin.client;

import com.newroze.nokaboom.config.NoKABOOMConfig;
import com.newroze.nokaboom.render.NoKABOOMArmorHighlight;
import com.newroze.nokaboom.render.NoKABOOMHeldHighlight;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.ModelWithArms;
import net.minecraft.client.render.entity.state.ArmedEntityRenderState;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Highlights held items (swords etc.) carrying an enabled tracked enchantment.
 *
 * <p>In 1.21.11 {@code HeldItemFeatureRenderer.renderItem(...)} is called once per hand
 * with the exact {@link ItemStack} held there, plus the {@link ArmedEntityRenderState}
 * (player vs armor stand). Injecting at {@code TAIL} keeps vanilla behavior untouched
 * and only adds a translucent pulsing box when the held stack matches.
 */
@Mixin(HeldItemFeatureRenderer.class)
public abstract class HeldItemFeatureRendererMixin {
	@Shadow
	public abstract EntityModel<?> getContextModel();

	@SuppressWarnings({"rawtypes", "unchecked"})
	@Inject(
		method = "renderItem(Lnet/minecraft/client/render/entity/state/ArmedEntityRenderState;"
				+ "Lnet/minecraft/client/render/item/ItemRenderState;"
				+ "Lnet/minecraft/item/ItemStack;"
				+ "Lnet/minecraft/util/Arm;"
				+ "Lnet/minecraft/client/util/math/MatrixStack;"
				+ "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V",
		at = @At("TAIL")
	)
	private void nokaboom$highlightHeldItem(ArmedEntityRenderState state, ItemRenderState itemState,
			ItemStack stack, Arm arm, MatrixStack matrices, OrderedRenderCommandQueue queue,
			int light, CallbackInfo ci) {
		Integer rgb = NoKABOOMHeldHighlight.highlightRgb(state, stack);
		if (rgb == null) {
			return;
		}
		if (!(getContextModel() instanceof ModelWithArms arms)) {
			return;
		}

		int argb = (NoKABOOMArmorHighlight.pulseAlpha() << 24) | (rgb & 0xFFFFFF);
		int renderLight = NoKABOOMHeldHighlight.renderLight(light);
		float boxSize = NoKABOOMConfig.get().heldBoxSize;

		matrices.push();
		try {
			// Same hand positioning vanilla uses: shoulder pivot -> palm.
			((ModelWithArms) arms).setArmAngle(state, arm, matrices);
			matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90.0F));
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
			boolean leftHand = arm == Arm.LEFT;
			matrices.translate((leftHand ? -1.0F : 1.0F) / 16.0F, 0.125F, -0.625F);
			matrices.scale(boxSize, boxSize, boxSize);
			queue.submitCustom(matrices, RenderLayers.entityTranslucent(NoKABOOMHeldHighlight.TEXTURE),
					(entry, consumer) -> NoKABOOMHeldHighlight.drawBox(entry, consumer, argb, renderLight));
		} catch (Exception ignored) {
			// Rendering must never crash the frame; a missed glow is better than a crash.
		} finally {
			matrices.pop();
		}
	}
}
