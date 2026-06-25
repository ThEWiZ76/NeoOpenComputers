package li.cil.oc.common.driver;

import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.InventoryProvider;
import li.cil.oc.api.internal.Database;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.item.DatabaseUpgradeItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class DatabaseInventoryProvider implements InventoryProvider {
    @Override
    public boolean worksWith(final ItemStack stack, final Player player) {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof DatabaseUpgradeItem;
    }

    @Override
    public Container getInventory(final ItemStack stack, final Player player) {
        if (!worksWith(stack, player)) {
            return null;
        }
        final DriverItem driver = Driver.driverFor(stack);
        if (driver == null) {
            return null;
        }
        final ManagedEnvironment environment = driver.createEnvironment(stack, null);
        if (environment instanceof Database database) {
            return new DatabaseInventory(database, environment);
        }
        return null;
    }

    private static final class DatabaseInventory implements Container {
        private final Database database;
        private final ManagedEnvironment environment;

        private DatabaseInventory(final Database database, final ManagedEnvironment environment) {
            this.database = database;
            this.environment = environment;
        }

        @Override
        public int getContainerSize() {
            return database.size();
        }

        @Override
        public boolean isEmpty() {
            for (int slot = 0; slot < getContainerSize(); slot++) {
                if (!database.getStackInSlot(slot).isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public ItemStack getItem(final int slot) {
            return validSlot(slot) ? database.getStackInSlot(slot) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItem(final int slot, final int amount) {
            if (!validSlot(slot) || amount <= 0) {
                return ItemStack.EMPTY;
            }
            final ItemStack stack = database.getStackInSlot(slot);
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
            final ItemStack removed = stack.split(amount);
            database.setStackInSlot(slot, stack);
            setChanged();
            return removed;
        }

        @Override
        public ItemStack removeItemNoUpdate(final int slot) {
            if (!validSlot(slot)) {
                return ItemStack.EMPTY;
            }
            final ItemStack stack = database.getStackInSlot(slot);
            database.setStackInSlot(slot, ItemStack.EMPTY);
            persist();
            return stack;
        }

        @Override
        public void setItem(final int slot, final ItemStack stack) {
            if (!validSlot(slot)) {
                return;
            }
            final ItemStack stored = stack == null ? ItemStack.EMPTY : stack.copy();
            if (!stored.isEmpty() && stored.getCount() > getMaxStackSize()) {
                stored.setCount(getMaxStackSize());
            }
            database.setStackInSlot(slot, stored);
            setChanged();
        }

        @Override
        public void setChanged() {
            persist();
        }

        @Override
        public boolean stillValid(final Player player) {
            return true;
        }

        @Override
        public void clearContent() {
            for (int slot = 0; slot < getContainerSize(); slot++) {
                database.setStackInSlot(slot, ItemStack.EMPTY);
            }
            setChanged();
        }

        private boolean validSlot(final int slot) {
            return slot >= 0 && slot < database.size();
        }

        private void persist() {
            final CompoundTag tag = new CompoundTag();
            environment.save(tag);
        }
    }
}
