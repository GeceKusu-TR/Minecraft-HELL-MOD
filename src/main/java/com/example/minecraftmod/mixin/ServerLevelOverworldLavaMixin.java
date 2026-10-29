package com.example.minecraftmod.mixin;

import com.example.minecraftmod.OverworldLavaRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ServerLevelOverworldLavaMixin {
	@Inject(method = "tickChunk", at = @At("HEAD"))
	private void minecraftmod$replaceOverworldWaterBeforeChunkTick(LevelChunk chunk, int randomTickSpeed, CallbackInfo ci) {
		OverworldLavaRules.replaceWaterInChunk((ServerLevel)(Object)this, chunk);
	}
}
