package com.example.minecraftmod.mixin;

import com.example.minecraftmod.VillagerCombatRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Villager.class)
public abstract class VillagerCombatMixin {
	@Unique
	private int minecraftmod$tridentCooldown;

	@Inject(method = "customServerAiStep", at = @At("TAIL"))
	private void minecraftmod$attackVisiblePlayers(ServerLevel serverLevel, CallbackInfo ci) {
		Villager villager = (Villager)(Object)this;
		VillagerCombatRules.armVillager(villager, serverLevel);

		Player target = VillagerCombatRules.findVisiblePlayer(serverLevel, villager);
		if (target == null) {
			if (minecraftmod$tridentCooldown > 0) {
				minecraftmod$tridentCooldown--;
			}
			return;
		}

		villager.setTarget(target);

		if (minecraftmod$tridentCooldown > 0) {
			minecraftmod$tridentCooldown--;
			return;
		}

		VillagerCombatRules.throwTrident(serverLevel, villager, target);
		minecraftmod$tridentCooldown = 40;
	}

	@Inject(method = "tellWitnessesThatIWasMurdered", at = @At("TAIL"))
	private void minecraftmod$summonGolemWhenWitnessesSeeMurder(Entity killer, CallbackInfo ci) {
		if (!(killer instanceof Player player)) {
			return;
		}

		Villager victim = (Villager)(Object)this;
		if (!(victim.level() instanceof ServerLevel serverLevel)) {
			return;
		}

		for (Villager witness : VillagerCombatRules.findWitnesses(serverLevel, victim, player)) {
			VillagerCombatRules.summonAngryGolem(serverLevel, witness, player);
			break;
		}
	}
}
