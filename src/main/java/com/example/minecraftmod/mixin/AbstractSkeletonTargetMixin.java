package com.example.minecraftmod.mixin;

import com.example.minecraftmod.SkeletonPlayerTargetGoal;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSkeleton.class)
public abstract class AbstractSkeletonTargetMixin {
	@Inject(method = "registerGoals", at = @At("TAIL"))
	private void minecraftmod$addPlayerTargetGoal(CallbackInfo ci) {
		GoalSelector targetSelector = ((MonsterAccessor)(Object)this).minecraftmod$getTargetSelector();
		targetSelector.addGoal(0, new SkeletonPlayerTargetGoal((AbstractSkeleton)(Object)this, 50.0D));
	}
}
