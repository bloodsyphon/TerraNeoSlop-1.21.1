package com.dfsek.terra.neoforge.mixin.fix;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.dfsek.terra.mod.CommonPlatform;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.bee.Bee.BeeGoToHiveGoal;
import net.minecraft.world.entity.animal.bee.Bee.BeeGoToKnownFlowerGoal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.LegacyRandomSource;


/**
 * Bees spawning uses world.random without synchronization. This causes issues when spawning bees during world generation.
 */
@Mixin({
    BeeGoToHiveGoal.class,
    BeeGoToKnownFlowerGoal.class
})
public class BeeMoveGoalsUnsynchronizedRandomAccessFix {
    @Redirect(method = "<init>",
              at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/Level;random:Lnet/minecraft/util/RandomSource;"))
    public RandomSource redirectRandomAccess(Level instance) {
        return new LegacyRandomSource(CommonPlatform.get().getServer().getTickCount()); // replace with new random seeded by tick time.
    }
}
