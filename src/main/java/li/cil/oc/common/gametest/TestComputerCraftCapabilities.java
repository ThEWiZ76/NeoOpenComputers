package li.cil.oc.common.gametest;

import li.cil.oc.common.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/** Explicit GameTest profile only; not registered by normal client/server startup. */
public final class TestComputerCraftCapabilities {
    public static void register(RegisterCapabilitiesEvent event) { Loaded.register(event); }
    public static AutoCloseable attach(ServerLevel level, BlockPos pos, Direction face, Object peripheral) {
        return Loaded.attach(level, pos, face, peripheral);
    }
    private static final class Loaded {
        record Fixture(Direction face, dan200.computercraft.api.peripheral.IPeripheral peripheral) { }
        private static final java.util.Map<GlobalPos, Fixture> fixtures = new java.util.HashMap<>();
        static void register(RegisterCapabilitiesEvent event) {
            event.registerBlock(dan200.computercraft.api.peripheral.PeripheralCapability.get(), (level, pos, state, entity, face) -> {
                final var fixture = fixtures.get(GlobalPos.of(level.dimension(), pos));
                return fixture != null && fixture.face() == face ? fixture.peripheral() : null;
            }, Blocks.DISPENSER, Blocks.COMMAND_BLOCK, ModBlocks.SCREEN_TIER1.get());
        }
        static AutoCloseable attach(ServerLevel level, BlockPos pos, Direction face, Object peripheral) {
            final var key = GlobalPos.of(level.dimension(), pos);
            fixtures.put(key, new Fixture(face, (dan200.computercraft.api.peripheral.IPeripheral) peripheral));
            level.invalidateCapabilities(pos);
            return () -> { fixtures.remove(key); level.invalidateCapabilities(pos); };
        }
    }
}
