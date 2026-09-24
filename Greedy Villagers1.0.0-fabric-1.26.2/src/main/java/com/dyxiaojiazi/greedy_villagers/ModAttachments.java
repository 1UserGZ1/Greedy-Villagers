package com.dyxiaojiazi.greedy_villagers;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

public class ModAttachments {
    public static final AttachmentType<Integer> EMERALD_COUNT = AttachmentRegistry.<Integer>builder()
            .initializer(() -> 0)
            .persistent(Codec.INT)
            .copyOnDeath()
            .buildAndRegister(Identifier.fromNamespaceAndPath(GreedyVillagersMod.MOD_ID, "emerald_count"));

    public static final AttachmentType<Long> NITWIT_COOLDOWN = AttachmentRegistry.<Long>builder()
            .initializer(() -> 0L)
            .persistent(Codec.LONG)
            .buildAndRegister(Identifier.fromNamespaceAndPath(GreedyVillagersMod.MOD_ID, "nitwit_cooldown"));

    // 待鉴赏标记：拾取到绿宝石后设为 true，鉴赏结束后清除
    public static final AttachmentType<Boolean> PENDING_APPRAISE = AttachmentRegistry.<Boolean>builder()
            .initializer(() -> false)
            .persistent(Codec.BOOL)
            .buildAndRegister(Identifier.fromNamespaceAndPath(GreedyVillagersMod.MOD_ID, "pending_appraise"));
}