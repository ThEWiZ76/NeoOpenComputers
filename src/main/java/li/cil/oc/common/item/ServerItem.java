package li.cil.oc.common.item;

import li.cil.oc.api.internal.Tiered;
import net.minecraft.world.item.Item;

public class ServerItem extends Item implements Tiered {
    private final int tier;

    public ServerItem(final Properties properties, final int tier) {
        super(properties.stacksTo(1));
        this.tier = Math.max(0, Math.min(2, tier));
    }

    @Override
    public int tier() {
        return tier;
    }
}
