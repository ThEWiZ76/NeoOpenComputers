package li.cil.oc;

import java.util.List;

final class ModClassPreloader {
    private static final List<String> PRELOADED_CLASS_NAMES = List.of(
        "li.cil.oc.common.FileSystemRegistry$ResourceFileSystem$ResourceHandle",
        "li.cil.oc.common.block.ScreenHitMapper",
        "li.cil.oc.common.block.ScreenHitMapper$ScreenClick",
        "li.cil.oc.common.block.ScreenClickHandler",
        "li.cil.oc.common.component.InternetCardEnvironment$HttpTransport"
    );

    static void preload() {
        final ClassLoader loader = ModClassPreloader.class.getClassLoader();
        for (final String className : PRELOADED_CLASS_NAMES) {
            try {
                Class.forName(className, true, loader);
            } catch (final ClassNotFoundException e) {
                throw new ExceptionInInitializerError(e);
            }
        }
    }

    static List<String> preloadedClassNames() {
        return PRELOADED_CLASS_NAMES;
    }

    private ModClassPreloader() {
    }
}
