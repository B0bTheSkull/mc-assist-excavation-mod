package com.b0btheskull.assistExcavation.client.excavation;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import com.b0btheskull.assistExcavation.client.Common;

import java.util.List;

/**
 * Draws a line outline around every block the active excavation mode would break, so the player
 * can see the target region before anything is mined. Gated on {@link Common#isPreviewOverlay()}.
 */
@Environment(EnvType.CLIENT)
public final class PreviewOverlay {
    // ARGB green for the outline, drawn fully opaque.
    private static final int COLOR = 0xFF55FF55;
    private static final float ALPHA = 1.0f;

    private static final VoxelShape CUBE = Shapes.block();

    private PreviewOverlay() {
    }

    public static void register() {
        LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(PreviewOverlay::render);
    }

    private static void render(net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext ctx) {
        if (!Common.isPreviewOverlay()) {
            return;
        }
        List<BlockPos> targets = ExcavationHandler.computePreviewTargets();
        if (targets.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Vec3 cam = mc.gameRenderer.getMainCamera().position();
        double camX = cam.x, camY = cam.y, camZ = cam.z;

        PoseStack pose = ctx.poseStack();
        MultiBufferSource.BufferSource buffers = ctx.bufferSource();
        var lines = buffers.getBuffer(RenderTypes.lines());

        for (BlockPos pos : targets) {
            ShapeRenderer.renderShape(pose, lines, CUBE,
                    pos.getX() - camX, pos.getY() - camY, pos.getZ() - camZ,
                    COLOR, ALPHA);
        }

        buffers.endBatch(RenderTypes.lines());
    }
}
