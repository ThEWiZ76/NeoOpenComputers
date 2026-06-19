package li.cil.oc.api.event;

import li.cil.oc.api.internal.Rack;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

final class RackMountableRenderEventTest {
    @Test
    void blockRenderEventCarriesRackMountableDataAndIsCancelable() {
        CompoundTag data = new CompoundTag();
        RackMountableRenderEvent.Block event = new RackMountableRenderEvent.Block(null, 2, data, Direction.NORTH);

        assertInstanceOf(Event.class, event);
        assertInstanceOf(ICancellableEvent.class, event);
        assertNull(event.rack);
        assertEquals(2, event.mountable);
        assertSame(data, event.data);
        assertEquals(Direction.NORTH, event.side);
        assertNull(event.getFrontTextureOverride());
        event.setFrontTextureOverride(null);
        assertNull(event.getFrontTextureOverride());
    }

    @Test
    void tileEntityRenderEventCarriesUvRangeAndKeepsOverlayHelpers() {
        RackMountableRenderEvent.TileEntity event = new RackMountableRenderEvent.TileEntity(null, 1, null, 0.125f, 0.25f);

        assertNull(event.rack);
        assertEquals(1, event.mountable);
        assertEquals(0.125f, event.v0);
        assertEquals(0.25f, event.v1);
        event.renderOverlay(ResourceLocation.fromNamespaceAndPath("neoopencomputers", "textures/gui/test.png"));
        event.renderOverlay(ResourceLocation.fromNamespaceAndPath("neoopencomputers", "textures/gui/test.png"), 0, 1);
        event.renderOverlayFromAtlas(ResourceLocation.fromNamespaceAndPath("minecraft", "block/stone"));
        event.renderOverlayFromAtlas(ResourceLocation.fromNamespaceAndPath("minecraft", "block/stone"), 0, 1);
    }
}
