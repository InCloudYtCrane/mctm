package com.mctaskmanager.client;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DataCache {

    public static class ModRow {
        public final String name;
        public final String id;
        public final String version;
        public final long sizeBytes;
        public final String color;

        public ModRow(String name, String id, String version, long sizeBytes, String color) {
            this.name = name;
            this.id = id;
            this.version = version;
            this.sizeBytes = sizeBytes;
            this.color = color;
        }

        public String sizeText() {
            if (sizeBytes <= 0) return "?";
            if (sizeBytes < 1024) return sizeBytes + " B";
            if (sizeBytes < 1024 * 1024) return String.format("%.1f KB", sizeBytes / 1024.0);
            return String.format("%.2f MB", sizeBytes / 1024.0 / 1024.0);
        }
    }

    private static final Set<String> OPTIMIZER_MODS = Set.of(
            "sodium", "lithium", "starlight", "ferritecore", "iris",
            "indium", "krypton", "memoryleakfix", "modernfix",
            "entityculling", "immediatelyfast", "c2me", "moreculling",
            "fabric-api", "embeddium", "oculus"
    );

    private static final Set<String> HEAVY_MODS = Set.of(
            "distant_horizons", "create", "ae2", "botania", "ars_nouveau",
            "mekanism", "thermal", "immersiveengineering", "dynmap",
            "journeymap", "xaerominimap", "xaeroworldmap",
            "physicsmod", "tacz"
    );

    // 模组列表缓存
    private static List<ModRow> modRows = null;
    private static long totalModSizeBytes = 0;
    private static long largestModSizeBytes = 0;

    // 方块实体统计
    private static Map<String, Integer> blockEntityCounts = new HashMap<>();
    private static Map<String, long[]> blockEntityPositions = new HashMap<>();
    private static Map<Long, Integer> blockEntityCountPerChunk = new HashMap<>();

    // 实体按区块统计
    private static Map<Long, Integer> entityCountPerChunk = new HashMap<>();

    // ===== 模组列表 =====

    public static synchronized List<ModRow> getModRows() {
        if (modRows == null) buildModRows();
        return modRows;
    }

    public static synchronized long getTotalModSizeBytes() {
        if (modRows == null) buildModRows();
        return totalModSizeBytes;
    }

    public static synchronized long getLargestModSizeBytes() {
        if (modRows == null) buildModRows();
        return largestModSizeBytes;
    }

    private static void buildModRows() {
        List<ModRow> rows = new ArrayList<>();
        totalModSizeBytes = 0;
        largestModSizeBytes = 0;

        for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
            String id = mod.getMetadata().getId();
            String name = mod.getMetadata().getName();
            String version = mod.getMetadata().getVersion().getFriendlyString();

            long bytes = 0;
            try {
                var paths = mod.getOrigin().getPaths();
                if (!paths.isEmpty()) {
                    Path p = paths.get(0);
                    if (Files.exists(p)) bytes = Files.size(p);
                }
            } catch (Exception ignored) {}

            rows.add(new ModRow(name, id, version, bytes, classify(id)));

            totalModSizeBytes += bytes;
            if (bytes > largestModSizeBytes) largestModSizeBytes = bytes;
        }
        rows.sort((a, b) -> Long.compare(b.sizeBytes, a.sizeBytes));
        modRows = rows;
    }

    private static String classify(String id) {
        String lower = id.toLowerCase();
        if (OPTIMIZER_MODS.contains(lower)) return "green";
        if (HEAVY_MODS.contains(lower)) return "yellow";
        return "white";
    }

    // ===== 扫描（方块实体 + 实体） =====

    public static synchronized void scanBlockEntities() {
        blockEntityCounts = new HashMap<>();
        blockEntityPositions = new HashMap<>();
        blockEntityCountPerChunk = new HashMap<>();
        entityCountPerChunk = new HashMap<>();

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        int radius = MctmConfig.get().scanRadius;
        BlockPos playerPos = mc.player.blockPosition();
        int centerChunkX = playerPos.getX() >> 4;
        int centerChunkZ = playerPos.getZ() >> 4;

        // ===== 方块实体扫描 =====
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int cx = centerChunkX + dx;
                int cz = centerChunkZ + dz;

                LevelChunk chunk = mc.level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) continue;

                int chunkCount = 0;
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    chunkCount++;

                    var key = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.getType());
                    String name = key != null ? key.getPath() : "unknown";

                    blockEntityCounts.merge(name, 1, Integer::sum);

                    long[] pos = blockEntityPositions.computeIfAbsent(name, k -> new long[3]);
                    BlockPos p = be.getBlockPos();
                    pos[0] += p.getX();
                    pos[1] += p.getY();
                    pos[2] += p.getZ();
                }

                long chunkKey = ((long) cx << 32) | (cz & 0xFFFFFFFFL);
                blockEntityCountPerChunk.put(chunkKey, chunkCount);
            }
        }

        // ===== 实体扫描 =====
        for (var entity : mc.level.entitiesForRendering()) {
            BlockPos p = entity.blockPosition();
            int cx = p.getX() >> 4;
            int cz = p.getZ() >> 4;
            long chunkKey = ((long) cx << 32) | (cz & 0xFFFFFFFFL);
            entityCountPerChunk.merge(chunkKey, 1, Integer::sum);
        }
    }

    // ===== Getter =====

    public static synchronized Map<String, Integer> getBlockEntityCounts() {
        return blockEntityCounts;
    }

    public static synchronized Map<String, long[]> getBlockEntityPositions() {
        return blockEntityPositions;
    }

    public static synchronized Map<Long, Integer> getBlockEntityCountPerChunk() {
        return blockEntityCountPerChunk;
    }

    public static synchronized Map<Long, Integer> getEntityCountPerChunk() {
        return entityCountPerChunk;
    }
}