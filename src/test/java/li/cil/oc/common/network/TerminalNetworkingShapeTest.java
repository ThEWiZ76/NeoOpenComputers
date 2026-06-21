package li.cil.oc.common.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TerminalNetworkingShapeTest {
    @Test
    void exposesRegisterPayloadHandlerEntryPoint() throws NoSuchMethodException {
        final Method register = TerminalNetworking.class.getMethod("register", RegisterPayloadHandlersEvent.class);

        assertEquals(void.class, register.getReturnType());
    }

    @Test
    void exposesTerminalKeyApplyEntryPoint() throws NoSuchMethodException {
        final Method apply = TerminalNetworking.class.getDeclaredMethod(
            "applyTerminalKey",
            net.minecraft.world.inventory.AbstractContainerMenu.class,
            TerminalKeyPayload.class,
            net.minecraft.world.entity.player.Player.class);

        assertEquals(void.class, apply.getReturnType());
    }

    @Test
    void exposesTerminalClipboardApplyEntryPoint() throws NoSuchMethodException {
        final Method apply = TerminalNetworking.class.getDeclaredMethod(
            "applyTerminalClipboard",
            net.minecraft.world.inventory.AbstractContainerMenu.class,
            TerminalClipboardPayload.class,
            net.minecraft.world.entity.player.Player.class);

        assertEquals(void.class, apply.getReturnType());
    }

    @Test
    void exposesTerminalMouseApplyEntryPoint() throws NoSuchMethodException {
        final Method apply = TerminalNetworking.class.getDeclaredMethod(
            "applyTerminalMouse",
            net.minecraft.world.inventory.AbstractContainerMenu.class,
            TerminalMousePayload.class,
            net.minecraft.world.entity.player.Player.class);

        assertEquals(void.class, apply.getReturnType());
    }

    @Test
    void exposesTerminalMouseBoundsHelper() throws NoSuchMethodException {
        final Method helper = TerminalNetworking.class.getDeclaredMethod(
            "mouseInside",
            li.cil.oc.common.component.TerminalScreenSnapshot.class,
            TerminalMousePayload.class);

        assertEquals(boolean.class, helper.getReturnType());
    }

    @Test
    void exposesTerminalInputReadinessHelper() throws NoSuchMethodException {
        final Method helper = TerminalNetworking.class.getDeclaredMethod(
            "acceptsTerminalInput",
            li.cil.oc.common.component.TerminalScreenSnapshot.class);

        assertEquals(boolean.class, helper.getReturnType());
    }
}
