package li.cil.oc.common.integration.ae2;

import appeng.api.stacks.AEItemKey;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.prefab.AbstractValue;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class CraftableValue extends AbstractValue {
    private CraftingTarget target;
    private long outputCount = 1;

    public CraftableValue() { }
    public CraftableValue(final BlockEntity tile, final Direction side, final AEItemKey item, final long outputCount) {
        target = new CraftingTarget(tile, side, item);
        this.outputCount = outputCount;
    }

    @Callback(doc = "function():table -- Get the recipe output item description.")
    public Object[] getItemStack(final Context context, final Arguments args) {
        final var description = new java.util.LinkedHashMap<Object, Object>((java.util.Map<?, ?>)
            li.cil.oc.common.OpenComputersApi.convert(new Object[]{target.item().toStack(1)})[0]);
        description.put("size", outputCount);
        return new Object[]{description};
    }

    @Callback(doc = "function():number -- Get the quantity currently requested by network crafting jobs.")
    public Object[] requesting(final Context context, final Arguments args) {
        try { return new Object[]{target.node().getGrid().getCraftingService().getRequestedAmount(target.item())}; }
        catch (IllegalStateException exception) { return new Object[]{null, exception.getMessage()}; }
    }

    @Callback(doc = "function([amount:number=1, prioritizePower:boolean=true, cpuName:string]):userdata -- Start an asynchronous crafting request.")
    public Object[] request(final Context context, final Arguments args) {
        final int amount = args.optInteger(0, 1);
        if (amount <= 0) throw new IllegalArgumentException("amount must be positive");
        final boolean prioritizePower = args.optBoolean(1, true);
        final String cpuName = args.optString(2, "");
        try {
            target.node();
            return new Object[]{new CraftingStatusValue(target.copy(), amount, prioritizePower, cpuName)};
        } catch (IllegalStateException exception) { return new Object[]{null, exception.getMessage()}; }
    }

    @Override public void save(final CompoundTag tag) {
        tag.put("target", target.save());
        tag.putLong("outputCount", outputCount);
    }

    @Override public void load(final CompoundTag tag) {
        target = new CraftingTarget(tag.getCompound("target"));
        outputCount = tag.getLong("outputCount");
    }
}
