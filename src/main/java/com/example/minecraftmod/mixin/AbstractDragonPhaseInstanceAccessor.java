package com.example.minecraftmod.mixin;

import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.AbstractDragonPhaseInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractDragonPhaseInstance.class)
public interface AbstractDragonPhaseInstanceAccessor {
	@Accessor("dragon")
	EnderDragon minecraftmod$getDragon();
}
