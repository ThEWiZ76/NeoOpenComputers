package li.cil.oc.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.item.MfuItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.List;

public final class MfuTargetRenderer {
    static final String COORD_TAG = MfuItem.COORD_TAG;
    static final int COLOR = 0x00FF00;
    static final double MAX_DISTANCE = 64D;
    private static final float RED = ((COLOR >> 16) & 0xFF) / 255F;
    private static final float GREEN = ((COLOR >> 8) & 0xFF) / 255F;
    private static final float BLUE = (COLOR & 0xFF) / 255F;
    private static final float ALPHA = 0.25F;

    private MfuTargetRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(final RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }
        final Minecraft minecraft = Minecraft.getInstance();
        final Player player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return;
        }
        final Target target = targetFrom(player.getMainHandItem(), minecraft.level);
        if (target == null || !withinRange(Math.sqrt(player.distanceToSqr(target.x(), target.y(), target.z())))) {
            return;
        }

        renderTarget(event.getPoseStack(), event.getCamera().getPosition(), target);
    }

    private static Target targetFrom(final ItemStack stack, final Level level) {
        if (stack == null || stack.isEmpty() || !stack.is(ModItems.MFU.get())) {
            return null;
        }
        final CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains(COORD_TAG, Tag.TAG_INT_ARRAY)) {
            return null;
        }
        final int[] coord = tag.getIntArray(COORD_TAG);
        if (coord.length < 5 || coord[3] != legacyDimensionId(level)) {
            return null;
        }
        final Direction side = Direction.values()[Math.floorMod(coord[4], Direction.values().length)];
        return new Target(coord[0], coord[1], coord[2], side);
    }

    static boolean withinRange(final double distance) {
        return distance <= MAX_DISTANCE;
    }

    static AABB targetBounds(final BlockPos pos) {
        return new AABB(pos).inflate(0.1D);
    }

    static List<Vertex> faceVertices(final AABB bounds, final Direction direction) {
        return switch (direction) {
            case DOWN -> List.of(
                new Vertex(bounds.minX, bounds.minY, bounds.minZ),
                new Vertex(bounds.minX, bounds.minY, bounds.maxZ),
                new Vertex(bounds.maxX, bounds.minY, bounds.maxZ),
                new Vertex(bounds.maxX, bounds.minY, bounds.minZ));
            case UP -> List.of(
                new Vertex(bounds.maxX, bounds.maxY, bounds.minZ),
                new Vertex(bounds.maxX, bounds.maxY, bounds.maxZ),
                new Vertex(bounds.minX, bounds.maxY, bounds.maxZ),
                new Vertex(bounds.minX, bounds.maxY, bounds.minZ));
            case NORTH -> List.of(
                new Vertex(bounds.minX, bounds.minY, bounds.minZ),
                new Vertex(bounds.maxX, bounds.minY, bounds.minZ),
                new Vertex(bounds.maxX, bounds.maxY, bounds.minZ),
                new Vertex(bounds.minX, bounds.maxY, bounds.minZ));
            case SOUTH -> List.of(
                new Vertex(bounds.maxX, bounds.maxY, bounds.maxZ),
                new Vertex(bounds.maxX, bounds.minY, bounds.maxZ),
                new Vertex(bounds.minX, bounds.minY, bounds.maxZ),
                new Vertex(bounds.minX, bounds.maxY, bounds.maxZ));
            case WEST -> List.of(
                new Vertex(bounds.minX, bounds.minY, bounds.minZ),
                new Vertex(bounds.minX, bounds.maxY, bounds.minZ),
                new Vertex(bounds.minX, bounds.maxY, bounds.maxZ),
                new Vertex(bounds.minX, bounds.minY, bounds.maxZ));
            case EAST -> List.of(
                new Vertex(bounds.maxX, bounds.minY, bounds.minZ),
                new Vertex(bounds.maxX, bounds.minY, bounds.maxZ),
                new Vertex(bounds.maxX, bounds.maxY, bounds.maxZ),
                new Vertex(bounds.maxX, bounds.maxY, bounds.minZ));
        };
    }

    private static void renderTarget(final PoseStack poseStack, final Vec3 camera, final Target target) {
        final AABB bounds = targetBounds(new BlockPos(target.x(), target.y(), target.z()));
        final MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();

        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        LevelRenderer.renderLineBox(poseStack, buffer.getBuffer(RenderType.lines()), bounds, RED, GREEN, BLUE, ALPHA);
        renderFace(poseStack, buffer.getBuffer(RenderType.debugQuads()), bounds, target.side());
        poseStack.popPose();

        buffer.endBatch(RenderType.lines());
        buffer.endBatch(RenderType.debugQuads());
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private static void renderFace(final PoseStack poseStack, final VertexConsumer consumer, final AABB bounds, final Direction side) {
        final PoseStack.Pose pose = poseStack.last();
        for (final Vertex vertex : faceVertices(bounds, side)) {
            consumer.addVertex(pose, (float) vertex.x(), (float) vertex.y(), (float) vertex.z())
                .setColor(RED, GREEN, BLUE, ALPHA);
        }
    }

    private static int legacyDimensionId(final Level level) {
        if (level.dimension().equals(Level.NETHER)) {
            return -1;
        }
        if (level.dimension().equals(Level.END)) {
            return 1;
        }
        return 0;
    }

    record Vertex(double x, double y, double z) {
    }

    private record Target(int x, int y, int z, Direction side) {
    }
}
