package li.cil.oc.common.block;

import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Network;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.blockentity.ScreenBlockEntity;
import li.cil.oc.common.component.ScreenInputDispatcher;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

final class ScreenBlockInteractionTest {
    @Test
    void mapsNorthFaceHitToScreenCell() {
        BlockHitResult hit = new BlockHitResult(new Vec3(0.25D, 0.60D, 0.0D), Direction.NORTH, BlockPos.ZERO, false);

        ScreenHitMapper.ScreenClick click = ScreenHitMapper.screenCoordinates(Direction.NORTH, BlockPos.ZERO, hit, 40, 16);

        assertArrayEquals(new int[]{6, 4}, new int[]{click.x(), click.y()});
    }

    @Test
    void ignoresHitsOutsideScreenFace() {
        BlockHitResult hit = new BlockHitResult(new Vec3(0.25D, 0.75D, 1.0D), Direction.SOUTH, BlockPos.ZERO, false);

        assertNull(ScreenHitMapper.screenCoordinates(Direction.NORTH, BlockPos.ZERO, hit, 40, 16));
    }

    @Test
    void ignoresHitsInsideScreenBorderLikeUpstream() {
        BlockHitResult hit = new BlockHitResult(new Vec3(0.05D, 0.60D, 0.0D), Direction.NORTH, BlockPos.ZERO, false);

        assertNull(ScreenHitMapper.screenCoordinates(Direction.NORTH, BlockPos.ZERO, hit, 40, 16));
    }

    @Test
    void mapsMultiblockHitThroughRenderedLetterboxLikeUpstream() {
        BlockHitResult hit = new BlockHitResult(new Vec3(0.75D, 0.60D, 0.0D), Direction.NORTH, BlockPos.ZERO, false);

        ScreenHitMapper.ScreenClick click = ScreenHitMapper.screenCoordinates(Direction.NORTH, Direction.UP, BlockPos.ZERO, hit, 80, 25, 3, 2, 2, 1);

        assertArrayEquals(new int[]{76, 0}, new int[]{click.x(), click.y()});
    }

    @Test
    void screenTerminalOpenFollowsUpstreamTouchModeInversion() {
        assertEquals(true, ScreenBlock.shouldOpenPhysicalTerminal(true, false, false));
        assertEquals(false, ScreenBlock.shouldOpenPhysicalTerminal(true, false, true));
        assertEquals(true, ScreenBlock.shouldOpenPhysicalTerminal(true, true, true));
        assertEquals(false, ScreenBlock.shouldOpenPhysicalTerminal(false, false, false));
    }

    @Test
    void physicalScreenClickEmitsOnlyTouchLikeUpstream() throws Exception {
        ScreenBlockEntity screen = allocateScreen();
        CapturingNode node = new CapturingNode();
        setField(screen, "inputDispatcher", new ScreenInputDispatcher());
        setField(screen, "node", node);

        ScreenClickHandler.clickScreen(screen, new ScreenHitMapper.ScreenClick(12, 4), null);

        assertEquals(1, node.reachableMessages.size());
        assertEquals(Arrays.asList("computer.checked_signal", null, "touch", 13, 5, 0), node.reachableMessages.getFirst());
    }

    private static ScreenBlockEntity allocateScreen() throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (ScreenBlockEntity) ((Unsafe) field.get(null)).allocateInstance(ScreenBlockEntity.class);
    }

    private static void setField(final Object target, final String name, final Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static final class CapturingNode implements Node {
        private final List<List<Object>> reachableMessages = new ArrayList<>();

        @Override public Environment host() { return null; }
        @Override public Visibility reachability() { return Visibility.Neighbors; }
        @Override public String address() { return "screen"; }
        @Override public Network network() { return null; }
        @Override public boolean isNeighborOf(final Node other) { return false; }
        @Override public boolean canBeReachedFrom(final Node other) { return false; }
        @Override public Iterable<Node> neighbors() { return List.of(); }
        @Override public Iterable<Node> reachableNodes() { return List.of(); }
        @Override public void connect(final Node node) {}
        @Override public void disconnect(final Node node) {}
        @Override public void remove() {}
        @Override public void sendToAddress(final String target, final String name, final Object... data) {}
        @Override public void sendToNeighbors(final String name, final Object... data) {}
        @Override public void sendToReachable(final String name, final Object... data) {
            List<Object> message = new ArrayList<>();
            message.add(name);
            message.addAll(Arrays.asList(data));
            reachableMessages.add(message);
        }
        @Override public void sendToVisible(final String name, final Object... data) {}
        @Override public void load(final CompoundTag nbt) {}
        @Override public void save(final CompoundTag nbt) {}
    }
}
