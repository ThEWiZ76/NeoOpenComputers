package li.cil.oc.common.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/** Opt-in GameTest provider; never enabled by normal client/server launch configurations. */
public final class TestFluidCapabilities {
    private record Fixture(Direction face, IFluidHandler handler) { }
    private static final java.util.Map<GlobalPos, Fixture> FIXTURES = new java.util.HashMap<>();

    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlock(Capabilities.FluidHandler.BLOCK, (level, pos, state, entity, face) -> {
            final var fixture = FIXTURES.get(GlobalPos.of(level.dimension(), pos));
            return fixture != null && fixture.face() == face ? fixture.handler() : null;
        }, Blocks.LODESTONE);
    }

    public static AutoCloseable attach(ServerLevel level, BlockPos pos, Direction face, FluidTank... tanks) {
        final var key = GlobalPos.of(level.dimension(), pos);
        final var oldState = level.getBlockState(pos);
        final IFluidHandler handler = new IFluidHandler() {
            @Override public int getTanks() { return tanks.length; }
            @Override public FluidStack getFluidInTank(int tank) { return tanks[tank].getFluid(); }
            @Override public int getTankCapacity(int tank) { return tanks[tank].getCapacity(); }
            @Override public boolean isFluidValid(int tank, FluidStack fluid) { return tanks[tank].isFluidValid(fluid); }
            @Override public int fill(FluidStack fluid, FluidAction action) {
                for (var tank : tanks) {
                    final int filled = tank.fill(fluid, action);
                    if (filled > 0) return filled;
                }
                return 0;
            }
            @Override public FluidStack drain(FluidStack fluid, FluidAction action) {
                for (var tank : tanks) {
                    final var drained = tank.drain(fluid, action);
                    if (!drained.isEmpty()) return drained;
                }
                return FluidStack.EMPTY;
            }
            @Override public FluidStack drain(int amount, FluidAction action) {
                for (var tank : tanks) {
                    final var drained = tank.drain(amount, action);
                    if (!drained.isEmpty()) return drained;
                }
                return FluidStack.EMPTY;
            }
        };
        FIXTURES.put(key, new Fixture(face, handler));
        level.setBlockAndUpdate(pos, Blocks.LODESTONE.defaultBlockState());
        level.invalidateCapabilities(pos);
        return () -> {
            FIXTURES.remove(key);
            level.setBlockAndUpdate(pos, oldState);
            level.invalidateCapabilities(pos);
        };
    }
}
