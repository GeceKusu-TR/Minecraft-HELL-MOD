package com.example.minecraftmod.mixin;

import com.example.minecraftmod.NoMobCommands;
import com.example.minecraftmod.EndTermsRules;
import com.example.minecraftmod.LightningWaveCommands;
import com.example.minecraftmod.TestBookRules;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Commands.class)
public abstract class CommandsMixin {
	@Shadow
	@Final
	private CommandDispatcher<CommandSourceStack> dispatcher;

	@Inject(method = "<init>", at = @At("TAIL"))
	private void minecraftmod$registerNoMobCommand(Commands.CommandSelection commandSelection, CommandBuildContext commandBuildContext, CallbackInfo ci) {
		NoMobCommands.register(this.dispatcher);
		EndTermsRules.register(this.dispatcher);
		LightningWaveCommands.register(this.dispatcher);
		TestBookRules.register(this.dispatcher);
	}
}
