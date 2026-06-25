package li.cil.oc.api;

import org.junit.jupiter.api.Test;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ExternalAddonCompileTest {
    @Test
    void addonSourceCompilesAgainstPublishedApiJarShape() throws IOException {
        final Path apiJar = Path.of(System.getProperty("neoopencomputers.apiJar", ""));
        assertTrue(apiJar.getFileName().toString().endsWith("-api.jar"), "test task must provide the API jar path");
        assertTrue(java.nio.file.Files.isRegularFile(apiJar), "API jar must exist before addon compile smoke");

        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "Tests must run on a JDK with javac");

        final DiagnosticCollector<javax.tools.JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, Locale.ROOT, java.nio.charset.StandardCharsets.UTF_8)) {
            fileManager.setLocationFromPaths(StandardLocation.CLASS_PATH, addonClasspath(apiJar));
            fileManager.setLocationFromPaths(StandardLocation.CLASS_OUTPUT, List.of(Files.createTempDirectory("neoopencomputers-addon-compile-")));

            final javax.tools.JavaFileObject source = new SourceFile("""
                package addon;

                import li.cil.oc.api.Driver;
                import li.cil.oc.api.FileSystem;
                import li.cil.oc.api.Network;
                import li.cil.oc.api.driver.DriverItem;
                import li.cil.oc.api.machine.Arguments;
                import li.cil.oc.api.machine.Callback;
                import li.cil.oc.api.machine.Context;
                import li.cil.oc.api.network.SimpleComponent;
                import li.cil.oc.api.network.Visibility;
                import li.cil.oc.api.prefab.AbstractManagedEnvironment;
                import li.cil.oc.api.prefab.AbstractValue;
                import net.minecraft.world.item.ItemStack;

                public final class ExampleAddonComponent extends AbstractManagedEnvironment implements SimpleComponent {
                    public ExampleAddonComponent() {
                        setNode(Network.newNode(this, Visibility.Network).create());
                    }

                    @Override
                    public String getComponentName() {
                        return "example";
                    }

                    @Callback
                    public Object[] greet(final Context context, final Arguments args) {
                        return new Object[]{"Hello, " + args.checkString(0)};
                    }

                    public void register(final DriverItem driver, final ItemStack stack) {
                        Driver.add(driver);
                        FileSystem.asManagedEnvironment(FileSystem.fromMemory(1024), "addon");
                        new AbstractValue() {};
                        driver.worksWith(stack);
                    }
                }
                """);

            final JavaCompiler.CompilationTask task = compiler.getTask(
                null,
                fileManager,
                diagnostics,
                List.of("--release", "21"),
                null,
                List.of(source));

            final boolean compiled = Boolean.TRUE.equals(task.call());
            assertTrue(compiled, () -> "Addon API compile smoke failed:\n" + diagnosticsText(diagnostics));
        }
    }

    private static List<Path> addonClasspath(final Path apiJar) {
        final List<Path> classpath = new ArrayList<>();
        classpath.add(apiJar);
        for (final String element : System.getProperty("java.class.path").split(java.io.File.pathSeparator)) {
            final Path path = Path.of(element).toAbsolutePath().normalize();
            final String normalized = path.toString().replace('\\', '/');
            if (!normalized.endsWith("/build/classes/java/main") && !normalized.endsWith("/build/resources/main")) {
                classpath.add(path);
            }
        }
        return classpath;
    }

    private static String diagnosticsText(final DiagnosticCollector<javax.tools.JavaFileObject> diagnostics) {
        final StringBuilder builder = new StringBuilder();
        for (final Diagnostic<? extends javax.tools.JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
            builder
                .append(diagnostic.getKind())
                .append(" line ")
                .append(diagnostic.getLineNumber())
                .append(": ")
                .append(diagnostic.getMessage(Locale.ROOT))
                .append('\n');
        }
        return builder.toString();
    }

    private static final class SourceFile extends javax.tools.SimpleJavaFileObject {
        private final String source;

        private SourceFile(final String source) {
            super(URI.create("string:///addon/ExampleAddonComponent.java"), Kind.SOURCE);
            this.source = source;
        }

        @Override
        public CharSequence getCharContent(final boolean ignoreEncodingErrors) {
            return source;
        }
    }
}
