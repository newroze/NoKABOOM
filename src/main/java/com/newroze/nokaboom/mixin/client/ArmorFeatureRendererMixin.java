package com.newroze.nokaboom.mixin.client;

import com.newroze.nokaboom.render.NoKABOOMArmorHighlight;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.equipment.EquipmentModel;
import net.minecraft.client.render.entity.equipment.EquipmentRenderer;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.equipment.EquipmentAsset;
import net.minecraft.registry.RegistryKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Tints armor pieces carrying an enabled tracked enchantment in the
 * enchantment's color — the armor's own texture, not a box or flat shell.
 *
 * <p>In 1.21.11 {@code ArmorFeatureRenderer.renderArmor(...)} draws one slot
 * via {@code EquipmentRenderer.render(...)} (real armor textures, dye, trim).
 * We redirect exactly that call: vanilla renders first, then — only when that
 * exact piece carries an enabled tracked enchantment — it renders again
 * slightly scaled up with the dye color swapped for the enchantment color
 * (see {@code EquipmentRendererMixin}). Vanilla leather dyeing works the same
 * way (texture × color), so the result looks native: same pixels, just colored.
 */
@Mixin(ArmorFeatureRenderer.class)
public abstract class ArmorFeatureRendererMixin {
	/**
	 * @Redirect on the armor draw call. The trailing parameters are the
	 * enclosing {@code renderArmor(...)} arguments (required by Mixin).
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	@Redirect(
		method = "renderArmor(Lnet/minecraft/client/util/math/MatrixStack;"
				+ "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;"
				+ "Lnet/minecraft/item/ItemStack;"
				+ "Lnet/minecraft/entity/EquipmentSlot;I"
				+ "Lnet/minecraft/client/render/entity/state/BipedEntityRenderState;)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/render/entity/equipment/EquipmentRenderer;render("
					+ "Lnet/minecraft/client/render/entity/equipment/EquipmentModel$LayerType;"
					+ "Lnet/minecraft/registry/RegistryKey;"
					+ "Lnet/minecraft/client/model/Model;"
					+ "Ljava/lang/Object;"
					+ "Lnet/minecraft/item/ItemStack;"
					+ "Lnet/minecraft/client/util/math/MatrixStack;"
					+ "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;II)V"
		)
	)
	private void nokaboom$renderWithTint(EquipmentRenderer renderer, EquipmentModel.LayerType layerType,
			RegistryKey<EquipmentAsset> asset, Model model, Object state, ItemStack stack,
			MatrixStack matrices, OrderedRenderCommandQueue queue, int light, int overlay,
			MatrixStack enclosingMatrices, OrderedRenderCommandQueue enclosingQueue, ItemStack enclosingStack,
			EquipmentSlot slot, int enclosingLight, BipedEntityRenderState enclosingState) {
		// Vanilla draw first — untouched.
		renderer.render(layerType, asset, model, state, stack, matrices, queue, light, overlay);
		Integer rgb = NoKABOOMArmorHighlight.highlightRgb(enclosingState, enclosingStack);
		if (rgb == null) {
			return;
		}
		// Slightly inflate the tint shell so it never z-fights the armor underneath.
		float scale = NoKABOOMArmorHighlight.tintScale();
		matrices.push();
		try {
			matrices.scale(scale, scale, scale);
			NoKABOOMArmorHighlight.TINT_OVERRIDE.set(0xFF000000 | (rgb & 0xFFFFFF));
			try {
				renderer.render(layerType, asset, model, state, stack, matrices, queue, light, overlay);
			} finally {
				NoKABOOMArmorHighlight.TINT_OVERRIDE.remove();
			}
		} catch (Exception ignored) {
			// Rendering must never crash the frame; a missed tint is better than a crash.
		} finally {
			matrices.pop();
		}
	}
}
