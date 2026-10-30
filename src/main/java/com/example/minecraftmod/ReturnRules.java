package com.example.minecraftmod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import java.util.Optional;

public final class ReturnRules {
	public static final ResourceKey<Enchantment> RETURN_ENCHANTMENT =
		ResourceKey.create(Registries.ENCHANTMENT, MinecraftMod.id("geri_donus"));
	private static final String RETURN_BOOK_TAG = "minecraftmod_return_book";
	private static final String RETURN_STICK_TAG = "minecraftmod_return_stick";
	private static final int RETURN_STICK_MAX_HEALTH = 64;
	private static final Component RETURN_BOOK_NAME = Component.translatable("item.minecraftmod.return_book");
	private static final Component RETURN_STICK_NAME = Component.translatable("item.minecraftmod.return_stick");

	private ReturnRules() {
	}

	public static ItemStack createReturnBook(HolderLookup.Provider holders) {
		ItemStack stack = returnEnchantment(holders)
			.map(enchantment -> EnchantmentHelper.createBook(new EnchantmentInstance(enchantment, 1)))
			.orElseGet(() -> new ItemStack(Items.ENCHANTED_BOOK));
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean(RETURN_BOOK_TAG, true));
		stack.set(DataComponents.ITEM_NAME, RETURN_BOOK_NAME);
		stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
		return stack;
	}

	public static boolean isReturnBook(ItemStack stack, HolderLookup.Provider holders) {
		if (!stack.is(Items.ENCHANTED_BOOK)) {
			return false;
		}

		CompoundTag probe = new CompoundTag();
		probe.putBoolean(RETURN_BOOK_TAG, true);
		if (stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).matchedBy(probe)) {
			return true;
		}

		return hasReturnEnchantment(stack, holders, DataComponents.STORED_ENCHANTMENTS);
	}

	public static void markReturnStick(ItemStack stack, HolderLookup.Provider holders) {
		if (!stack.is(Items.STICK)) {
			return;
		}

		returnEnchantment(holders).ifPresent(enchantment -> stack.enchant(enchantment, 1));
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean(RETURN_STICK_TAG, true));
		stack.set(DataComponents.ITEM_NAME, RETURN_STICK_NAME);
		stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
		stack.set(DataComponents.MAX_STACK_SIZE, 1);
		stack.set(DataComponents.MAX_DAMAGE, RETURN_STICK_MAX_HEALTH);
		stack.set(DataComponents.DAMAGE, 0);
	}

	public static boolean isReturnStick(ItemStack stack, HolderLookup.Provider holders) {
		if (!stack.is(Items.STICK)) {
			return false;
		}

		CompoundTag probe = new CompoundTag();
		probe.putBoolean(RETURN_STICK_TAG, true);
		if (stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).matchedBy(probe)) {
			return true;
		}

		return hasReturnEnchantment(stack, holders, DataComponents.ENCHANTMENTS);
	}

	public static boolean pullBlockToInventory(ServerLevel level, ServerPlayer player, BlockPos pos) {
		if (!level.isLoaded(pos) || player.isSpectator() || !level.mayInteract(player, pos)) {
			return false;
		}

		BlockState state = level.getBlockState(pos);
		if (state.isAir()) {
			return false;
		}

		Item blockItem = state.getBlock().asItem();
		if (blockItem == Items.AIR) {
			return false;
		}

		ItemStack pulledStack = new ItemStack(blockItem);
		copyBlockEntityData(level, pos, pulledStack);
		level.addDestroyBlockEffect(pos, state);
		// Inventory data is already in pulledStack; do not also spill it into the world.
		boolean removed = level.setBlock(pos, level.getFluidState(pos).createLegacyBlock(),
			net.minecraft.world.level.block.Block.UPDATE_ALL | net.minecraft.world.level.block.Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS);
		if (!removed) {
			return false;
		}

		level.gameEvent(player, GameEvent.BLOCK_DESTROY, pos);
		level.playSound(null, pos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 0.6F, 1.35F);
		if (!player.getInventory().add(pulledStack)) {
			player.drop(pulledStack, false, false);
		}
		return true;
	}

	public static void damageReturnStickAfterUse(ServerLevel level, ServerPlayer player, ItemStack stack, BlockPos pulledPos, BlockState pulledState) {
		if (player.isCreative()) {
			return;
		}

		ensureReturnStickHealth(stack);
		int damage = DifficultyScalingRules.more(returnStickDamageFor(level, pulledPos, pulledState));
		int currentDamage = stack.getOrDefault(DataComponents.DAMAGE, 0);
		int nextDamage = currentDamage + damage;
		if (nextDamage >= RETURN_STICK_MAX_HEALTH) {
			stack.shrink(1);
			level.sendParticles(ParticleTypes.POOF, player.getX(), player.getY(0.6D), player.getZ(), 32, 0.25D, 0.35D, 0.25D, 0.03D);
			level.playSound(null, player.blockPosition(), SoundEvents.ITEM_BREAK.value(), SoundSource.PLAYERS, 0.8F, 0.55F);
			player.displayClientMessage(Component.literal("Geri D\u00f6n\u00fc\u015f \u00e7ubu\u011fu teslimattan sonra kendini feshetti."), true);
			return;
		}

		stack.set(DataComponents.DAMAGE, nextDamage);
		int remaining = RETURN_STICK_MAX_HEALTH - nextDamage;
		player.displayClientMessage(Component.literal("Geri D\u00f6n\u00fc\u015f \u00e7ubu\u011fu kalan can: " + remaining + "/" + RETURN_STICK_MAX_HEALTH), true);
	}

	private static void ensureReturnStickHealth(ItemStack stack) {
		stack.set(DataComponents.MAX_STACK_SIZE, 1);
		stack.set(DataComponents.MAX_DAMAGE, RETURN_STICK_MAX_HEALTH);
		if (!stack.has(DataComponents.DAMAGE)) {
			stack.set(DataComponents.DAMAGE, 0);
		}
	}

	private static int returnStickDamageFor(ServerLevel level, BlockPos pos, BlockState state) {
		if (state.is(Blocks.BEDROCK)) {
			return RETURN_STICK_MAX_HEALTH;
		}
		if (state.is(Blocks.OBSIDIAN) || state.is(Blocks.CRYING_OBSIDIAN)) {
			return 16;
		}
		if (state.is(Blocks.REINFORCED_DEEPSLATE) || state.is(Blocks.END_PORTAL_FRAME)) {
			return 32;
		}

		float destroySpeed = state.getDestroySpeed(level, pos);
		if (destroySpeed < 0.0F) {
			return RETURN_STICK_MAX_HEALTH;
		}
		if (destroySpeed >= 50.0F) {
			return 16;
		}
		if (destroySpeed >= 30.0F) {
			return 8;
		}
		if (destroySpeed >= 10.0F) {
			return 4;
		}
		return 1;
	}

	private static void copyBlockEntityData(ServerLevel level, BlockPos pos, ItemStack pulledStack) {
		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (blockEntity == null) {
			return;
		}

		pulledStack.applyComponents(blockEntity.collectComponents());
		pulledStack.set(
			DataComponents.BLOCK_ENTITY_DATA,
			TypedEntityData.of(blockEntity.getType(), blockEntity.saveCustomOnly(level.registryAccess()))
		);
	}

	private static Optional<Holder<Enchantment>> returnEnchantment(HolderLookup.Provider holders) {
		HolderGetter<Enchantment> enchantments = (HolderGetter<Enchantment>) holders.lookupOrThrow(Registries.ENCHANTMENT);
		return enchantments.get(RETURN_ENCHANTMENT).map(holder -> (Holder<Enchantment>) holder);
	}

	private static boolean hasReturnEnchantment(ItemStack stack, HolderLookup.Provider holders, net.minecraft.core.component.DataComponentType<ItemEnchantments> componentType) {
		Optional<Holder<Enchantment>> returnEnchantment = returnEnchantment(holders);
		if (returnEnchantment.isEmpty()) {
			return false;
		}

		ItemEnchantments enchantments = stack.getOrDefault(componentType, ItemEnchantments.EMPTY);
		return enchantments.getLevel(returnEnchantment.get()) > 0;
	}
}
