package li.cil.oc.api.event;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc.api.internal.Rack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public abstract class RackMountableRenderEvent extends Event {
    public final Rack rack;
    public final int mountable;
    public final CompoundTag data;

    public RackMountableRenderEvent(final Rack rack, final int mountable, final CompoundTag data) {
        this.rack = rack;
        this.mountable = mountable;
        this.data = data;
    }

    public static class Block extends RackMountableRenderEvent implements ICancellableEvent {
        public final Direction side;
        private TextureAtlasSprite frontTextureOverride;

        public Block(final Rack rack, final int mountable, final CompoundTag data, final Direction side) {
            super(rack, mountable, data);
            this.side = side;
        }

        public TextureAtlasSprite getFrontTextureOverride() {
            return frontTextureOverride;
        }

        public void setFrontTextureOverride(final TextureAtlasSprite texture) {
            frontTextureOverride = texture;
        }
    }

    public static class TileEntity extends RackMountableRenderEvent {
        public final float v0;
        public final float v1;
        private final PoseStack poseStack;
        private final MultiBufferSource bufferSource;
        private final int packedLight;
        private final int packedOverlay;

        public TileEntity(final Rack rack, final int mountable, final CompoundTag data, final float v0, final float v1) {
            this(rack, mountable, data, v0, v1, null, null, 0, 0);
        }

        public TileEntity(
            final Rack rack,
            final int mountable,
            final CompoundTag data,
            final float v0,
            final float v1,
            final PoseStack poseStack,
            final MultiBufferSource bufferSource,
            final int packedLight,
            final int packedOverlay) {
            super(rack, mountable, data);
            this.v0 = v0;
            this.v1 = v1;
            this.poseStack = poseStack;
            this.bufferSource = bufferSource;
            this.packedLight = packedLight;
            this.packedOverlay = packedOverlay;
        }

        public void renderOverlay(final ResourceLocation texture) {
            renderOverlay(texture, 0, 1);
        }

        public void renderOverlay(final ResourceLocation texture, final float u0, final float u1) {
            if (poseStack == null || bufferSource == null || texture == null) {
                return;
            }
            final TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(texture);
            final VertexConsumer consumer = sprite.wrap(bufferSource.getBuffer(RenderType.cutout()));
            final PoseStack.Pose pose = poseStack.last();
            final float left = u0 - 0.5F;
            final float right = u1 - 0.5F;
            final float top = 0.5F - v0;
            final float bottom = 0.5F - v1;
            vertex(consumer, pose, left, bottom, 0.50625F, u0, v1);
            vertex(consumer, pose, right, bottom, 0.50625F, u1, v1);
            vertex(consumer, pose, right, top, 0.50625F, u1, v0);
            vertex(consumer, pose, left, top, 0.50625F, u0, v0);
        }

        public void renderOverlayFromAtlas(final ResourceLocation texture) {
            renderOverlayFromAtlas(texture, 0, 1);
        }

        public void renderOverlayFromAtlas(final ResourceLocation texture, final float u0, final float u1) {
            renderOverlay(texture, u0, u1);
        }

        private void vertex(
            final VertexConsumer consumer,
            final PoseStack.Pose pose,
            final float x,
            final float y,
            final float z,
            final float u,
            final float v) {
            consumer.addVertex(pose, x, y, z)
                .setColor(0xFFFFFFFF)
                .setUv(u, v)
                .setOverlay(packedOverlay)
                .setLight(packedLight)
                .setNormal(pose, 0F, 0F, 1F);
        }
    }
}
