package com.example.minecraftmod.mixin;

import com.example.minecraftmod.TntRules;
import net.minecraft.world.entity.item.PrimedTnt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PrimedTnt.class)
public abstract class PrimedTntMixin {
	@Inject(method = "tick", at = @At("HEAD"))
	private void minecraftmod$pullEndermenAndExplode(CallbackInfo ci) {
		TntRules.tickEndermanTnt((PrimedTnt)(Object)this);
	}
}
