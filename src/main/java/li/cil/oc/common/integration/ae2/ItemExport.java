package li.cil.oc.common.integration.ae2;

import appeng.api.config.Actionable;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.MEStorage;
import appeng.api.storage.StorageHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.function.Consumer;

/** Extract before committing destination insertion; return rejected items to ME or the world. */
public final class ItemExport {
    private ItemExport() { }

    public static int transfer(final IEnergySource energy, final MEStorage storage, final AEItemKey key,
                               final IActionSource source, final IItemHandler target, final int slot,
                               final int requested, final Consumer<ItemStack> drop) {
        if (requested <= 0) return 0;
        final int accepted = requested - insert(target, slot, key.toStack(requested), true).getCount();
        if (accepted <= 0) return 0;
        final int extracted = (int) StorageHelper.poweredExtraction(energy, storage, key, accepted, source, Actionable.MODULATE);
        if (extracted <= 0) return 0;
        final var remainder = insert(target, slot, key.toStack(extracted), false);
        if (!remainder.isEmpty()) {
            final long refunded = storage.insert(key, remainder.getCount(), Actionable.MODULATE, source);
            final int stranded = remainder.getCount() - (int) refunded;
            if (stranded > 0) drop.accept(key.toStack(stranded));
        }
        return extracted - remainder.getCount();
    }

    private static ItemStack insert(final IItemHandler target, final int slot, final ItemStack stack, final boolean simulate) {
        return slot < 0 ? ItemHandlerHelper.insertItemStacked(target, stack, simulate) : target.insertItem(slot, stack, simulate);
    }
}
