package li.cil.oc.common.gametest;

import li.cil.oc.api.network.Connector;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.ModSettings;
import li.cil.oc.common.block.ComputerCaseBlock;
import li.cil.oc.common.blockentity.AssemblerBlockEntity;
import li.cil.oc.common.blockentity.DisassemblerBlockEntity;
import li.cil.oc.common.blockentity.RobotBlockEntity;
import li.cil.oc.common.item.DroneItem;
import li.cil.oc.common.template.DisassemblerTemplates;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.ArrayList;

@GameTestHolder("neoopencomputers")
@PrefixGameTestTemplate(false)
public final class AgentDisassemblerGameTests {
    private static final BlockPos ASSEMBLER_POS = new BlockPos(0, 1, 0);
    private static final BlockPos POS = new BlockPos(1, 1, 1);

    @GameTest(template = "empty")
    public static void assembledRobotsReturnTheirTierCasesAndHardwareThroughPaidDisassembly(GameTestHelper helper) throws Exception {
        roundTrip(helper, List.of(new ItemStack(ModBlocks.COMPUTER_CASE_TIER1.get()), new ItemStack(ModBlocks.COMPUTER_CASE_TIER2.get()), new ItemStack(ModBlocks.COMPUTER_CASE_TIER3.get())));
    }

    @GameTest(template = "empty")
    public static void assembledDronesReturnSurvivalAndCreativeCasesWithHardware(GameTestHelper helper) throws Exception {
        roundTrip(helper, List.of(new ItemStack(ModItems.DRONE_CASE_TIER1.get()), new ItemStack(ModItems.DRONE_CASE_TIER2.get()), new ItemStack(ModItems.DRONE_CASE_CREATIVE.get())));
    }

    @GameTest(template = "empty")
    public static void assembledMicrocontrollersReturnSurvivalAndCreativeCasesWithHardware(GameTestHelper helper) throws Exception {
        roundTrip(helper, List.of(new ItemStack(ModItems.MICROCONTROLLER_CASE_TIER1.get()), new ItemStack(ModItems.MICROCONTROLLER_CASE_TIER2.get()), new ItemStack(ModItems.MICROCONTROLLER_CASE_CREATIVE.get())));
    }

    private static void roundTrip(GameTestHelper helper, List<ItemStack> cases) throws Exception {
        NeoOpenComputersGameTests.withCachedConfig(ModSettings.DISASSEMBLER_BREAK_CHANCE, 0D, () ->
            NeoOpenComputersGameTests.withCachedConfig(ModSettings.DISASSEMBLER_ITEM_COST, 4D, () ->
                NeoOpenComputersGameTests.withCachedConfig(ModSettings.DISASSEMBLER_TICK_AMOUNT, 2D, () -> {
                    for (var chassis : cases) {
                        helper.setBlock(ASSEMBLER_POS, Blocks.AIR); helper.setBlock(ASSEMBLER_POS, ModBlocks.ASSEMBLER.get());
                        final AssemblerBlockEntity assembler = helper.getBlockEntity(ASSEMBLER_POS);
                        final var memory = new ItemStack(ModItems.MEMORY_TIER1.get());
                        final var enchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                        enchantments.set(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.UNBREAKING), 1);
                        memory.set(DataComponents.ENCHANTMENTS, enchantments.toImmutable());
                        final var marker = new CompoundTag(); marker.putInt("retained", 42); memory.set(DataComponents.CUSTOM_DATA, CustomData.of(marker));
                        final var cpu = new ItemStack(ModItems.CPU_TIER1.get());
                        final var eeprom = RobotMovementPersistenceGameTests.eeprom("return");
                        assembler.setItem(AssemblerBlockEntity.SLOT_TEMPLATE, chassis.copy());
                        assembler.setItem(AssemblerBlockEntity.SLOT_COMPONENT_START, cpu.copy());
                        assembler.setItem(AssemblerBlockEntity.SLOT_COMPONENT_START + 1, memory.copy());
                        assembler.setItem(AssemblerBlockEntity.SLOT_COMPONENT_START + 2, eeprom.copy());
                        final var expected = new ArrayList<ItemStack>(List.of(chassis, cpu, memory, eeprom));
                        if (chassis.getItem() instanceof BlockItem block && block.getBlock() instanceof ComputerCaseBlock) {
                            final var container = new ItemStack(ModItems.CARD_CONTAINER_TIER1.get());
                            container.set(DataComponents.ENCHANTMENTS, enchantments.toImmutable());
                            container.set(DataComponents.CUSTOM_DATA, CustomData.of(marker));
                            assembler.setItem(AssemblerBlockEntity.SLOT_CONTAINER_START, container.copy()); expected.add(container);
                        }
                        helper.assertTrue(assembler.canAssemble() && assembler.start(true), "Agent fixture rejected chassis " + chassis);
                        final var agent = assembler.removeItemNoUpdate(AssemblerBlockEntity.SLOT_TEMPLATE);
                        helper.setBlock(POS, Blocks.AIR); helper.setBlock(POS, ModBlocks.DISASSEMBLER.get());
                        final DisassemblerBlockEntity original = helper.getBlockEntity(POS);
                        final var initialPower = (Connector) original.node(); initialPower.changeBuffer(-initialPower.globalBuffer());
                        original.setItem(0, agent.copy());
                        helper.assertTrue(original.canDisassemble() && original.disassemble(), "Builtin agent disassembly rejected " + chassis);
                        helper.assertTrue(original.getItem(0).isEmpty() && outputs(original) == 0
                            && original.saveWithFullMetadata(helper.getLevel().registryAccess()).getList("oc:queue", Tag.TAG_COMPOUND).size() == expected.size(),
                            "Agent hardware bypassed the paid queue: " + chassis);
                        initialPower.changeBuffer(2D);
                        DisassemblerBlockEntity.serverTick(helper.getLevel(), original.getBlockPos(), original.getBlockState(), original);
                        final var saved = original.saveWithFullMetadata(helper.getLevel().registryAccess()); original.onChunkUnloaded(); original.setRemoved();
                        final var machine = (DisassemblerBlockEntity) BlockEntity.loadStatic(original.getBlockPos(), original.getBlockState(), saved, helper.getLevel().registryAccess());
                        helper.getLevel().setBlockEntity(machine); machine.onLoad();
                        final var connector = (Connector) machine.node(); connector.changeBuffer(4D * expected.size() - 2D);
                        for (int tick = 0; tick < expected.size() * 2 + 4; tick++) DisassemblerBlockEntity.serverTick(helper.getLevel(), machine.getBlockPos(), machine.getBlockState(), machine);
                        helper.assertTrue(outputs(machine) == expected.size() && expected.stream().allMatch(part -> count(machine, part) == 1) && connector.globalBuffer() == 0D,
                            "Agent recovery lost/duplicated tier case, registry-bound hardware or energy: " + chassis);
                        machine.clearContent();
                        final var empty = editedHardware(agent, "empty");
                        final var emptyPlan = DisassemblerTemplates.select(empty).orElseThrow().plan(empty, new ItemStack[0], helper.getLevel().registryAccess());
                        helper.assertTrue(emptyPlan.queued().length == 1 && ItemStack.matches(emptyPlan.queued()[0], chassis)
                            && emptyPlan.immediate().length == 0, "Empty agent hardware lost its tier case: " + chassis);
                        for (var defect : List.of("unknown", "slot", "duplicate")) {
                            final var broken = editedHardware(agent, defect); machine.setItem(0, broken.copy());
                            helper.assertTrue(!machine.canDisassemble() && !machine.disassemble() && ItemStack.matches(machine.getItem(0), broken),
                                "Malformed agent hardware consumed or mutated its chassis: " + chassis + "/" + defect);
                            machine.clearContent();
                        }
                    }
                })));
        helper.succeed();
    }

    private static ItemStack editedHardware(ItemStack source, String defect) {
        final var result = source.copy(); final boolean drone = source.is(ModItems.DRONE.get());
        final var component = drone ? DataComponents.CUSTOM_DATA : DataComponents.BLOCK_ENTITY_DATA;
        final var root = result.getOrDefault(component, CustomData.EMPTY).copyTag();
        final var data = drone ? root.getCompound(DroneItem.DATA_TAG) : root;
        final var key = drone ? DroneItem.COMPONENTS_TAG : source.is(ModItems.ROBOT.get()) ? RobotBlockEntity.TAG_HARDWARE : "Items";
        final var items = data.getList(key, Tag.TAG_COMPOUND);
        if (defect.equals("empty")) data.put(key, new ListTag());
        else {
            final var entry = items.getCompound(0);
            if (defect.equals("unknown")) (drone ? entry.getCompound(DroneItem.STACK_TAG) : entry).putString("id", "missing:hardware");
            else if (defect.equals("slot")) entry.putByte(drone ? DroneItem.SLOT_TAG : "Slot", (byte) 127);
            else items.add(entry.copy());
        }
        result.set(component, CustomData.of(root)); return result;
    }

    private static int count(DisassemblerBlockEntity machine, ItemStack expected) {
        int count = 0;
        for (int slot = 1; slot < machine.getContainerSize(); slot++) {
            final var stack = machine.getItem(slot); if (ItemStack.isSameItemSameComponents(stack, expected)) count += stack.getCount();
        }
        return count;
    }

    private static int outputs(DisassemblerBlockEntity machine) {
        int count = 0;
        for (int slot = 1; slot < machine.getContainerSize(); slot++) count += machine.getItem(slot).getCount();
        return count;
    }
}
