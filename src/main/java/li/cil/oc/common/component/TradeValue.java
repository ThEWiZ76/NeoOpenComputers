package li.cil.oc.common.component;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.internal.Agent;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.prefab.AbstractValue;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;

public class TradeValue extends AbstractValue {
    private static final double TRADING_RANGE = 8.0D;

    private final EnvironmentHost host;
    private final Entity merchantEntity;
    private final Merchant merchant;
    private final int offerIndex;
    private final int merchantId;

    public TradeValue(
        final EnvironmentHost host,
        final Entity merchantEntity,
        final Merchant merchant,
        final int offerIndex,
        final int merchantId
    ) {
        this.host = host;
        this.merchantEntity = merchantEntity;
        this.merchant = merchant;
        this.offerIndex = offerIndex;
        this.merchantId = merchantId;
    }

    @Callback(doc = "function():number -- Returns a sort index of the merchant that provides this trade.")
    public Object[] getMerchantId(final Context context, final Arguments arguments) {
        return new Object[]{merchantId};
    }

    @Callback(doc = "function():table, table -- Returns the items the merchant wants for this trade.")
    public Object[] getInput(final Context context, final Arguments arguments) {
        final MerchantOffer offer = offer();
        if (offer == null) {
            return new Object[]{ItemStack.EMPTY, ItemStack.EMPTY};
        }
        final ItemStack costB = offer.getCostB();
        return new Object[]{offer.getCostA().copy(), costB.isEmpty() ? null : costB.copy()};
    }

    @Callback(doc = "function():table -- Returns the item the merchant offers for this trade.")
    public Object[] getOutput(final Context context, final Arguments arguments) {
        final MerchantOffer offer = offer();
        return new Object[]{offer == null ? ItemStack.EMPTY : offer.getResult().copy()};
    }

    @Callback(doc = "function():boolean -- Returns whether the merchant currently wants to trade this.")
    public Object[] isEnabled(final Context context, final Arguments arguments) {
        final MerchantOffer offer = offer();
        return new Object[]{merchantEntity.isAlive() && isInRange() && offer != null && !offer.isOutOfStock()};
    }

    @Callback(doc = "function():boolean, string -- Returns true when trade succeeds and nil, error when not.")
    public Object[] trade(final Context context, final Arguments arguments) {
        if (!(host instanceof Agent agent)) {
            return new Object[]{false, "trading requires an inventory upgrade to be installed"};
        }
        if (!merchantEntity.isAlive() || !isInRange()) {
            return new Object[]{false, "trade has become invalid"};
        }
        final MerchantOffer offer = offer();
        if (offer == null) {
            return new Object[]{false, "trade has become invalid"};
        }
        if (offer.isOutOfStock()) {
            return new Object[]{false, "trade is disabled"};
        }

        final Container inventory = agent.mainInventory();
        final ItemStack firstCost = offer.getCostA().copy();
        final ItemStack secondCost = offer.getCostB().copy();
        final ItemStack output = offer.getResult().copy();
        if (!extract(inventory, firstCost, true) || !extract(inventory, secondCost, true)) {
            return new Object[]{false, "not enough items to trade"};
        }
        if (!insert(inventory, output, true)) {
            return new Object[]{false, "not enough inventory space to trade"};
        }

        final ItemStack[] snapshot = snapshot(inventory);
        extract(inventory, firstCost, false);
        extract(inventory, secondCost, false);
        if (!insert(inventory, output, false)) {
            restore(inventory, snapshot);
            return new Object[]{false, "not enough inventory space to trade"};
        }
        merchant.notifyTrade(offer);
        return new Object[]{true};
    }

    private MerchantOffer offer() {
        if (offerIndex < 0 || offerIndex >= merchant.getOffers().size()) {
            return null;
        }
        return merchant.getOffers().get(offerIndex);
    }

    private boolean isInRange() {
        return host != null
            && merchantEntity.distanceToSqr(host.xPosition(), host.yPosition(), host.zPosition()) <= TRADING_RANGE * TRADING_RANGE;
    }

    private static boolean extract(final Container inventory, final ItemStack cost, final boolean simulate) {
        if (cost.isEmpty()) {
            return true;
        }
        int remaining = cost.getCount();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            final ItemStack stack = inventory.getItem(slot);
            if (!ItemStack.isSameItemSameComponents(stack, cost)) {
                continue;
            }
            final int extracted = Math.min(remaining, stack.getCount());
            remaining -= extracted;
            if (!simulate) {
                stack.shrink(extracted);
                if (stack.isEmpty()) {
                    inventory.setItem(slot, ItemStack.EMPTY);
                }
            }
            if (remaining <= 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean insert(final Container inventory, final ItemStack stack, final boolean simulate) {
        if (stack.isEmpty()) {
            return true;
        }
        int remaining = stack.getCount();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            final ItemStack existing = inventory.getItem(slot);
            if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, stack)) {
                final int limit = Math.min(existing.getMaxStackSize(), inventory.getMaxStackSize());
                final int inserted = Math.min(remaining, limit - existing.getCount());
                if (inserted > 0) {
                    remaining -= inserted;
                    if (!simulate) {
                        existing.grow(inserted);
                    }
                }
            }
            if (remaining <= 0) {
                return true;
            }
        }
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (!inventory.getItem(slot).isEmpty()) {
                continue;
            }
            final int inserted = Math.min(remaining, Math.min(stack.getMaxStackSize(), inventory.getMaxStackSize()));
            remaining -= inserted;
            if (!simulate) {
                final ItemStack insertedStack = stack.copy();
                insertedStack.setCount(inserted);
                inventory.setItem(slot, insertedStack);
            }
            if (remaining <= 0) {
                return true;
            }
        }
        return false;
    }

    private static ItemStack[] snapshot(final Container inventory) {
        final ItemStack[] snapshot = new ItemStack[inventory.getContainerSize()];
        for (int slot = 0; slot < snapshot.length; slot++) {
            snapshot[slot] = inventory.getItem(slot).copy();
        }
        return snapshot;
    }

    private static void restore(final Container inventory, final ItemStack[] snapshot) {
        for (int slot = 0; slot < snapshot.length; slot++) {
            inventory.setItem(slot, snapshot[slot]);
        }
    }
}
