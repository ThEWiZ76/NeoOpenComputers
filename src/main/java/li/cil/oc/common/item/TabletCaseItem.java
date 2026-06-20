package li.cil.oc.common.item;

import li.cil.oc.api.internal.Tiered;
import net.minecraft.world.item.Item;

public class TabletCaseItem extends Item implements Tiered {
    private final int tier;

    public TabletCaseItem(final Properties properties, final int tier) {
        super(properties);
        this.tier = tier;
    }

    @Override
    public int tier() {
        return tier;
    }
}
