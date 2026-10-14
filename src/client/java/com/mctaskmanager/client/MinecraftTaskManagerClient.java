package com.mctaskmanager.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class MinecraftTaskManagerClient implements ClientModInitializer {

	private static final FpsMonitor fpsMonitor = new FpsMonitor();
	private static final LagTracker lagTracker = new LagTracker();

	public static FpsMonitor getFpsMonitor() {
		return fpsMonitor;
	}

	public static LagTracker getLagTracker() {
		return lagTracker;
	}

	@Override
	public void onInitializeClient() {
		MctmConfig.load();
		HardwareInfoCollector.preloadAsync();

		KeyMapping openKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.mctaskmanager.open",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_K,
				"category.mctaskmanager"
		));

		// ===== 进入世界 =====
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			SessionManager.startSession(client);
		});

		// ===== 退出世界 / 服务器 =====
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			SessionManager.SessionRecord rec = SessionManager.endSession();
			if (rec != null) {
				client.getToasts().addToast(new SystemToast(
						SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
						Component.translatable("mctm.session.saved.title"),
						Component.translatable("mctm.session.saved.body",
								rec.durationText(), rec.avgFps)
				));
			}
		});

		// ===== 每 tick =====
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openKey.consumeClick()) {
				if (client.screen == null) {
					client.setScreen(new TaskManagerScreen());
				}
			}

			if (client.level == null || client.player == null) return;

			long usedMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			long usedMemMB = usedMem / 1024 / 1024;

			fpsMonitor.tick(client.getFps(), usedMemMB);
			lagTracker.tick(client.getFps());
			SessionManager.tick(client);

			if (MctmConfig.get().showLowFpsToast
					&& fpsMonitor.isLowFps() && fpsMonitor.canShowToast()) {
				showLowFpsToast(client);
			}

			if (fpsMonitor.isLowFps() && fpsMonitor.canRecordSnapshot()) {
				BlockPos pos = client.player.blockPosition();
				int entityCount = client.level.getEntityCount();
				String dim = client.level.dimension().location().toString();
				fpsMonitor.recordSnapshot(pos.getX(), pos.getY(), pos.getZ(),
						entityCount, dim);
			}

			if (lagTracker.consumePendingNotification()) {
				LagTracker.LagRecord rec = lagTracker.getLatestRecord();
				if (rec != null) {
					showLagToast(client, rec);
				}
			}
		});

		// ===== 命令 =====
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(ClientCommandManager.literal("mctm")
					.then(ClientCommandManager.literal("mods")
							.executes(context -> { printModList(context.getSource()); return 1; })
					)
					.then(ClientCommandManager.literal("info")
							.executes(context -> { printGameInfo(context.getSource()); return 1; })
					)
					.then(ClientCommandManager.literal("lag")
							.executes(context -> { printLagRecords(context.getSource()); return 1; })
					)
			);
		});
	}

	private void showLowFpsToast(Minecraft client) {
		client.getToasts().addToast(new SystemToast(
				SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
				Component.translatable("mctm.toast.low_fps.title"),
				Component.translatable("mctm.toast.low_fps.body", fpsMonitor.getAverageFps())
		));
	}

	private void showLagToast(Minecraft client, LagTracker.LagRecord rec) {
		client.getToasts().addToast(new SystemToast(
				SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
				Component.translatable("mctm.toast.lag.title",
						String.format("%.1f", rec.durationMs / 1000.0)),
				Component.translatable("mctm.toast.lag.body", rec.minFps, rec.reasonText())
		));
	}

	private void printModList(FabricClientCommandSource source) {
		var mods = FabricLoader.getInstance().getAllMods();
		source.sendFeedback(Component.literal("===== Mods (" + mods.size() + ") ====="));
		int count = 0;
		for (ModContainer mod : mods) {
			if (count >= 15) {
				source.sendFeedback(Component.literal("... more"));
				break;
			}
			source.sendFeedback(Component.literal(
					"- " + mod.getMetadata().getName() + " (" + mod.getMetadata().getId() + ")"
			));
			count++;
		}
	}

	private void printGameInfo(FabricClientCommandSource source) {
		Minecraft mc = Minecraft.getInstance();
		source.sendFeedback(Component.literal("===== Game Settings ====="));
		source.sendFeedback(Component.literal("Resolution: " + mc.getWindow().getWidth() + "x" + mc.getWindow().getHeight()));
		source.sendFeedback(Component.literal("Render Distance: " + mc.options.renderDistance().get()));
		source.sendFeedback(Component.literal("VSync: " + mc.options.enableVsync().get()));
		source.sendFeedback(Component.literal("FPS Limit: " + mc.options.framerateLimit().get()));
		source.sendFeedback(Component.literal("===== Resource Packs ====="));
		for (String pack : mc.options.resourcePacks) {
			source.sendFeedback(Component.literal("- " + pack));
		}
	}

	private void printLagRecords(FabricClientCommandSource source) {
		var records = lagTracker.getRecords();
		if (records.isEmpty()) {
			source.sendFeedback(Component.literal("No lag records yet."));
			return;
		}
		source.sendFeedback(Component.literal("===== Lag Records (" + records.size() + ") ====="));
		for (int i = records.size() - 1; i >= Math.max(0, records.size() - 10); i--) {
			var rec = records.get(i);
			source.sendFeedback(Component.literal(String.format(
					"%s | %.1fs | min %d FPS | %s",
					rec.timeText(), rec.durationMs / 1000.0, rec.minFps, rec.reason)));
		}
	}
}