package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.item.MutableProcessor;
import li.cil.oc.common.ItemRegistry;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.blockentity.ComputerCaseBlockEntity;
import li.cil.oc.common.item.ItemDriverData;
import li.cil.oc.common.item.LinkedCardItem;
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
public final class CardPersistenceGameTests {
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void dataTier1ProxySurvivesReload(GameTestHelper helper) { data(helper, 0); }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void dataTier2ProxySurvivesReload(GameTestHelper helper) { data(helper, 1); }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void dataTier3ProxySurvivesReload(GameTestHelper helper) { data(helper, 2); }

    private static void data(GameTestHelper helper, int tier) {
        final var card = new ItemStack(switch (tier) {
            case 0 -> ModItems.DATA_CARD_TIER1.get();
            case 1 -> ModItems.DATA_CARD_TIER2.get();
            default -> ModItems.DATA_CARD_TIER3.get();
        });
        restore(helper, card, """
            local card = component.proxy(component.list('data')())
            local encoded = card.encode64('port-test')
            assert(encoded == 'cG9ydC10ZXN0')
            local encryptAvailable = card.encrypt ~= nil
            """, """
            assert(card.decode64(encoded) == 'port-test', 'old data proxy failed')
            assert((card.encrypt ~= nil) == encryptAvailable, 'tier callbacks changed')
            assert(card.encode64('port-test') == encoded, 'data callback changed')
            """);
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void linkedProxyRetainsChannelAndWakeSettings(GameTestHelper helper) {
        final var card = new ItemStack(ModItems.LINKED_CARD.get());
        ItemDriverData.dataTag(card).putString(LinkedCardItem.TUNNEL_TAG, "persistence-probe");
        restore(helper, card, """
            local card = component.proxy(component.list('tunnel')())
            assert(card.getChannel() == 'persistence-probe')
            card.setWakeMessage('resume-linked', true)
            """, """
            assert(card.getChannel() == 'persistence-probe', 'linked channel changed')
            local wake, fuzzy = card.getWakeMessage()
            assert(wake == 'resume-linked' and fuzzy == true, 'linked wake settings lost')
            """);
    }

    private static void restore(GameTestHelper helper, ItemStack card, String setup, String verify) {
        final var pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.COMPUTER_CASE_TIER3.get());
        final ComputerCaseBlockEntity initial = helper.getBlockEntity(pos);
        final var cpu = new ItemStack(ModItems.CPU_TIER1.get());
        ((MutableProcessor) Driver.driverFor(cpu)).setArchitecture(cpu, NativeLuaArchitecture.class);
        initial.setItem(8, cpu);
        initial.setItem(3, new ItemStack(ModItems.MEMORY_TIER2.get()));
        initial.setItem(0, card);
        initial.setItem(9, RobotMovementPersistenceGameTests.eeprom("""
            local eeprom = component.proxy(component.list('eeprom')())
            assert(eeprom.getData() == '', 'unexpected reboot')
            """ + setup + """
            eeprom.setData('waiting')
            repeat until computer.pullSignal() == 'continue_card'
            """ + verify + """
            eeprom.setData('restored')
            while true do computer.pullSignal() end
            """));
        helper.assertTrue(initial.toggleMachine(), "Card test computer did not start");
        final ComputerCaseBlockEntity[] active = {initial};
        helper.startSequence()
            .thenWaitUntil(() -> helper.assertTrue(marker(initial).equals("waiting"), "Card setup failed: " + initial.machine().lastError()))
            .thenExecute(() -> {
                final var saved = initial.saveWithFullMetadata(helper.getLevel().registryAccess());
                initial.setRemoved();
                active[0] = (ComputerCaseBlockEntity) BlockEntity.loadStatic(initial.getBlockPos(), initial.getBlockState(), saved, helper.getLevel().registryAccess());
                helper.getLevel().setBlockEntity(active[0]);
                active[0].onLoad();
                helper.assertTrue(active[0].machine().signal("continue_card"), "Restored card program rejected signal");
            })
            .thenWaitUntil(() -> helper.assertTrue(marker(active[0]).equals("restored"), "Card continuation failed: " + active[0].machine().lastError()))
            .thenSucceed();
    }

    private static String marker(ComputerCaseBlockEntity computer) {
        return new String(ItemDriverData.dataTag(computer.getItem(9)).getByteArray(ItemRegistry.EEPROM_DATA_SECTION_TAG), StandardCharsets.UTF_8);
    }
}
