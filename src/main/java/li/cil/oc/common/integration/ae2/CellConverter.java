package li.cil.oc.common.integration.ae2;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.StorageCells;
import appeng.api.storage.cells.StorageCell;
import appeng.me.cells.BasicCellInventory;
import appeng.me.storage.DriveWatcher;
import li.cil.oc.api.driver.Converter;
import li.cil.oc.common.OpenComputersApi;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/** Registered only with AE2 loaded. Metrics are supplied only by cells that actually expose them. */
public final class CellConverter implements Converter {
    @Override
    public void convert(final Object value, final Map<Object, Object> output) {
        final StorageCell cell;
        if (value instanceof StorageCell inventory) cell = inventory;
        else if (value instanceof DriveWatcher watcher) cell = watcher.getCell();
        else if (value instanceof ItemStack stack) cell = StorageCells.getCellInventory(stack.copy(), null);
        else return;
        if (cell == null) return;

        output.put("name", cell.getDescription().getString());
        final var contents = new ArrayList<Map<Object, Object>>();
        for (final var entry : cell.getAvailableStacks()) {
            final var key = entry.getKey();
            final Object stack = key instanceof AEItemKey item ? item.toStack(1) : key instanceof AEFluidKey fluid ? fluid.toStack(1) : null;
            if (stack == null || entry.getLongValue() <= 0) continue;
            final var description = new LinkedHashMap<Object, Object>((Map<?, ?>) OpenComputersApi.convert(new Object[]{stack})[0]);
            description.put(key instanceof AEItemKey ? "size" : "amount", entry.getLongValue());
            contents.add(description);
        }
        output.put("getAvailableItems", contents.toArray());
        if (cell instanceof BasicCellInventory basic) {
            output.put("storedItemTypes", basic.getStoredItemTypes());
            output.put("storedItemCount", basic.getStoredItemCount());
            output.put("remainingItemCount", basic.getRemainingItemCount());
            output.put("remainingItemTypes", basic.getRemainingItemTypes());
            output.put("getTotalItemTypes", basic.getTotalItemTypes());
            output.put("totalBytes", basic.getTotalBytes());
            output.put("freeBytes", basic.getFreeBytes());
            output.put("usedBytes", basic.getUsedBytes());
            output.put("unusedItemCount", basic.getUnusedItemCount());
            output.put("canHoldNewItem", basic.canHoldNewItem());
            output.put("fuzzyMode", basic.getFuzzyMode().toString());
        }
    }
}
