package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.item.MutableProcessor;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.entity.DroneEntity;
import li.cil.oc.common.item.DroneItem;
import li.cil.oc.common.machine.NativeLuaArchitecture;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(NeoOpenComputers.MODID)
@PrefixGameTestTemplate(false)
public final class DroneCargoPersistenceGameTests {
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void nativeDroneResumesCargoAndTractorOperations(GameTestHelper helper) {
        final var original = new DroneEntity(helper.getLevel());
        final var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        original.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        final var cpu = new ItemStack(ModItems.CPU_TIER1.get());
        ((MutableProcessor) Driver.driverFor(cpu)).setArchitecture(cpu, NativeLuaArchitecture.class);
        original.loadFromItemStack(((DroneItem) ModItems.DRONE.get()).assembleFromCase(
            new ItemStack(ModItems.DRONE_CASE_TIER2.get()), cpu, new ItemStack(ModItems.MEMORY_TIER1.get()),
            new ItemStack(ModItems.TRACTOR_BEAM_UPGRADE.get()), new ItemStack(ModItems.INVENTORY_CONTROLLER_UPGRADE.get()),
            new ItemStack(ModItems.INVENTORY_UPGRADE.get()), RobotMovementPersistenceGameTests.eeprom("""
                local drone = component.proxy(component.list('drone')())
                local tractor = component.proxy(assert(component.list('tractor_beam')(), 'tractor missing'))
                local inventory = component.proxy(assert(component.list('inventory_controller')(), 'controller missing'))
                assert(drone.getStatusText() == '', 'unexpected reboot')
                local marker = 731
                assert(drone.inventorySize() == 4)
                assert(drone.select(1) == 1 and drone.count() == 11)
                assert(drone.transferTo(2, 3))
                assert(drone.count(1) == 8 and drone.count(2) == 3)
                assert(drone.compareTo(2))
                drone.select(2)
                drone.setStatusText('waiting')
                repeat until computer.pullSignal() == 'continue_cargo'
                assert(marker == 731 and drone.select() == 2, 'continuation lost state')
                assert(drone.count(1) == 8 and drone.count() == 3, 'cargo lost on reload')
                assert(inventory.getStackInInternalSlot(1).name == 'minecraft:diamond')
                assert(tractor.suck(), 'restored drone tractor failed')
                assert(drone.count(3) == 5)
                assert(inventory.getStackInInternalSlot(3).name == 'minecraft:iron_ingot')
                assert(drone.transferTo(1))
                assert(drone.count(1) == 11 and drone.count(2) == 0)
                drone.setStatusText('restored')
                while true do computer.pullSignal() end
                """)), null);
        original.mainInventory().setItem(0, new ItemStack(Items.DIAMOND, 11));
        helper.assertTrue(helper.getLevel().addFreshEntity(original), "Drone did not spawn");
        helper.assertTrue(original.toggleMachine(), "Drone did not start");
        final CompoundTag[] saved = {null};
        final DroneEntity[] loaded = {null};
        helper.startSequence()
            .thenWaitUntil(() -> helper.assertTrue(status(original).equals("waiting"), "Cargo setup failed: " + original.machine().lastError()))
            .thenExecute(() -> {
                saved[0] = original.saveWithoutId(new CompoundTag());
                original.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
                original.onRemovedFromLevel();
            })
            .thenIdle(2)
            .thenExecute(() -> {
                loaded[0] = new DroneEntity(helper.getLevel());
                loaded[0].load(saved[0]);
                helper.assertTrue(helper.getLevel().addFreshEntity(loaded[0]), "Restored drone did not spawn");
                final var item = new ItemEntity(helper.getLevel(), loaded[0].getX(), loaded[0].getY(), loaded[0].getZ(), new ItemStack(Items.IRON_INGOT, 5));
                item.setNoPickUpDelay();
                helper.getLevel().addFreshEntity(item);
                helper.assertTrue(loaded[0].machine().signal("continue_cargo"), "Resume signal rejected");
            })
            .thenWaitUntil(() -> helper.assertTrue(status(loaded[0]).equals("restored"), "Cargo continuation failed: " + loaded[0].machine().lastError()))
            .thenExecute(() -> {
                final var inventory = loaded[0].mainInventory();
                helper.assertTrue(inventory.getItem(0).is(Items.DIAMOND) && inventory.getItem(0).getCount() == 11, "Diamond cargo was not conserved");
                helper.assertTrue(inventory.getItem(1).isEmpty() && inventory.getItem(2).is(Items.IRON_INGOT)
                    && inventory.getItem(2).getCount() == 5, "Tractor pickup or merge changed cargo");
            })
            .thenSucceed();
    }

    private static String status(DroneEntity drone) { return (String) drone.getStatusText(null, null)[0]; }
}
