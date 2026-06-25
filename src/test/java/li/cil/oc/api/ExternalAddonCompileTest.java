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
                import li.cil.oc.api.Machine;
                import li.cil.oc.api.Network;
                import li.cil.oc.api.driver.Converter;
                import li.cil.oc.api.driver.EnvironmentProvider;
                import li.cil.oc.api.driver.DriverItem;
                import li.cil.oc.api.driver.InventoryProvider;
                import li.cil.oc.api.driver.MethodWhitelist;
                import li.cil.oc.api.driver.NamedBlock;
                import li.cil.oc.api.machine.Arguments;
                import li.cil.oc.api.machine.Architecture;
                import li.cil.oc.api.machine.Callback;
                import li.cil.oc.api.machine.Context;
                import li.cil.oc.api.machine.ExecutionResult;
                import li.cil.oc.api.nanomachines.Behavior;
                import li.cil.oc.api.nanomachines.DisableReason;
                import li.cil.oc.api.network.EnvironmentHost;
                import li.cil.oc.api.network.ManagedEnvironment;
                import li.cil.oc.api.network.SimpleComponent;
                import li.cil.oc.api.network.Visibility;
                import li.cil.oc.api.prefab.AbstractBehavior;
                import li.cil.oc.api.prefab.AbstractManagedEnvironment;
                import li.cil.oc.api.prefab.AbstractProvider;
                import li.cil.oc.api.prefab.AbstractValue;
                import net.minecraft.core.BlockPos;
                import net.minecraft.core.Direction;
                import net.minecraft.nbt.CompoundTag;
                import net.minecraft.world.Container;
                import net.minecraft.world.SimpleContainer;
                import net.minecraft.world.entity.player.Player;
                import net.minecraft.world.item.ItemStack;
                import net.minecraft.world.item.Items;
                import net.minecraft.world.level.Level;
                import net.minecraft.world.level.block.entity.BlockEntity;

                import java.util.List;
                import java.util.Map;

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
                        Driver.add(new ExampleBlockDriver());
                        Driver.add(new ExampleItemDriver());
                        Driver.add(new LegacySidedBlockDriver());
                        Driver.add(new LegacyItemDriver());
                        Driver.add(new LegacyBlockDriver());
                        Driver.add(new LegacyPrefabBlockDriver());
                        Driver.add(new ExampleTileEntityDriver());
                        Driver.add(new LegacyTileEntityDriver());
                        Driver.add(new ExampleConverter());
                        Driver.add(new ExampleEnvironmentProvider());
                        Driver.add(new ExampleInventoryProvider());
                        Machine.add(ExampleArchitecture.class);
                        FileSystem.asManagedEnvironment(FileSystem.fromMemory(1024), "addon");
                        new AbstractValue() {};
                        new ExampleNamedBlock().preferredName();
                        new ExampleWhitelist().whitelistedMethods();
                        new ExampleNanomachineProvider().writeToNBT(new ExampleBehavior(playerFromHost(hostFromStack(stack))));
                        driver.worksWith(stack);
                    }

                    private static EnvironmentHost hostFromStack(final ItemStack stack) {
                        return null;
                    }

                    private static Player playerFromHost(final EnvironmentHost host) {
                        return null;
                    }

                    public static final class ExampleBlockDriver extends li.cil.oc.api.prefab.DriverSidedBlock {
                        public ExampleBlockDriver() {
                            super(new ItemStack(Items.STONE));
                        }

                        @Override
                        public ManagedEnvironment createEnvironment(final Level world, final BlockPos pos, final Direction side) {
                            return new ExampleAddonComponent();
                        }
                    }

                    @SuppressWarnings("deprecation")
                    public static final class LegacySidedBlockDriver implements li.cil.oc.api.driver.SidedBlock {
                        @Override
                        public boolean worksWith(final Level world, final BlockPos pos, final Direction side) {
                            return world != null && pos != null;
                        }

                        @Override
                        public ManagedEnvironment createEnvironment(final Level world, final BlockPos pos, final Direction side) {
                            return new ExampleAddonComponent();
                        }
                    }

                    @SuppressWarnings("deprecation")
                    public static final class LegacyBlockDriver implements li.cil.oc.api.driver.Block {
                        @Override
                        public boolean worksWith(final Level world, final BlockPos pos) {
                            return world != null && pos != null;
                        }

                        @Override
                        public ManagedEnvironment createEnvironment(final Level world, final BlockPos pos) {
                            return new ExampleAddonComponent();
                        }
                    }

                    @SuppressWarnings("deprecation")
                    public static final class LegacyPrefabBlockDriver extends li.cil.oc.api.prefab.DriverBlock {
                        public LegacyPrefabBlockDriver() {
                            super(new ItemStack(Items.STONE));
                        }

                        @Override
                        public ManagedEnvironment createEnvironment(final Level world, final BlockPos pos, final Direction side) {
                            return new ExampleAddonComponent();
                        }
                    }

                    public static final class ExampleTileEntityDriver extends li.cil.oc.api.prefab.DriverSidedTileEntity {
                        @Override
                        public Class<?> getTileEntityClass() {
                            return BlockEntity.class;
                        }

                        @Override
                        public ManagedEnvironment createEnvironment(final Level world, final BlockPos pos, final Direction side) {
                            return new ExampleAddonComponent();
                        }
                    }

                    @SuppressWarnings("deprecation")
                    public static final class LegacyTileEntityDriver extends li.cil.oc.api.prefab.DriverTileEntity {
                        @Override
                        public Class<?> getTileEntityClass() {
                            return BlockEntity.class;
                        }

                        @Override
                        public ManagedEnvironment createEnvironment(final Level world, final BlockPos pos, final Direction side) {
                            return new ExampleAddonComponent();
                        }
                    }

                    public static final class ExampleItemDriver extends li.cil.oc.api.prefab.DriverItem {
                        public ExampleItemDriver() {
                            super(new ItemStack(Items.STICK));
                        }

                        @Override
                        public ManagedEnvironment createEnvironment(final ItemStack stack, final EnvironmentHost host) {
                            return new ExampleAddonComponent();
                        }

                        @Override
                        public String slot(final ItemStack stack) {
                            return "card";
                        }
                    }

                    @SuppressWarnings("deprecation")
                    public static final class LegacyItemDriver implements li.cil.oc.api.driver.Item {
                        @Override
                        public boolean worksWith(final ItemStack stack) {
                            return stack != null;
                        }

                        @Override
                        public ManagedEnvironment createEnvironment(final ItemStack stack, final EnvironmentHost host) {
                            return new ExampleAddonComponent();
                        }

                        @Override
                        public String slot(final ItemStack stack) {
                            return "upgrade";
                        }

                        @Override
                        public int tier(final ItemStack stack) {
                            return 0;
                        }

                        @Override
                        public CompoundTag dataTag(final ItemStack stack) {
                            return new CompoundTag();
                        }
                    }

                    public static final class ExampleEnvironmentProvider implements EnvironmentProvider {
                        @Override
                        public Class<?> getEnvironment(final ItemStack stack) {
                            return ExampleAddonComponent.class;
                        }
                    }

                    public static final class ExampleInventoryProvider implements InventoryProvider {
                        @Override
                        public boolean worksWith(final ItemStack stack, final Player player) {
                            return !stack.isEmpty();
                        }

                        @Override
                        public Container getInventory(final ItemStack stack, final Player player) {
                            return new SimpleContainer(1);
                        }
                    }

                    public static final class ExampleConverter implements Converter {
                        @Override
                        public void convert(final Object value, final Map<Object, Object> output) {
                            output.put("class", value.getClass().getName());
                        }
                    }

                    public static final class ExampleNamedBlock implements NamedBlock {
                        @Override
                        public String preferredName() {
                            return "example";
                        }

                        @Override
                        public int priority() {
                            return 0;
                        }
                    }

                    public static final class ExampleWhitelist implements MethodWhitelist {
                        @Override
                        public String[] whitelistedMethods() {
                            return new String[]{"getValue"};
                        }
                    }

                    public static final class ExampleBehavior extends AbstractBehavior {
                        public ExampleBehavior(final Player player) {
                            super(player);
                        }

                        @Override
                        public String getNameHint() {
                            return "example";
                        }

                        @Override
                        public void onDisable(final DisableReason reason) {
                        }
                    }

                    public static final class ExampleNanomachineProvider extends AbstractProvider {
                        public ExampleNanomachineProvider() {
                            super("example");
                        }

                        @Override
                        public Iterable<Behavior> createBehaviors(final Player player) {
                            return List.of(new ExampleBehavior(player));
                        }

                        @Override
                        protected Behavior readBehaviorFromNBT(final Player player, final CompoundTag nbt) {
                            return new ExampleBehavior(player);
                        }
                    }

                    @Architecture.Name("Example")
                    public static final class ExampleArchitecture implements Architecture {
                        @Override public boolean isInitialized() { return true; }
                        @Override public boolean recomputeMemory(final Iterable<ItemStack> components) { return true; }
                        @Override public boolean initialize() { return true; }
                        @Override public void close() {}
                        @Override public void runSynchronized() {}
                        @Override public ExecutionResult runThreaded(final boolean isSynchronizedReturn) { return new ExecutionResult.Sleep(0); }
                        @Override public void onSignal() {}
                        @Override public void onConnect() {}
                        @Override public void load(final CompoundTag nbt) {}
                        @Override public void save(final CompoundTag nbt) {}
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
