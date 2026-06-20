package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(NeoOpenComputers.MODID)
@PrefixGameTestTemplate(false)
public final class NeoOpenComputersGameTests {
    @GameTest(template = "empty")
    public static void registeredContentAvailable(final GameTestHelper helper) {
        ModBlocks.COMPUTER_CASE_TIER1.get();
        ModBlocks.DISK_DRIVE.get();
        ModBlocks.SCREEN_TIER1.get();
        ModBlocks.KEYBOARD.get();
        ModItems.CPU_TIER1.get();
        ModItems.EEPROM.get();
        ModItems.FLOPPY.get();
        ModItems.GRAPHICS_CARD_TIER1.get();
        ModItems.HDD_TIER1.get();
        ModItems.MEMORY_TIER1.get();
        ModItems.NETWORK_CARD.get();
        helper.succeed();
    }

    private NeoOpenComputersGameTests() {
    }
}
