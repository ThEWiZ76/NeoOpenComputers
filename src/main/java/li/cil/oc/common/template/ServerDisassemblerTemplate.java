package li.cil.oc.common.template;

import com.mojang.serialization.DynamicOps;
import li.cil.oc.common.component.ServerRackMountableEnvironment;
import li.cil.oc.common.item.ServerItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.Arrays;

final class ServerDisassemblerTemplate implements DisassemblerTemplate {
    @Override public String name() { return "server"; }
    @Override public boolean matches(ItemStack stack) { return stack.getItem() instanceof ServerItem; }

    @Override public ItemStack[] disassemble(ItemStack stack) {
        final var contents = contents(stack, null);
        return contents == null ? new ItemStack[0] : contents;
    }

    @Override public DisassemblyResult plan(ItemStack stack, ItemStack[] ingredients) {
        return plan(stack, ingredients, null);
    }

    @Override public DisassemblyResult plan(ItemStack stack, ItemStack[] ingredients, HolderLookup.Provider registries) {
        final var contents = contents(stack, registries);
        return contents == null ? DisassemblyResult.EMPTY : new DisassemblyResult(ingredients, contents);
    }

    private static ItemStack[] contents(ItemStack stack, HolderLookup.Provider registries) {
        // Legacy migration is read-only here: readiness must not rewrite the caller's server item.
        final var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (data.contains("oc:rackMountable", Tag.TAG_COMPOUND)) data.merge(data.getCompound("oc:rackMountable").copy());
        final var slots = new ItemStack[ServerRackMountableEnvironment.slotCountForTier(((ServerItem) stack.getItem()).tier())];
        Arrays.fill(slots, ItemStack.EMPTY);
        final DynamicOps<Tag> ops = registries == null ? NbtOps.INSTANCE : registries.createSerializationContext(NbtOps.INSTANCE);
        final var entries = data.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            final CompoundTag entry = entries.getCompound(i);
            final int slot = entry.getByte("Slot") & 255;
            if (slot >= slots.length) continue;
            final var parsed = ItemStack.OPTIONAL_CODEC.parse(ops, entry).result();
            // Keep the assembled input intact if installed hardware cannot be recovered.
            if (parsed.isEmpty()) return null;
            slots[slot] = parsed.get();
        }
        return Arrays.stream(slots).filter(part -> !part.isEmpty()).toArray(ItemStack[]::new);
    }
}
