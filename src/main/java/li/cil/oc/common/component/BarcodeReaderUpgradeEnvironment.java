package li.cil.oc.common.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.network.Analyzable;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.SidedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Map;

public final class BarcodeReaderUpgradeEnvironment extends AbstractManagedEnvironment implements DeviceInfo {
    private static final String COMPONENT_NAME = "barcode_reader";
    private static final Map<String, String> DEVICE_INFO = Map.of(
        DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Generic,
        DeviceInfo.DeviceAttribute.Description, "Barcode reader upgrade",
        DeviceInfo.DeviceAttribute.Vendor, "MightyPirates GmbH & Co. KG",
        DeviceInfo.DeviceAttribute.Product, "Readerizer Deluxe"
    );

    @SuppressWarnings("unused")
    private final EnvironmentHost host;

    public BarcodeReaderUpgradeEnvironment(final EnvironmentHost host) {
        this.host = host;
        final var builder = Network.newNode(this, Visibility.Network);
        if (builder != null) {
            setNode(builder.withComponent(COMPONENT_NAME).withConnector().create());
        }
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return DEVICE_INFO;
    }

    @Override
    public void onMessage(final Message message) {
        super.onMessage(message);
        if (message == null || !"tablet.use".equals(message.name())) {
            return;
        }
        final Object[] data = message.data();
        if (data.length < 8 || !(data[0] instanceof CompoundTag nbt) || !(data[3] instanceof BlockPos blockPos) || !(data[4] instanceof Direction side)) {
            return;
        }
        if (host == null || host.world() == null) {
            return;
        }

        final BlockEntity blockEntity = host.world().getBlockEntity(blockPos);
        final Player player = data[2] instanceof Player value ? value : null;
        final float hitX = data[5] instanceof Float value ? value : 0F;
        final float hitY = data[6] instanceof Float value ? value : 0F;
        final float hitZ = data[7] instanceof Float value ? value : 0F;
        if (blockEntity instanceof Analyzable analyzable) {
            processNodes(analyzable.onAnalyze(player, side, hitX, hitY, hitZ), nbt);
        } else if (blockEntity instanceof SidedEnvironment sidedEnvironment) {
            processNodes(new Node[]{sidedEnvironment.sidedNode(side)}, nbt);
        } else if (blockEntity instanceof Environment environment) {
            processNodes(new Node[]{environment.node()}, nbt);
        }
    }

    private static void processNodes(final Node[] nodes, final CompoundTag nbt) {
        if (nodes == null) {
            return;
        }
        final ListTag analyzed = new ListTag();
        for (final Node node : nodes) {
            if (node == null) {
                continue;
            }
            final CompoundTag nodeData = new CompoundTag();
            if (node instanceof Component component) {
                nodeData.putString("type", component.name());
            }
            final String address = node.address();
            if (address != null && !address.isEmpty()) {
                nodeData.putString("address", address);
            }
            analyzed.add(nodeData);
        }
        nbt.put("analyzed", analyzed);
    }
}
