package com.newroze.nokaboom.mixin.client;

import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Read-only view into {@link ItemRenderState} internals.
 *
 * <p>Needed for the held-item glow: to tint the sword itself (not a box around
 * it) we re-submit its baked quads with an enchant-colored tint. The quads are
 * public via {@code getQuads()}, but the layer array, layer count and display
 * context are private — hence accessors (remapped at build time, safe for
 * production, unlike raw reflection on Yarn names).
 */
@Mixin(ItemRenderState.class)
public interface ItemRenderStateAccessor {
	@Accessor("layers")
	ItemRenderState.LayerRenderState[] nokaboom$getLayers();

	@Accessor("layerCount")
	int nokaboom$getLayerCount();

	@Accessor("displayContext")
	ItemDisplayContext nokaboom$getDisplayContext();
}
