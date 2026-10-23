package faygolover.s5glow;

import de.markusbordihn.glowsticks.item.GlowStickColor;
import faygolover.flrender.light.ColorTable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;

/** Цвета ХИС Glow Sticks для цветного света (отдельный класс — грузится, только если Glow Sticks есть). */
final class GlowStickColors {
    private GlowStickColors() {}

    /** Свет ХИС в руке и в полёте (у лежащей — её ванильный свет, RP Fixes ограничивает его 8). */
    private static final int HELD_LEVEL = 8;

    static void register() {
        for (GlowStickColor c : GlowStickColor.values()) {
            for (String prefix : new String[]{"glow_stick_", "creative_glow_stick_"}) {
                ResourceLocation id = new ResourceLocation("glow_sticks", prefix + c.getName());
                Block b = ForgeRegistries.BLOCKS.getValue(id);
                if (b != null && b != Blocks.AIR) ColorTable.block(b, c.getRgb(), 0, true);
                Item i = ForgeRegistries.ITEMS.getValue(id);
                if (i != null && i != Items.AIR) ColorTable.item(i, c.getRgb(), HELD_LEVEL, true);
            }
        }
    }
}
