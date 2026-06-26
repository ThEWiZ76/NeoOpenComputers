package li.cil.oc.common.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.internal.Database;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.function.Consumer;

public class DatabaseEnvironment extends AbstractManagedEnvironment implements Database, DeviceInfo {
    private static final String COMPONENT_NAME = "database";
    private static final String TAG_ITEMS = "items";
    private static final String TAG_SLOT = "slot";
    private static final String TAG_STACK = "stack";

    private final ItemStack[] items;
    private final Consumer<CompoundTag> saveData;

    public DatabaseEnvironment(final int slots) {
        this(slots, null);
    }

    public DatabaseEnvironment(final int slots, final Consumer<CompoundTag> saveData) {
        items = new ItemStack[Math.max(1, slots)];
        this.saveData = saveData;
        final var builder = Network.newNode(this, Visibility.Network);
        if (builder != null) {
            setNode(builder.withComponent(COMPONENT_NAME, Visibility.Network).create());
        }
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return Map.of(
            DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Generic,
            DeviceInfo.DeviceAttribute.Description, "Object catalogue",
            DeviceInfo.DeviceAttribute.Vendor, "MightyPirates GmbH & Co. KG",
            DeviceInfo.DeviceAttribute.Product, "iCatalogue (patent pending)",
            DeviceInfo.DeviceAttribute.Capacity, Integer.toString(size())
        );
    }

    @Override
    public int size() {
        return items.length;
    }

    @Override
    public ItemStack getStackInSlot(final int slot) {
        if (slot < 0 || slot >= items.length) {
            return ItemStack.EMPTY;
        }
        final ItemStack stack = items[slot];
        return stack == null ? ItemStack.EMPTY : stack.copy();
    }

    @Override
    public void setStackInSlot(final int slot, final ItemStack stack) {
        if (slot < 0 || slot >= items.length) {
            return;
        }
        items[slot] = stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
    }

    @Override
    public int findStackWithHash(final String needle) {
        for (int slot = 0; slot < items.length; slot++) {
            final String hash = hash(getStackInSlot(slot));
            if (hash != null && hash.equals(needle)) {
                return slot;
            }
        }
        return -1;
    }

    @Override
    public void load(final CompoundTag nbt) {
        super.load(nbt);
        clearAll();
        if (!nbt.contains(TAG_ITEMS, Tag.TAG_LIST)) {
            return;
        }
        final ListTag list = nbt.getList(TAG_ITEMS, Tag.TAG_COMPOUND);
        for (int index = 0; index < list.size(); index++) {
            final CompoundTag entry = list.getCompound(index);
            final int slot = entry.getInt(TAG_SLOT);
            if (slot >= 0 && slot < items.length && entry.contains(TAG_STACK, Tag.TAG_COMPOUND)) {
                items[slot] = ItemStack.OPTIONAL_CODEC.parse(NbtOps.INSTANCE, entry.getCompound(TAG_STACK)).result().orElse(ItemStack.EMPTY);
            }
        }
    }

    @Override
    public void save(final CompoundTag nbt) {
        super.save(nbt);
        final ListTag list = new ListTag();
        for (int slot = 0; slot < items.length; slot++) {
            final ItemStack stack = items[slot];
            if (stack != null && !stack.isEmpty()) {
                final CompoundTag entry = new CompoundTag();
                entry.putInt(TAG_SLOT, slot);
                entry.put(TAG_STACK, ItemStack.OPTIONAL_CODEC.encodeStart(NbtOps.INSTANCE, stack).result().orElseGet(CompoundTag::new));
                list.add(entry);
            }
        }
        nbt.put(TAG_ITEMS, list);
        if (saveData != null) {
            saveData.accept(nbt.copy());
        }
    }

    @Callback(doc = "function(slot:number):table -- Get the representation of the item stack stored in the specified slot.")
    public Object[] get(final Context context, final Arguments arguments) {
        return new Object[]{getStackInSlot(checkSlot(arguments, 0))};
    }

    @Callback(doc = "function(slot:number):string -- Computes a hash value for the item stack in the specified slot.")
    public Object[] computeHash(final Context context, final Arguments arguments) {
        final String hash = hash(getStackInSlot(checkSlot(arguments, 0)));
        return hash == null ? null : new Object[]{hash};
    }

    @Callback(doc = "function(hash:string):number -- Get the index of an item stack with the specified hash. Returns a negative value if no such stack was found.")
    public Object[] indexOf(final Context context, final Arguments arguments) {
        final int slot = findStackWithHash(arguments.checkString(0));
        return new Object[]{slot < 0 ? -1 : slot + 1};
    }

    @Callback(doc = "function(slot:number):boolean -- Clears the specified slot. Returns true if there was something in the slot before.")
    public Object[] clear(final Context context, final Arguments arguments) {
        final int slot = checkSlot(arguments, 0);
        final boolean hadStack = items[slot] != null && !items[slot].isEmpty();
        items[slot] = ItemStack.EMPTY;
        return new Object[]{hadStack};
    }

    @Callback(doc = "function(fromSlot:number, toSlot:number[, address:string]):boolean -- Copies an entry to another slot, optionally to another database. Returns true if something was overwritten.")
    public Object[] copy(final Context context, final Arguments arguments) {
        final int fromSlot = checkSlot(arguments, 0);
        final Database target = arguments.count() > 2 ? database(arguments.checkString(2)) : this;
        final int toSlot = checkSlot(arguments, 1, target.size());
        final boolean overwritten = !target.getStackInSlot(toSlot).isEmpty();
        target.setStackInSlot(toSlot, getStackInSlot(fromSlot));
        return new Object[]{overwritten};
    }

    @Callback(doc = "function(address:string):number -- Copies the data stored in this database to another database with the specified address.")
    public Object[] clone(final Context context, final Arguments arguments) {
        final Database target = database(arguments.checkString(0));
        final int slots = Math.min(size(), target.size());
        for (int slot = 0; slot < slots; slot++) {
            target.setStackInSlot(slot, getStackInSlot(slot));
        }
        if (context != null) {
            context.pause(0.25);
        }
        return new Object[]{slots};
    }

    private int checkSlot(final Arguments arguments, final int index) {
        return checkSlot(arguments, index, items.length);
    }

    private int checkSlot(final Arguments arguments, final int index, final int size) {
        final int slot = arguments.checkInteger(index) - 1;
        if (slot < 0 || slot >= size) {
            throw new IllegalArgumentException("invalid slot");
        }
        return slot;
    }

    private Database database(final String address) {
        if (node() == null || node().network() == null) {
            throw new IllegalArgumentException("no such component");
        }
        if (!(node().network().node(address) instanceof Component component)) {
            throw new IllegalArgumentException("no such component");
        }
        if (!(component.host() instanceof Database database)) {
            throw new IllegalArgumentException("not a database");
        }
        return database;
    }

    private void clearAll() {
        for (int slot = 0; slot < items.length; slot++) {
            items[slot] = ItemStack.EMPTY;
        }
    }

    private static String hash(final ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        final Tag encoded = ItemStack.OPTIONAL_CODEC.encodeStart(NbtOps.INSTANCE, stack).result().orElseGet(CompoundTag::new);
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(encoded.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
