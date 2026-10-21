package li.cil.oc.common.gametest;

import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.blockentity.AssemblerBlockEntity;
import li.cil.oc.common.blockentity.MicrocontrollerBlockEntity;
import li.cil.oc.common.blockentity.RobotBlockEntity;
import li.cil.oc.common.entity.DroneEntity;
import li.cil.oc.common.item.DroneItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

@GameTestHolder("neoopencomputers")
@PrefixGameTestTemplate(false)
public final class AgentRegistryItemCodecGameTests {
    @GameTest(template = "empty")
    public static void robotAssemblyPlacementAndPickupRetainRegistryBoundHardware(GameTestHelper helper) { roundTrip(helper, "robot"); }
    @GameTest(template = "empty")
    public static void droneAssemblyPlacementAndPickupRetainRegistryBoundHardware(GameTestHelper helper) { roundTrip(helper, "drone"); }
    @GameTest(template = "empty")
    public static void microcontrollerAssemblyPlacementAndPickupRetainRegistryBoundHardware(GameTestHelper helper) { roundTrip(helper, "microcontroller"); }

    private static void roundTrip(GameTestHelper helper, String kind) {
        final var registries = helper.getLevel().registryAccess();
        final var memory = new ItemStack(ModItems.MEMORY_TIER1.get());
        final var enchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        enchantments.set(registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.UNBREAKING), 1);
        memory.set(DataComponents.ENCHANTMENTS, enchantments.toImmutable());
        final var marker = new CompoundTag(); marker.putInt("retained", 42); memory.set(DataComponents.CUSTOM_DATA, CustomData.of(marker));
        final var template = switch (kind) {
            case "robot" -> new ItemStack(ModBlocks.COMPUTER_CASE_TIER1.get());
            case "drone" -> new ItemStack(ModItems.DRONE_CASE_TIER1.get());
            default -> new ItemStack(ModItems.MICROCONTROLLER_CASE_TIER1.get());
        };
        final var assemblerPos = new BlockPos(0, 1, 0); helper.setBlock(assemblerPos, ModBlocks.ASSEMBLER.get());
        final AssemblerBlockEntity assembler = helper.getBlockEntity(assemblerPos);
        assembler.setItem(AssemblerBlockEntity.SLOT_TEMPLATE, template);
        assembler.setItem(AssemblerBlockEntity.SLOT_COMPONENT_START, new ItemStack(ModItems.CPU_TIER1.get()));
        assembler.setItem(AssemblerBlockEntity.SLOT_COMPONENT_START + 1, memory);
        assembler.setItem(AssemblerBlockEntity.SLOT_COMPONENT_START + 2, RobotMovementPersistenceGameTests.eeprom("return"));
        helper.assertTrue(assembler.canAssemble() && assembler.start(true), "Agent assembler rejected " + kind);
        final var assembled = assembler.removeItemNoUpdate(AssemblerBlockEntity.SLOT_TEMPLATE);
        assertMemory(helper, hardware(helper, assembled, kind), "Physical " + kind + " assembly discarded registry-bound hardware");
        final var player = helper.makeMockServerPlayerInLevel();
        try {
            player.setGameMode(GameType.SURVIVAL);
            final var support = new BlockPos(2, 0, 1); helper.setBlock(support, Blocks.STONE);
            final var absolute = helper.absolutePos(support);
            player.setItemInHand(InteractionHand.MAIN_HAND, assembled.copy());
            final var hit = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
            helper.assertTrue(player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction()
                && player.getMainHandItem().isEmpty(), "Survival placement did not consume one " + kind);
            final ItemStack picked;
            if (kind.equals("drone")) {
                final var area = new AABB(absolute.above()).inflate(1);
                final var drones = helper.getLevel().getEntitiesOfClass(DroneEntity.class, area);
                helper.assertTrue(drones.size() == 1, "Drone placement did not create exactly one entity");
                final var drone = drones.getFirst(); assertMemory(helper, drone.internalComponents(), "Placed drone lost registry-bound hardware");
                player.setShiftKeyDown(true); player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.WRENCH.get()));
                helper.assertTrue(drone.interact(player, InteractionHand.MAIN_HAND).consumesAction(), "Drone wrench pickup failed");
                final var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, area);
                helper.assertTrue(drone.isRemoved() && drops.size() == 1 && drops.getFirst().getItem().is(ModItems.DRONE.get()), "Drone pickup did not return one packed entity");
                picked = drops.getFirst().getItem().copy(); drops.forEach(ItemEntity::discard);
            } else if (kind.equals("robot")) {
                final RobotBlockEntity host = helper.getBlockEntity(support.above());
                assertMemory(helper, host.internalComponents(), "Placed robot lost registry-bound hardware");
                picked = host.createRobotDrop(registries);
            } else {
                final MicrocontrollerBlockEntity host = helper.getBlockEntity(support.above());
                final var contents = new ArrayList<ItemStack>();
                for (int slot = 0; slot < host.getContainerSize(); slot++) contents.add(host.getItem(slot));
                assertMemory(helper, contents, "Placed microcontroller lost registry-bound hardware");
                picked = host.createMicrocontrollerItem(registries);
            }
            final var saved = ItemStack.parseOptional(registries, (CompoundTag) picked.save(registries));
            assertMemory(helper, hardware(helper, saved, kind), "Packed/saved " + kind + " lost registry-bound hardware");
        } finally { helper.getLevel().getServer().getPlayerList().remove(player); }
        helper.succeed();
    }

    private static List<ItemStack> hardware(GameTestHelper helper, ItemStack stack, String kind) {
        final var components = kind.equals("drone") ? ((DroneItem) stack.getItem()).components(stack)
            : stack.get(DataComponents.BLOCK_ENTITY_DATA).copyTag().getList(kind.equals("robot") ? RobotBlockEntity.TAG_HARDWARE : "Items", Tag.TAG_COMPOUND);
        final var result = new ArrayList<ItemStack>();
        for (int i = 0; i < components.size(); i++) {
            final var entry = components.getCompound(i);
            result.add(ItemStack.parseOptional(helper.getLevel().registryAccess(), kind.equals("drone") ? entry.getCompound("stack") : entry));
        }
        return result;
    }

    private static void assertMemory(GameTestHelper helper, Iterable<ItemStack> contents, String message) {
        int count = 0;
        for (var part : contents) if (part.is(ModItems.MEMORY_TIER1.get())) {
            count += part.getCount();
            helper.assertTrue(part.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt("retained") == 42
                && part.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY)
                    .getLevel(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.UNBREAKING)) == 1, message);
        }
        helper.assertTrue(count == 1, message);
    }
}
