package li.cil.oc.api;

import li.cil.oc.api.detail.DriverAPI;
import li.cil.oc.api.detail.FileSystemAPI;
import li.cil.oc.api.detail.ItemAPI;
import li.cil.oc.api.detail.ItemInfo;
import li.cil.oc.api.detail.MachineAPI;
import li.cil.oc.api.detail.ManualAPI;
import li.cil.oc.api.detail.NanomachinesAPI;
import li.cil.oc.api.driver.Converter;
import li.cil.oc.api.driver.DriverBlock;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.InventoryProvider;
import li.cil.oc.api.fs.Label;
import li.cil.oc.api.machine.Architecture;
import li.cil.oc.api.machine.MachineHost;
import li.cil.oc.api.manual.ContentProvider;
import li.cil.oc.api.manual.ImageProvider;
import li.cil.oc.api.manual.ImageRenderer;
import li.cil.oc.api.manual.PathProvider;
import li.cil.oc.api.manual.TabIconRenderer;
import li.cil.oc.api.nanomachines.BehaviorProvider;
import li.cil.oc.api.nanomachines.Controller;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.items.IItemHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("deprecation")
final class StaticFacadeApiTest {
    @AfterEach
    void resetApiReferences() {
        API.driver = null;
        API.fileSystem = null;
        API.items = null;
        API.machine = null;
        API.manual = null;
        API.nanomachines = null;
    }

    @Test
    void driverFacadeDelegatesToDriverApi() {
        TestDriverAPI api = new TestDriverAPI();
        API.driver = api;
        DriverBlock block = new TestDriverBlock();
        DriverItem item = new TestDriverItem();
        Converter converter = (value, output) -> {};
        EnvironmentProvider environmentProvider = stack -> String.class;
        InventoryProvider inventoryProvider = new TestInventoryProvider();

        Driver.add(block);
        Driver.add(item);
        Driver.add(converter);
        Driver.add(environmentProvider);
        Driver.add(inventoryProvider);

        assertSame(block, Driver.driverFor(null, BlockPos.ZERO, Direction.NORTH));
        assertSame(item, Driver.driverFor(null, EnvironmentHost.class));
        assertSame(item, Driver.driverFor((ItemStack) null));
        assertEquals(String.class, Driver.environmentFor(null));
        assertEquals(Set.of(String.class), Driver.environmentsFor(null));
        assertNull(Driver.itemHandlerFor(null, null));
        assertEquals(List.of(item), Driver.itemDrivers());
        assertSame(converter, api.converter);
        assertSame(environmentProvider, api.environmentProvider);
        assertSame(inventoryProvider, api.inventoryProvider);
    }

    @Test
    void itemFacadeDelegatesToItemApi() {
        TestItemAPI api = new TestItemAPI();
        API.items = api;
        Callable<li.cil.oc.api.fs.FileSystem> factory = () -> null;

        assertSame(api.info, Items.get("disk"));
        assertSame(api.info, Items.get((ItemStack) null));
        assertNull(Items.registerFloppy("loot", DyeColor.BLUE, factory, true));
        assertEquals("loot", api.floppyName);
        assertEquals(DyeColor.BLUE, api.floppyColor);
        assertSame(factory, api.floppyFactory);
        assertTrue(api.recipeCycling);
        assertNull(Items.registerEEPROM("bios", new byte[]{1}, new byte[]{2}, true));
        assertEquals("bios", api.eepromName);
        assertTrue(api.readonly);
    }

    @Test
    void machineFacadeDelegatesToMachineApi() {
        TestMachineAPI api = new TestMachineAPI();
        API.machine = api;

        Machine.add(TestArchitecture.class);

        assertSame(TestArchitecture.class, api.architecture);
        assertEquals(List.of(TestArchitecture.class), Machine.architectures());
        assertEquals("test-architecture", Machine.getArchitectureName(TestArchitecture.class));
        assertNull(Machine.create(null));
        assertNull(api.host);
    }

    @Test
    void manualFacadeDelegatesToManualApi() {
        TestManualAPI api = new TestManualAPI();
        API.manual = api;
        TabIconRenderer tab = () -> {};
        PathProvider pathProvider = new TestPathProvider();
        ContentProvider contentProvider = path -> List.of("custom");
        ImageProvider imageProvider = path -> api.renderer;

        Manual.addTab(tab, "tooltip", "index");
        Manual.addProvider(pathProvider);
        Manual.addProvider(contentProvider);
        Manual.addProvider("chart", imageProvider);

        assertSame(tab, api.tab);
        assertSame(pathProvider, api.pathProvider);
        assertSame(contentProvider, api.contentProvider);
        assertSame(imageProvider, api.imageProvider);
        assertEquals("item/path", Manual.pathFor((ItemStack) null));
        assertEquals("block/path", Manual.pathFor(null, BlockPos.ZERO));
        assertIterableEquals(List.of("line"), Manual.contentFor("page"));
        assertSame(api.renderer, Manual.imageFor("chart:data"));
        Manual.openFor(null);
        Manual.reset();
        Manual.navigate("next");
        assertTrue(api.opened);
        assertTrue(api.reset);
        assertEquals("next", api.navigatedPath);
    }

    @Test
    void nanomachinesFacadeDelegatesToNanomachinesApi() {
        TestNanomachinesAPI api = new TestNanomachinesAPI();
        API.nanomachines = api;
        BehaviorProvider provider = new TestBehaviorProvider();

        Nanomachines.addProvider(provider);

        assertSame(provider, api.provider);
        assertIterableEquals(List.of(provider), Nanomachines.getProviders());
        assertTrue(Nanomachines.hasController(null));
        assertNull(Nanomachines.getController(null));
        assertNull(Nanomachines.installController(null));
        Nanomachines.uninstallController(null);
        assertTrue(api.uninstalled);
    }

    @Test
    void networkPacketFactoryKeepsVarargsAddonSignature() throws NoSuchMethodException {
        assertTrue(Network.class.getMethod("newPacket", String.class, String.class, int.class, Object[].class).isVarArgs());
    }

    @Test
    void fileSystemFacadeDelegatesToFileSystemApiAndConvenienceOverloadsUseDefaults() {
        TestFileSystemAPI api = new TestFileSystemAPI();
        API.fileSystem = api;
        Label label = new TestLabel();

        assertNull(FileSystem.fromClass(StaticFacadeApiTest.class, "neoopencomputers", "loot"));
        assertEquals(StaticFacadeApiTest.class, api.clazz);
        assertEquals("neoopencomputers", api.domain);
        assertEquals("loot", api.root);
        assertNull(FileSystem.fromSaveDirectory("drive", 1024, false));
        assertFalse(api.buffered);
        assertNull(FileSystem.fromSaveDirectory("buffered", 2048));
        assertTrue(api.buffered);
        assertNull(FileSystem.fromMemory(512));
        assertEquals(512, api.capacity);
        assertNull(FileSystem.asReadOnly(null));
        assertTrue(api.readOnlyWrapped);

        assertNull(FileSystem.asManagedEnvironment(null, label, null, "sound", 3));
        assertSame(label, api.label);
        assertEquals(3, api.speed);
        assertNull(FileSystem.asManagedEnvironment(null, "readonly", null, "sound", 4));
        assertEquals("readonly", api.labelName);
        assertEquals(4, api.speed);
        assertNull(FileSystem.asManagedEnvironment(null, label, null, "sound"));
        assertEquals(1, api.speed);
        assertNull(FileSystem.asManagedEnvironment(null, "readonly", null, "sound"));
        assertEquals(1, api.speed);
        assertNull(FileSystem.asManagedEnvironment(null, label));
        assertSame(label, api.label);
        assertNull(api.accessSound);
        assertNull(FileSystem.asManagedEnvironment(null, "readonly"));
        assertEquals("readonly", api.labelName);
        assertNull(api.host);
        assertNull(FileSystem.asManagedEnvironment(null));
        assertNull(api.label);
        assertNull(api.labelName);
    }

    @Test
    void facadesReturnSafeFallbacksWhenBackingApiIsMissing() {
        assertNull(Driver.driverFor(null, BlockPos.ZERO, Direction.NORTH));
        assertNull(Driver.driverFor(null, EnvironmentHost.class));
        assertNull(Driver.driverFor((ItemStack) null));
        assertNull(Driver.environmentFor(null));
        assertNull(Driver.environmentsFor(null));
        assertNull(Driver.itemHandlerFor(null, null));
        assertNull(Driver.itemDrivers());
        assertNull(Items.get("missing"));
        assertNull(Items.get((ItemStack) null));
        assertNull(Items.registerFloppy("missing", DyeColor.WHITE, () -> null, false));
        assertNull(Items.registerEEPROM("missing", null, null, false));
        assertEquals(List.of(), Machine.architectures());
        assertNull(Machine.getArchitectureName(TestArchitecture.class));
        assertNull(Machine.create(null));
        assertNull(Manual.pathFor((ItemStack) null));
        assertNull(Manual.pathFor(null, BlockPos.ZERO));
        assertNull(Manual.contentFor("missing"));
        assertNull(Manual.imageFor("missing"));
        assertIterableEquals(List.of(), Nanomachines.getProviders());
        assertFalse(Nanomachines.hasController(null));
        assertNull(Nanomachines.getController(null));
        assertNull(Nanomachines.installController(null));
        assertNull(FileSystem.fromMemory(1));
        assertNull(FileSystem.asManagedEnvironment(null));
    }

    private static final class TestDriverAPI implements DriverAPI {
        private DriverBlock block;
        private DriverItem item;
        private Converter converter;
        private EnvironmentProvider environmentProvider;
        private InventoryProvider inventoryProvider;

        @Override
        public void add(final DriverBlock driver) {
            block = driver;
        }

        @Override
        public void add(final DriverItem driver) {
            item = driver;
        }

        @Override
        public void add(final Converter converter) {
            this.converter = converter;
        }

        @Override
        public void add(final EnvironmentProvider provider) {
            environmentProvider = provider;
        }

        @Override
        public void add(final InventoryProvider provider) {
            inventoryProvider = provider;
        }

        @Override
        public DriverBlock driverFor(final Level world, final BlockPos pos, final Direction side) {
            return block;
        }

        @Override
        public DriverItem driverFor(final ItemStack stack, final Class<? extends EnvironmentHost> host) {
            return item;
        }

        @Override
        public DriverItem driverFor(final ItemStack stack) {
            return item;
        }

        @Override
        public Class<?> environmentFor(final ItemStack stack) {
            return environmentProvider.getEnvironment(stack);
        }

        @Override
        public Set<Class<?>> environmentsFor(final ItemStack stack) {
            return Set.of(environmentFor(stack));
        }

        @Override
        public IItemHandler itemHandlerFor(final ItemStack stack, final Player player) {
            return null;
        }

        @Override
        public Collection<DriverItem> itemDrivers() {
            return List.of(item);
        }
    }

    private static final class TestItemAPI implements ItemAPI {
        private final ItemInfo info = new TestItemInfo();
        private String floppyName;
        private DyeColor floppyColor;
        private Callable<li.cil.oc.api.fs.FileSystem> floppyFactory;
        private boolean recipeCycling;
        private String eepromName;
        private boolean readonly;

        @Override
        public ItemInfo get(final String name) {
            return info;
        }

        @Override
        public ItemInfo get(final ItemStack stack) {
            return info;
        }

        @Override
        public ItemStack registerFloppy(final String name, final DyeColor color, final Callable<li.cil.oc.api.fs.FileSystem> factory, final boolean doRecipeCycling) {
            floppyName = name;
            floppyColor = color;
            floppyFactory = factory;
            recipeCycling = doRecipeCycling;
            return null;
        }

        @Override
        public ItemStack registerEEPROM(final String name, final byte[] code, final byte[] data, final boolean readonly) {
            eepromName = name;
            this.readonly = readonly;
            return null;
        }
    }

    private static final class TestMachineAPI implements MachineAPI {
        private Class<? extends Architecture> architecture;
        private MachineHost host;

        @Override
        public void add(final Class<? extends Architecture> architecture) {
            this.architecture = architecture;
        }

        @Override
        public Collection<Class<? extends Architecture>> architectures() {
            return List.of(architecture);
        }

        @Override
        public String getArchitectureName(final Class<? extends Architecture> architecture) {
            return "test-architecture";
        }

        @Override
        public li.cil.oc.api.machine.Machine create(final MachineHost host) {
            this.host = host;
            return null;
        }
    }

    private static final class TestManualAPI implements ManualAPI {
        private final ImageRenderer renderer = new TestImageRenderer();
        private TabIconRenderer tab;
        private PathProvider pathProvider;
        private ContentProvider contentProvider;
        private ImageProvider imageProvider;
        private boolean opened;
        private boolean reset;
        private String navigatedPath;

        @Override
        public void addTab(final TabIconRenderer renderer, final String tooltip, final String path) {
            tab = renderer;
        }

        @Override
        public void addProvider(final PathProvider provider) {
            pathProvider = provider;
        }

        @Override
        public void addProvider(final ContentProvider provider) {
            contentProvider = provider;
        }

        @Override
        public void addProvider(final String prefix, final ImageProvider provider) {
            imageProvider = provider;
        }

        @Override
        public String pathFor(final ItemStack stack) {
            return "item/path";
        }

        @Override
        public String pathFor(final Level world, final BlockPos pos) {
            return "block/path";
        }

        @Override
        public Iterable<String> contentFor(final String path) {
            return List.of("line");
        }

        @Override
        public ImageRenderer imageFor(final String path) {
            return renderer;
        }

        @Override
        public void openFor(final Player player) {
            opened = true;
        }

        @Override
        public void reset() {
            reset = true;
        }

        @Override
        public void navigate(final String path) {
            navigatedPath = path;
        }
    }

    private static final class TestNanomachinesAPI implements NanomachinesAPI {
        private BehaviorProvider provider;
        private boolean uninstalled;

        @Override
        public void addProvider(final BehaviorProvider provider) {
            this.provider = provider;
        }

        @Override
        public Iterable<BehaviorProvider> getProviders() {
            return List.of(provider);
        }

        @Override
        public boolean hasController(final Player player) {
            return true;
        }

        @Override
        public Controller getController(final Player player) {
            return null;
        }

        @Override
        public Controller installController(final Player player) {
            return null;
        }

        @Override
        public void uninstallController(final Player player) {
            uninstalled = true;
        }
    }

    private static final class TestFileSystemAPI implements FileSystemAPI {
        private Class<?> clazz;
        private String domain;
        private String root;
        private long capacity;
        private boolean buffered;
        private boolean readOnlyWrapped;
        private Label label;
        private String labelName;
        private EnvironmentHost host;
        private String accessSound;
        private int speed;

        @Override
        public li.cil.oc.api.fs.FileSystem fromClass(final Class<?> clazz, final String domain, final String root) {
            this.clazz = clazz;
            this.domain = domain;
            this.root = root;
            return null;
        }

        @Override
        public li.cil.oc.api.fs.FileSystem fromSaveDirectory(final String root, final long capacity, final boolean buffered) {
            this.root = root;
            this.capacity = capacity;
            this.buffered = buffered;
            return null;
        }

        @Override
        public li.cil.oc.api.fs.FileSystem fromMemory(final long capacity) {
            this.capacity = capacity;
            return null;
        }

        @Override
        public li.cil.oc.api.fs.FileSystem asReadOnly(final li.cil.oc.api.fs.FileSystem fileSystem) {
            readOnlyWrapped = true;
            return null;
        }

        @Override
        public ManagedEnvironment asManagedEnvironment(final li.cil.oc.api.fs.FileSystem fileSystem, final Label label, final EnvironmentHost host, final String accessSound, final int speed) {
            this.label = label;
            this.labelName = null;
            this.host = host;
            this.accessSound = accessSound;
            this.speed = speed;
            return null;
        }

        @Override
        public ManagedEnvironment asManagedEnvironment(final li.cil.oc.api.fs.FileSystem fileSystem, final String label, final EnvironmentHost host, final String accessSound, final int speed) {
            this.label = null;
            labelName = label;
            this.host = host;
            this.accessSound = accessSound;
            this.speed = speed;
            return null;
        }
    }

    private static final class TestDriverBlock implements DriverBlock {
        @Override
        public boolean worksWith(final Level world, final BlockPos pos, final Direction side) {
            return true;
        }

        @Override
        public ManagedEnvironment createEnvironment(final Level world, final BlockPos pos, final Direction side) {
            return null;
        }
    }

    private static final class TestDriverItem implements DriverItem {
        @Override
        public boolean worksWith(final ItemStack stack) {
            return true;
        }

        @Override
        public ManagedEnvironment createEnvironment(final ItemStack stack, final EnvironmentHost host) {
            return null;
        }

        @Override
        public String slot(final ItemStack stack) {
            return "test";
        }

        @Override
        public int tier(final ItemStack stack) {
            return 0;
        }

        @Override
        public CompoundTag dataTag(final ItemStack stack) {
            return null;
        }
    }

    private static final class TestInventoryProvider implements InventoryProvider {
        @Override
        public boolean worksWith(final ItemStack stack, final Player player) {
            return false;
        }

        @Override
        public net.minecraft.world.Container getInventory(final ItemStack stack, final Player player) {
            return null;
        }
    }

    private static final class TestItemInfo implements ItemInfo {
        @Override
        public String name() {
            return "disk";
        }

        @Override
        public Block block() {
            return null;
        }

        @Override
        public Item item() {
            return null;
        }

        @Override
        public ItemStack createItemStack(final int size) {
            return null;
        }
    }

    private static final class TestPathProvider implements PathProvider {
        @Override
        public String pathFor(final ItemStack stack) {
            return "item/path";
        }

        @Override
        public String pathFor(final Level world, final BlockPos pos) {
            return "block/path";
        }
    }

    private static final class TestImageRenderer implements ImageRenderer {
        @Override
        public int getWidth() {
            return 16;
        }

        @Override
        public int getHeight() {
            return 16;
        }

        @Override
        public void render(final int mouseX, final int mouseY) {
        }
    }

    private static final class TestBehaviorProvider implements BehaviorProvider {
        @Override
        public Iterable<li.cil.oc.api.nanomachines.Behavior> createBehaviors(final Player player) {
            return List.of();
        }

        @Override
        public CompoundTag writeToNBT(final li.cil.oc.api.nanomachines.Behavior behavior) {
            return new CompoundTag();
        }

        @Override
        public li.cil.oc.api.nanomachines.Behavior readFromNBT(final Player player, final CompoundTag nbt) {
            return null;
        }
    }

    private static final class TestLabel implements Label {
        @Override
        public String getLabel() {
            return "test";
        }

        @Override
        public void setLabel(final String value) {
        }

        @Override
        public void load(final CompoundTag nbt) {
        }

        @Override
        public void save(final CompoundTag nbt) {
        }
    }

    private static final class TestArchitecture implements Architecture {
        @Override
        public boolean isInitialized() {
            return false;
        }

        @Override
        public boolean recomputeMemory(final Iterable<ItemStack> components) {
            return false;
        }

        @Override
        public boolean initialize() {
            return false;
        }

        @Override
        public void close() {
        }

        @Override
        public void runSynchronized() {
        }

        @Override
        public li.cil.oc.api.machine.ExecutionResult runThreaded(final boolean isSynchronizedReturn) {
            return null;
        }

        @Override
        public void onSignal() {
        }

        @Override
        public void onConnect() {
        }

        @Override
        public void load(final CompoundTag nbt) {
        }

        @Override
        public void save(final CompoundTag nbt) {
        }
    }
}
