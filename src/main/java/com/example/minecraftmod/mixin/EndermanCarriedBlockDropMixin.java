package com.example.minecraftmod.mixin;

import com.example.minecraftmod.EndermanRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.monster.EnderMan;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnderMan.class)
public abstract class EndermanCarriedBlockDropMixin {
	@Inject(method = "dropCustomDeathLoot", at = @At("HEAD"))
	private void minecraftmod$blockGrassCarriedDeathDrop(ServerLevel level, DamageSource damageSource, boolean recentlyHit, CallbackInfo ci) {
		EndermanRules.clearForbiddenCarriedDeathDrops((EnderMan)(Object)this);
	}
}
