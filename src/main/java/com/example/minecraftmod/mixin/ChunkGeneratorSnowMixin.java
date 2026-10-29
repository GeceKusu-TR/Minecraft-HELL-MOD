package com.example.minecraftmod.mixin;

import com.example.minecraftmod.SnowRules;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkGenerator.class)
public abstract class ChunkGeneratorSnowMixin {
	@Inject(method = "applyBiomeDecoration", at = @At("TAIL"))
	private void minecraftmod$applySnowOnChunkGeneration(WorldGenLevel worldGenLevel, ChunkAccess chunk, StructureManager structureManager, CallbackInfo ci) {
		SnowRules.applyToChunk(worldGenLevel, chunk);
	}
}
