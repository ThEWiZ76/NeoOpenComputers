package li.cil.oc.common;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.API;
import li.cil.oc.api.Items;
import li.cil.oc.api.detail.ItemAPI;
import li.cil.oc.api.fs.FileSystem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;
import java.util.concurrent.Callable;

public final class ModLootDisks {
    static final String LOOT_ROOT = "loot";
    static final String LOOT_PROPERTIES = LOOT_ROOT + "/loot.properties";
    static final String OPENOS_PATH = "openos";

    public static void registerDefaults() {
        for (Descriptor descriptor : bundledDescriptors()) {
            final Callable<FileSystem> factory = () -> bundledFileSystem(descriptor.path());
            registerBundledFloppy(descriptor, factory);
        }
    }

    public static FileSystem openOsFileSystem() {
        return bundledFileSystem(OPENOS_PATH);
    }

    public static List<ItemStack> bundledStacksForCycling() {
        final List<ItemStack> stacks = new ArrayList<>();
        for (Descriptor descriptor : bundledDescriptors()) {
            final ItemStack stack = bundledStack(descriptor);
            if (!stack.isEmpty()) {
                stacks.add(stack);
            }
        }
        return List.copyOf(stacks);
    }

    static List<Descriptor> bundledDescriptors() {
        final Properties properties = new Properties();
        final String resourcePath = "/assets/" + NeoOpenComputers.MODID + "/" + LOOT_PROPERTIES;
        try (InputStream stream = ModLootDisks.class.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                return List.of();
            }
            properties.load(stream);
        } catch (IOException e) {
            return List.of();
        }

        final List<Descriptor> descriptors = new ArrayList<>();
        for (String path : new TreeSet<>(properties.stringPropertyNames())) {
            descriptors.add(parseDescriptor(path, properties.getProperty(path)));
        }
        return List.copyOf(descriptors);
    }

    static String factoryId(final String path) {
        return NeoOpenComputers.MODID + ":" + LOOT_ROOT + "/" + path;
    }

    private static void registerBundledFloppy(final Descriptor descriptor, final Callable<FileSystem> factory) {
        final ItemAPI items = API.items;
        if (items instanceof ItemRegistry registry) {
            registry.registerFloppy(descriptor.label(), descriptor.color(), factoryId(descriptor.path()), factory, true);
        } else {
            Items.registerFloppy(descriptor.label(), descriptor.color(), factory, true);
        }
    }

    private static ItemStack bundledStack(final Descriptor descriptor) {
        final ItemAPI items = API.items;
        if (!(items instanceof ItemRegistry registry)) {
            return ItemStack.EMPTY;
        }
        final var info = registry.get(ModContentIds.FLOPPY);
        if (info == null) {
            return ItemStack.EMPTY;
        }
        final ItemStack stack = info.createItemStack(1);
        if (stack == null) {
            return ItemStack.EMPTY;
        }
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(descriptor.label()));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(ItemRegistry.createFloppyData(
            descriptor.label(),
            descriptor.color(),
            factoryId(descriptor.path()),
            true)));
        return stack;
    }

    static Descriptor parseDescriptor(final String path, final String value) {
        final String[] parts = value.split(":");
        if (parts.length >= 3) {
            return new Descriptor(path, parts[0], nonNegativeInt(parts[1]), dyeColor(parts[2]));
        }
        if (parts.length == 2) {
            return new Descriptor(path, parts[0], nonNegativeInt(parts[1]), DyeColor.LIGHT_GRAY);
        }
        return new Descriptor(path, value, 1, DyeColor.LIGHT_GRAY);
    }

    private static FileSystem bundledFileSystem(final String path) {
        if (API.fileSystem == null) {
            OpenComputersApi.initialize();
        }
        return API.fileSystem.fromClass(ModLootDisks.class, NeoOpenComputers.MODID, LOOT_ROOT + "/" + path);
    }

    private static int nonNegativeInt(final String value) {
        try {
            return Math.max(0, Integer.parseInt(value));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private static DyeColor dyeColor(final String oreName) {
        return switch (oreName) {
            case "dyeBlack" -> DyeColor.BLACK;
            case "dyeBlue" -> DyeColor.BLUE;
            case "dyeBrown" -> DyeColor.BROWN;
            case "dyeCyan" -> DyeColor.CYAN;
            case "dyeGray" -> DyeColor.GRAY;
            case "dyeGreen" -> DyeColor.GREEN;
            case "dyeLightBlue" -> DyeColor.LIGHT_BLUE;
            case "dyeLightGray", "dyeSilver" -> DyeColor.LIGHT_GRAY;
            case "dyeLime" -> DyeColor.LIME;
            case "dyeMagenta" -> DyeColor.MAGENTA;
            case "dyeOrange" -> DyeColor.ORANGE;
            case "dyePink" -> DyeColor.PINK;
            case "dyePurple" -> DyeColor.PURPLE;
            case "dyeRed" -> DyeColor.RED;
            case "dyeWhite" -> DyeColor.WHITE;
            case "dyeYellow" -> DyeColor.YELLOW;
            default -> DyeColor.LIGHT_GRAY;
        };
    }

    record Descriptor(String path, String label, int weight, DyeColor color) {
    }

    private ModLootDisks() {
    }
}
