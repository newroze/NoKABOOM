package com.newroze.nokaboom.mixin.client;

import com.newroze.nokaboom.gui.NoKABOOMConfigScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds a small "NoKABOOM..." button to the pause menu so the config is reachable without a hotkey. */
@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin extends Screen {
	protected GameMenuScreenMixin(Text title) {
		super(title);
	}

	@Shadow
	protected abstract <T extends Element & Drawable & Selectable> T addDrawableChild(T drawableChild);

	@Inject(method = "init", at = @At("TAIL"))
	private void nokaboom$addConfigButton(CallbackInfo ci) {
		GameMenuScreen self = (GameMenuScreen) (Object) this;
		ButtonWidget button = ButtonWidget.builder(Text.literal("NoKABOOM..."), btn -> {
			MinecraftClient client = MinecraftClient.getInstance();
			client.setScreen(new NoKABOOMConfigScreen(self));
		}).dimensions(10, this.height - 30, 110, 20).build();
		addDrawableChild(button);
	}
}
