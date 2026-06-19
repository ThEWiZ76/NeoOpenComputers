package li.cil.oc.api.event;

import li.cil.oc.api.internal.Rack;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
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

        public TileEntity(final Rack rack, final int mountable, final CompoundTag data, final float v0, final float v1) {
            super(rack, mountable, data);
            this.v0 = v0;
            this.v1 = v1;
        }

        public void renderOverlay(final ResourceLocation texture) {
            renderOverlay(texture, 0, 1);
        }

        public void renderOverlay(final ResourceLocation texture, final float u0, final float u1) {
        }

        public void renderOverlayFromAtlas(final ResourceLocation texture) {
            renderOverlayFromAtlas(texture, 0, 1);
        }

        public void renderOverlayFromAtlas(final ResourceLocation texture, final float u0, final float u1) {
        }
    }
}
