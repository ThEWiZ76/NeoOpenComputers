package li.cil.oc.common.gametest;

import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.item.MutableProcessor;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.network.Connector;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.blockentity.ComputerCaseBlockEntity;
import li.cil.oc.common.blockentity.MicrocontrollerBlockEntity;
import li.cil.oc.common.component.RedstoneControllerHost;
import li.cil.oc.common.machine.NativeLuaArchitecture;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Real installed hardware and running native VM for world-output fixtures. */
final class RedstoneMachineFixture {
    static Fixture boot(GameTestHelper helper, BlockEntity entity, String program, boolean advanced) {
        final var inventory = (Container) entity;
        final Machine machine = entity instanceof ComputerCaseBlockEntity computer ? computer.machine()
            : ((MicrocontrollerBlockEntity) entity).machine();
        int eeprom = -1;
        int card = -1;
        boolean cpu = false;
        boolean memory = false;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            final var type = entity instanceof ComputerCaseBlockEntity computer ? ComputerCaseBlockEntity.slotType(computer.tier(), slot)
                : MicrocontrollerBlockEntity.slotType(((MicrocontrollerBlockEntity) entity).tier(), slot);
            ItemStack stack = ItemStack.EMPTY;
            if (type.equals("cpu") && !cpu) {
                stack = new ItemStack(ModItems.CPU_TIER1.get());
                ((MutableProcessor) Driver.driverFor(stack)).setArchitecture(stack, NativeLuaArchitecture.class);
                cpu = true;
            } else if (type.equals("memory") && !memory) {
                stack = new ItemStack(ModItems.MEMORY_TIER1.get());
                memory = true;
            } else if (type.equals("eeprom") && eeprom < 0) {
                stack = RobotMovementPersistenceGameTests.eeprom(program);
                eeprom = slot;
            } else if (type.equals("card") && card < 0) {
                stack = new ItemStack(advanced ? ModItems.REDSTONE_CARD_TIER2.get() : ModItems.REDSTONE_CARD.get());
                if (!MicrocontrollerFixture.canPlaceItem(inventory, slot, stack)) continue;
                card = slot;
            }
            if (!stack.isEmpty()) MicrocontrollerFixture.setItem(inventory, slot, stack);
        }
        helper.assertTrue(cpu && memory && eeprom >= 0 && card >= 0, "Redstone fixture hardware does not fit the host");
        entity.onLoad();
        ((Connector) machine.node()).changeBuffer(10000);
        helper.assertTrue(machine.start(), "Redstone fixture did not start: " + machine.lastError());
        return new Fixture(inventory, (RedstoneControllerHost) entity, machine, eeprom, card);
    }

    static void powerOn(GameTestHelper helper, BlockEntity entity, boolean advanced) {
        if (entity instanceof ComputerCaseBlockEntity || entity instanceof MicrocontrollerBlockEntity) {
            boot(helper, entity, "while true do computer.pullSignal() end", advanced);
        }
    }

    record Fixture(Container inventory, RedstoneControllerHost host, Machine machine, int eepromSlot, int cardSlot) {}
    private RedstoneMachineFixture() {}
}
