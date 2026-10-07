package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModSettings;
import li.cil.oc.common.blockentity.CapacitorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
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
