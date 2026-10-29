package com.example.minecraftmod;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class VisibilityRules {
	private VisibilityRules() {
	}

	public static boolean canSeeThroughBlocksAndLava(Level level, Entity viewer, Vec3 start, Vec3 end) {
		HitResult blockHit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, viewer));
		if (blockHit.getType() != HitResult.Type.MISS) {
			return false;
		}

		Vec3 delta = end.subtract(start);
		int steps = Math.max(1, (int)Math.ceil(delta.length() * 4.0D));
		for (int step = 0; step <= steps; step++) {
			Vec3 check = start.add(delta.scale((double)step / (double)steps));
			if (level.getFluidState(BlockPos.containing(check)).is(FluidTags.LAVA)) {
				return false;
			}
		}
		return true;
	}
}
