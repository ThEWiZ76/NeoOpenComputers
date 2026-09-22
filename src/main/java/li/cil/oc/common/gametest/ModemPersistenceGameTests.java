package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.item.MutableProcessor;
import li.cil.oc.common.ItemRegistry;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.blockentity.ComputerCaseBlockEntity;
import li.cil.oc.common.item.ItemDriverData;
import li.cil.oc.common.machine.NativeLuaArchitecture;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.nio.charset.StandardCharsets;

@GameTestHolder(NeoOpenComputers.MODID)
@PrefixGameTestTemplate(false)
public final class ModemPersistenceGameTests {
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void wiredModemRetainsAddressAndSettings(GameTestHelper helper) { restore(helper, 0); }
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void wirelessTier1RetainsAddressAndSettings(GameTestHelper helper) { restore(helper, 1); }
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void wirelessTier2RetainsAddressAndSettings(GameTestHelper helper) { restore(helper, 2); }

    private static void restore(GameTestHelper helper, int kind) {
        final var pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.COMPUTER_CASE_TIER3.get());
        final ComputerCaseBlockEntity initial = helper.getBlockEntity(pos);
        final var cpu = new ItemStack(ModItems.CPU_TIER1.get());
        ((MutableProcessor) Driver.driverFor(cpu)).setArchitecture(cpu, NativeLuaArchitecture.class);
        // Tier3 case layout: CPU8, memory3, EEPROM9 (the public constants describe tier1).
        initial.setItem(8, cpu);
        initial.setItem(3, new ItemStack(ModItems.MEMORY_TIER2.get()));
        initial.setItem(ComputerCaseBlockEntity.SLOT_CARD_0, new ItemStack(switch (kind) {
            case 0 -> ModItems.NETWORK_CARD.get();
            case 1 -> ModItems.WIRELESS_NETWORK_CARD_TIER1.get();
            default -> ModItems.WIRELESS_NETWORK_CARD_TIER2.get();
        }));
        initial.setItem(9, RobotMovementPersistenceGameTests.eeprom("""
            local eeprom = component.proxy(component.list('eeprom')())
            assert(eeprom.getData() == '', 'unexpected reboot')
            local modem = component.proxy(component.list('modem')())
            assert(modem.open(123))
            modem.setWakeMessage('resume-me', true)
            if modem.isWireless() then modem.setStrength(7) end
            eeprom.setData('waiting')
            repeat until computer.pullSignal() == 'continue_modem'
            assert(component.type(modem.address) == 'modem', 'modem address lost')
            assert(modem.isOpen(123), 'open port lost')
            local wake, fuzzy = modem.getWakeMessage()
            assert(wake == 'resume-me' and fuzzy == true, 'wake settings lost')
            if modem.isWireless() then assert(modem.getStrength() == 7, 'wireless strength lost') end
            eeprom.setData('restored')
            while true do computer.pullSignal() end
            """));
        helper.assertTrue(initial.toggleMachine(), "Modem test computer did not start");
        final ComputerCaseBlockEntity[] active = {initial};
        helper.startSequence()
            .thenWaitUntil(() -> helper.assertTrue(marker(initial).equals("waiting"), "Modem setup failed: " + initial.machine().lastError()))
            .thenExecute(() -> {
                final var modemAddress = initial.machine().components().entrySet().stream()
                    .filter(entry -> entry.getValue().equals("modem"))
                    .map(java.util.Map.Entry::getKey).findFirst().orElseThrow();
                final var saved = initial.saveWithFullMetadata(helper.getLevel().registryAccess());
                initial.setRemoved();
                active[0] = (ComputerCaseBlockEntity) BlockEntity.loadStatic(initial.getBlockPos(), initial.getBlockState(), saved, helper.getLevel().registryAccess());
                helper.getLevel().setBlockEntity(active[0]);
                active[0].onLoad();
                helper.assertTrue("modem".equals(active[0].machine().components().get(modemAddress)), "Modem component address changed on reload");
                helper.assertTrue(active[0].machine().signal("continue_modem"), "Restored modem program rejected signal");
            })
            .thenWaitUntil(() -> helper.assertTrue(marker(active[0]).equals("restored"), "Modem continuation failed: " + active[0].machine().lastError()))
            .thenSucceed();
    }

    private static String marker(ComputerCaseBlockEntity computer) {
        return new String(ItemDriverData.dataTag(computer.getItem(9)).getByteArray(ItemRegistry.EEPROM_DATA_SECTION_TAG), StandardCharsets.UTF_8);
    }
}
