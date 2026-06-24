package li.cil.oc.common.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.NeoOpenComputers;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;

import java.util.Map;

public final class ChunkloaderUpgradeEnvironment extends AbstractManagedEnvironment implements DeviceInfo {
    private static final String COMPONENT_NAME = "chunkloader";
    private static final String ACTIVE_TAG = "active";
    private static final TicketController TICKETS = new TicketController(ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "chunkloader_upgrade"));

    private static final Map<String, String> DEVICE_INFO = Map.of(
        DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Generic,
        DeviceInfo.DeviceAttribute.Description, "World stabilizer",
        DeviceInfo.DeviceAttribute.Vendor, "MightyPirates GmbH & Co. KG",
        DeviceInfo.DeviceAttribute.Product, "Realizer9001-CL"
    );

    private final EnvironmentHost host;
    private boolean active;
    private boolean forced;

    public ChunkloaderUpgradeEnvironment(final EnvironmentHost host) {
        this.host = host;
        final var builder = Network.newNode(this, Visibility.Network);
        if (builder != null) {
            setNode(builder.withComponent(COMPONENT_NAME, Visibility.Network).withConnector().create());
        }
    }

    public static void registerTicketController(final RegisterTicketControllersEvent event) {
        event.register(TICKETS);
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return DEVICE_INFO;
    }

    @Callback(doc = "function():boolean -- Gets whether the chunkloader is currently active.")
    public Object[] isActive(final Context context, final Arguments arguments) {
        return new Object[]{active};
    }

    @Callback(doc = "function(enabled:boolean):boolean -- Enables or disables the chunkloader, returns true if active changed.")
    public Object[] setActive(final Context context, final Arguments arguments) {
        return new Object[]{setActive(arguments.checkBoolean(0))};
    }

    @Override
    public void onMessage(final Message message) {
        super.onMessage(message);
        if ("computer.stopped".equals(message.name())) {
            setActive(false);
        } else if ("computer.started".equals(message.name()) && host != null && host.world() != null && !host.world().isClientSide()) {
            setActive(true);
        }
    }

    @Override
    public void onDisconnect(final li.cil.oc.api.network.Node node) {
        super.onDisconnect(node);
        if (node == node()) {
            setActive(false);
        }
    }

    @Override
    public void load(final CompoundTag tag) {
        super.load(tag);
        active = tag.getBoolean(ACTIVE_TAG);
        updateChunkTicket();
    }

    @Override
    public void save(final CompoundTag tag) {
        super.save(tag);
        if (active) {
            tag.putBoolean(ACTIVE_TAG, true);
        }
    }

    private boolean setActive(final boolean value) {
        if (active == value) {
            return false;
        }
        active = value;
        updateChunkTicket();
        if (host != null) {
            host.markChanged();
        }
        return true;
    }

    private void updateChunkTicket() {
        final boolean shouldForce = active && host != null && host.world() instanceof ServerLevel;
        if (forced == shouldForce) {
            return;
        }
        final ServerLevel level = host != null && host.world() instanceof ServerLevel serverLevel ? serverLevel : null;
        if (level != null) {
            final BlockPos owner = ownerPosition();
            final ChunkPos chunk = new ChunkPos(owner);
            TICKETS.forceChunk(level, owner, chunk.x, chunk.z, shouldForce, true);
        }
        forced = shouldForce;
    }

    private BlockPos ownerPosition() {
        return BlockPos.containing(host.xPosition(), host.yPosition(), host.zPosition());
    }
}
