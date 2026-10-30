package com.example.minecraftmod.audit;

import com.example.minecraftmod.GrassCurseRules;
import com.example.minecraftmod.DifficultyScalingRules;
import com.example.minecraftmod.WitherRules;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.MixinEnvironment;

public final class BugAudit implements ModInitializer {
    @Override
    public void onInitialize() {
        MixinEnvironment.getCurrentEnvironment().audit();
        try {
            var setTicks = GrassCurseRules.class.getDeclaredMethod("setRemainingTicks", ItemStack.class, int.class);
            setTicks.setAccessible(true);
            ItemStack grass = new ItemStack(Items.GRASS_BLOCK);
            int scaledGrassTicks = DifficultyScalingRules.sooner(200);
            setTicks.invoke(null, grass, scaledGrassTicks);
            check(GrassCurseRules.isActiveCursedGrass(grass), "Grass is cursed and active");
            check(grass.getMaxDamage() == scaledGrassTicks && grass.getDamageValue() == 0, "Scaled grass durability");
            check(grass.getHoverName().getString().equals(Integer.toString((scaledGrassTicks + 19) / 20)), "Initial countdown label");
            setTicks.invoke(null, grass, 1);
            check(grass.getDamageValue() == scaledGrassTicks - 1 && grass.getHoverName().getString().equals("1"), "Final countdown tick");
            setTicks.invoke(null, grass, 0);
            check(!GrassCurseRules.isActiveCursedGrass(grass), "Expired grass is inactive");
            check(grass.get(DataComponents.MAX_STACK_SIZE) == 1, "Cursed grass cannot stack");
            check(WitherRules.extendWitherEffect(new MobEffectInstance(MobEffects.WITHER, -1)).isInfiniteDuration(), "Infinite wither stays infinite");
            check(WitherRules.extendWitherEffect(new MobEffectInstance(MobEffects.WITHER, 20)).getDuration() == DifficultyScalingRules.more(400), "Short wither is extended");
            String mixinConfig = new String(BugAudit.class.getClassLoader()
                .getResourceAsStream("minecraftmod.mixins.json")
                .readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            check(!mixinConfig.contains("PlayerExhaustionMixin"), "PlayerExhaustionMixin must stay disabled so lunge cost reaches FoodDataMixin");
            System.out.println("BUG AUDIT PASSED: server mixin audit and 9 regression assertions.");
            java.nio.file.Files.writeString(java.nio.file.Path.of(System.getProperty("minecraftmod.auditResult")), "passed\n");
        } catch (ReflectiveOperationException | java.io.IOException exception) {
            throw new AssertionError(exception);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
