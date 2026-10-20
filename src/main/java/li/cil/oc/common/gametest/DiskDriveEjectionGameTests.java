package li.cil.oc.common.gametest;

import li.cil.oc.api.Network;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Connector;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.blockentity.ComputerCaseBlockEntity;
import li.cil.oc.common.blockentity.DiskDriveBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;

import java.util.Arrays;

@GameTestHolder("neoopencomputers")
@net.neoforged.neoforge.gametest.PrefixGameTestTemplate(false)
public final class DiskDriveEjectionGameTests {
    record Hardware(DiskDriveBlockEntity drive, ComputerCaseBlockEntity computer) { }

    static Hardware hardware(GameTestHelper helper) {
        final var world = helper.getLevel(); final var pos = helper.absolutePos(new BlockPos(3, 2, 3));
        world.setBlockAndUpdate(pos, ModBlocks.DISK_DRIVE.get().defaultBlockState());
        world.setBlockAndUpdate(pos.west(), ModBlocks.COMPUTER_CASE_TIER1.get().defaultBlockState());
        final var drive = (DiskDriveBlockEntity) world.getBlockEntity(pos);
        final var computer = (ComputerCaseBlockEntity) world.getBlockEntity(pos.west());
        drive.onLoad(); computer.onLoad(); Network.joinOrCreateNetwork(computer);
        ((Connector) computer.node()).changeBuffer(10000);
        return new Hardware(drive, computer);
    }
    static Component filesystem(GameTestHelper helper, Hardware hardware) {
        for (var node : hardware.drive.node().reachableNodes()) if (node instanceof Component component && component.name().equals("filesystem")) return component;
        helper.fail("Inserted disk did not expose a filesystem"); return null;
    }

    @GameTest(template = "ae2_export")
    public static void floppySurvivesEveryInventoryRemoval(GameTestHelper helper) throws Exception {
        final var hardware = hardware(helper); final var context = hardware.computer.machine();
        final byte[] expected = new byte[]{0, (byte) 128, (byte) 255};
        for (int path = 0; path < 4; path++) {
            hardware.drive.setItem(0, new ItemStack(ModItems.FLOPPY.get()));
            final var original = filesystem(helper, hardware); final var address = original.address();
            var handle = original.invoke("open", context, "ejected.bin", "wb")[0];
            original.invoke("write", context, handle, expected); original.invoke("close", context, handle);
            final ItemStack removed;
            if (path == 0) removed = hardware.drive.removeItem(0, 1);
            else if (path == 1) removed = hardware.drive.removeItemNoUpdate(0);
            else {
                removed = hardware.drive.getItem(0);
                if (path == 2) hardware.drive.setItem(0, ItemStack.EMPTY); else hardware.drive.clearContent();
            }
            hardware.drive.setItem(0, removed);
            final var restored = filesystem(helper, hardware);
            helper.assertTrue(address.equals(restored.address()), "Disk identity lost during removal path " + path);
            helper.assertTrue(Boolean.TRUE.equals(restored.invoke("exists", context, "ejected.bin")[0]), "Disk contents lost during removal path " + path);
            handle = restored.invoke("open", context, "ejected.bin", "rb")[0];
            helper.assertTrue(Arrays.equals((byte[]) restored.invoke("read", context, handle, 3)[0], expected), "Ejected disk binary data changed");
            restored.invoke("close", context, handle); hardware.drive.clearContent();
        }
        helper.succeed();
    }
}
