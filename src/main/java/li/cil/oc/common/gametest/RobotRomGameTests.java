package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Node;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.blockentity.RobotBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(NeoOpenComputers.MODID)
@PrefixGameTestTemplate(false)
public final class RobotRomGameTests {
    @GameTest(template = "empty")
    public static void robotRomSurvivesReloadAndDetachesOnRemoval(final GameTestHelper helper) throws Exception {
        final BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.ROBOT.get());
        final RobotBlockEntity robot = helper.getBlockEntity(pos);
        robot.onLoad();
        final Component rom = rom(robot);
        helper.assertTrue(rom != null, "Robot has no ROM filesystem");
        helper.assertTrue(rom.canBeSeenFrom(robot.machine().node()), "Robot computer cannot see its ROM");
        helper.assertTrue("robot".equals(rom.invoke("getLabel", null)[0]), "Wrong ROM label");
        helper.assertTrue(Boolean.TRUE.equals(rom.invoke("isReadOnly", null)[0]), "Robot ROM is writable");
        helper.assertTrue(Boolean.TRUE.equals(rom.invoke("exists", null, "lib/robot.lua")[0]), "Robot library missing");
        helper.assertTrue(Boolean.TRUE.equals(rom.invoke("exists", null, "bin/go.lua")[0]), "Go program missing");
        final String address = rom.address();
        final CompoundTag saved = robot.saveWithFullMetadata(helper.getLevel().registryAccess());

        helper.setBlock(pos, Blocks.AIR);
        helper.assertTrue(rom.network() == null, "Removed robot left its ROM on the network");
        helper.setBlock(pos, ModBlocks.ROBOT.get());
        final RobotBlockEntity loaded = helper.getBlockEntity(pos);
        loaded.loadWithComponents(saved, helper.getLevel().registryAccess());
        loaded.onLoad();
        final Component loadedRom = rom(loaded);
        helper.assertTrue(loadedRom != null && address.equals(loadedRom.address()), "ROM address changed after reload");
        helper.assertTrue(loadedRom.canBeSeenFrom(loaded.machine().node()), "Reloaded robot cannot see ROM");
        loaded.onChunkUnloaded();
        helper.assertTrue(loadedRom.network() == null, "Unloaded robot left its ROM on the network");
        loaded.onLoad();
        helper.assertTrue(rom(loaded) == loadedRom, "Chunk reload lost the ROM");
        helper.succeed();
    }

    private static Component rom(final RobotBlockEntity robot) {
        for (final Node node : robot.node().neighbors()) {
            if (node instanceof Component component && "filesystem".equals(component.name())) {
                return component;
            }
        }
        return null;
    }
}
