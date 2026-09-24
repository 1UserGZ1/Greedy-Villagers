package com.dyxiaojiazi.greedy_villagers.behavior;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class TemptFollowBehavior extends Behavior<Villager> {
    private static final double SEARCH_RANGE = 10.0D;
    private static final double STOP_DISTANCE = 2.0D;
    private static final float SPEED = 0.5F;

    private Player temptingPlayer;

    public TemptFollowBehavior() {
        super(Map.of(), 200);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, Villager villager) {
        if (villager.getBrain().hasMemoryValue(MemoryModuleType.IS_PANICKING)) return false;

        // 如果附近有掉落绿宝石，让拾取行为优先，引诱不启动
        if (hasNearbyEmeraldItem(villager)) {
            temptingPlayer = null;
            return false;
        }

        Player player = findPlayerWithEmerald(villager);
        if (player == null) {
            temptingPlayer = null;
            return false;
        }
        temptingPlayer = player;
        return true;
    }

    @Override
    protected void start(ServerLevel level, Villager villager, long gameTime) {
        // 交给 tick
    }

    @Override
    protected boolean canStillUse(ServerLevel level, Villager villager, long gameTime) {
        if (villager.getBrain().hasMemoryValue(MemoryModuleType.IS_PANICKING)) return false;

        // 附近有掉落绿宝石，退出让拾取行为接管
        if (hasNearbyEmeraldItem(villager)) {
            temptingPlayer = null;
            return false;
        }

        Player player = findPlayerWithEmerald(villager);
        if (player == null) {
            temptingPlayer = null;
            return false;
        }
        temptingPlayer = player;

        if (villager.distanceTo(player) > SEARCH_RANGE) return false;

        return true;
    }

    @Override
    protected void tick(ServerLevel level, Villager villager, long gameTime) {
        if (temptingPlayer == null) return;
        if (!temptingPlayer.isAlive()) return;

        double dist = villager.distanceTo(temptingPlayer);

        if (dist > STOP_DISTANCE) {
            villager.getBrain().setMemory(
                    MemoryModuleType.WALK_TARGET,
                    new WalkTarget(
                            new Vec3(temptingPlayer.getX(), temptingPlayer.getY(), temptingPlayer.getZ()),
                            SPEED,
                            0
                    )
            );
        } else {
            villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        }
    }

    @Override
    protected void stop(ServerLevel level, Villager villager, long gameTime) {
        villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        temptingPlayer = null;
    }

    private Player findPlayerWithEmerald(Villager villager) {
        List<Player> players = villager.level().getEntitiesOfClass(
                Player.class,
                villager.getBoundingBox().inflate(SEARCH_RANGE),
                player -> player.getMainHandItem().is(Items.EMERALD)
                        || player.getOffhandItem().is(Items.EMERALD)
        );

        return players.stream()
                .min(Comparator.comparingDouble(villager::distanceTo))
                .orElse(null);
    }

    private boolean hasNearbyEmeraldItem(Villager villager) {
        List<ItemEntity> items = villager.level().getEntitiesOfClass(
                ItemEntity.class,
                villager.getBoundingBox().inflate(SEARCH_RANGE),
                item -> item.getItem().is(Items.EMERALD) && item.isAlive()
        );
        return !items.isEmpty();
    }
}