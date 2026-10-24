package faygolover.s5glow;

import net.minecraft.world.item.BlockItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumMap;
import java.util.Map;

/**
 * Цветной свет: невидимые блоки света 15 цветов (без чёрного) (s5glow:colored_light_<цвет>, как ванильный light) и ХИС (Glow Sticks:
 * лежащие, в руках, брошенные) светят своим цветом — сам свет рисует FL Render, здесь только цвета (клиент, ClientHooks).
 */
@Mod(S5Glow.MODID)
public final class S5Glow {
    public static final String MODID = "s5glow";
    /** Вкладка «RP: разное» из RP Core (без зависимости от него: нет RP Core — блоки только через /give). */
    private static final ResourceKey<CreativeModeTab> RP_TAB =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, new ResourceLocation("rpcore", "misc"));

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final Map<DyeColor, RegistryObject<Block>> LIGHTS = new EnumMap<>(DyeColor.class);
    private static final Map<DyeColor, RegistryObject<Item>> LIGHT_ITEMS = new EnumMap<>(DyeColor.class);

    static {
        for (DyeColor c : DyeColor.values()) {
            // Чёрного света не бывает.
            if (c == DyeColor.BLACK) continue;
            RegistryObject<Block> b = BLOCKS.register("colored_light_" + c.getName(),
                    () -> new ColoredLightBlock(BlockBehaviour.Properties.copy(net.minecraft.world.level.block.Blocks.LIGHT)));
            LIGHTS.put(c, b);
            LIGHT_ITEMS.put(c, ITEMS.register("colored_light_" + c.getName(), () -> new BlockItem(b.get(), new Item.Properties().rarity(Rarity.EPIC))));
        }
    }

    public S5Glow() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(bus);
        ITEMS.register(bus);
        bus.addListener((BuildCreativeModeTabContentsEvent e) -> {
            if (e.getTabKey().equals(RP_TAB)) LIGHT_ITEMS.values().forEach(i -> e.accept(i.get()));
        });
        if (FMLEnvironment.dist == Dist.CLIENT) ClientHooks.init(bus, LIGHT_ITEMS);
    }
}
