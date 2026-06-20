package li.cil.oc.common.component;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.prefab.AbstractValue;
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
}
