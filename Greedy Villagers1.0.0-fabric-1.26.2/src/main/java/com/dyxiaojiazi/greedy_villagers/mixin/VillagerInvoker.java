package com.dyxiaojiazi.greedy_villagers.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Villager.class)
public interface VillagerInvoker {
    @Invoker("increaseMerchantCareer")
    void invokeIncreaseMerchantCareer(ServerLevel level);

    @Invoker("updateTrades")
    void invokeUpdateTrades(ServerLevel level);
}