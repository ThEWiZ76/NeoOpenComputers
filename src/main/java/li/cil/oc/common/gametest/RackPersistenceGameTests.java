package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.internal.Server;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.blockentity.RackBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(NeoOpenComputers.MODID)
@PrefixGameTestTemplate(false)
public final class RackPersistenceGameTests {
    @GameTest(template = "empty")
    public static void rackLoadsBeforeLevelAssignmentWithoutLosingServerInventory(final GameTestHelper helper) {
        final BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.RACK.get());
        final RackBlockEntity original = helper.getBlockEntity(pos);
        original.setItem(0, new ItemStack(ModItems.SERVER_TIER1.get()));
        final Server server = (Server) original.getMountable(0);
        final Container inventory = (Container) server;
        inventory.setItem(2, new ItemStack(ModItems.CPU_TIER1.get()));
        inventory.setItem(4, new ItemStack(ModItems.MEMORY_TIER1.get()));
        inventory.setItem(8, RobotMovementPersistenceGameTests.eeprom("while true do computer.pullSignal() end"));
        final String address = server.machine().node().address();
        final ItemStack[] expected = new ItemStack[inventory.getContainerSize()];
        original.setItem(1, new ItemStack(ModItems.DISK_DRIVE_MOUNTABLE.get()));
        final Container drive = (Container) original.getMountable(1);
        final ItemStack floppy = new ItemStack(ModItems.FLOPPY.get());
        floppy.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Saved rack disk"));
        drive.setItem(0, floppy);
        final String driveAddress = original.getMountable(1).node().address();
        final var registries = helper.getLevel().registryAccess();
        final var saved = original.saveWithFullMetadata(registries);
        // Compare the actual inventories after their environments have flushed state.
        for (int slot = 0; slot < expected.length; slot++) expected[slot] = inventory.getItem(slot).copy();
        final ItemStack expectedFloppy = drive.getItem(0).copy();
        final var state = original.getBlockState();
        final var absolute = original.getBlockPos();
        original.setRemoved();

        // Vanilla deserializes chunk block entities before assigning their level.
        final BlockEntity restored = BlockEntity.loadStatic(absolute, state, saved, registries);
        helper.assertTrue(restored instanceof RackBlockEntity, "Vanilla discarded the saved rack during level-less loading");
        final RackBlockEntity detached = (RackBlockEntity) restored;
        helper.assertTrue(detached.getLevel() == null, "Fixture prematurely attached the world");
        // Saving again before attachment must preserve the pending nested inventories too.
        final var pendingSave = detached.saveWithFullMetadata(registries);
        final BlockEntity restoredAgain = BlockEntity.loadStatic(absolute, state, pendingSave, registries);
        helper.assertTrue(restoredAgain instanceof RackBlockEntity, "Detached save discarded rack data");
        final RackBlockEntity loaded = (RackBlockEntity) restoredAgain;
        helper.getLevel().setBlockEntity(loaded);
        loaded.onLoad();
        helper.assertTrue(loaded.getMountable(0) instanceof Server, "Loaded rack did not initialize its server");
        final Server loadedServer = (Server) loaded.getMountable(0);
        final Container loadedInventory = (Container) loadedServer;
        helper.assertTrue(loadedServer.machine().components().containsValue("eeprom"), "Loaded server did not reconnect its hardware");
        for (int slot = 0; slot < expected.length; slot++) {
            helper.assertTrue(ItemStack.matches(expected[slot], loadedInventory.getItem(slot)), "Server inventory changed in slot " + slot + ": expected=" + expected[slot].saveOptional(registries) + ", actual=" + loadedInventory.getItem(slot).saveOptional(registries));
        }
        helper.assertTrue(address.equals(loadedServer.machine().node().address()), "Reload changed server address");
        helper.assertTrue(loaded.getMountable(1) instanceof Container, "Loaded rack did not initialize its disk drive");
        helper.assertTrue(ItemStack.matches(expectedFloppy, ((Container) loaded.getMountable(1)).getItem(0)), "Rack drive lost disk or item components: expected=" + expectedFloppy.saveOptional(registries) + ", actual=" + ((Container) loaded.getMountable(1)).getItem(0).saveOptional(registries));
        helper.assertTrue(driveAddress.equals(loaded.getMountable(1).node().address()), "Reload changed drive address");
        final var initializedServer = loaded.getMountable(0);
        loaded.onLoad();
        helper.assertTrue(loaded.getMountable(0) == initializedServer, "Repeated onLoad rebuilt a live server");
        helper.succeed();
    }
}
