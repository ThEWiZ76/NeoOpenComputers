package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.machine.MachineHost;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.blockentity.ComputerCaseBlockEntity;
import li.cil.oc.common.blockentity.MicrocontrollerBlockEntity;
import li.cil.oc.common.blockentity.RackBlockEntity;
import li.cil.oc.common.entity.DroneEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@GameTestHolder(NeoOpenComputers.MODID)
@PrefixGameTestTemplate(false)
public final class ComponentSlotMappingGameTests {
    @GameTest(template = "empty")
    public static void caseMapsComponentsDespiteNestedMemoryEnumeration(final GameTestHelper helper) {
        final BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.COMPUTER_CASE_TIER1.get());
        final ComputerCaseBlockEntity host = helper.getBlockEntity(pos);
        verify(helper, host, host);
    }

    @GameTest(template = "empty")
    public static void microcontrollerMapsComponentsDespiteNestedMemoryEnumeration(final GameTestHelper helper) {
        final BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.MICROCONTROLLER_TIER1.get());
        final MicrocontrollerBlockEntity host = helper.getBlockEntity(pos);
        verify(helper, host, host);
    }

    @GameTest(template = "empty")
    public static void droneMapsComponentsDespiteNestedMemoryEnumeration(final GameTestHelper helper) {
        final DroneEntity host = new DroneEntity(helper.getLevel());
        verify(helper, host, host);
    }

    @GameTest(template = "empty")
    public static void rackServerMapsComponentsDespiteNestedMemoryEnumeration(final GameTestHelper helper) {
        final BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.RACK.get());
        final RackBlockEntity rack = helper.getBlockEntity(pos);
        rack.setItem(0, new ItemStack(ModItems.SERVER_TIER1.get()));
        final var server = rack.getMountable(0);
        verify(helper, (MachineHost) server, (Container) server);
    }

    private static void verify(final GameTestHelper helper, final MachineHost host, final Container inventory) {
        final Set<Integer> installedSlots = new HashSet<>();
        for (final ItemStack stack : List.of(new ItemStack(ModItems.CPU_TIER1.get()),
            new ItemStack(ModItems.MEMORY_TIER1.get()), RobotMovementPersistenceGameTests.eeprom("while true do computer.pullSignal() end"))) {
            boolean installed = false;
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                if (inventory.getItem(slot).isEmpty() && inventory.canPlaceItem(slot, stack)) {
                    inventory.setItem(slot, stack);
                    installedSlots.add(slot);
                    installed = true;
                    break;
                }
            }
            helper.assertTrue(installed, "Fixture could not install " + stack);
        }
        for (int rebuild = 0; rebuild < 2; rebuild++) {
            host.machine().onHostChanged();
            final Set<Integer> mappedSlots = new HashSet<>();
            for (final var node : host.machine().node().neighbors()) {
                final int slot = host.componentSlot(node.address());
                if (slot < 0) continue;
                helper.assertTrue(mappedSlots.add(slot), "Multiple installed components share slot " + slot);
            }
            helper.assertTrue(mappedSlots.equals(installedSlots), "Component mapping does not cover exact installed slots");
            helper.assertTrue(host.componentSlot(null) == -1 && host.componentSlot("missing-component") == -1,
                "Unknown address resolved an installed slot");
        }
        helper.succeed();
    }
}
