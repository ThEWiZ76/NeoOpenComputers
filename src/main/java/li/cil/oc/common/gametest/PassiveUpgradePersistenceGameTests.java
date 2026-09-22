package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.Driver;
import li.cil.oc.api.Network;
import li.cil.oc.common.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(NeoOpenComputers.MODID)
@PrefixGameTestTemplate(false)
public final class PassiveUpgradePersistenceGameTests {
    @GameTest(template = "empty")
    public static void angelAddressSurvivesItemSerialization(GameTestHelper helper) {
        verify(helper, ModItems.ANGEL_UPGRADE.get());
    }

    @GameTest(template = "empty")
    public static void barcodeAddressSurvivesItemSerialization(GameTestHelper helper) {
        verify(helper, ModItems.BARCODE_READER_UPGRADE.get());
    }

    private static void verify(GameTestHelper helper, Item item) {
        final var stack = new ItemStack(item);
        final var driver = Driver.driverFor(stack);
        final var original = driver.createEnvironment(stack, null);
        Network.joinNewNetwork(original.node());
        final String address = original.node().address();
        helper.assertTrue(address != null && !address.isEmpty(), "Upgrade has no network address");
        original.save(new CompoundTag());
        original.node().remove();
        final var registries = helper.getLevel().registryAccess();
        final var restoredStack = ItemStack.parseOptional(registries, (CompoundTag) stack.save(registries));
        final var restored = driver.createEnvironment(restoredStack, null);
        try {
            Network.joinNewNetwork(restored.node());
            helper.assertTrue(address.equals(restored.node().address()), "Serialized upgrade lost its network address");
        } finally {
            restored.node().remove();
        }
        helper.succeed();
    }
}
