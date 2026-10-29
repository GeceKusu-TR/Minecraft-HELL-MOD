package com.example.minecraftmod.mixin;

import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Mob.class)
public interface MonsterAccessor {
	@Accessor("goalSelector")
	GoalSelector minecraftmod$getGoalSelector();

	@Accessor("targetSelector")
	GoalSelector minecraftmod$getTargetSelector();
}
