package li.cil.oc.common.driver;

import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.InventoryProvider;
import li.cil.oc.common.component.DatabaseEnvironment;
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
        if (environment instanceof DatabaseEnvironment database) {
            return new DatabaseInventory(stack, database, environment);
        }
        return null;
    }

    private static final class DatabaseInventory implements Container {
        private final ItemStack container;
        private final DatabaseEnvironment database;
        private final ManagedEnvironment environment;

        private DatabaseInventory(final ItemStack container, final DatabaseEnvironment database, final ManagedEnvironment environment) {
            this.container = container;
            this.database = database;
            this.environment = environment;
        }

        @Override
        public int getContainerSize() {
            return database.size();
        }

        @Override
        public boolean isEmpty() {
            if (container.getCount() != 1) return true;
            for (int slot = 0; slot < getContainerSize(); slot++) {
                if (!database.getPhysicalStackInSlot(slot).isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public ItemStack getItem(final int slot) {
            return validSlot(slot) && container.getCount() == 1 ? database.getPhysicalStackInSlot(slot) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItem(final int slot, final int amount) {
            if (!validSlot(slot) || amount <= 0 || container.getCount() != 1) {
                return ItemStack.EMPTY;
            }
            final ItemStack stack = database.getPhysicalStackInSlot(slot);
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
            final ItemStack removed = stack.split(amount);
            database.setPhysicalStackInSlot(slot, stack);
            setChanged();
            return removed;
        }

        @Override
        public ItemStack removeItemNoUpdate(final int slot) {
            if (!validSlot(slot) || container.getCount() != 1) {
                return ItemStack.EMPTY;
            }
            final ItemStack stack = database.getPhysicalStackInSlot(slot);
            database.setPhysicalStackInSlot(slot, ItemStack.EMPTY);
            persist();
            return stack;
        }

        @Override
        public void setItem(final int slot, final ItemStack stack) {
            if (!validSlot(slot) || container.getCount() != 1) {
                return;
            }
            final ItemStack stored = stack == null ? ItemStack.EMPTY : stack.copy();
            if (!stored.isEmpty() && stored.getCount() > getMaxStackSize()) {
                stored.setCount(getMaxStackSize());
            }
            database.setPhysicalStackInSlot(slot, stored);
            setChanged();
        }

        @Override
        public void setChanged() {
            persist();
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public boolean canPlaceItem(final int slot, final ItemStack stack) {
            return container.getCount() == 1 && stack != container;
        }

        @Override
        public boolean stillValid(final Player player) {
            return true;
        }

        @Override
        public void clearContent() {
            if (container.getCount() != 1) return;
            for (int slot = 0; slot < getContainerSize(); slot++) {
                database.setPhysicalStackInSlot(slot, ItemStack.EMPTY);
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
