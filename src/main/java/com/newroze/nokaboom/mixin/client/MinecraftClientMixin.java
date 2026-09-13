package com.newroze.nokaboom.mixin.client;

import com.newroze.nokaboom.gui.NoKABOOMKeybinds;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Polls the NoKABOOM menu hotkey once per client tick (no Fabric API tick events needed). */
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
	@Inject(method = "tick", at = @At("TAIL"))
	private void nokaboom$pollMenuKey(CallbackInfo ci) {
		NoKABOOMKeybinds.poll((MinecraftClient) (Object) this);
	}
}
