package com.example.minecraftmod.mixin;

import com.example.minecraftmod.NetherWaterRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ServerLevelNetherWaterMixin {
	@Inject(method = "tickChunk", at = @At("HEAD"))
	private void minecraftmod$replaceNetherLavaBeforeChunkTick(LevelChunk chunk, int randomTickSpeed, CallbackInfo ci) {
		NetherWaterRules.replaceLavaInChunk((ServerLevel)(Object)this, chunk);
	}
}
