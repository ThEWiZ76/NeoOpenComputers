package li.cil.oc.common.item;

import li.cil.oc.api.Machine;
import li.cil.oc.api.internal.Keyboard;
import li.cil.oc.api.internal.Tablet;
import li.cil.oc.api.internal.TextBuffer;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Node;
import li.cil.oc.common.blockentity.ScreenItemEnvironment;
import li.cil.oc.common.component.TabletEnvironment;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Server-side computer belonging to one assembled tablet. */
public final class TabletRuntime implements Tablet {
    private static final String RUNTIME_TAG = "runtime";
    private final TabletItem item;
    private final ItemStack stack;
    private final Player player;
    private final Level world;
    private final List<ItemStack> components = new ArrayList<>();
    private final Set<ManagedEnvironment> environments = new LinkedHashSet<>();
    private final li.cil.oc.api.machine.Machine machine;
    private final ScreenItemEnvironment screen;
    private final TabletEnvironment tablet;
    private double publishedCharge;
    private boolean closed;

    public TabletRuntime(final ItemStack stack, final Player player) {
        if (!(stack.getItem() instanceof TabletItem tabletItem) || !tabletItem.hasData(stack)
            || player == null || player.level().isClientSide) {
            throw new IllegalArgumentException("A server player and assembled tablet are required");
        }
        this.item = tabletItem;
        this.stack = stack;
        this.player = player;
        world = player.level();
        for (int slot = 1; slot < TabletItem.COMPONENT_SLOTS; slot++) components.add(item.getComponent(stack, slot));
        final CompoundTag saved = TabletItem.readData(stack).getCompound(RUNTIME_TAG);
        // The integrated screen bypasses the blacklist for installable screens.
        screen = new ScreenItemEnvironment(this, 1, saved.getCompound("screen"), null);
        screen.setMaximumResolution(80, 25);
        screen.setMaximumColorDepth(TextBuffer.ColorDepth.FourBit);
        tablet = new TabletEnvironment(this);
        tablet.load(saved.getCompound("tablet"));
        machine = Machine.create(this);
        // Node addresses must be restored before onHostChanged builds the network.
        if (saved.contains("machine")) machine.node().load(saved.getCompound("machine").getCompound("node"));
        machine.onHostChanged();
        machine.node().connect(screen.node());
        machine.node().connect(tablet.node());
        if (saved.contains("machine")) machine.load(saved.getCompound("machine"));
        final Connector battery = (Connector) machine.node();
        battery.setLocalBufferSize(item.maxCharge(stack));
        battery.changeBuffer(-battery.globalBuffer());
        battery.changeBuffer(item.getCharge(stack));
        publishedCharge = item.getCharge(stack);
    }

    public boolean start() {
        if (closed) return false;
        final boolean started = machine.start();
        publish();
        return started;
    }

    public void tick() {
        if (closed) return;
        acceptCharge();
        if (item.tier(stack) >= 3) ((Connector) machine.node()).changeBuffer(Double.POSITIVE_INFINITY);
        machine.update();
        for (ManagedEnvironment environment : List.copyOf(environments)) {
            if (environment.canUpdate()) environment.update();
        }
        screen.update();
        publish();
    }

    private void acceptCharge() {
        final double charge = item.getCharge(stack);
        if (charge != publishedCharge) ((Connector) machine.node()).changeBuffer(charge - publishedCharge);
    }

    private void publish() {
        final double charge = ((Connector) machine.node()).globalBuffer();
        if (item.getCharge(stack) != charge) item.setCharge(stack, charge);
        publishedCharge = item.getCharge(stack);
        if (item.isRunning(stack) != machine.isRunning()) item.setRunning(stack, machine.isRunning());
    }

    /** Snapshot components before encoding them back into the item. */
    public void save() {
        if (closed) return;
        acceptCharge();
        publish();
        final CompoundTag saved = new CompoundTag();
        final CompoundTag machineData = new CompoundTag();
        machine.save(machineData);
        saved.put("machine", machineData);
        final CompoundTag screenData = new CompoundTag();
        screen.save(screenData);
        saved.put("screen", screenData);
        final CompoundTag tabletData = new CompoundTag();
        tablet.save(tabletData);
        saved.put("tablet", tabletData);
        for (int slot = 1; slot < TabletItem.COMPONENT_SLOTS; slot++) item.setComponent(stack, slot, components.get(slot - 1));
        final CompoundTag data = TabletItem.readData(stack);
        data.put(RUNTIME_TAG, saved);
        TabletItem.writeData(stack, data);
    }

    /** Dimension handoff preserves execution; ordinary eviction stops the tablet. */
    public void close(final boolean preserveExecution) {
        if (closed) return;
        if (!preserveExecution) machine.stop();
        save();
        final List<Node> nodes = new ArrayList<>();
        if (machine.node().network() != null) machine.node().network().nodes().forEach(nodes::add);
        nodes.forEach(Node::remove);
        machine.stop();
        closed = true;
    }

    public ScreenItemEnvironment screen() { return screen; }
    public boolean isClosed() { return closed; }
    public ItemStack stack() { return stack; }
    @Override public Player player() { return player; }
    @Override public Level world() { return world; }
    @Override public li.cil.oc.api.machine.Machine machine() { return machine; }
    @Override public Iterable<ItemStack> internalComponents() { return components; }
    @Override public double xPosition() { return player.getX(); }
    @Override public double yPosition() { return player.getEyeY(); }
    @Override public double zPosition() { return player.getZ(); }
    @Override public void markChanged() { player.getInventory().setChanged(); }
    @Override public Direction facing() { return player.getDirection(); }
    @Override public Direction toGlobal(Direction side) {
        return side.getAxis().isVertical() ? side : Direction.from2DDataValue(side.get2DDataValue() + facing().get2DDataValue() - Direction.NORTH.get2DDataValue());
    }
    @Override public Direction toLocal(Direction side) {
        return side.getAxis().isVertical() ? side : Direction.from2DDataValue(side.get2DDataValue() - facing().get2DDataValue() + Direction.NORTH.get2DDataValue());
    }
    @Override public int componentSlot(String address) {
        if (address != null && address.equals(screen.node().address())) return 0;
        for (int slot = 0; slot < components.size(); slot++) {
            if (address != null && address.equals(ItemDriverData.dataTag(components.get(slot)).getCompound("node").getString("address"))) return slot + 1;
        }
        return -1;
    }
    @Override public void onMachineConnect(Node node) {
        if (node.host() instanceof ManagedEnvironment environment) environments.add(environment);
        if (node.host() instanceof Keyboard) screen.node().connect(node);
    }
    @Override public void onMachineDisconnect(Node node) {
        environments.remove(node.host());
    }
}
