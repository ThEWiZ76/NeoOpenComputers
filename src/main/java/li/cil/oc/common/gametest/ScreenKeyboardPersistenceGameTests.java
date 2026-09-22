package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.Driver;
import li.cil.oc.api.Network;
import li.cil.oc.api.internal.TextBuffer;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.blockentity.ScreenItemEnvironment;
import li.cil.oc.common.component.TerminalScreenSnapshot;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(NeoOpenComputers.MODID)
@PrefixGameTestTemplate(false)
public final class ScreenKeyboardPersistenceGameTests {
    @GameTest(template = "empty")
    public static void screenDriverPersistsAddressTextColorsAndViewportInItem(final GameTestHelper helper) {
        final ItemStack stack = new ItemStack(ModItems.SCREEN_TIER3.get());
        final var driver = Driver.driverFor(stack);
        final ScreenItemEnvironment screen = (ScreenItemEnvironment) driver.createEnvironment(stack, null);
        helper.assertTrue(screen.getWidth() == screen.getMaximumWidth(), "Fresh screen lost its default width");
        Network.joinNewNetwork(screen.node());
        final String address = screen.node().address();
        screen.setResolution(12, 4);
        screen.setViewport(8, 3);
        screen.setForegroundColor(0x33AAFF);
        screen.setBackgroundColor(0x112233);
        screen.set(0, 0, "saved é", false);
        screen.setPaletteColor(2, 0xAABBCC);
        screen.setForegroundColor(2, true);
        screen.setBackgroundColor(3, true);
        screen.set(0, 1, "palette", false);
        screen.setColorDepth(TextBuffer.ColorDepth.FourBit);
        screen.setPowerState(false);
        final TerminalScreenSnapshot snapshot = screen.terminalSnapshot();
        screen.save(new CompoundTag());
        final ItemStack saved = stack.copy();
        screen.node().remove();
        final ScreenItemEnvironment restored = (ScreenItemEnvironment) driver.createEnvironment(saved, null);
        Network.joinNewNetwork(restored.node());
        helper.assertTrue(address.equals(restored.node().address()), "Screen component address changed after driver recreation");
        helper.assertTrue(snapshot.contentEquals(restored.terminalSnapshot()), "Screen text, cell colors or viewport lost on driver recreation");
        helper.assertTrue(restored.getWidth() == 12 && restored.getHeight() == 4, "Backing resolution was not saved");
        helper.assertTrue(restored.getPaletteColor(2) == 0xAABBCC, "Palette was not saved");
        helper.assertTrue(restored.isForegroundFromPalette() && restored.isBackgroundFromPalette(), "Current palette modes lost");
        helper.assertTrue(restored.getColorDepth() == TextBuffer.ColorDepth.FourBit, "Current color depth lost");
        helper.assertFalse(restored.getPowerState(), "Screen power state lost");
        restored.node().remove();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void keyboardDriverPersistsAddressInItem(final GameTestHelper helper) {
        final ItemStack stack = new ItemStack(ModItems.KEYBOARD.get());
        final var driver = Driver.driverFor(stack);
        final var keyboard = driver.createEnvironment(stack, null);
        Network.joinNewNetwork(keyboard.node());
        final String address = keyboard.node().address();
        keyboard.save(new CompoundTag());
        final ItemStack saved = stack.copy();
        keyboard.node().remove();
        final var restored = driver.createEnvironment(saved, null);
        Network.joinNewNetwork(restored.node());
        helper.assertTrue(address.equals(restored.node().address()), "Keyboard component address changed after driver recreation");
        restored.node().remove();
        helper.succeed();
    }
}
