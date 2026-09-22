package li.cil.oc.common.menu;

import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.item.Container;
import li.cil.oc.common.ModMenus;
import li.cil.oc.common.driver.ScreenItemDriver;
import li.cil.oc.common.item.TabletItem;
import li.cil.oc.common.item.TabletRuntime;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Upstream tablet editor: only the final expansion slot is replaceable. */
public final class TabletMenu extends DiskDriveMenu {
    private final TabletRuntime runtime;
    private final int lockedInventorySlot;

    public TabletMenu(int id, Inventory inventory, RegistryFriendlyByteBuf data) {
        this(id, inventory, data.readVarInt(), null, clientInventory(data.readUtf(), data.readVarInt()));
    }

    public TabletMenu(int id, Inventory inventory, TabletRuntime runtime) {
        this(id, inventory, inventorySlot(inventory, runtime.stack()), runtime, runtime.expansionInventory());
    }

    private TabletMenu(int id, Inventory inventory, int lockedSlot, TabletRuntime runtime, net.minecraft.world.Container contents) {
        super(ModMenus.TABLET.get(), id, inventory, contents);
        this.runtime = runtime;
        lockedInventorySlot = lockedSlot;
    }

    public static int inventorySlot(Inventory inventory, ItemStack stack) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) if (inventory.getItem(slot) == stack) return slot;
        throw new IllegalArgumentException("Tablet is not in this inventory");
    }

    public static String expansionType(ItemStack tablet) {
        final ItemStack container = ((TabletItem) tablet.getItem()).getContainer(tablet);
        final var driver = Driver.driverFor(container, TabletRuntime.class);
        return driver instanceof Container upgrade ? upgrade.providedSlot(container) : li.cil.oc.api.driver.item.Slot.None;
    }

    public static int expansionTier(ItemStack tablet) {
        final ItemStack container = ((TabletItem) tablet.getItem()).getContainer(tablet);
        final var driver = Driver.driverFor(container, TabletRuntime.class);
        return driver instanceof Container upgrade ? upgrade.providedTier(container) : -1;
    }

    public static boolean acceptsExpansion(ItemStack tablet, ItemStack candidate) {
        return accepts(expansionType(tablet), expansionTier(tablet), candidate);
    }

    private static boolean accepts(String type, int tier, ItemStack candidate) {
        final var driver = Driver.driverFor(candidate, TabletRuntime.class);
        return tier >= 0 && driver != null && !(driver instanceof ScreenItemDriver)
            && type.equals(driver.slot(candidate)) && driver.tier(candidate) <= tier;
    }

    private static SimpleContainer clientInventory(String type, int tier) {
        return new SimpleContainer(1) {
            @Override public int getMaxStackSize() { return 1; }
            @Override public boolean canPlaceItem(int slot, ItemStack stack) { return accepts(type, tier, stack); }
        };
    }

    @Override protected Slot playerSlot(Inventory inventory, int inventoryIndex, int x, int y) {
        return new Slot(inventory, inventoryIndex, x, y) {
            @Override public boolean mayPickup(Player player) { return inventoryIndex != lockedInventorySlot; }
            @Override public boolean mayPlace(ItemStack stack) { return inventoryIndex != lockedInventorySlot; }
        };
    }

    @Override public boolean stillValid(Player player) {
        return runtime == null ? player.level().isClientSide : player == runtime.player() && player.level() == runtime.world()
            && !runtime.isClosed() && !runtime.machine().isRunning() && runtime.machine().canInteract(player.getGameProfile().getName())
            && player.getInventory().getItem(lockedInventorySlot) == runtime.stack();
    }

    @Override public void clicked(int slot, int button, ClickType type, Player player) {
        if (!stillValid(player) || type == ClickType.SWAP && button == lockedInventorySlot) return;
        if (slot >= 0 && slot < slots.size() && slots.get(slot).container == player.getInventory()
            && slots.get(slot).getContainerSlot() == lockedInventorySlot) return;
        super.clicked(slot, button, type, player);
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || index < 0 || index >= slots.size() || !slots.get(index).mayPickup(player)) return ItemStack.EMPTY;
        return super.quickMoveStack(player, index);
    }
}
