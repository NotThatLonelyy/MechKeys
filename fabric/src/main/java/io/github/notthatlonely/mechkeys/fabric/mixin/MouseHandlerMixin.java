package io.github.notthatlonely.mechkeys.fabric.mixin;

import io.github.notthatlonely.mechkeys.fabric.MechKeysMod;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Read-only tap on the game's mouse-button callback: covers in-world and in-GUI clicks. Never cancels anything.
 *
 * <p><b>PORTING NOTES (next update)</b>: the injection target is {@code MouseHandler.onButton(long, MouseButtonInfo, int)}. If a new
 * version changes that method's name or parameter list, update {@code method = "onButton"} and the handler signature below.
 */
@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
    @Inject(method = "onButton", at = @At("HEAD"))
    private void mechkeys$onButton(long window, MouseButtonInfo info, int action, CallbackInfo ci) {
        MechKeysMod.onMouse(info.button(), action);
    }
}
