package li.cil.oc.common.network;

import li.cil.oc.common.ItemRegistry;
import li.cil.oc.common.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Client-main-thread metadata, separate from the shared/integrated-server item registry. */
public final class LootDiskClientState {
    private static final Map<String, ItemStack> disks = new LinkedHashMap<>();
    private static final List<Subscription> subscriptions = new ArrayList<>();
    private static boolean dirty;

    public static void accept(LootDiskMetadataPayload payload) {
        final var stack = payload.stack();
        if (stack.isEmpty()) {
            reset();
            return;
        }
        final var factory = factoryId(stack);
        if (factory == null || ItemStack.isSameItemSameComponents(stack, disks.getOrDefault(factory, ItemStack.EMPTY))) return;
        disks.put(factory, stack);
        dirty = true;
    }

    public static String factoryId(ItemStack stack) {
        if (stack.isEmpty() || !stack.is(ModItems.FLOPPY.get())) return null;
        final var data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return null;
        final var tag = data.copyTag();
        return tag.contains(ItemRegistry.FLOPPY_FACTORY_ID_TAG, Tag.TAG_STRING) ? tag.getString(ItemRegistry.FLOPPY_FACTORY_ID_TAG) : null;
    }

    public static List<ItemStack> stacks() {
        return disks.values().stream().map(ItemStack::copy).toList();
    }

    public static void reset() {
        disks.clear();
        dirty = true;
    }

    public static AutoCloseable subscribe(Runnable listener) {
        final var subscription = new Subscription(Objects.requireNonNull(listener));
        subscriptions.add(subscription);
        dirty = true;
        return subscription;
    }

    public static void flush() {
        if (!dirty) return;
        dirty = false;
        for (final var subscription : List.copyOf(subscriptions)) {
            if (subscription.active) subscription.listener.run();
        }
    }

    private static final class Subscription implements AutoCloseable {
        final Runnable listener;
        boolean active = true;
        Subscription(Runnable listener) { this.listener = listener; }
        @Override public void close() {
            active = false;
            subscriptions.remove(this);
        }
    }

    private LootDiskClientState() {}
}
