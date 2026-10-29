package com.example.minecraftmod.mixin;

import com.example.minecraftmod.NOSpeedrunRules;
import java.util.function.Consumer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(LivingEntity.class)
public abstract class LivingEntityDeathLootMixin {
	@ModifyArg(
			method = "dropFromLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;ZLnet/minecraft/resources/ResourceKey;Ljava/util/function/Consumer;)V",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/level/storage/loot/LootTable;getRandomItems(Lnet/minecraft/world/level/storage/loot/LootParams;JLjava/util/function/Consumer;)V"
			),
			index = 2
	)
	private Consumer<ItemStack> minecraftmod$blockEndermanPearls(Consumer<ItemStack> originalDropper) {
		return NOSpeedrunRules.filterEndermanDeathDrops((LivingEntity)(Object)this, originalDropper);
	}
}
