package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModSettings;
import li.cil.oc.common.blockentity.CapacitorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(NeoOpenComputers.MODID)
@PrefixGameTestTemplate(false)
public final class CapacitorGameTests {
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void carpetedCapacitorTickerSuppliesNetworkPower(GameTestHelper helper) {
        final var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        final var world = helper.getLevel();
        world.setBlockAndUpdate(pos, ModBlocks.CARPETED_CAPACITOR.get().defaultBlockState());
        final var capacitor = (CapacitorBlockEntity) world.getBlockEntity(pos);
        capacitor.onLoad();
        final var consumer = place(helper, pos.relative(Direction.EAST));
        final var first = animal(helper, pos.above(), EntityType.SHEEP);
        final var second = animal(helper, pos.above(), EntityType.SHEEP);
        // Keep the generation fixture in the target block; movement is a separate acceptance scenario.
        for (final var sheep : List.of(first, second)) {
            sheep.noPhysics = true;
            sheep.setInvulnerable(true);
            ((net.minecraft.world.entity.Mob) sheep).setNoAi(true);
        }
        final long[] started = {0};
        final double[] baseline = {0};
        helper.startSequence()
            .thenExecute(() -> {
                started[0] = world.getGameTime();
                baseline[0] = consumer.node().globalBuffer();
                helper.assertTrue(consumer.node().network() == capacitor.node().network(), "Capacitor cluster did not join a network");
            })
            .thenExecuteAfter(20, () -> {
                helper.assertTrue(world.getGameTime() - started[0] == 20, "Ticker observation was not exactly twenty game ticks");
                final double generated = ModSettings.carpetSheepPower();
                helper.assertTrue(consumer.node().globalBuffer() == baseline[0] + generated,
                    "Natural capacitor ticker did not generate exactly one second of power");
                helper.assertTrue(consumer.node().tryChangeBuffer(-generated)
                    && consumer.node().globalBuffer() == baseline[0], "Generated energy was unavailable to the neighboring node");
                first.discard();
                second.discard();
            })
            .thenSucceed();
    }

    @GameTest(template = "empty")
    public static void carpetedCapacitorGeneratesFromAnimalGroups(GameTestHelper helper) {
        final var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        final var world = helper.getLevel();
        world.setBlockAndUpdate(pos, ModBlocks.CARPETED_CAPACITOR.get().defaultBlockState());
        final var capacitor = (CapacitorBlockEntity) world.getBlockEntity(pos);
        capacitor.onLoad();
        final var animals = new ArrayList<LivingEntity>();
        final double oldChance = ModSettings.CARPET_DAMAGE_CHANCE.get();
        ModSettings.CARPET_DAMAGE_CHANCE.set(0D);
        final var random = RandomSource.create(731);
        try {
            animals.add(animal(helper, pos.above(), EntityType.SHEEP));
            animals.add(animal(helper, pos.above(), EntityType.CAT));
            capacitor.generatePower(20, random);
            helper.assertTrue(capacitor.node().localBuffer() == 0, "Single animals generated power");
            animals.add(animal(helper, pos.above(), EntityType.SHEEP));
            capacitor.generatePower(40, random);
            final double sheep = ModSettings.carpetSheepPower();
            helper.assertTrue(capacitor.node().localBuffer() == sheep, "Sheep group did not generate configured power");
            animals.add(animal(helper, pos.above(), EntityType.SHEEP));
            capacitor.generatePower(60, random);
            helper.assertTrue(capacitor.node().localBuffer() == 2 * sheep, "Power incorrectly multiplied by group size");
            animals.add(animal(helper, pos.above(), EntityType.OCELOT));
            capacitor.generatePower(80, random);
            helper.assertTrue(capacitor.node().localBuffer() == 3 * sheep + ModSettings.carpetOcelotPower(),
                "Mixed domestic/wild cat group did not generate power");
            capacitor.node().changeBuffer(capacitor.node().localBufferSize());
            capacitor.generatePower(100, random);
            helper.assertTrue(capacitor.node().localBuffer() == capacitor.node().localBufferSize(), "Generation overfilled capacitor");
            final var normal = place(helper, pos.relative(Direction.EAST));
            helper.assertTrue(normal.node().localBufferSize() == ModSettings.capacitorBuffer() + ModSettings.capacitorAdjacencyBonus()
                && capacitor.node().localBufferSize() == normal.node().localBufferSize(), "Mixed cluster adjacency failed");
            final var saved = capacitor.saveWithFullMetadata(world.registryAccess());
            final double charge = capacitor.node().localBuffer();
            world.removeBlockEntity(pos);
            final var loaded = (CapacitorBlockEntity) BlockEntity.loadStatic(pos, ModBlocks.CARPETED_CAPACITOR.get().defaultBlockState(), saved, world.registryAccess());
            world.setBlockEntity(loaded);
            loaded.onLoad();
            helper.assertTrue(loaded.node().localBuffer() == charge
                && "CarpetedCapBank3x".equals(loaded.getDeviceInfo().get(li.cil.oc.api.driver.DeviceInfo.DeviceAttribute.Product)),
                "Carpeted capacitor reload lost charge or variant");
        } finally {
            ModSettings.CARPET_DAMAGE_CHANCE.set(oldChance);
            animals.forEach(LivingEntity::discard);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void carpetedCapacitorShockUsesMinuteCooldown(GameTestHelper helper) {
        final var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.getLevel().setBlockAndUpdate(pos, ModBlocks.CARPETED_CAPACITOR.get().defaultBlockState());
        final var capacitor = (CapacitorBlockEntity) helper.getLevel().getBlockEntity(pos);
        capacitor.onLoad();
        final var first = animal(helper, pos.above(), EntityType.SHEEP);
        final var second = animal(helper, pos.above(), EntityType.SHEEP);
        final double oldChance = ModSettings.CARPET_DAMAGE_CHANCE.get();
        ModSettings.CARPET_DAMAGE_CHANCE.set(1D);
        try {
            final var random = RandomSource.create(731);
            final float health = first.getHealth() + second.getHealth();
            capacitor.generatePower(20, random);
            helper.assertTrue(first.getHealth() + second.getHealth() == health - 1, "First shock did not damage exactly one animal");
            capacitor.generatePower(40, random);
            helper.assertTrue(first.getHealth() + second.getHealth() == health - 1, "Shock cooldown was ignored");
            first.invulnerableTime = 0;
            second.invulnerableTime = 0;
            capacitor.generatePower(1240, random);
            helper.assertTrue(first.getHealth() + second.getHealth() == health - 2, "Shock did not resume after cooldown");
        } finally {
            ModSettings.CARPET_DAMAGE_CHANCE.set(oldChance);
            first.discard();
            second.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void carpetedCapacitorCraftsWithColoredCarpet(GameTestHelper helper) {
        final var input = CraftingInput.of(2, 1, List.of(new ItemStack(ModItems.CAPACITOR.get()), new ItemStack(Items.RED_CARPET)));
        final var recipe = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
        helper.assertTrue(recipe.isPresent() && recipe.get().value().assemble(input, helper.getLevel().registryAccess()).is(ModItems.CARPETED_CAPACITOR.get()),
            "Colored carpet did not craft the carpeted capacitor");
        helper.succeed();
    }

    private static LivingEntity animal(GameTestHelper helper, BlockPos pos, EntityType<? extends LivingEntity> type) {
        final var animal = type.create(helper.getLevel());
        animal.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
        animal.setNoGravity(true);
        helper.assertTrue(helper.getLevel().addFreshEntity(animal), "Animal fixture did not spawn");
        return animal;
    }

    @GameTest(template = "empty")
    public static void capacitorAdjacencyEnergyAndReload(GameTestHelper helper) {
        final var origin = helper.absolutePos(new BlockPos(1, 2, 1));
        final double base = ModSettings.capacitorBuffer();
        final double bonus = ModSettings.capacitorAdjacencyBonus();
        final var root = place(helper, origin);
        helper.assertTrue(root.node().localBufferSize() == base, "Isolated capacity differs from base");
        final var adjacent = place(helper, origin.relative(Direction.EAST));
        helper.assertTrue(root.node().localBufferSize() == base + bonus && adjacent.node().localBufferSize() == base + bonus,
            "Direct adjacency was not updated on both capacitors");
        final var distant = place(helper, origin.relative(Direction.EAST, 2));
        helper.assertTrue(root.node().localBufferSize() == base + 1.5 * bonus
            && adjacent.node().localBufferSize() == base + 2 * bonus, "Second-degree adjacency was not applied");
        helper.getLevel().setBlockAndUpdate(adjacent.getBlockPos(), Blocks.AIR.defaultBlockState());
        helper.assertTrue(root.node().localBufferSize() == base + bonus / 2
            && distant.node().localBufferSize() == base + bonus / 2, "Removal did not refresh second-degree neighbors");
        final double charge = (base + bonus / 2) * 0.9;
        helper.assertTrue(root.node().changeBuffer(charge) == 0 && root.node().localBuffer() == charge, "Capacitor did not store network energy");
        helper.assertTrue(root.getBlockState().getAnalogOutputSignal(helper.getLevel(), origin) == 14, "Comparator did not round fill level");
        final var saved = root.saveWithFullMetadata(helper.getLevel().registryAccess());
        final String address = root.node().address();
        helper.getLevel().removeBlockEntity(origin);
        final var restored = (CapacitorBlockEntity) BlockEntity.loadStatic(origin, ModBlocks.CAPACITOR.get().defaultBlockState(), saved,
            helper.getLevel().registryAccess());
        helper.assertTrue(restored != null && restored.node().localBuffer() == charge, "Detached load truncated clustered energy");
        helper.getLevel().setBlockEntity(restored);
        restored.onLoad();
        helper.assertTrue(restored.node().localBuffer() == charge && address.equals(restored.node().address()), "Reload changed energy/address");
        helper.getLevel().setBlockAndUpdate(distant.getBlockPos(), Blocks.AIR.defaultBlockState());
        helper.assertTrue(restored.node().localBufferSize() == base && restored.node().localBuffer() == Math.min(charge, base),
            "Removed adjacency did not clamp to upstream capacity");
        helper.succeed();
    }

    private static CapacitorBlockEntity place(GameTestHelper helper, BlockPos pos) {
        helper.getLevel().setBlockAndUpdate(pos, ModBlocks.CAPACITOR.get().defaultBlockState());
        final var capacitor = (CapacitorBlockEntity) helper.getLevel().getBlockEntity(pos);
        capacitor.onLoad();
        return capacitor;
    }

    @GameTest(template = "empty")
    public static void capacitorRecipeResultIsPlaceable(GameTestHelper helper) {
        final var block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "capacitor"));
        helper.assertTrue(block != Blocks.AIR && ModItems.CAPACITOR.get() instanceof BlockItem item && item.getBlock() == block,
            "Capacitor recipe result is not a registered block item");
        helper.succeed();
    }
}
