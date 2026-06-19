package li.cil.oc.api.event;

import li.cil.oc.api.internal.Agent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RobotEventApiTest {
    @Test
    void robotEventBaseUsesNeoForgeEventBus() {
        RobotEvent event = new RobotExhaustionEvent(null, 1.5);

        assertInstanceOf(Event.class, event);
        assertNull(event.agent);
    }

    @Test
    void robotMoveEventsExposeDirectionAndPreIsCancelable() {
        RobotMoveEvent.Pre pre = new RobotMoveEvent.Pre(null, Direction.NORTH);
        RobotMoveEvent.Post post = new RobotMoveEvent.Post(null, Direction.SOUTH);

        assertEquals(Direction.NORTH, pre.direction);
        assertEquals(Direction.SOUTH, post.direction);
        assertCancelable(pre);
    }

    @Test
    void robotBreakBlockEventsUseModernLevelAndBlockPosAndClampBreakTime() {
        RobotBreakBlockEvent.Pre pre = new RobotBreakBlockEvent.Pre(null, null, BlockPos.ZERO, 2);
        RobotBreakBlockEvent.Post post = new RobotBreakBlockEvent.Post(null, 3);

        assertNull(pre.world);
        assertSame(BlockPos.ZERO, pre.pos);
        assertEquals(2, pre.getBreakTime());
        pre.setBreakTime(-1);
        assertEquals(0.05, pre.getBreakTime());
        assertEquals(3, post.experience);
        assertCancelable(pre);
    }

    @Test
    void robotPlaceBlockEventsUseModernItemStackLevelAndBlockPos() {
        RobotPlaceBlockEvent.Pre pre = new RobotPlaceBlockEvent.Pre(null, null, null, BlockPos.ZERO);
        RobotPlaceBlockEvent.Post post = new RobotPlaceBlockEvent.Post(null, null, null, BlockPos.ZERO);

        assertNull(pre.stack);
        assertNull(pre.world);
        assertSame(BlockPos.ZERO, pre.pos);
        assertSame(BlockPos.ZERO, post.pos);
        assertCancelable(pre);
    }

    @Test
    void robotAttackEntityPreIsCancelable() {
        RobotAttackEntityEvent.Pre pre = new RobotAttackEntityEvent.Pre(null, null);
        RobotAttackEntityEvent.Post post = new RobotAttackEntityEvent.Post(null, null);

        assertNull(pre.target);
        assertNull(post.target);
        assertCancelable(pre);
    }

    @Test
    void robotAnalyzeEventUsesModernPlayerType() throws NoSuchMethodException {
        Constructor<RobotAnalyzeEvent> constructor = RobotAnalyzeEvent.class.getConstructor(Agent.class, Player.class);
        RobotAnalyzeEvent event = new RobotAnalyzeEvent(null, null);

        assertEquals(Player.class, constructor.getParameterTypes()[1]);
        assertNull(event.player);
    }

    @Test
    void robotToolUseEventsClampDamageRate() {
        RobotUsedToolEvent.ComputeDamageRate compute = new RobotUsedToolEvent.ComputeDamageRate(null, null, null, 0.5);
        RobotUsedToolEvent.ApplyDamageRate apply = new RobotUsedToolEvent.ApplyDamageRate(null, null, null, 0.25);

        assertNull(compute.toolBeforeUse);
        assertNull(compute.toolAfterUse);
        assertEquals(0.5, compute.getDamageRate());
        compute.setDamageRate(-1);
        assertEquals(0, compute.getDamageRate());
        compute.setDamageRate(2);
        assertEquals(1, compute.getDamageRate());
        assertEquals(0.25, apply.getDamageRate());
    }

    @Test
    void robotPlaceInAirEventDefaultsToDeniedAndCanBeAllowed() {
        RobotPlaceInAirEvent event = new RobotPlaceInAirEvent(null);

        assertFalse(event.isAllowed());
        event.setAllowed(true);
        assertTrue(event.isAllowed());
    }

    @Test
    void robotEventConstructorsExposeModernMinecraftTypes() throws NoSuchMethodException {
        RobotBreakBlockEvent.Pre.class.getConstructor(Agent.class, Level.class, BlockPos.class, double.class);
        RobotPlaceBlockEvent.Pre.class.getConstructor(Agent.class, ItemStack.class, Level.class, BlockPos.class);
        RobotAttackEntityEvent.Pre.class.getConstructor(Agent.class, Entity.class);
    }

    private static void assertCancelable(final Event event) {
        ICancellableEvent cancellable = assertInstanceOf(ICancellableEvent.class, event);
        assertFalse(cancellable.isCanceled());
        cancellable.setCanceled(true);
        assertTrue(cancellable.isCanceled());
    }
}
