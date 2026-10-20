package li.cil.oc.common.gametest;

import li.cil.oc.api.API;
import li.cil.oc.api.fs.FileSystem;
import li.cil.oc.common.ItemRegistry;
import li.cil.oc.common.ModContentCatalog;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.concurrent.Callable;

@GameTestHolder("neoopencomputers")
@PrefixGameTestTemplate(false)
public final class AddonFloppyIdentityGameTests {
    @GameTest(template = "empty")
    public static void equalDiskNamesRemainIsolatedByRegisteringMod(GameTestHelper helper) {
        final var previousApi = API.items;
        final var context = ModLoadingContext.get();
        final var previousContainer = context.getActiveContainer();
        try {
            final var registry = new ItemRegistry(); API.items = registry; ModContentCatalog.registerDefaults();
            final Callable<FileSystem> firstFactory = () -> null;
            final Callable<FileSystem> secondFactory = () -> null;
            context.setActiveContainer(ModList.get().getModContainerById("neoopencomputers").orElseThrow());
            final var first = li.cil.oc.api.Items.registerFloppy("same fixture", DyeColor.BLUE, firstFactory, true);
            context.setActiveContainer(ModList.get().getModContainerById("neoforge").orElseThrow());
            final var second = li.cil.oc.api.Items.registerFloppy("same fixture", DyeColor.RED, secondFactory, false);
            helper.assertTrue(identity(first).equals("neoopencomputers:same fixture") && identity(second).equals("neoforge:same fixture"), "Addon disk identity ignored registering mod namespace");
            final var restoredFirst = ItemStack.parseOptional(helper.getLevel().registryAccess(), (net.minecraft.nbt.CompoundTag) first.save(helper.getLevel().registryAccess()));
            final var restoredSecond = ItemStack.parseOptional(helper.getLevel().registryAccess(), (net.minecraft.nbt.CompoundTag) second.save(helper.getLevel().registryAccess()));
            helper.assertTrue(registry.floppyFactory(restoredFirst) == firstFactory && registry.floppyFactory(restoredSecond) == secondFactory, "Serialized same-name addon disks resolved another mod's factory");
            helper.assertTrue(registry.disksForCycling().size() == 1 && identity(registry.disksForCycling().getFirst()).equals(identity(first)), "Stable addon identity changed cycling opt-out");
        } finally { context.setActiveContainer(previousContainer); API.items = previousApi; }
        helper.succeed();
    }

    private static String identity(ItemStack stack) { return stack.get(DataComponents.CUSTOM_DATA).copyTag().getString(ItemRegistry.FLOPPY_FACTORY_ID_TAG); }
}
