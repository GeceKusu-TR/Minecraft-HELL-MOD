package com.example.minecraftmod.mixin;

import com.example.minecraftmod.UndeadRules;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Zombie.class)
public abstract class ZombieHelmetMixin {
	@Inject(method = "finalizeSpawn", at = @At("TAIL"))
	private void minecraftmod$giveZombieHelmet(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, SpawnGroupData spawnData, CallbackInfoReturnable<SpawnGroupData> cir) {
		UndeadRules.ensureLeatherHelmet((Zombie)(Object)this);
	}
}
