package li.cil.oc;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ModClassPreloaderTest {
    @Test
    void preloadsKnownLiveJarLazyLoadCrashClasses() throws Exception {
        final String modSource = Files.readString(Path.of("src/main/java/li/cil/oc/NeoOpenComputers.java"));
        final List<String> classNames = ModClassPreloader.preloadedClassNames();

        assertTrue(modSource.contains("ModClassPreloader.preload();"));
        assertTrue(classNames.contains("li.cil.oc.common.FileSystemRegistry$ResourceFileSystem$ResourceHandle"));
        assertTrue(classNames.contains("li.cil.oc.common.block.ScreenHitMapper"));
        assertTrue(classNames.contains("li.cil.oc.common.block.ScreenHitMapper$ScreenClick"));
        assertTrue(classNames.contains("li.cil.oc.common.block.ScreenClickHandler"));
        assertTrue(classNames.contains("li.cil.oc.common.component.InternetCardEnvironment$HttpTransport"));
        assertTrue(classNames.contains("li.cil.oc.common.component.InternetCardEnvironment$HttpResponse"));
        assertTrue(classNames.contains("li.cil.oc.common.component.InternetCardEnvironment$HttpRequest"));
        assertTrue(classNames.contains("li.cil.oc.common.component.InternetCardEnvironment$TcpAddress"));
        assertTrue(classNames.contains("li.cil.oc.common.component.InternetCardEnvironment$TcpSocket"));
        assertTrue(classNames.contains("li.cil.oc.common.component.InternetCardEnvironment$InternetFilteringRule"));
        assertTrue(classNames.contains("li.cil.oc.common.component.InternetCardEnvironment$RulePredicate"));
        assertTrue(classNames.contains("li.cil.oc.common.component.InternetCardEnvironment$InetAddressRange"));
        assertTrue(classNames.contains("li.cil.oc.common.component.TerminalScreenDelta$Row"));
        assertTrue(classNames.contains("li.cil.oc.client.TerminalScreen$TextCell"));
        assertTrue(classNames.contains("li.cil.oc.common.item.data.PrintRenderModel"));
        assertTrue(classNames.contains("li.cil.oc.common.item.data.PrintRenderModel$RenderShape"));

        for (final String className : classNames) {
            assertEquals(className, Class.forName(className, false, ModClassPreloader.class.getClassLoader()).getName());
        }
    }
}
