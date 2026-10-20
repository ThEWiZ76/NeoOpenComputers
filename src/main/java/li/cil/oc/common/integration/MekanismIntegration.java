package li.cil.oc.common.integration;

import li.cil.oc.common.DriverRegistry;
import li.cil.oc.common.ModSettings;
import li.cil.oc.common.WrenchTools;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/** Optional chemical descriptions and state-aware wrench interoperability. */
public final class MekanismIntegration {
    private MekanismIntegration() { }

    private static boolean loaded() { return ModList.get() != null && ModList.get().isLoaded("mekanism"); }

    public static void register(final DriverRegistry registry) {
        if (!loaded()) return;
        Loaded.register(registry);
        try {
            WrenchTools.addCheck(MekanismIntegration.class.getMethod("isWrench", ItemStack.class));
            WrenchTools.addUsage(MekanismIntegration.class.getMethod("useWrench", Player.class, BlockPos.class, boolean.class));
        } catch (NoSuchMethodException exception) {
            throw new IllegalStateException("Missing Mekanism wrench callbacks", exception);
        }
    }

    public static boolean isWrench(final ItemStack stack) {
        return loaded() && stack != null && !stack.isEmpty() && Loaded.isWrench(stack);
    }

    public static boolean useWrench(final Player player, final BlockPos pos, final boolean changeDurability) {
        return loaded() && player != null && pos != null && Loaded.canUse(player.getMainHandItem());
    }

    private static final class Loaded {
        private static final java.util.List<net.neoforged.neoforge.common.ItemAbility> WRENCH_MODES = java.util.List.of(
            mekanism.api.MekanismItemAbilities.WRENCH_CONFIGURE, mekanism.api.MekanismItemAbilities.WRENCH_EMPTY,
            mekanism.api.MekanismItemAbilities.WRENCH_ROTATE, mekanism.api.MekanismItemAbilities.WRENCH_DISMANTLE);

        static boolean isWrench(final ItemStack stack) { return WRENCH_MODES.stream().anyMatch(stack::canPerformAction); }
        static boolean canUse(final ItemStack stack) { return stack.canPerformAction(mekanism.api.MekanismItemAbilities.WRENCH_DISMANTLE); }

        static void register(final DriverRegistry registry) {
            registry.add((li.cil.oc.api.driver.Converter) (value, output) -> {
                if (!(value instanceof mekanism.api.chemical.ChemicalStack stack)) return;
                output.put("amount", stack.getAmount());
                output.put("name", stack.getChemicalHolder().getRegisteredName());
                output.put("label", stack.getTextComponent().getString());
                if (ModSettings.insertIdsInConverters()) output.put("id", mekanism.api.MekanismAPI.CHEMICAL_REGISTRY.getId(stack.getChemical()));
            });
        }
    }
}
