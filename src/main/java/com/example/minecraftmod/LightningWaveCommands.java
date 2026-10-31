package com.example.minecraftmod;

import static net.minecraft.commands.Commands.literal;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class LightningWaveCommands {
	private LightningWaveCommands() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(literal("weather")
			.then(literal("ultra-thunder")
				.requires(source -> Commands.LEVEL_GAMEMASTERS.check(source.permissions()))
				.executes(context -> start(context.getSource()))));
	}

	private static int start(CommandSourceStack source) {
		if (!LightningWaveRules.forceUltraThunder(source.getLevel())) {
			source.sendFailure(Component.literal("Ultra-Thunder yalnızca Overworld'de kullanılabilir."));
			return 0;
		}

		source.sendSuccess(() -> Component.literal("Ultra-Thunder başlatıldı. Yıldırım Dalgası doğrudan tetiklendi."), true);
		return 1;
	}
}
