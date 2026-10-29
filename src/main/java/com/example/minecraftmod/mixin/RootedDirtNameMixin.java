package com.example.minecraftmod.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class RootedDirtNameMixin {
    @Inject(method = "getName", at = @At("HEAD"), cancellable = true)
    private void minecraftmod$renameRootedDirt(CallbackInfoReturnable<Component> cir) {
        if ((Object) this == Items.ROOTED_DIRT) {
            cir.setReturnValue(Component.literal("Poop"));
        }
    }
}
