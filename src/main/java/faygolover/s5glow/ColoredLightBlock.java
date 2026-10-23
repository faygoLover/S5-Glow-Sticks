package faygolover.s5glow;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Цветной блок света — как ванильный light: невидим, уровень 0..15 (правый клик ведущего с предметом света меняет
 * уровень), виден и выделяется, когда в руке любой предмет света (ванильный или цветной), тогда же вокруг маркеры.
 */
public class ColoredLightBlock extends LightBlock {
    public ColoredLightBlock(Properties properties) {
        super(properties);
    }

    static boolean isLightItem(ItemStack stack) {
        return stack.is(Items.LIGHT) || stack.getItem() instanceof net.minecraft.world.item.BlockItem bi && bi.getBlock() instanceof ColoredLightBlock;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context.isHoldingItem(Items.LIGHT)) return Shapes.block();
        for (var b : S5Glow.LIGHTS.values()) if (context.isHoldingItem(b.get().asItem())) return Shapes.block();
        return Shapes.empty();
    }

    /** Маркер на месте блока, пока в руке предмет света (ванильный показывает маркеры только своему блоку). */
    @Override
    @OnlyIn(Dist.CLIENT)
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        ClientHooks.marker(state, level, pos);
    }
}
