package faygolover.s5glow;

import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

/** Клиент: лежащие ХИС (Glow Sticks) подкрашивают блоки вокруг своим цветом. Без Glow Sticks и на сервере ничего не делает. */
@Mod(S5Glow.MODID)
public final class S5Glow {
    public static final String MODID = "s5glow";

    public S5Glow() {
        if (FMLEnvironment.dist != Dist.CLIENT || !ModList.get().isLoaded("glow_sticks")) return;
        GlowMap.enabled = true;
        MinecraftForge.EVENT_BUS.addListener((ChunkEvent.Load e) -> {
            if (e.getLevel().isClientSide() && e.getChunk() instanceof LevelChunk c) GlowMap.scan(c);
        });
        MinecraftForge.EVENT_BUS.addListener((ChunkEvent.Unload e) -> {
            if (e.getLevel().isClientSide()) GlowMap.unload(e.getChunk().getPos());
        });
        MinecraftForge.EVENT_BUS.addListener((LevelEvent.Unload e) -> {
            if (e.getLevel().isClientSide()) GlowMap.clear();
        });
    }
}
