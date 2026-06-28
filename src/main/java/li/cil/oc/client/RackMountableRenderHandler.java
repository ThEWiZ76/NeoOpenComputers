package li.cil.oc.client;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.event.RackMountableRenderEvent;
import li.cil.oc.common.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;

public final class RackMountableRenderHandler {
    private static final long DISK_ACTIVITY_MILLIS = 400L;
    private static final long SERVER_ACTIVITY_MILLIS = 400L;
    private static final long NETWORK_ACTIVITY_MILLIS = 300L;
    private static final ResourceLocation RACK_DISK_DRIVE =
        ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "block/rack_disk_drive");
    private static final ResourceLocation RACK_SERVER =
        ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "block/rack_server");
    private static final ResourceLocation RACK_TERMINAL_SERVER =
        ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "block/rack_terminal_server");
    private static final ResourceLocation RACK_DISK_DRIVE_ACTIVITY =
        ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "block/overlay/rack_disk_drive_activity");
    private static final ResourceLocation RACK_SERVER_ON =
        ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "block/overlay/rack_server_on");
    private static final ResourceLocation RACK_SERVER_ERROR =
        ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "block/overlay/rack_server_error");
    private static final ResourceLocation RACK_SERVER_ACTIVITY =
        ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "block/overlay/rack_server_activity");
    private static final ResourceLocation RACK_SERVER_NETWORK_ACTIVITY =
        ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "block/overlay/rack_server_network_activity");
    private static final ResourceLocation RACK_TERMINAL_SERVER_ON =
        ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "block/overlay/rack_terminal_server_on");
    private static final ResourceLocation RACK_TERMINAL_SERVER_PRESENCE =
        ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "block/overlay/rack_terminal_server_presence");

    private RackMountableRenderHandler() {
    }

    @SubscribeEvent
    public static void onRackMountableRendering(final RackMountableRenderEvent.Block event) {
        if (isDiskDriveMountable(event)) {
            event.setFrontTextureOverride(Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(RACK_DISK_DRIVE));
        } else if (isServer(event)) {
            event.setFrontTextureOverride(Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(RACK_SERVER));
        } else if (isTerminalServer(event)) {
            event.setFrontTextureOverride(Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(RACK_TERMINAL_SERVER));
        }
    }

    @SubscribeEvent
    public static void onRackMountableRendering(final RackMountableRenderEvent.TileEntity event) {
        if (event.data == null) {
            return;
        }
        if (isDiskDriveMountable(event)) {
            if (isRecent(event.data.getLong("lastAccess"), DISK_ACTIVITY_MILLIS)) {
                event.renderOverlayFromAtlas(RACK_DISK_DRIVE_ACTIVITY);
            }
        } else if (isServer(event)) {
            if (event.data.getBoolean("isRunning")) {
                event.renderOverlayFromAtlas(RACK_SERVER_ON);
            }
            if (event.data.getBoolean("hasErrored") && showErrorLight(event.rack.hashCode() * (event.mountable + 1))) {
                event.renderOverlayFromAtlas(RACK_SERVER_ERROR);
            }
            if (isRecent(event.data.getLong("lastFileSystemAccess"), SERVER_ACTIVITY_MILLIS)) {
                event.renderOverlayFromAtlas(RACK_SERVER_ACTIVITY);
            }
            if (event.data.getBoolean("isRunning")
                && isRecent(event.data.getLong("lastNetworkActivity"), NETWORK_ACTIVITY_MILLIS)
                && System.currentTimeMillis() % 200L > 100L) {
                event.renderOverlayFromAtlas(RACK_SERVER_NETWORK_ACTIVITY);
            }
        } else if (isTerminalServer(event)) {
            event.renderOverlayFromAtlas(RACK_TERMINAL_SERVER_ON);
            final ListTag keys = event.data.getList("keys", ListTag.TAG_STRING);
            if (!keys.isEmpty()) {
                final float u0 = 7F / 16F;
                final float u1 = u0 + (2F * Mth.clamp(keys.size(), 0, 4) - 1F) / 16F;
                event.renderOverlayFromAtlas(RACK_TERMINAL_SERVER_PRESENCE, u0, u1);
            }
        }
    }

    private static boolean isDiskDriveMountable(final RackMountableRenderEvent event) {
        return item(event) == ModItems.DISK_DRIVE_MOUNTABLE.get();
    }

    private static boolean isServer(final RackMountableRenderEvent event) {
        final Item item = item(event);
        return item == ModItems.SERVER_TIER1.get()
            || item == ModItems.SERVER_TIER2.get()
            || item == ModItems.SERVER_TIER3.get();
    }

    private static boolean isTerminalServer(final RackMountableRenderEvent event) {
        return item(event) == ModItems.TERMINAL_SERVER.get();
    }

    private static Item item(final RackMountableRenderEvent event) {
        if (event.rack == null || event.mountable < 0 || event.mountable >= event.rack.getContainerSize()) {
            return null;
        }
        final ItemStack stack = event.rack.getItem(event.mountable);
        return stack.isEmpty() ? null : stack.getItem();
    }

    private static boolean isRecent(final long timestamp, final long windowMillis) {
        return timestamp > 0L && System.currentTimeMillis() - timestamp < windowMillis;
    }

    private static boolean showErrorLight(final int seed) {
        return (System.currentTimeMillis() / 500L + Math.abs(seed)) % 2L == 0L;
    }
}
