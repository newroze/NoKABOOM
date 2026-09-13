package com.newroze.nokaboom.mixin.client;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.item.model.special.SpecialModelRenderer;
import net.minecraft.client.render.model.json.Transformation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Read-only view into one baked item layer (quads + which texture/pipeline
 * they render with). Used by the held-item glow to re-submit the exact same
 * sword geometry with an enchant-colored tint.
 */
@Mixin(ItemRenderState.LayerRenderState.class)
public interface ItemLayerAccessor {
	@Accessor("renderLayer")
	RenderLayer nokaboom$getRenderLayer();

	@Accessor("transform")
	Transformation nokaboom$getTransform();

	@Accessor("specialModelType")
	SpecialModelRenderer<?> nokaboom$getSpecialModel();
}
