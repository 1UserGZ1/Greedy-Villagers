package com.dyxiaojiazi.greedy_villagers.mixin;

import com.dyxiaojiazi.greedy_villagers.behavior.AppraiseBehavior;
import com.dyxiaojiazi.greedy_villagers.behavior.PickupEmeraldBehavior;
import com.dyxiaojiazi.greedy_villagers.behavior.TemptFollowBehavior;
import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.VillagerGoalPackages;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VillagerGoalPackages.class)
public class VillagerGoalPackagesMixin {

    @Inject(method = "getCorePackage", at = @At("RETURN"), cancellable = true)
    private static void addGreedyBehaviors(
            Holder<VillagerProfession> profession,
            float speedModifier,
            CallbackInfoReturnable<ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>>> cir
    ) {
        ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> original = cir.getReturnValue();
        if (original == null) original = ImmutableList.of();

        ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> modified =
                ImmutableList.<Pair<Integer, ? extends BehaviorControl<? super Villager>>>builder()
                        .addAll(original)
                        .add(Pair.of(0, new AppraiseBehavior()))      // 鉴赏最高
                        .add(Pair.of(1, new PickupEmeraldBehavior())) // 拾取第二
                        .add(Pair.of(2, new TemptFollowBehavior()))   // 引诱最低
                        .build();

        cir.setReturnValue(modified);
    }
}