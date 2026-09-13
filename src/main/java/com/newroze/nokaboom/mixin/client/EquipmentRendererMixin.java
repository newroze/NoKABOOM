package com.newroze.nokaboom.mixin.client;

import com.newroze.nokaboom.render.NoKABOOMArmorHighlight;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.command.RenderCommandQueue;
import net.minecraft.client.render.entity.equipment.EquipmentModel;
import net.minecraft.client.render.entity.equipment.EquipmentRenderer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Applies the enchant tint during NoKABOOM's second armor pass.
 *
 * <p>Vanilla submits every armor layer (base + trim) via
 * {@code RenderCommandQueue.submitModel(...)} with the dye color. When
 * {@link NoKABOOMArmorHighlight#TINT_OVERRIDE} is set (only around our tint
 * pass, never during the normal render), the dye color is swapped for the
 * enchantment color and fullbright is forced — the armor's own texture stays,
 * only its color changes. Outside the tint pass everything is passed through
 * untouched.
 */
@Mixin(EquipmentRenderer.class)
public abstract class EquipmentRendererMixin {
	/**
	 * @Redirect on every armor model submit in the full
	 * {@code EquipmentRenderer.render(...)} overload (base layers + trim).
	 * The trailing parameters are the enclosing {@code render(...)} arguments
	 * (required by Mixin).
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	@Redirect(
		method = "render(Lnet/minecraft/client/render/entity/equipment/EquipmentModel$LayerType;"
				+ "Lnet/minecraft/registry/RegistryKey;"
				+ "Lnet/minecraft/client/model/Model;"
				+ "Ljava/lang/Object;"
				+ "Lnet/minecraft/item/ItemStack;"
				+ "Lnet/minecraft/client/util/math/MatrixStack;"
				+ "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;"
				+ "ILnet/minecraft/util/Identifier;II)V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/render/command/RenderCommandQueue;submitModel("
					+ "Lnet/minecraft/client/model/Model;"
					+ "Ljava/lang/Object;"
					+ "Lnet/minecraft/client/util/math/MatrixStack;"
					+ "Lnet/minecraft/client/render/RenderLayer;III"
					+ "Lnet/minecraft/client/texture/Sprite;I"
					+ "Lnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;)V"
		)
	)
	private void nokaboom$swapDyeForEnchant(RenderCommandQueue queue, Model model, Object state,
			MatrixStack matrices, RenderLayer layer, int light, int overlay, int color,
			Sprite sprite, int outlineColor, ModelCommandRenderer.CrumblingOverlayCommand crumbling,
			EquipmentModel.LayerType layerType, RegistryKey asset, Model enclosingModel, Object enclosingState,
			ItemStack stack, MatrixStack enclosingMatrices, OrderedRenderCommandQueue enclosingQueue,
			int enclosingLight, Identifier textureOverride, int enclosingOutline, int enclosingOrder) {
		Integer tint = NoKABOOMArmorHighlight.TINT_OVERRIDE.get();
		if (tint != null) {
			color = tint;
			light = NoKABOOMArmorHighlight.renderLight(light);
		}
		queue.submitModel(model, state, matrices, layer, light, overlay, color, sprite, outlineColor, crumbling);
	}
}
