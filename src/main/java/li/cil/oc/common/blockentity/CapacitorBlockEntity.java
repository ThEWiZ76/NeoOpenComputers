package li.cil.oc.common.blockentity;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.SidedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.ModBlockEntities;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModSettings;
import li.cil.oc.common.OpenComputersApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.phys.AABB;
import java.util.List;
import java.util.Map;

public class CapacitorBlockEntity extends BlockEntity implements Environment, SidedEnvironment, DeviceInfo {
    private final Connector node;
    private double lastBuffer = -1;
    private long nextShockTime;

    public CapacitorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CAPACITOR.get(), pos, state);
        OpenComputersApi.initialize();
        // Detached loading must accept the full clustered capacity before neighbors are available.
        node = Network.newNode(this, Visibility.Network).withConnector(maxCapacity()).create();
    }

    @Override public Connector node() { return node; }
    @Override public Node sidedNode(Direction side) { return side == null ? null : node; }
    @Override public boolean canConnect(Direction side) { return side != null; }
    @Override public void onConnect(Node connected) { if (connected == node) recomputeCapacity(); }
    @Override public void onDisconnect(Node disconnected) { }
    @Override public void onMessage(Message message) { }

    public static double maxCapacity() { return ModSettings.capacitorBuffer() + 9 * ModSettings.capacitorAdjacencyBonus(); }

    public void recomputeCapacity() {
        if (level == null || level.isClientSide || isRemoved()) return;
        double capacity = ModSettings.capacitorBuffer();
        for (final var side : Direction.values()) {
            if (isCapacitor(worldPosition.relative(side))) capacity += ModSettings.capacitorAdjacencyBonus();
            if (isCapacitor(worldPosition.relative(side, 2))) capacity += ModSettings.capacitorAdjacencyBonus() / 2;
        }
        node.setLocalBufferSize(capacity);
        setChanged();
        level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
    }

    private boolean isCapacitor(BlockPos pos) {
        return level.isLoaded(pos) && level.getBlockEntity(pos) instanceof CapacitorBlockEntity capacitor && !capacitor.isRemoved();
    }

    public void refreshNearby() {
        if (level == null || level.isClientSide) return;
        recomputeCapacity();
        for (final var side : Direction.values()) {
            for (int distance = 1; distance <= 2; distance++) {
                final var pos = worldPosition.relative(side, distance);
                if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof CapacitorBlockEntity capacitor) capacitor.recomputeCapacity();
            }
        }
    }

    @Override public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            Network.joinOrCreateNetwork(level, worldPosition);
            refreshNearby();
        }
    }

    public void serverTick() {
        if (isCarpeted() && Math.floorMod(level.getGameTime() + worldPosition.asLong(), 20) == 0) {
            generatePower(level.getGameTime(), level.random);
        }
        if (node.localBuffer() != lastBuffer) {
            lastBuffer = node.localBuffer();
            setChanged();
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }

    private boolean isCarpeted() { return getBlockState().is(ModBlocks.CARPETED_CAPACITOR.get()); }

    public void generatePower(long gameTime, RandomSource random) {
        if (level == null || level.isClientSide || isRemoved() || !isCarpeted()) return;
        final var animals = level.getEntitiesOfClass(LivingEntity.class, new AABB(worldPosition.above()), LivingEntity::isAlive);
        final double sheep = powerFromGroup(animals.stream().filter(entity -> entity instanceof Sheep).toList(),
            ModSettings.carpetSheepPower(), gameTime, random);
        // Modern cats are the domestic branch of the old ocelot entity.
        final double cats = powerFromGroup(animals.stream().filter(entity -> entity instanceof Cat || entity instanceof Ocelot).toList(),
            ModSettings.carpetOcelotPower(), gameTime, random);
        if (sheep + cats > 0) node.changeBuffer(sheep + cats);
    }

    private double powerFromGroup(List<LivingEntity> animals, double power, long gameTime, RandomSource random) {
        if (animals.size() < 2) return 0;
        final double chance = ModSettings.carpetDamageChance();
        if (chance > 0 && nextShockTime < gameTime) {
            for (final var animal : animals) {
                if (random.nextDouble() < chance) {
                    animal.hurt(level.damageSources().generic(), 1);
                    animal.setLastHurtByMob(animal);
                    animal.knockback(0, 0.25, 0);
                    nextShockTime = gameTime + 1200;
                    break;
                }
            }
        }
        return power;
    }

    public int comparatorOutput() {
        return node.localBufferSize() <= 0 ? 0 : (int) Math.round(15 * node.localBuffer() / node.localBufferSize());
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        node.setLocalBufferSize(maxCapacity());
        node.load(tag.getCompound("oc:node"));
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (node.address() == null) Network.joinNewNetwork(node);
        final var saved = new CompoundTag();
        node.save(saved);
        tag.put("oc:node", saved);
    }

    @Override public void setRemoved() {
        super.setRemoved();
        node.remove();
        refreshNearby();
    }

    @Override public void onChunkUnloaded() {
        super.onChunkUnloaded();
        node.remove();
    }

    @Override public Map<String, String> getDeviceInfo() {
        return Map.of(DeviceAttribute.Class, DeviceClass.Power, DeviceAttribute.Description, "Battery",
            DeviceAttribute.Vendor, "MightyPirates GmbH & Co. KG", DeviceAttribute.Product, isCarpeted() ? "CarpetedCapBank3x" : "CapBank3x",
            DeviceAttribute.Capacity, Double.toString(maxCapacity()));
    }
}
