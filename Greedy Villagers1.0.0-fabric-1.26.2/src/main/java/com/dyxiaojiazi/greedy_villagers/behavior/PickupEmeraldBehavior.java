package com.dyxiaojiazi.greedy_villagers.behavior;

import com.dyxiaojiazi.greedy_villagers.ModAttachments;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class PickupEmeraldBehavior extends Behavior<Villager> {
    private static final double SEARCH_RANGE = 10.0D;
    private static final double PICKUP_DISTANCE = 1.5D;
    private static final float SPEED = 0.5F;
    private static final int PICKUP_ANIMATION_TICKS = 30;

    private ItemEntity targetItem;
    private int pickupTimer = 0;
    private boolean pickingUp = false;

    public PickupEmeraldBehavior() {
        super(Map.of(), 100);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, Villager villager) {
        if (villager.getBrain().hasMemoryValue(MemoryModuleType.IS_PANICKING)) return false;
        if (villager.isBaby()) return false;
        if (villager.getMainHandItem().is(Items.EMERALD)) return false;

        // 已有待鉴赏标记，不重复拾取
        if (villager.getAttachedOrCreate(ModAttachments.PENDING_APPRAISE, () -> false)) return false;

        if (villager.getVillagerData().profession().is(VillagerProfession.NITWIT)) {
            long cooldown = villager.getAttachedOrCreate(ModAttachments.NITWIT_COOLDOWN, () -> 0L);
            long now = villager.level().getGameTime();
            if (now < cooldown) return false;
        } else if (villager.getVillagerData().profession().is(VillagerProfession.NONE)) {
            return false;
        }

        if (pickingUp) return true;

        List<ItemEntity> emeralds = villager.level().getEntitiesOfClass(
                ItemEntity.class,
                villager.getBoundingBox().inflate(SEARCH_RANGE),
                item -> item.getItem().is(Items.EMERALD) && item.isAlive()
        );

        if (emeralds.isEmpty()) return false;

        targetItem = emeralds.stream()
                .min(Comparator.comparingDouble(villager::distanceTo))
                .orElse(null);

        return targetItem != null;
    }

    @Override
    protected void start(ServerLevel level, Villager villager, long gameTime) {
        if (targetItem != null && targetItem.isAlive()) {
            villager.getBrain().setMemory(
                    MemoryModuleType.WALK_TARGET,
                    new WalkTarget(targetItem.position(), SPEED, 1)
            );
        }
    }

    @Override
    protected boolean canStillUse(ServerLevel level, Villager villager, long gameTime) {
        if (targetItem == null || !targetItem.isAlive()) return false;
        if (villager.getBrain().hasMemoryValue(MemoryModuleType.IS_PANICKING)) return false;
        if (villager.distanceTo(targetItem) > SEARCH_RANGE) return false;

        double distance = villager.distanceTo(targetItem);

        if (distance <= PICKUP_DISTANCE) {
            villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            villager.getLookControl().setLookAt(targetItem, 30.0F, 30.0F);

            if (!pickingUp) {
                pickingUp = true;
                pickupTimer = PICKUP_ANIMATION_TICKS;
            }

            pickupTimer--;

            if (pickupTimer <= 0) {
                performPickup(level, villager, targetItem);
                return false;
            }
            return true;
        } else {
            pickingUp = false;
            pickupTimer = 0;
            villager.getBrain().setMemory(
                    MemoryModuleType.WALK_TARGET,
                    new WalkTarget(targetItem.position(), SPEED, 1)
            );
            return true;
        }
    }

    @Override
    protected void stop(ServerLevel level, Villager villager, long gameTime) {
        villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        targetItem = null;
        pickingUp = false;
        pickupTimer = 0;
    }

    private void performPickup(ServerLevel level, Villager villager, ItemEntity item) {
        villager.playSound(SoundEvents.ITEM_PICKUP, 1.0F, 1.0F);
        villager.swing(InteractionHand.MAIN_HAND);
        villager.getLookControl().setLookAt(item, 30.0F, 30.0F);

        ItemStack stack = item.getItem();
        if (stack.getCount() > 1) {
            stack.shrink(1);
        } else {
            item.discard();
        }

        int current = villager.getAttachedOrCreate(ModAttachments.EMERALD_COUNT, () -> 0);
        villager.setAttached(ModAttachments.EMERALD_COUNT, current + 1);

        // 设置待鉴赏标记（替代 INTERACTION_TARGET）
        villager.setAttached(ModAttachments.PENDING_APPRAISE, true);

        // 傻子额外设置冷却
        if (villager.getVillagerData().profession().is(VillagerProfession.NITWIT)) {
            long now = villager.level().getGameTime();
            villager.setAttached(ModAttachments.NITWIT_COOLDOWN, now + 100);
        }

        // 有职业村民：主手放 1 个绿宝石用于渲染
        if (!villager.getVillagerData().profession().is(VillagerProfession.NITWIT)) {
            villager.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.EMERALD, 1));
        }
    }
}