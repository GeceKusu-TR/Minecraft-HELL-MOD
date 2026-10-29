package com.example.minecraftmod.mixin;

import com.example.minecraftmod.AdvancementRules;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.advancements.TreeNodePosition;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.LinkedHashMap;
import java.util.Map;

@Mixin(ServerAdvancementManager.class)
public abstract class ServerAdvancementManagerMixin {
	@Shadow @Final @Mutable private Map<Identifier, AdvancementHolder> advancements;
	@Shadow @Final @Mutable private AdvancementTree tree;

	@Inject(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V", at = @At("TAIL"))
	private void minecraftmod$addWelcomeAdvancement(Map<Identifier, Advancement> input, ResourceManager resourceManager, ProfilerFiller profiler, CallbackInfo ci) {
		minecraftmod$rebuildAdvancements();
	}

	@Inject(method = "<init>", at = @At("RETURN"))
	private void minecraftmod$initializeWelcomeAdvancement(HolderLookup.Provider registries, CallbackInfo ci) {
		minecraftmod$rebuildAdvancements();
	}

	private void minecraftmod$rebuildAdvancements() {
		Map<Identifier, Advancement> rawAdvancements = new LinkedHashMap<>();
		for (AdvancementHolder holder : this.advancements.values()) {
			rawAdvancements.put(holder.id(), holder.value());
		}

		AdvancementRules.disableConflictingVanillaAdvancements(rawAdvancements);
		rawAdvancements.putIfAbsent(AdvancementRules.ROOT_ID, AdvancementRules.createRootAdvancement());
		rawAdvancements.putIfAbsent(AdvancementRules.WELCOME_TO_HELL_ID, AdvancementRules.createWelcomeToHellAdvancement());
		rawAdvancements.putIfAbsent(AdvancementRules.GHAST_DEATH_ID, AdvancementRules.createGhastDeathAdvancement());
		rawAdvancements.putIfAbsent(AdvancementRules.DIRT_SHOVEL_ID, AdvancementRules.createDirtShovelAdvancement());
		rawAdvancements.putIfAbsent(AdvancementRules.WELCOME_TO_OVERWORLD_ID, AdvancementRules.createWelcomeToOverworldAdvancement());
		rawAdvancements.putIfAbsent(AdvancementRules.PIGLIN_POOP_BARTER_ID, AdvancementRules.createPiglinPoopBarterAdvancement());
		rawAdvancements.putIfAbsent(AdvancementRules.NPC_ENDERMAN_ID, AdvancementRules.createNpcEndermanAdvancement());
		rawAdvancements.putIfAbsent(AdvancementRules.WITHER_SKULLS_ID, AdvancementRules.createWitherSkullsAdvancement());
		rawAdvancements.putIfAbsent(AdvancementRules.WITHER_SPAWN_ID, AdvancementRules.createWitherSpawnAdvancement());
		rawAdvancements.putIfAbsent(AdvancementRules.WITHER_KILL_ID, AdvancementRules.createWitherKillAdvancement());
		rawAdvancements.putIfAbsent(AdvancementRules.BEACON_TAX_ID, AdvancementRules.createBeaconTaxAdvancement());
		rawAdvancements.putIfAbsent(AdvancementRules.READ_AND_ACCEPTED_ID, AdvancementRules.createReadAndAcceptedAdvancement());

		Map<Identifier, AdvancementHolder> updatedAdvancements = new LinkedHashMap<>();
		for (Map.Entry<Identifier, Advancement> entry : rawAdvancements.entrySet()) {
			updatedAdvancements.put(entry.getKey(), new AdvancementHolder(entry.getKey(), entry.getValue()));
		}

		AdvancementTree updatedTree = new AdvancementTree();
		updatedTree.addAll(updatedAdvancements.values());
		for (AdvancementNode root : updatedTree.roots()) {
			if (root.holder().value().display().isPresent()) {
				TreeNodePosition.run(root);
			}
		}
		this.advancements = updatedAdvancements;
		this.tree = updatedTree;
	}
}
