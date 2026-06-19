package li.cil.oc.api.internal;

import li.cil.oc.api.Persistable;
import li.cil.oc.api.machine.MachineHost;
import li.cil.oc.api.network.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.IFluidTank;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class InternalLeafApiTest {
    @Test
    void rotatableUsesModernDirection() {
        Rotatable rotatable = new TestRotatable();

        assertSame(Direction.NORTH, rotatable.facing());
        assertSame(Direction.EAST, rotatable.toGlobal(Direction.SOUTH));
        assertSame(Direction.WEST, rotatable.toLocal(Direction.SOUTH));
    }

    @Test
    void wrenchUsesModernPlayerLevelAndBlockPos() throws NoSuchMethodException {
        Method useWrench = Wrench.class.getMethod("useWrenchOnBlock", Player.class, Level.class, BlockPos.class, boolean.class);
        Wrench wrench = (player, world, pos, simulate) -> simulate && pos.equals(BlockPos.ZERO);

        assertArrayEquals(new Class<?>[]{Player.class, Level.class, BlockPos.class, boolean.class}, useWrench.getParameterTypes());
        assertTrue(wrench.useWrenchOnBlock(null, null, BlockPos.ZERO, true));
    }

    @Test
    void databaseUsesModernItemStack() throws NoSuchMethodException {
        Method getStack = Database.class.getMethod("getStackInSlot", int.class);
        Method setStack = Database.class.getMethod("setStackInSlot", int.class, ItemStack.class);
        Database database = new TestDatabase();

        database.setStackInSlot(0, null);

        assertEquals(ItemStack.class, getStack.getReturnType());
        assertArrayEquals(new Class<?>[]{int.class, ItemStack.class}, setStack.getParameterTypes());
        assertEquals(1, database.size());
        assertNull(database.getStackInSlot(0));
        assertEquals(0, database.findStackWithHash("empty"));
    }

    @Test
    void keyboardUsesModernPlayerAndPersistableEnvironment() throws NoSuchMethodException {
        Method usability = Keyboard.UsabilityChecker.class.getMethod("isUsableByPlayer", Keyboard.class, Player.class);
        TestKeyboard keyboard = new TestKeyboard();

        keyboard.setUsableOverride((candidate, player) -> candidate == keyboard);

        assertTrue(Environment.class.isAssignableFrom(Keyboard.class));
        assertTrue(Persistable.class.isAssignableFrom(Keyboard.class));
        assertArrayEquals(new Class<?>[]{Keyboard.class, Player.class}, usability.getParameterTypes());
        assertTrue(keyboard.usable());
    }

    @Test
    void multiTankUsesNeoForgeFluidTank() throws NoSuchMethodException {
        Method getFluidTank = MultiTank.class.getMethod("getFluidTank", int.class);
        MultiTank multiTank = new TestMultiTank();

        assertEquals(IFluidTank.class, getFluidTank.getReturnType());
        assertEquals(0, multiTank.tankCount());
        assertNull(multiTank.getFluidTank(0));
    }

    @Test
    void tabletAndDroneUseModernPlayerAndVec3() throws NoSuchMethodException {
        Method tabletPlayer = Tablet.class.getMethod("player");
        Method droneTarget = Drone.class.getMethod("getTarget");
        Method droneSetTarget = Drone.class.getMethod("setTarget", Vec3.class);

        assertTrue(MachineHost.class.isAssignableFrom(Tablet.class));
        assertEquals(Player.class, tabletPlayer.getReturnType());
        assertEquals(Vec3.class, droneTarget.getReturnType());
        assertArrayEquals(new Class<?>[]{Vec3.class}, droneSetTarget.getParameterTypes());
    }

    private static final class TestRotatable implements Rotatable {
        @Override
        public Direction facing() {
            return Direction.NORTH;
        }

        @Override
        public Direction toGlobal(final Direction value) {
            return Direction.EAST;
        }

        @Override
        public Direction toLocal(final Direction value) {
            return Direction.WEST;
        }
    }

    private static final class TestDatabase implements Database {
        @Override
        public int size() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(final int slot) {
            return null;
        }

        @Override
        public void setStackInSlot(final int slot, final ItemStack stack) {
        }

        @Override
        public int findStackWithHash(final String hash) {
            return "empty".equals(hash) ? 0 : -1;
        }
    }

    private static final class TestKeyboard implements Keyboard {
        private UsabilityChecker checker;

        @Override
        public void setUsableOverride(final UsabilityChecker callback) {
            checker = callback;
        }

        private boolean usable() {
            return checker != null && checker.isUsableByPlayer(this, null);
        }

        @Override
        public li.cil.oc.api.network.Node node() {
            return null;
        }

        @Override
        public void onConnect(final li.cil.oc.api.network.Node node) {
        }

        @Override
        public void onDisconnect(final li.cil.oc.api.network.Node node) {
        }

        @Override
        public void onMessage(final li.cil.oc.api.network.Message message) {
        }

        @Override
        public void load(final CompoundTag nbt) {
        }

        @Override
        public void save(final CompoundTag nbt) {
        }
    }

    private static final class TestMultiTank implements MultiTank {
        @Override
        public int tankCount() {
            return 0;
        }

        @Override
        public IFluidTank getFluidTank(final int index) {
            return null;
        }
    }
}
