package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.common.machine.NativeLuaState;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(NeoOpenComputers.MODID)
@PrefixGameTestTemplate(false)
public final class NativeLuaLoadingGameTests {
    @GameTest(template = "empty")
    public static void nativeLuaLoadsInsideNeoForgeModuleLayer(final GameTestHelper helper) throws Exception {
        for (final var version : NativeLuaState.Version.values()) {
            try (final var nativeState = NativeLuaState.create(version, 1024 * 1024)) {
                final var lua = nativeState.state();
                helper.assertTrue(lua.getClass().getClassLoader() == NativeLuaState.class.getClassLoader(),
                    "JNI and native loader classes are in different module layers");
                lua.load("assert(io == nil and debug == nil and eris == nil); return 6 * 7", "=native-module-test");
                lua.call(0, 1);
                helper.assertTrue(lua.toInteger(-1) == 42, "Native VM failed inside NeoForge: " + version);
            }
        }
        helper.succeed();
    }
}
