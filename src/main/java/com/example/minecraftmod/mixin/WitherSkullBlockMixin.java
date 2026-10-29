package com.example.minecraftmod.mixin;

import com.example.minecraftmod.AdvancementRules;
import net.minecraft.world.entity.Entity;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.WitherSkullBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(WitherSkullBlock.class)
public abstract class WitherSkullBlockMixin {
	@WrapOperation(method = "checkSpawn(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/SkullBlockEntity;)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
	private static boolean minecraftmod$grantWitherSpawnAdvancement(Level level, Entity entity, Operation<Boolean> original) {
		boolean added = original.call(level, entity);
		if (added) {
			AdvancementRules.grantPlacedWither(entity);
		}
		return added;
	}
}
