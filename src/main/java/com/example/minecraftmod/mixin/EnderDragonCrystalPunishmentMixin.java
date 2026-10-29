package com.example.minecraftmod.mixin;

import com.example.minecraftmod.DragonRules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnderDragon.class)
public abstract class EnderDragonCrystalPunishmentMixin {
	@Inject(method = "onCrystalDestroyed", at = @At("TAIL"))
	private void minecraftmod$punishCrystalBreak(ServerLevel level, EndCrystal crystal, BlockPos pos, DamageSource damageSource, CallbackInfo ci) {
		DragonRules.onCrystalDestroyed(level);
	}
}
