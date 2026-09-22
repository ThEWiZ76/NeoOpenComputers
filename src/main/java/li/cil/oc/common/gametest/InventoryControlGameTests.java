package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.internal.Agent;
import li.cil.oc.api.network.Component;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.blockentity.RobotBlockEntity;
import li.cil.oc.common.entity.DroneEntity;
import li.cil.oc.common.item.DroneItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(NeoOpenComputers.MODID)
@PrefixGameTestTemplate(false)
public final class InventoryControlGameTests {
    @GameTest(template = "empty")
    public static void droneInventoryCallbacksMatchUpstream(GameTestHelper helper) throws Exception {
        final var drone = new DroneEntity(helper.getLevel());
        drone.loadFromItemStack(((DroneItem) ModItems.DRONE.get()).assembleFromCase(
            new ItemStack(ModItems.DRONE_CASE_TIER2.get()), new ItemStack(ModItems.INVENTORY_UPGRADE.get())), null);
        verify(helper, drone);
        drone.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void robotInventoryCallbacksMatchUpstream(GameTestHelper helper) throws Exception {
        final var pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.ROBOT.get());
        final RobotBlockEntity robot = helper.getBlockEntity(pos);
        robot.onLoad();
        verify(helper, robot);
        helper.succeed();
    }

    private static void verify(GameTestHelper helper, Agent agent) throws Exception {
        final var component = (Component) ((li.cil.oc.api.network.Environment) agent).node();
        final var inventory = agent.mainInventory();
        helper.assertTrue(((Number) component.invoke("inventorySize", agent.machine())[0]).intValue() == inventory.getContainerSize(), "Wrong inventory size");
        helper.assertTrue(((Number) component.invoke("select", agent.machine(), 2)[0]).intValue() == 2
            && agent.selectedSlot() == 1, "Slot selection must be one based");
        component.invoke("select", agent.machine(), 1);
        helper.assertTrue(Boolean.FALSE.equals(component.invoke("transferTo", agent.machine(), 2)[0]), "Empty source must reject transfer");
        helper.assertTrue(Boolean.TRUE.equals(component.invoke("compareTo", agent.machine(), 2)[0]), "Empty slots must compare equal");
        inventory.setItem(0, new ItemStack(Items.DIAMOND, 10));
        inventory.setItem(1, new ItemStack(Items.DIAMOND, 63));
        helper.assertTrue(Boolean.TRUE.equals(component.invoke("transferTo", agent.machine(), 2, 5)[0]), "Partial merge failed");
        helper.assertTrue(inventory.getItem(0).getCount() == 9 && inventory.getItem(1).getCount() == 64, "Merge exceeded capacity or lost items");
        helper.assertTrue(Boolean.FALSE.equals(component.invoke("transferTo", agent.machine(), 2)[0]), "Full destination accepted transfer");
        helper.assertTrue(Boolean.TRUE.equals(component.invoke("transferTo", agent.machine(), 1)[0]), "Self-transfer must succeed without mutation");
        helper.assertTrue(Boolean.TRUE.equals(component.invoke("transferTo", agent.machine(), 3, 0)[0]), "Zero transfer must succeed");
        inventory.setItem(2, new ItemStack(Items.IRON_INGOT, 3));
        helper.assertTrue(Boolean.FALSE.equals(component.invoke("transferTo", agent.machine(), 3, 8)[0]), "Partial incompatible swap accepted");
        helper.assertTrue(Boolean.TRUE.equals(component.invoke("transferTo", agent.machine(), 3)[0]), "Full incompatible stacks did not swap");
        helper.assertTrue(inventory.getItem(0).is(Items.IRON_INGOT) && inventory.getItem(0).getCount() == 3
            && inventory.getItem(2).is(Items.DIAMOND) && inventory.getItem(2).getCount() == 9, "Swap changed stacks");
        final var named = new ItemStack(Items.IRON_INGOT, 1);
        named.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("named"));
        inventory.setItem(3, named);
        helper.assertTrue(Boolean.TRUE.equals(component.invoke("compareTo", agent.machine(), 4)[0]), "Default comparison must ignore item data");
        helper.assertTrue(Boolean.FALSE.equals(component.invoke("compareTo", agent.machine(), 4, true)[0]), "Strict comparison ignored item data");
        helper.assertTrue(((Number) component.invoke("count", agent.machine())[0]).intValue() == 3, "Wrong selected count");
        helper.assertTrue(((Number) component.invoke("space", agent.machine())[0]).intValue() == 61, "Wrong selected space");
        try {
            component.invoke("select", agent.machine(), 0);
            throw new AssertionError("Invalid slot accepted");
        } catch (IllegalArgumentException expected) {
            helper.assertTrue(expected.getMessage().contains("slot"), "Unexpected invalid-slot error");
        }
    }
}
