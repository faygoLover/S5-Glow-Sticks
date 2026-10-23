package faygolover.s5glow;

import de.markusbordihn.glowsticks.item.GlowStickColor;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.item.DyeColor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Лежащие ХИС рядом с игроком (клиент) и подкраска блоков вокруг них. Свет ХИС доходит до блока, если ванильный
 * блочный свет у блока не меньше ожидаемого от этой ХИС (сила минус расстояние) — так стены держат и цвет.
 * Идея — Colorful Lighting (MIT, erykczy); здесь только ХИС и без замены освещения.
 */
public final class GlowMap {
    private GlowMap() {}

    static volatile boolean enabled;
    /** Насколько сильно цвет ХИС перекрашивает блок у самой палочки (0..1). */
    private static final float STRENGTH = 0.75f;
    private static final int[][] DIRS = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

    /** pos.asLong() → rgb << 4 | сила света (1..15). */
    private static final Map<Long, Integer> STICKS = new ConcurrentHashMap<>();
    private static volatile Map<Block, Integer> colors;

    private static Map<Block, Integer> colors() {
        Map<Block, Integer> m = colors;
        if (m == null) {
            m = new IdentityHashMap<>();
            for (Map.Entry<DyeColor, RegistryObject<Block>> e : S5Glow.LIGHTS.entrySet())
                m.put(e.getValue().get(), e.getKey().getTextColor());
            if (ModList.get().isLoaded("glow_sticks")) glowSticks(m);
            colors = m;
        }
        return m;
    }

    /** Цвета ХИС: glow_sticks:glow_stick_<цвет> и creative_glow_stick_<цвет>. Отдельно — класс есть только с Glow Sticks. */
    private static void glowSticks(Map<Block, Integer> m) {
        for (GlowStickColor c : GlowStickColor.values()) {
            for (String prefix : new String[]{"glow_stick_", "creative_glow_stick_"}) {
                Block b = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("glow_sticks", prefix + c.getName()));
                if (b != null && b != Blocks.AIR) m.put(b, c.getRgb());
            }
        }
    }

    private static int packed(BlockState s) {
        Integer rgb = colors().get(s.getBlock());
        int light = s.getLightEmission();
        return rgb == null || light <= 0 ? 0 : rgb << 4 | Math.min(light, 15);
    }

    /** Клиент, основной поток: блок поменялся (поставили, убрали, ХИС погасла на ступень). */
    public static void changed(BlockPos pos, BlockState old, BlockState now) {
        if (!enabled) return;
        int before = packed(old), after = packed(now);
        if (before == after) return;
        if (after == 0) STICKS.remove(pos.asLong());
        else STICKS.put(pos.asLong(), after);
        dirty(pos);
    }

    static void scan(LevelChunk chunk) {
        Map<Block, Integer> cs = colors();
        if (cs.isEmpty()) return;
        LevelChunkSection[] sections = chunk.getSections();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int x0 = chunk.getPos().getMinBlockX(), z0 = chunk.getPos().getMinBlockZ();
        for (int i = 0; i < sections.length; i++) {
            LevelChunkSection sec = sections[i];
            if (sec == null || sec.hasOnlyAir() || !sec.maybeHas(s -> cs.containsKey(s.getBlock()))) continue;
            int y0 = chunk.getSectionYFromSectionIndex(i) << 4;
            for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
                int v = packed(sec.getBlockState(x, y, z));
                if (v == 0) continue;
                p.set(x0 + x, y0 + y, z0 + z);
                STICKS.put(p.asLong(), v);
                dirty(p);
            }
        }
    }

    static void unload(ChunkPos cp) {
        STICKS.keySet().removeIf(k -> (BlockPos.getX(k) >> 4) == cp.x && (BlockPos.getZ(k) >> 4) == cp.z);
    }

    static void clear() {
        STICKS.clear();
    }

    private static void dirty(BlockPos p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.levelRenderer != null)
            mc.levelRenderer.setBlocksDirty(p.getX() - 15, p.getY() - 15, p.getZ() - 15, p.getX() + 15, p.getY() + 15, p.getZ() + 15);
    }

    /** Поток сборки чанков: подкраска блока (вес << 24 | rgb) или 0. */
    public static int tint(BlockAndTintGetter world, BlockPos pos) {
        if (STICKS.isEmpty()) return 0;
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        float r = 0, g = 0, b = 0, sum = 0, max = 0;
        int lightHere = -1;
        for (Map.Entry<Long, Integer> e : STICKS.entrySet()) {
            long k = e.getKey();
            int v = e.getValue(), ls = v & 15;
            int d = Math.abs(BlockPos.getX(k) - x) + Math.abs(BlockPos.getY(k) - y) + Math.abs(BlockPos.getZ(k) - z);
            int expect = ls - d;
            if (expect <= 0) continue;
            if (lightHere < 0) lightHere = blockLight(world, x, y, z);
            if (lightHere < expect) continue;   // между ХИС и блоком стена
            float w = Math.min(1f, expect / (float) Math.max(lightHere, 1));   // доля этого света: цвет держится по всей освещённой зоне
            int rgb = v >>> 4;
            r += w * (rgb >> 16 & 255);
            g += w * (rgb >> 8 & 255);
            b += w * (rgb & 255);
            sum += w;
            max = Math.max(max, w);
        }
        if (sum <= 0) return 0;
        float top = Math.max(r, Math.max(g, b));
        if (top <= 0) return 0;      // чёрная ХИС — не красим
        float k = 255f / top;        // самый яркий канал = 255: блок окрашивается, но не темнеет по нему
        return Math.round(max * 255) << 24 | Math.round(r * k) << 16 | Math.round(g * k) << 8 | Math.round(b * k);
    }

    /** Свет у граней: у сплошного блока сам он 0, поэтому максимум с соседями. */
    private static int blockLight(BlockAndTintGetter w, int x, int y, int z) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(x, y, z);
        int m = w.getBrightness(LightLayer.BLOCK, p);
        for (int[] d : DIRS) m = Math.max(m, w.getBrightness(LightLayer.BLOCK, p.set(x + d[0], y + d[1], z + d[2])));
        return m;
    }

    /** Цвет вершины Embeddium (ABGR) с подкраской. */
    public static int apply(int abgr, int tint) {
        float s = (tint >>> 24) / 255f * STRENGTH;
        float tr = (tint >> 16 & 255) / 255f, tg = (tint >> 8 & 255) / 255f, tb = (tint & 255) / 255f;
        int r = Math.round((abgr & 255) * (1 - s + s * tr));
        int g = Math.round((abgr >> 8 & 255) * (1 - s + s * tg));
        int b = Math.round((abgr >> 16 & 255) * (1 - s + s * tb));
        return abgr & 0xFF000000 | b << 16 | g << 8 | r;
    }
}
