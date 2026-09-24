package com.dyxiaojiazi.greedy_villagers;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;

public class GreedyVillagersMod implements ModInitializer {
	public static final String MOD_ID = "greedy_villagers";

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		// 触发 ModAttachments 类加载
		ModAttachments.EMERALD_COUNT.toString();
		ModAttachments.NITWIT_COOLDOWN.toString();
		// 注册村民死亡掉落事件
		VillagerDeathHandler.register();
	}
}