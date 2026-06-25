package li.cil.oc.common.blockentity;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.SidedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.ForgeEnergyStorageView;
import li.cil.oc.common.ModBlockEntities;
import li.cil.oc.common.ModSettings;
import li.cil.oc.common.OpenComputersApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.Map;

public class PowerConverterBlockEntity extends BlockEntity implements Environment, SidedEnvironment, DeviceInfo {
    private static final String TAG_NODE = "oc:node";

    private final Connector node;
    private final IEnergyStorage energyStorage;

    public PowerConverterBlockEntity(final BlockPos pos, final BlockState blockState) {
        super(ModBlockEntities.POWER_CONVERTER.get(), pos, blockState);
        OpenComputersApi.initialize();
        node = Network.newNode(this, Visibility.None)
            .withConnector(connectorBufferSize())
            .create();
        energyStorage = new ForgeEnergyStorageView(() -> node, PowerConverterBlockEntity::energyThroughput);
    }

    @Override
    public Node node() {
        return node;
    }

    @Override
    public Node sidedNode(final Direction side) {
        return side == null ? null : node;
    }

    @Override
    public boolean canConnect(final Direction side) {
        return side != null;
    }

    @Override
    public void onConnect(final Node node) {
    }

    @Override
    public void onDisconnect(final Node node) {
    }

    @Override
    public void onMessage(final Message message) {
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return deviceInfo();
    }

    public IEnergyStorage energyStorage(final Direction side) {
        return energyStorage;
    }

    @Override
    protected void loadAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        node.load(tag.getCompound(TAG_NODE));
    }

    @Override
    protected void saveAdditional(final CompoundTag tag, final HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (node.address() == null) {
            Network.joinNewNetwork(node);
        }
        final CompoundTag nodeTag = new CompoundTag();
        node.save(nodeTag);
        tag.put(TAG_NODE, nodeTag);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        removeNode();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        removeNode();
    }

    public void removeNode() {
        node.remove();
    }

    public static double connectorBufferSize() {
        return ModSettings.converterBuffer();
    }

    public static double energyThroughput() {
        return ModSettings.powerConverterRate();
    }

    public static Map<String, String> deviceInfo() {
        return Map.of(
            DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Power,
            DeviceInfo.DeviceAttribute.Description, "Power converter",
            DeviceInfo.DeviceAttribute.Vendor, "MightyPirates GmbH & Co. KG",
            DeviceInfo.DeviceAttribute.Product, "Transgizer-PX5",
            DeviceInfo.DeviceAttribute.Capacity, Double.toString(energyThroughput()));
    }

}
