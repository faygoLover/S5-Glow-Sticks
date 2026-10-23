package faygolover.s5glow;

import faygolover.flrender.light.ColorTable;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.LightBlock;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.RegistryObject;

import java.util.Map;

/** Клиентская часть блоков света (отдельно, чтобы сервер не грузил клиентские классы). */
final class ClientHooks {
    private ClientHooks() {}

    static void init(IEventBus bus, Map<DyeColor, RegistryObject<Item>> items) {
        bus.addListener((RegisterColorHandlersEvent.Item e) -> items.forEach((c, i) ->
                e.register((stack, layer) -> 0xFF000000 | c.getTextColor(), i.get())));
        // значок предмета с цифрой уровня — как у ванильного light (predicate level = уровень / 16)
        bus.addListener((FMLClientSetupEvent e) -> e.enqueueWork(() -> items.values().forEach(i ->
                ItemProperties.register(i.get(), new ResourceLocation("level"), (stack, level, entity, seed) -> {
                    CompoundTag t = stack.getTagElement("BlockStateTag");
                    Tag v = t == null ? null : t.get(LightBlock.LEVEL.getName());
                    try {
                        return v == null ? 1.0F : Integer.parseInt(v.getAsString()) / 16.0F;
                    } catch (NumberFormatException ex) {
                        return 1.0F;
                    }
                }))));
        // Цвета для цветного света FL Render: блоки света и ХИС (лежащие, в руках, брошенные).
        bus.addListener((FMLClientSetupEvent e) -> e.enqueueWork(() -> {
            S5Glow.LIGHTS.forEach((c, b) -> ColorTable.block(b.get(), c.getTextColor(), 0, true));
            if (ModList.get().isLoaded("glow_sticks")) GlowStickColors.register();
        }));
    }

    static void marker(BlockState state, Level level, BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameMode == null || mc.gameMode.getPlayerMode() != GameType.CREATIVE) return;
        if (!ColoredLightBlock.isLightItem(mc.player.getMainHandItem())) return;
        level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK_MARKER, state), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0, 0, 0);
    }
}
