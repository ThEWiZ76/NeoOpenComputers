package li.cil.oc.common.item;

import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.item.Chargeable;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.internal.Tablet;
import li.cil.oc.api.internal.Tiered;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Node;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.ModSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class TabletItem extends Item implements Chargeable, DriverItem {
    public static final int COMPONENT_SLOTS = 32;
    public static final double DEFAULT_MAX_CHARGE = 10000D;

    private static final String DATA_TAG = "oc:tablet";
    private static final String DRIVER_DATA_TAG = "oc:data";
    private static final String STACK_COMPONENTS_TAG = "components";
    private static final String STACK_CUSTOM_DATA_TAG = "minecraft:custom_data";
    private static final String ENERGY_TAG = "energy";
    private static final String MAX_ENERGY_TAG = "maxEnergy";
    private static final String TIER_TAG = "tier";
    private static final String RUNNING_TAG = "running";
    private static final String CONTAINER_TAG = "container";
    private static final String COMPONENTS_TAG = "components";
    private static final String SLOT_TAG = "slot";
    private static final String STACK_TAG = "stack";

    public TabletItem(final Properties properties) {
        super(properties);
    }

    @Override
    public boolean canCharge(final ItemStack stack) {
        return stack != null && stack.getItem() == this;
    }

    @Override
    public boolean worksWith(final ItemStack stack) {
        return canCharge(stack);
    }

    @Override
    public ManagedEnvironment createEnvironment(final ItemStack stack, final EnvironmentHost host) {
        return null;
    }

    @Override
    public String slot(final ItemStack stack) {
        return Slot.Tablet;
    }

    @Override
    public double charge(final ItemStack stack, final double amount, final boolean simulate) {
        if (!canCharge(stack)) {
            return 0D;
        }
        if (amount < 0D) {
            return amount;
        }
        final double stored = getCharge(stack);
        final double accepted = Math.min(amount, Math.max(0D, maxCharge(stack) - stored));
        if (!simulate && accepted > 0D) {
            setCharge(stack, stored + accepted);
        }
        return accepted;
    }

    @Override
    public boolean isBarVisible(final ItemStack stack) {
        return canCharge(stack);
    }

    @Override
    public int getBarWidth(final ItemStack stack) {
        final double maxCharge = maxCharge(stack);
        if (maxCharge <= 0D) {
            return 0;
        }
        return Mth.clamp((int) Math.round(13D * getCharge(stack) / maxCharge), 0, 13);
    }

    @Override
    public int getBarColor(final ItemStack stack) {
        final double maxCharge = maxCharge(stack);
        if (maxCharge <= 0D) {
            return 0xFF0000;
        }
        return Mth.hsvToRgb(Math.max(0F, (float) (getCharge(stack) / maxCharge) / 3F), 1F, 1F);
    }

    public double getCharge(final ItemStack stack) {
        return Math.max(0D, Math.min(readData(stack).getDouble(ENERGY_TAG), maxCharge(stack)));
    }

    public void setCharge(final ItemStack stack, final double amount) {
        final CompoundTag data = readData(stack);
        data.putDouble(ENERGY_TAG, Math.max(0D, Math.min(amount, maxCharge(stack))));
        writeData(stack, data);
    }

    public double maxCharge(final ItemStack stack) {
        return Math.max(0D, readData(stack).getDouble(MAX_ENERGY_TAG));
    }

    public void setMaxCharge(final ItemStack stack, final double amount) {
        final CompoundTag data = readData(stack);
        final double maxCharge = Math.max(0D, amount);
        data.putDouble(MAX_ENERGY_TAG, maxCharge);
        data.putDouble(ENERGY_TAG, Math.max(0D, Math.min(data.getDouble(ENERGY_TAG), maxCharge)));
        writeData(stack, data);
    }

    public ItemStack assembleFromCase(final ItemStack caseStack, final ItemStack container, final ItemStack... components) {
        final ItemStack stack = new ItemStack(this);
        setTier(stack, caseTier(caseStack));
        setContainer(stack, container);
        final double maxCharge = ModSettings.tabletBuffer();
        setMaxCharge(stack, maxCharge);
        setCharge(stack, maxCharge);
        setComponent(stack, 0, new ItemStack(ModItems.SCREEN_TIER1.get()));
        if (components != null) {
            for (int index = 0; index < components.length && index + 1 < COMPONENT_SLOTS; index++) {
                setComponent(stack, index + 1, components[index]);
            }
        }
        return stack;
    }

    public ItemStack[] disassembleToIngredients(final ItemStack stack) {
        final ArrayList<ItemStack> ingredients = new ArrayList<>();
        ingredients.add(caseForTier(tier(stack)));
        final ItemStack container = getContainer(stack);
        if (!container.isEmpty()) {
            ingredients.add(container);
        }
        for (int slot = 1; slot < COMPONENT_SLOTS; slot++) {
            final ItemStack component = getComponent(stack, slot);
            if (!component.isEmpty()) {
                ingredients.add(component);
            }
        }
        return ingredients.toArray(ItemStack[]::new);
    }

    @Override
    public int tier(final ItemStack stack) {
        return Math.max(0, readData(stack).getInt(TIER_TAG));
    }

    @Override
    public CompoundTag dataTag(final ItemStack stack) {
        if (stack == null) {
            return new CompoundTag();
        }
        final CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return new CompoundTag();
        }
        final CompoundTag data = customData.getUnsafe().getCompound(DATA_TAG);
        final ListTag components = data.getList(COMPONENTS_TAG, Tag.TAG_COMPOUND);
        for (int index = 0; index < components.size(); index++) {
            final CompoundTag entry = components.getCompound(index);
            final CompoundTag stackData = entry.getCompound(STACK_TAG);
            final ItemStack component = decodeStack(stackData);
            if (isFilesystemComponent(component)) {
                return stackDriverDataTag(stackData);
            }
        }
        return new CompoundTag();
    }

    public CompoundTag analyzeBlock(
        final ItemStack stack,
        final Level level,
        final Player player,
        final BlockPos pos,
        final Direction side,
        final float hitX,
        final float hitY,
        final float hitZ) {
        final CompoundTag data = new CompoundTag();
        if (!isRunning(stack) || level == null || pos == null || side == null) {
            return data;
        }

        final TabletAnalysisHost host = new TabletAnalysisHost(stack, level, player, pos, side);
        final li.cil.oc.api.machine.Machine machine = host.machine();
        if (machine == null || machine.node() == null) {
            return data;
        }

        machine.onHostChanged();
        machine.node().sendToReachable(
            "tablet.use",
            data,
            stack,
            player,
            pos,
            side,
            Float.valueOf(hitX),
            Float.valueOf(hitY),
            Float.valueOf(hitZ));
        machine.node().remove();
        return data;
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        final BlockPos pos = context.getClickedPos();
        final Vec3 hit = context.getClickLocation();
        final CompoundTag data = analyzeBlock(
            context.getItemInHand(),
            context.getLevel(),
            context.getPlayer(),
            pos,
            context.getClickedFace(),
            (float) (hit.x - pos.getX()),
            (float) (hit.y - pos.getY()),
            (float) (hit.z - pos.getZ()));
        return data.isEmpty() ? InteractionResult.PASS : InteractionResult.CONSUME;
    }

    public void setTier(final ItemStack stack, final int tier) {
        final CompoundTag data = readData(stack);
        data.putInt(TIER_TAG, Math.max(0, tier));
        writeData(stack, data);
    }

    public boolean isRunning(final ItemStack stack) {
        return readData(stack).getBoolean(RUNNING_TAG);
    }

    public void setRunning(final ItemStack stack, final boolean running) {
        final CompoundTag data = readData(stack);
        data.putBoolean(RUNNING_TAG, running);
        writeData(stack, data);
    }

    public ItemStack getContainer(final ItemStack stack) {
        return decodeStack(readData(stack).getCompound(CONTAINER_TAG));
    }

    public void setContainer(final ItemStack stack, final ItemStack container) {
        final CompoundTag data = readData(stack);
        if (container == null || container.isEmpty()) {
            data.remove(CONTAINER_TAG);
        } else {
            data.put(CONTAINER_TAG, encodeStack(container));
        }
        writeData(stack, data);
    }

    public ItemStack getComponent(final ItemStack stack, final int slot) {
        if (slot < 0 || slot >= COMPONENT_SLOTS) {
            return ItemStack.EMPTY;
        }
        final ListTag components = readData(stack).getList(COMPONENTS_TAG, Tag.TAG_COMPOUND);
        for (int index = 0; index < components.size(); index++) {
            final CompoundTag entry = components.getCompound(index);
            if (entry.getInt(SLOT_TAG) == slot) {
                return decodeStack(entry.getCompound(STACK_TAG));
            }
        }
        return ItemStack.EMPTY;
    }

    public void setComponent(final ItemStack stack, final int slot, final ItemStack component) {
        if (slot < 0 || slot >= COMPONENT_SLOTS) {
            return;
        }
        final CompoundTag data = readData(stack);
        final ListTag oldComponents = data.getList(COMPONENTS_TAG, Tag.TAG_COMPOUND);
        final ListTag newComponents = new ListTag();
        for (int index = 0; index < oldComponents.size(); index++) {
            final CompoundTag entry = oldComponents.getCompound(index);
            if (entry.getInt(SLOT_TAG) != slot) {
                newComponents.add(entry.copy());
            }
        }
        if (component != null && !component.isEmpty()) {
            final CompoundTag entry = new CompoundTag();
            entry.putInt(SLOT_TAG, slot);
            entry.put(STACK_TAG, encodeStack(component));
            newComponents.add(entry);
        }
        if (newComponents.isEmpty()) {
            data.remove(COMPONENTS_TAG);
        } else {
            data.put(COMPONENTS_TAG, newComponents);
        }
        writeData(stack, data);
    }

    private static CompoundTag readData(final ItemStack stack) {
        if (stack == null) {
            return new CompoundTag();
        }
        final CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return new CompoundTag();
        }
        return customData.copyTag().getCompound(DATA_TAG);
    }

    private static void writeData(final ItemStack stack, final CompoundTag data) {
        if (stack == null) {
            return;
        }
        final CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        final CompoundTag root = customData == null ? new CompoundTag() : customData.copyTag();
        root.put(DATA_TAG, data.copy());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }

    private static CompoundTag encodeStack(final ItemStack stack) {
        return ItemStack.OPTIONAL_CODEC.encodeStart(NbtOps.INSTANCE, stack)
            .result()
            .filter(tag -> tag instanceof CompoundTag)
            .map(tag -> (CompoundTag) tag)
            .orElseGet(CompoundTag::new);
    }

    private static ItemStack decodeStack(final CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return ItemStack.OPTIONAL_CODEC.parse(NbtOps.INSTANCE, tag).result().orElse(ItemStack.EMPTY);
    }

    private static boolean isFilesystemComponent(final ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        final DriverItem driver = Driver.driverFor(stack);
        if (driver == null) {
            return false;
        }
        final String slot = driver.slot(stack);
        return Slot.HDD.equals(slot) || Slot.Floppy.equals(slot);
    }

    private static CompoundTag stackDriverDataTag(final CompoundTag stackData) {
        if (!stackData.contains(STACK_COMPONENTS_TAG, Tag.TAG_COMPOUND)) {
            stackData.put(STACK_COMPONENTS_TAG, new CompoundTag());
        }
        final CompoundTag components = stackData.getCompound(STACK_COMPONENTS_TAG);
        if (!components.contains(STACK_CUSTOM_DATA_TAG, Tag.TAG_COMPOUND)) {
            components.put(STACK_CUSTOM_DATA_TAG, new CompoundTag());
        }
        final CompoundTag customData = components.getCompound(STACK_CUSTOM_DATA_TAG);
        if (!customData.contains(DRIVER_DATA_TAG, Tag.TAG_COMPOUND)) {
            customData.put(DRIVER_DATA_TAG, new CompoundTag());
        }
        return customData.getCompound(DRIVER_DATA_TAG);
    }

    private static int caseTier(final ItemStack caseStack) {
        if (caseStack != null && caseStack.getItem() instanceof Tiered tiered) {
            return tiered.tier();
        }
        return 0;
    }

    private static ItemStack caseForTier(final int tier) {
        if (tier >= 3) {
            return new ItemStack(ModItems.TABLET_CASE_CREATIVE.get());
        }
        if (tier >= 1) {
            return new ItemStack(ModItems.TABLET_CASE_TIER2.get());
        }
        return new ItemStack(ModItems.TABLET_CASE_TIER1.get());
    }

    private final class TabletAnalysisHost implements Tablet {
        private final ItemStack stack;
        private final Level level;
        private final Player player;
        private final BlockPos pos;
        private final Direction facing;
        private final li.cil.oc.api.machine.Machine machine;

        private TabletAnalysisHost(final ItemStack stack, final Level level, final Player player, final BlockPos pos, final Direction facing) {
            this.stack = stack;
            this.level = level;
            this.player = player;
            this.pos = pos;
            this.facing = facing;
            machine = li.cil.oc.api.Machine.create(this);
        }

        @Override
        public Player player() {
            return player;
        }

        @Override
        public Direction facing() {
            return facing;
        }

        @Override
        public Direction toGlobal(final Direction value) {
            return value;
        }

        @Override
        public Direction toLocal(final Direction value) {
            return value;
        }

        @Override
        public li.cil.oc.api.machine.Machine machine() {
            return machine;
        }

        @Override
        public Iterable<ItemStack> internalComponents() {
            final List<ItemStack> components = new ArrayList<>();
            for (int slot = 0; slot < COMPONENT_SLOTS; slot++) {
                final ItemStack component = getComponent(stack, slot);
                if (!component.isEmpty()) {
                    components.add(component);
                }
            }
            return components;
        }

        @Override
        public int componentSlot(final String address) {
            return -1;
        }

        @Override
        public void onMachineConnect(final Node node) {
        }

        @Override
        public void onMachineDisconnect(final Node node) {
        }

        @Override
        public Level world() {
            return level;
        }

        @Override
        public double xPosition() {
            return player == null ? pos.getX() + 0.5D : player.getX();
        }

        @Override
        public double yPosition() {
            return player == null ? pos.getY() + 0.5D : player.getEyeY();
        }

        @Override
        public double zPosition() {
            return player == null ? pos.getZ() + 0.5D : player.getZ();
        }

        @Override
        public void markChanged() {
        }
    }
}
