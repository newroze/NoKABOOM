package com.newroze.nokaboom.mixin.client;

import com.newroze.nokaboom.config.NoKABOOMConfig;
import com.newroze.nokaboom.render.NoKABOOMArmorHighlight;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks vanilla armor rendering to paint the red Blast Protection film.
 *
 * <p><b>Why this injection point?</b> In 1.21.11
 * {@code ArmorFeatureRenderer.renderArmor(...)} is called once per armor slot
 * (head/chest/legs/feet) with the exact {@link ItemStack} worn there, and it
 * already receives the {@link BipedEntityRenderState} (so we know whether we
 * look at a player or an armor stand). Injecting at {@code TAIL} means vanilla
 * draws the armor first and we add a slightly larger translucent shell on top —
 * per piece, with zero changes to vanilla behavior when the enchant is absent.
 *
 * <p>This single hook covers players <em>and</em> armor stands, because both
 * renderers ({@code PlayerEntityRenderer}, {@code ArmorStandEntityRenderer})
 * delegate armor to {@code ArmorFeatureRenderer}.
 *
 * <p>Threading: rendering happens on the client render thread; the overlay
 * submits into the same {@link OrderedRenderCommandQueue}, so no sync needed.
 */
@Mixin(ArmorFeatureRenderer.class)
public abstract class ArmorFeatureRendererMixin {
	/** Vanilla's per-slot model lookup (inner model for leggings, outer otherwise). */
	@Shadow
	private abstract BipedEntityModel<?> getModel(BipedEntityRenderState state, EquipmentSlot slot);

	/** Vanilla's generic model submitter (handles texture + ARGB tint + lighting). */
	@Shadow
	protected static void renderModel(Model model, Identifier texture, MatrixStack matrices,
			OrderedRenderCommandQueue queue, int light, LivingEntityRenderState state, int color, int queueOrder) {
		throw new AssertionError("Mixin shadow");
	}

	/**
	 * Runs after vanilla finished one armor piece. Draws the red shell only
	 * when that exact piece carries Blast Protection.
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
		if (!NoKABOOMArmorHighlight.shouldHighlight(state, stack)) {
			return;
		}
		BipedEntityModel<?> armorModel = getModel(state, slot);
		if (armorModel == null) {
			return;
		}

		// Slightly inflate the shell so it never z-fights the armor underneath.
		float scale = NoKABOOMConfig.get().expandScale;
		matrices.push();
		try {
			matrices.scale(scale, scale, scale);
			renderModel(armorModel, NoKABOOMArmorHighlight.TEXTURE, matrices, queue,
					NoKABOOMArmorHighlight.renderLight(light), state,
					NoKABOOMArmorHighlight.highlightColor(), NoKABOOMArmorHighlight.QUEUE_ORDER);
		} finally {
			matrices.pop();
		}
	}
}
