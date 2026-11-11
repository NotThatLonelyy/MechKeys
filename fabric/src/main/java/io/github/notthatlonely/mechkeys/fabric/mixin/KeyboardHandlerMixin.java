package io.github.notthatlonely.mechkeys.fabric.mixin;

import io.github.notthatlonely.mechkeys.fabric.MechKeysMod;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Read-only tap on the game's key callback: covers in-world and in-GUI keys. Never cancels anything.
 *
 * <p><b>PORTING NOTES (next update)</b>: the injection target is {@code KeyboardHandler.keyPress(long, int, KeyEvent)}. If a new version
 * changes that method's name or parameter list, update {@code method = "keyPress"} and the handler signature below. A mixin that
 * no longer matches fails at game start ({@code defaultRequire = 1} in mechkeys.mixins.json), so it cannot go unnoticed.
 */
@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {
    @Inject(method = "keyPress", at = @At("HEAD"))
    private void mechkeys$onKey(long window, int action, KeyEvent event, CallbackInfo ci) {
        MechKeysMod.onKey(event.key(), action);
    }
}
