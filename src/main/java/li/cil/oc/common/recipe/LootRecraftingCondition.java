package li.cil.oc.common.recipe;

import com.mojang.serialization.MapCodec;
import li.cil.oc.common.ModSettings;
import net.neoforged.neoforge.common.conditions.ICondition;

public record LootRecraftingCondition() implements ICondition {
    public static final MapCodec<LootRecraftingCondition> CODEC = MapCodec.unit(new LootRecraftingCondition());

    @Override public boolean test(IContext context) { return ModSettings.lootRecrafting(); }

    @Override public MapCodec<? extends ICondition> codec() { return CODEC; }
}
