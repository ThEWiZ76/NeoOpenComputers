package li.cil.oc.common.component;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.internal.Agent;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.prefab.AbstractValue;
import li.cil.oc.common.ModSettings;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.phys.AABB;

import java.util.UUID;
import java.util.function.Predicate;

public class TradeValue extends AbstractValue {
    private static final String MERCHANT_UUID_MOST_TAG = "merchantUUIDMost";
    private static final String MERCHANT_UUID_LEAST_TAG = "merchantUUIDLeast";
    private static final String OFFER_INDEX_TAG = "offerIndex";
    private static final String MERCHANT_ID_TAG = "merchantId";

    private EnvironmentHost host;
    private Entity merchantEntity;
    private Merchant merchant;
    private int offerIndex;
    private int merchantId;

    public TradeValue() {
        this(null);
    }

    public TradeValue(final EnvironmentHost host) {
        this(host, null, null, -1, -1);
    }

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

    @Override
    public void load(final CompoundTag nbt) {
        offerIndex = nbt.getInt(OFFER_INDEX_TAG);
        merchantId = nbt.getInt(MERCHANT_ID_TAG);
        if (nbt.contains(MERCHANT_UUID_MOST_TAG) && nbt.contains(MERCHANT_UUID_LEAST_TAG)) {
            bindMerchant(new UUID(nbt.getLong(MERCHANT_UUID_MOST_TAG), nbt.getLong(MERCHANT_UUID_LEAST_TAG)));
        }
    }

    @Override
    public void save(final CompoundTag nbt) {
        if (merchantEntity != null) {
            final UUID uuid = merchantEntity.getUUID();
            nbt.putLong(MERCHANT_UUID_MOST_TAG, uuid.getMostSignificantBits());
            nbt.putLong(MERCHANT_UUID_LEAST_TAG, uuid.getLeastSignificantBits());
        }
        nbt.putInt(OFFER_INDEX_TAG, offerIndex);
        nbt.putInt(MERCHANT_ID_TAG, merchantId);
    }

    @Callback(doc = "function():number -- Returns a sort index of the merchant that provides this trade.")
    public Object[] getMerchantId(final Context context, final Arguments arguments) {
        return new Object[]{merchantId};
    }

    @Callback(doc = "function():table, table -- Returns the items the merchant wants for this trade.")
    public Object[] getInput(final Context context, final Arguments arguments) {
        final MerchantOffer offer = offer();
        if (offer == null) {
            return new Object[]{null, null};
        }
        final ItemStack costB = offer.getCostB();
        return new Object[]{offer.getCostA().copy(), costB.isEmpty() ? null : costB.copy()};
    }

    @Callback(doc = "function():table -- Returns the item the merchant offers for this trade.")
    public Object[] getOutput(final Context context, final Arguments arguments) {
        final MerchantOffer offer = offer();
        return new Object[]{offer == null ? null : offer.getResult().copy()};
    }

    @Callback(doc = "function():boolean -- Returns whether the merchant currently wants to trade this.")
    public Object[] isEnabled(final Context context, final Arguments arguments) {
        final MerchantOffer offer = offer();
        return new Object[]{merchantEntity != null && offer != null && !offer.isOutOfStock()};
    }

    @Callback(doc = "function():boolean, string -- Returns true when trade succeeds and nil, error when not.")
    public Object[] trade(final Context context, final Arguments arguments) {
        if (!(host instanceof Agent agent)) {
            return new Object[]{false, "trading requires an inventory upgrade to be installed"};
        }
        if (merchantEntity == null || merchant == null || !merchantEntity.isAlive() || !isInRange()) {
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
        final ItemCost firstCost = offer.getItemCostA();
        final int firstCostCount = offer.getCostA().getCount();
        final ItemCost secondCost = offer.getItemCostB().orElse(null);
        final int secondCostCount = offer.getCostB().getCount();
        final ItemStack output = offer.getResult().copy();

        final ItemStack[] snapshot = snapshot(inventory);
        if (!extract(inventory, firstCost::test, firstCostCount, false)
            || !extract(inventory, stack -> secondCost == null || secondCost.test(stack), secondCost == null ? 0 : secondCostCount, false)) {
            restore(inventory, snapshot);
            return new Object[]{false, "not enough items to trade"};
        }
        if (!insert(inventory, output, false)) {
            restore(inventory, snapshot);
            return new Object[]{false, "not enough inventory space to trade"};
        }
        merchant.notifyTrade(offer);
        return new Object[]{true};
    }

    private MerchantOffer offer() {
        if (merchant == null) {
            return null;
        }
        if (offerIndex < 0 || offerIndex >= merchant.getOffers().size()) {
            return null;
        }
        return merchant.getOffers().get(offerIndex);
    }

    private boolean isInRange() {
        final double range = ModSettings.tradingRange();
        return host != null
            && merchantEntity != null
            && merchantEntity.distanceToSqr(host.xPosition(), host.yPosition(), host.zPosition()) <= range * range;
    }

    private void bindMerchant(final UUID uuid) {
        if (host == null || host.world() == null || uuid == null) {
            merchantEntity = null;
            merchant = null;
            return;
        }
        Entity entity = null;
        if (host.world() instanceof ServerLevel serverLevel) {
            entity = serverLevel.getEntity(uuid);
        }
        if (entity == null) {
            final double range = ModSettings.tradingRange();
            final AABB bounds = AABB.ofSize(
                new net.minecraft.world.phys.Vec3(host.xPosition(), host.yPosition(), host.zPosition()),
                range * 2D,
                range * 2D,
                range * 2D);
            entity = host.world().getEntitiesOfClass(Entity.class, bounds, candidate -> uuid.equals(candidate.getUUID()))
                .stream()
                .findFirst()
                .orElse(null);
        }
        if (entity instanceof Merchant reboundMerchant) {
            merchantEntity = entity;
            merchant = reboundMerchant;
        } else {
            merchantEntity = null;
            merchant = null;
        }
    }

    private static boolean extract(final Container inventory, final Predicate<ItemStack> matches, final int count, final boolean simulate) {
        if (count <= 0) {
            return true;
        }
        int remaining = count;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            final ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty() || !matches.test(stack)) {
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
