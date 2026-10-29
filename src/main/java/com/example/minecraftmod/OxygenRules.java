package com.example.minecraftmod;

import net.minecraft.ChatFormatting;
import net.minecraft.world.Difficulty;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundClearTitlesPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class OxygenRules {
	public static final int OXYGEN_HEIGHT = 190;
	public static final int CRITICAL_OXYGEN_HEIGHT = 200;
	public static final int DEADLY_OXYGEN_HEIGHT = 210;
	public static final int DAMAGE_INTERVAL = 20;
	public static final int INFINITE_EFFECT_DURATION = MobEffectInstance.INFINITE_DURATION;
	public static final int TITLE_STAY_TICKS = 2_147_483_647;
	public static final Identifier TURTLE_SHELL_HEALTH_BOOST_ID = MinecraftMod.id("turtle_shell_health_boost");
	public static final double TURTLE_SHELL_HEALTH_BOOST_AMOUNT = 20.0D;
	public static final Identifier PEACEFUL_HEALTH_CAP_ID = MinecraftMod.id("peaceful_health_cap");
	public static final double PEACEFUL_HEALTH_CAP_AMOUNT = -18.0D;

	private OxygenRules() {
	}

	public static boolean isAboveOxygenHeight(Player player) {
		return oxygenStage(player) != OxygenStage.NONE;
	}

	public static boolean hasLeatherHelmet(Player player) {
		return helmet(player).is(Items.LEATHER_HELMET);
	}

	public static boolean hasTurtleShellHelmet(Player player) {
		return helmet(player).is(Items.TURTLE_HELMET);
	}

	public static boolean isUnderwater(Player player) {
		return isUnderwater((LivingEntity)player);
	}

	public static boolean isUnderwater(LivingEntity entity) {
		return entity.isEyeInFluid(FluidTags.WATER);
	}

	public static boolean isOutOfAir(Player player) {
		return player.getAirSupply() <= -20;
	}

	public static boolean isPeaceful(Player player) {
		return player.level().getDifficulty() == Difficulty.PEACEFUL;
	}

	public static boolean isBreathingButtonPressed(Player player) {
		if (!(player instanceof ServerPlayer serverPlayer)) {
			return false;
		}

		Input input = serverPlayer.getLastClientInput();
		return input != null && input.jump();
	}

	public static OxygenStage oxygenStage(Player player) {
		double y = player.getY();
		if (y >= DEADLY_OXYGEN_HEIGHT) {
			return OxygenStage.DEADLY;
		}

		if (y >= CRITICAL_OXYGEN_HEIGHT) {
			return OxygenStage.CRITICAL;
		}

		if (y >= OXYGEN_HEIGHT) {
			return OxygenStage.LOW;
		}

		return OxygenStage.NONE;
	}

	public static float oxygenDeprivationDamage(Player player) {
		return switch (oxygenStage(player)) {
			case LOW -> hasLeatherHelmet(player) ? 0.0F : 4.0F;
			case CRITICAL -> hasLeatherHelmet(player) ? 0.0F : 6.0F;
			case DEADLY -> 10.0F;
			case NONE -> 0.0F;
		};
	}

	public static float highAltitudeDamage(Player player) {
		return oxygenDeprivationDamage(player);
	}

	public static Component titleForStage(OxygenStage oxygenStage) {
		return switch (oxygenStage) {
			case LOW -> Component.literal("OKS\u0130JEN YETERS\u0130Z!").withStyle(ChatFormatting.RED);
			case CRITICAL -> Component.literal("KR\u0130T\u0130K OKS\u0130JEN YETERS\u0130ZL\u0130\u011E\u0130!!").withStyle(ChatFormatting.RED);
			case DEADLY -> Component.literal("OKS\u0130JEN YOK!!!").withStyle(ChatFormatting.RED);
			case NONE -> Component.empty();
		};
	}

	public static void showTitle(ServerPlayer player, Component title) {
		player.connection.send(new ClientboundSetTitlesAnimationPacket(0, TITLE_STAY_TICKS, 0));
		player.connection.send(new ClientboundSetTitleTextPacket(title));
	}

	public static void clearTitle(ServerPlayer player) {
		player.connection.send(new ClientboundClearTitlesPacket(true));
	}

	public static void applyInfiniteEffect(ServerPlayer player) {
		player.removeEffect(OxygenEffects.OXYGENLESS);
		player.addEffect(new MobEffectInstance(OxygenEffects.OXYGENLESS, INFINITE_EFFECT_DURATION, 0, false, false, true));
	}

	public static void applyTurtleShellHealthBoost(ServerPlayer player) {
		AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
		if (maxHealth == null || maxHealth.getModifier(TURTLE_SHELL_HEALTH_BOOST_ID) != null) {
			return;
		}

		maxHealth.addTransientModifier(new AttributeModifier(
			TURTLE_SHELL_HEALTH_BOOST_ID,
			TURTLE_SHELL_HEALTH_BOOST_AMOUNT,
			AttributeModifier.Operation.ADD_VALUE
		));
	}

	public static void applyPeacefulHealthCap(ServerPlayer player) {
		AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
		if (maxHealth == null) {
			return;
		}

		if (maxHealth.getModifier(PEACEFUL_HEALTH_CAP_ID) == null) {
			maxHealth.addTransientModifier(new AttributeModifier(
				PEACEFUL_HEALTH_CAP_ID,
				PEACEFUL_HEALTH_CAP_AMOUNT,
				AttributeModifier.Operation.ADD_VALUE
			));
		}

		float maxHealthValue = player.getMaxHealth();
		if (player.getHealth() > maxHealthValue) {
			player.setHealth(maxHealthValue);
		}
	}

	public static void applyPeacefulFoodCap(ServerPlayer player) {
		player.getFoodData().setFoodLevel(2);
		player.getFoodData().setSaturation(0.0F);
	}

	public static void clearPeacefulHealthCap(ServerPlayer player) {
		AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
		if (maxHealth == null) {
			return;
		}

		if (!maxHealth.removeModifier(PEACEFUL_HEALTH_CAP_ID)) {
			return;
		}

		float maxHealthValue = player.getMaxHealth();
		if (player.getHealth() > maxHealthValue) {
			player.setHealth(maxHealthValue);
		}
	}

	public static void clearTurtleShellHealthBoost(ServerPlayer player) {
		AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
		if (maxHealth == null) {
			return;
		}

		if (!maxHealth.removeModifier(TURTLE_SHELL_HEALTH_BOOST_ID)) {
			return;
		}

		float maxHealthValue = player.getMaxHealth();
		if (player.getHealth() > maxHealthValue) {
			player.setHealth(maxHealthValue);
		}
	}

	private static ItemStack helmet(Player player) {
		return player.getItemBySlot(EquipmentSlot.HEAD);
	}

	public enum OxygenStage {
		NONE,
		LOW,
		CRITICAL,
		DEADLY
	}
}
