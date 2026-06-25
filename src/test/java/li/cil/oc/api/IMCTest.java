package li.cil.oc.api;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class IMCTest {
    @Test
    void exposesOpenComputersImcMessageKeys() {
        assertEquals("registerAssemblerFilter", IMC.REGISTER_ASSEMBLER_FILTER);
        assertEquals("registerAssemblerTemplate", IMC.REGISTER_ASSEMBLER_TEMPLATE);
        assertEquals("registerDisassemblerTemplate", IMC.REGISTER_DISASSEMBLER_TEMPLATE);
        assertEquals("registerToolDurabilityProvider", IMC.REGISTER_TOOL_DURABILITY_PROVIDER);
        assertEquals("registerWrenchTool", IMC.REGISTER_WRENCH_TOOL);
        assertEquals("registerWrenchToolCheck", IMC.REGISTER_WRENCH_TOOL_CHECK);
        assertEquals("registerItemCharge", IMC.REGISTER_ITEM_CHARGE);
        assertEquals("registerInkProvider", IMC.REGISTER_INK_PROVIDER);
        assertEquals("blacklistPeripheral", IMC.BLACKLIST_PERIPHERAL);
        assertEquals("blacklistHost", IMC.BLACKLIST_HOST);
        assertEquals("registerCustomPowerSystem", IMC.REGISTER_CUSTOM_POWER_SYSTEM);
        assertEquals("registerProgramDiskLabel", IMC.REGISTER_PROGRAM_DISK_LABEL);
    }

    @Test
    void helperMethodsKeepAddonFacingSignatures() throws NoSuchMethodException {
        assertVoidMethod("registerAssemblerFilter", String.class);
        assertVoidMethod("registerAssemblerTemplate", String.class, String.class, String.class, String.class, Class.class, int[].class, int[].class, Iterable.class);
        assertVoidMethod("registerDisassemblerTemplate", String.class, String.class, String.class);
        assertVoidMethod("registerToolDurabilityProvider", String.class);
        assertVoidMethod("registerWrenchTool", String.class);
        assertVoidMethod("registerWrenchToolCheck", String.class);
        assertVoidMethod("registerItemCharge", String.class, String.class, String.class);
        assertVoidMethod("registerInkProvider", String.class);
        assertVoidMethod("blacklistPeripheral", Class.class);
        assertVoidMethod("blacklistHost", String.class, Class.class, ItemStack.class);
        assertVoidMethod("registerProgramDiskLabel", String.class, String.class, String[].class);
    }

    @Test
    void assemblerTemplateStillAcceptsPairIterableForComponentSlots() throws NoSuchMethodException {
        Method method = IMC.class.getMethod("registerAssemblerTemplate", String.class, String.class, String.class, String.class, Class.class, int[].class, int[].class, Iterable.class);

        assertEquals(void.class, method.getReturnType());
        Pair<String, Integer> slot = Pair.of("card", 1);
        assertEquals("card", slot.getLeft());
        assertEquals(1, slot.getRight());
    }

    @Test
    void hostBlacklistPayloadUsesUpstreamNbtShape() {
        CompoundTag payload = IMC.blacklistHostPayload("computer", IMCTest.class, null);

        assertEquals("computer", payload.getString("name"));
        assertEquals(IMCTest.class.getName(), payload.getString("host"));
        CompoundTag item = payload.getCompound("item");
        assertEquals(new CompoundTag(), item);
    }

    private static void assertVoidMethod(final String name, final Class<?>... parameterTypes) throws NoSuchMethodException {
        assertEquals(void.class, IMC.class.getMethod(name, parameterTypes).getReturnType());
    }
}
