package com.dyxiaojiazi.greedy_villagers.behavior;

import com.dyxiaojiazi.greedy_villagers.ModAttachments;
import com.dyxiaojiazi.greedy_villagers.mixin.VillagerInvoker;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class AppraiseBehavior extends Behavior<Villager> {
    private static final int APPRAISE_DURATION = 30;
    private static final int MAX_REROLL_ATTEMPTS = 20;

    private static final List<ItemStack> NITWIT_LOOT = List.of(
            new ItemStack(Items.BONE_MEAL, 6),
            new ItemStack(Items.STRING, 4),
            new ItemStack(Items.FEATHER, 2),
            new ItemStack(Items.STICK, 6),
            new ItemStack(Items.BREAD, 4),
            new ItemStack(Items.WHEAT_SEEDS, 5),
            new ItemStack(Items.PUMPKIN_SEEDS, 5),
            new ItemStack(Items.MELON_SEEDS, 5),
            new ItemStack(Items.BEETROOT_SEEDS, 5),
            new ItemStack(Items.TORCHFLOWER_SEEDS, 1),
            new ItemStack(Items.PUFFERFISH, 1)
    );

    private int appraiseTimer;
    private boolean appraising;
    private final Random random = new Random();

    public AppraiseBehavior() {
        super(Map.of(), 200);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, Villager villager) {
        if (villager.isBaby()) return false;
        if (villager.getVillagerData().profession().is(VillagerProfession.NONE)) return false;
        if (appraising) return false;

        // 只认 PENDING_APPRAISE 标记
        return villager.getAttachedOrCreate(ModAttachments.PENDING_APPRAISE, () -> false);
    }

    @Override
    protected void start(ServerLevel level, Villager villager, long gameTime) {
        appraising = true;
        appraiseTimer = APPRAISE_DURATION;

        if (!villager.getVillagerData().profession().is(VillagerProfession.NITWIT)) {
            villager.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.EMERALD, 1));
        }

        villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        villager.playSound(SoundEvents.VILLAGER_YES, 1.0F, 1.0F);
    }

    @Override
    protected boolean canStillUse(ServerLevel level, Villager villager, long gameTime) {
        if (!appraising) return false;

        if (villager.getMainHandItem().is(Items.EMERALD)) {
            villager.getLookControl().setLookAt(
                    villager.getX(),
                    villager.getY() + 1.0D,
                    villager.getZ(),
                    30.0F,
                    30.0F
            );
        }

        appraiseTimer--;
        return appraiseTimer > 0;
    }

    @Override
    protected void stop(ServerLevel level, Villager villager, long gameTime) {
        if (appraising) {
            appraising = false;

            villager.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);

            // 清除待鉴赏标记
            villager.setAttached(ModAttachments.PENDING_APPRAISE, false);

            if (villager.getVillagerData().profession().is(VillagerProfession.NITWIT)) {
                dropNitwitLoot(level, villager);
            } else {
                dropTradeItem(level, villager);
            }
        }
    }

    private void dropNitwitLoot(ServerLevel level, Villager villager) {
        ItemStack loot = NITWIT_LOOT.get(random.nextInt(NITWIT_LOOT.size())).copy();
        villager.spawnAtLocation(level, loot);
        villager.playSound(SoundEvents.ITEM_PICKUP, 1.0F, 1.0F);

        villager.setVillagerXp(villager.getVillagerXp() + 1);
    }

    private void dropTradeItem(ServerLevel level, Villager villager) {
        ItemStack result = ItemStack.EMPTY;

        MerchantOffers offers = villager.getOffers();
        if (offers == null || offers.isEmpty()) {
            ((VillagerInvoker) villager).invokeUpdateTrades(level);
            offers = villager.getOffers();
        }

        if (offers != null && !offers.isEmpty()) {
            result = pickValidTradeResult(villager, offers);
            int attempts = 0;
            while (result.is(Items.EMERALD) && attempts < MAX_REROLL_ATTEMPTS) {
                ItemStack rerolled = pickValidTradeResult(villager, offers);
                if (!rerolled.isEmpty()) {
                    result = rerolled;
                }
                attempts++;
            }
        }

        if (result.isEmpty() || result.is(Items.EMERALD)) {
            result = new ItemStack(Items.BREAD);
        }

        villager.spawnAtLocation(level, result);
        villager.playSound(SoundEvents.ITEM_PICKUP, 1.0F, 1.0F);

        villager.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 0, false, true));
        villager.heal(4.0F);

        villager.setVillagerXp(villager.getVillagerXp() + 1);

        int currentLevel = villager.getVillagerData().level();
        if (currentLevel < 5 && villager.getVillagerXp() >= getMaxXpPerLevel(currentLevel)) {
            ((VillagerInvoker) villager).invokeIncreaseMerchantCareer(level);
        }
    }

    private int getMaxXpPerLevel(int level) {
        return switch (level) {
            case 1 -> 10;
            case 2 -> 70;
            case 3 -> 150;
            case 4 -> 250;
            default -> Integer.MAX_VALUE;
        };
    }

    private ItemStack pickValidTradeResult(Villager villager, MerchantOffers offers) {
        int villagerLevel = villager.getVillagerData().level();
        int selectedIndex = selectTradeIndex(villagerLevel, offers.size(), random);

        if (selectedIndex >= offers.size()) selectedIndex = offers.size() - 1;
        if (selectedIndex < 0) selectedIndex = 0;

        MerchantOffer offer = offers.get(selectedIndex);
        if (offer != null) {
            ItemStack result = offer.getResult().copy();
            if (!result.isEmpty()) return result;
        }

        for (MerchantOffer candidate : offers) {
            if (candidate == null) continue;
            ItemStack result = candidate.getResult().copy();
            if (!result.isEmpty()) return result;
        }

        return ItemStack.EMPTY;
    }

    private int selectTradeIndex(int level, int offerCount, Random random) {
        float roll = random.nextFloat();

        int index = switch (level) {
            case 1 -> 0;
            case 2 -> roll < 0.8f ? 0 : 1;
            case 3 -> roll < 0.6f ? 0 : roll < 0.9f ? 1 : 2;
            case 4 -> roll < 0.5f ? 0 : roll < 0.75f ? 1 : roll < 0.9f ? 2 : 3;
            case 5 -> roll < 0.4f ? 0 : roll < 0.7f ? 1 : roll < 0.85f ? 2 : roll < 0.95f ? 3 : 4;
            default -> 0;
        };

        if (index >= offerCount) index = offerCount - 1;
        if (index < 0) index = 0;
        return index;
    }
}