package li.cil.oc.common.gametest;

import li.cil.oc.api.Driver;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.item.MutableProcessor;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.blockentity.ComputerCaseBlockEntity;
import li.cil.oc.common.item.ItemDriverData;
import li.cil.oc.common.machine.NativeLua54Architecture;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("neoopencomputers")
@PrefixGameTestTemplate(false)
public final class ItemSaveContractGameTests {
    private static ComputerCaseBlockEntity host(final GameTestHelper helper) {
        final var pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.COMPUTER_CASE_TIER3.get());
        return helper.getBlockEntity(pos);
    }

    @GameTest(template = "empty")
    public static void cpuFreshSaveKeepsArchitecture(final GameTestHelper helper) {
        freshProcessorSave(helper, new ItemStack(ModItems.CPU_TIER1.get()));
    }

    @GameTest(template = "empty")
    public static void apuFreshSaveKeepsArchitecture(final GameTestHelper helper) {
        freshProcessorSave(helper, new ItemStack(ModItems.APU_TIER1.get()));
    }

    private static void freshProcessorSave(final GameTestHelper helper, final ItemStack stack) {
        final var host = host(helper);
        final var driver = (MutableProcessor) Driver.driverFor(stack, host.getClass());
        driver.setArchitecture(stack, NativeLua54Architecture.class);
        final var environment = driver.createEnvironment(stack, host);
        try {
            environment.load(ItemDriverData.driverDataTag(driver, stack).copy());
            final var saved = new CompoundTag();
            environment.save(saved);
            ItemDriverData.writeDriverDataTag(driver, stack, saved);
            helper.assertTrue(driver.architecture(stack) == NativeLua54Architecture.class,
                "Fresh component save lost selected processor architecture");
        } finally {
            if (environment.node() != null) environment.node().remove();
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void apuGpuDisconnectKeepsArchitecture(final GameTestHelper helper) {
        final var host = host(helper);
        final var stack = new ItemStack(ModItems.APU_TIER1.get());
        final var driver = (MutableProcessor) Driver.driverFor(stack, host.getClass());
        driver.setArchitecture(stack, NativeLua54Architecture.class);
        final var environment = driver.createEnvironment(stack, host);
        Network.joinNewNetwork(environment.node());
        environment.node().remove();
        helper.assertTrue(driver.architecture(stack) == NativeLua54Architecture.class,
            "GPU disconnect replaced the selected APU architecture");
        helper.succeed();
    }
}
