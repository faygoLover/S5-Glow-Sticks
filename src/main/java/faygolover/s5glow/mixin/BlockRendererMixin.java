package faygolover.s5glow.mixin;

import faygolover.s5glow.GlowMap;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderContext;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Embeddium 0.3.31: сборка чанка — цвет вершин блока рядом с ХИС смешивается с цветом ХИС. */
@Mixin(value = BlockRenderer.class, remap = false)
public abstract class BlockRendererMixin {
    @Unique private int s5glow$tint;

    @Inject(method = "renderModel", at = @At("HEAD"))
    private void s5glow$begin(BlockRenderContext ctx, ChunkBuildBuffers buffers, CallbackInfo ci) {
        s5glow$tint = GlowMap.tint(ctx.world(), ctx.pos());
    }

    @ModifyArg(method = "writeGeometry", at = @At(value = "INVOKE",
            target = "Lorg/embeddedt/embeddium/render/chunk/ChunkColorWriter;writeColor(IF)I"), index = 0)
    private int s5glow$color(int abgr) {
        return s5glow$tint == 0 ? abgr : GlowMap.apply(abgr, s5glow$tint);
    }
}
