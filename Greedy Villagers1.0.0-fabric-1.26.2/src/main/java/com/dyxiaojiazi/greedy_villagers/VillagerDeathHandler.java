package com.dyxiaojiazi.greedy_villagers;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class VillagerDeathHandler {
    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (entity instanceof Villager villager && !villager.level().isClientSide()) {
                Integer count = villager.getAttached(ModAttachments.EMERALD_COUNT);
                if (count == null || count <= 0) return;

                // 40% ~ 100% 随机，取正整数
                float ratio = 0.4f + villager.getRandom().nextFloat() * 0.6f;
                int dropAmount = Math.round(count * ratio);
                dropAmount = Math.max(0, Math.min(dropAmount, count));
                if (dropAmount <= 0) return;

                ServerLevel level = (ServerLevel) villager.level();
                ItemStack stack = new ItemStack(Items.EMERALD, dropAmount);
                villager.spawnAtLocation(level, stack);

                villager.setAttached(ModAttachments.EMERALD_COUNT, 0);
            }
        });
    }
}