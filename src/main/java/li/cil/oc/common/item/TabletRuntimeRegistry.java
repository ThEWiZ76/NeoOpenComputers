package li.cil.oc.common.item;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Server-only live runtimes; item NBT remains the durable source after eviction. */
public final class TabletRuntimeRegistry {
    private static final Map<UUID, Entry> runtimes = new HashMap<>();
    private static final int IDLE_TICKS = 200;
    private static final class Entry {
        TabletRuntime runtime;
        int lastSeen;
        int lastUpdate = Integer.MIN_VALUE;
        Entry(TabletRuntime runtime, int tick) { this.runtime = runtime; lastSeen = tick; }
    }

    private TabletRuntimeRegistry() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(TabletRuntimeRegistry::onServerTick);
        NeoForge.EVENT_BUS.addListener(TabletRuntimeRegistry::onLogout);
        NeoForge.EVENT_BUS.addListener(TabletRuntimeRegistry::onToss);
        NeoForge.EVENT_BUS.addListener(TabletRuntimeRegistry::onWorldUnload);
        NeoForge.EVENT_BUS.addListener(TabletRuntimeRegistry::onServerStopped);
    }

    public static TabletRuntime get(final ItemStack stack, final Player player) {
        if (player.level().isClientSide) throw new IllegalArgumentException("Tablet runtime is server-only");
        var data = TabletItem.readData(stack);
        boolean assignId = !data.hasUUID("id");
        UUID id = data.hasUUID("id") ? data.getUUID("id") : UUID.randomUUID();
        Entry entry = runtimes.get(id);
        if (entry != null && entry.runtime.stack() != stack && isCarried(entry.runtime.player(), entry.runtime.stack())) {
            // A creative copy must not take over another carried tablet's VM.
            id = UUID.randomUUID();
            data.remove("runtime");
            data.putBoolean("running", false);
            entry = null;
            assignId = true;
        }
        if (assignId) {
            data.putUUID("id", id);
            TabletItem.writeData(stack, data);
        }
        final int tick = player.level().getServer().getTickCount();
        if (entry != null && entry.runtime.world() != player.level()) {
            entry.runtime.close(true);
            TabletItem.writeData(stack, TabletItem.readData(entry.runtime.stack()));
            entry = null;
        }
        if (entry == null || entry.runtime.isClosed()) {
            entry = new Entry(new TabletRuntime(stack, player), tick);
            runtimes.put(id, entry);
        } else if (entry.runtime.stack() != stack || entry.runtime.player() != player) {
            entry.runtime.rebind(stack, player);
        }
        entry.lastSeen = tick;
        return entry.runtime;
    }

    public static void tick(final ItemStack stack, final Player player) {
        final TabletRuntime runtime = get(stack, player);
        final Entry entry = runtimes.get(TabletItem.readData(stack).getUUID("id"));
        final int tick = player.level().getServer().getTickCount();
        if (entry.lastUpdate != tick) {
            entry.lastUpdate = tick;
            runtime.tick();
        }
    }

    public static boolean isCarried(final Player player, final ItemStack stack) {
        if (player == null || player.isRemoved() || stack.isEmpty()) return false;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (player.getInventory().getItem(slot) == stack) return true;
        }
        return player.containerMenu != null && player.containerMenu.getCarried() == stack;
    }

    /** Called before vanilla serializes inventory items, including autosaves. */
    public static void savePlayer(final Player player) {
        if (player.level().isClientSide) return;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            saveIfCached(player.getInventory().getItem(slot), player);
        }
    }

    private static void saveIfCached(ItemStack stack, Player player) {
        if (!(stack.getItem() instanceof TabletItem)) return;
        final var data = TabletItem.readData(stack);
        if (data.hasUUID("id") && runtimes.containsKey(data.getUUID("id"))) get(stack, player).save();
    }

    public static void beforeContainerClick(AbstractContainerMenu menu, Player player) {
        if (player.level().isClientSide) return;
        savePlayer(player);
        saveIfCached(menu.getCarried(), player);
    }

    public static void afterContainerClick(AbstractContainerMenu menu, Player player) {
        if (player.level().isClientSide) return;
        for (var entry : List.copyOf(runtimes.entrySet())) {
            final TabletRuntime runtime = entry.getValue().runtime;
            if (runtime.player() != player || isCarried(player, runtime.stack())) continue;
            final ItemStack target = findInMenu(menu, entry.getKey());
            if (target.isEmpty()) {
                runtime.close(false);
                runtimes.remove(entry.getKey());
            } else {
                final TabletRuntime moved = get(target, player);
                if (!isCarried(player, target)) {
                    moved.close(false);
                    runtimes.remove(entry.getKey());
                    for (var slot : menu.slots) if (slot.getItem() == target) slot.setChanged();
                }
            }
        }
        // A creative middle-click copy on the cursor gets its own identity now.
        final ItemStack carried = menu.getCarried();
        if (carried.getItem() instanceof TabletItem) {
            final var data = TabletItem.readData(carried);
            if (data.hasUUID("id") && runtimes.containsKey(data.getUUID("id"))) get(carried, player);
        }
    }

    private static ItemStack findInMenu(AbstractContainerMenu menu, UUID id) {
        if (hasId(menu.getCarried(), id)) return menu.getCarried();
        for (var slot : menu.slots) if (hasId(slot.getItem(), id)) return slot.getItem();
        return ItemStack.EMPTY;
    }

    private static boolean hasId(ItemStack stack, UUID id) {
        if (!(stack.getItem() instanceof TabletItem)) return false;
        final var data = TabletItem.readData(stack);
        return data.hasUUID("id") && data.getUUID("id").equals(id);
    }

    public static void closePlayer(final Player player) {
        if (player.level().isClientSide) return;
        for (var entry : List.copyOf(runtimes.entrySet())) {
            if (entry.getValue().runtime.player() == player) {
                entry.getValue().runtime.close(false);
                runtimes.remove(entry.getKey());
            }
        }
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        for (var entry : List.copyOf(runtimes.entrySet())) {
            if (event.getServer().getTickCount() - entry.getValue().lastSeen >= IDLE_TICKS) {
                entry.getValue().runtime.close(false);
                runtimes.remove(entry.getKey());
            }
        }
    }
    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) { closePlayer(event.getEntity()); }
    private static void onToss(ItemTossEvent event) {
        if (event.getPlayer().level().isClientSide) return;
        final ItemStack dropped = event.getEntity().getItem();
        if (!(dropped.getItem() instanceof TabletItem)) return;
        final var data = TabletItem.readData(dropped);
        if (!data.hasUUID("id")) return;
        final UUID id = data.getUUID("id");
        final Entry entry = runtimes.get(id);
        if (entry != null && entry.runtime.player() == event.getPlayer()
            && !isCarried(event.getPlayer(), entry.runtime.stack())) {
            // Inventory removal split the original stack. Save to the actual drop.
            get(dropped, event.getPlayer()).close(false);
            runtimes.remove(id);
        }
    }
    private static void onWorldUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) return;
        for (var entry : List.copyOf(runtimes.entrySet())) {
            if (entry.getValue().runtime.world() == event.getLevel()) {
                entry.getValue().runtime.close(false);
                runtimes.remove(entry.getKey());
            }
        }
    }
    private static void onServerStopped(ServerStoppedEvent event) {
        for (Entry entry : runtimes.values()) entry.runtime.close(false);
        runtimes.clear();
    }
}
