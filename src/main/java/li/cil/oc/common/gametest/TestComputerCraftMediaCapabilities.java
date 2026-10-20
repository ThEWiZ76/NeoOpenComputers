package li.cil.oc.common.gametest;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/** Tagged fixture only, installed exclusively by the optional GameTest profile. */
public final class TestComputerCraftMediaCapabilities {
    public static void register(RegisterCapabilitiesEvent event) { Loaded.register(event); }
    public static ItemStack resourceDisk() {
        final var stack = new ItemStack(Items.PAPER); final var tag = new CompoundTag(); tag.putBoolean("oc.test.ccResourceMedia", true);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag)); return stack;
    }
    private static final class Loaded {
        static void register(RegisterCapabilitiesEvent event) {
            final var media = new dan200.computercraft.api.media.IMedia() {
                public String getLabel(net.minecraft.core.HolderLookup.Provider registries, ItemStack stack) { return "CC ROM"; }
                public dan200.computercraft.api.filesystem.Mount createDataMount(ItemStack stack, net.minecraft.server.level.ServerLevel level) {
                    return dan200.computercraft.api.ComputerCraftAPI.createResourceMount(level.getServer(), "computercraft", "lua/rom");
                }
            };
            event.registerItem(dan200.computercraft.api.media.MediaCapability.get(), (stack, unused) -> {
                final var data = stack.get(DataComponents.CUSTOM_DATA);
                return data != null && data.copyTag().getBoolean("oc.test.ccResourceMedia") ? media : null;
            }, Items.PAPER);
        }
    }
}
