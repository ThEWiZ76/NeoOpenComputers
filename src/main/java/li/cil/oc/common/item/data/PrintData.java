package li.cil.oc.common.item.data;

import li.cil.oc.common.InkProviders;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.ModSettings;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.AABB;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class PrintData {
    public static final int UPSTREAM_MAX_SHAPES = 24;
    public static final double UPSTREAM_RECYCLE_RATE = 0.75D;
    public static final int UPSTREAM_MAX_BASE_LIGHT_LEVEL = 8;
    public static final int UPSTREAM_CUSTOM_REDSTONE_COST = 300;
    public static final int UPSTREAM_MATERIAL_VALUE = 2000;
    public static final int UPSTREAM_NOCLIP_MULTIPLIER = 2;

    private static final int STEPPING = 4;
    private static final double STEP = STEPPING / 16D;
    private static final float INV_MAX_VOLUME = 1F / (STEPPING * STEPPING * STEPPING);
    private static final Comparator<Shape> SHAPE_ORDER = Comparator
        .comparingDouble((Shape shape) -> shape.bounds().minX).reversed()
        .thenComparing(Comparator.comparingDouble((Shape shape) -> shape.bounds().minY).reversed())
        .thenComparing(Comparator.comparingDouble((Shape shape) -> shape.bounds().minZ).reversed())
        .thenComparing(Comparator.comparingDouble((Shape shape) -> shape.bounds().maxX).reversed())
        .thenComparing(Comparator.comparingDouble((Shape shape) -> shape.bounds().maxY).reversed())
        .thenComparing(Comparator.comparingDouble((Shape shape) -> shape.bounds().maxZ).reversed())
        .thenComparing(Comparator.comparing((Shape shape) -> shape.tint() == null ? Integer.MIN_VALUE : shape.tint()).reversed())
        .thenComparing(Comparator.comparing(Shape::texture).reversed());

    private String label;
    private String tooltip;
    private boolean buttonMode;
    private int redstoneLevel;
    private boolean pressurePlate;
    private final Set<Shape> stateOff = new LinkedHashSet<>();
    private final Set<Shape> stateOn = new LinkedHashSet<>();
    private boolean beaconBase;
    private int lightLevel;
    private boolean noclipOff;
    private boolean noclipOn;
    private boolean opacityDirty = true;
    private float opacity;

    public PrintData() {
    }

    public PrintData(final ItemStack stack) {
        load(stack);
    }

    public String label() {
        return label;
    }

    public void setLabel(final String label) {
        this.label = label;
    }

    public String tooltip() {
        return tooltip;
    }

    public void setTooltip(final String tooltip) {
        this.tooltip = tooltip;
    }

    public boolean isButtonMode() {
        return buttonMode;
    }

    public void setButtonMode(final boolean buttonMode) {
        this.buttonMode = buttonMode;
    }

    public int redstoneLevel() {
        return redstoneLevel;
    }

    public void setRedstoneLevel(final int redstoneLevel) {
        this.redstoneLevel = Math.max(0, Math.min(15, redstoneLevel));
    }

    public boolean isPressurePlate() {
        return pressurePlate;
    }

    public void setPressurePlate(final boolean pressurePlate) {
        this.pressurePlate = pressurePlate;
    }

    public Set<Shape> stateOff() {
        return Set.copyOf(stateOff);
    }

    public Set<Shape> stateOn() {
        return Set.copyOf(stateOn);
    }

    public void addStateOff(final Shape shape) {
        if (shape != null) {
            stateOff.add(shape);
            opacityDirty = true;
        }
    }

    public void addStateOn(final Shape shape) {
        if (shape != null) {
            stateOn.add(shape);
            opacityDirty = true;
        }
    }

    public boolean isBeaconBase() {
        return beaconBase;
    }

    public void setBeaconBase(final boolean beaconBase) {
        this.beaconBase = beaconBase;
    }

    public int lightLevel() {
        return lightLevel;
    }

    public void setLightLevel(final int lightLevel) {
        this.lightLevel = Math.max(0, Math.min(15, lightLevel));
    }

    public boolean isNoclipOff() {
        return noclipOff;
    }

    public void setNoclipOff(final boolean noclipOff) {
        this.noclipOff = noclipOff;
    }

    public boolean isNoclipOn() {
        return noclipOn;
    }

    public void setNoclipOn(final boolean noclipOn) {
        this.noclipOn = noclipOn;
    }

    public int complexity() {
        return Math.max(stateOn.size(), stateOff.size());
    }

    public boolean hasActiveState() {
        return !stateOn.isEmpty();
    }

    public boolean emitLight() {
        return lightLevel > 0;
    }

    public boolean emitRedstone() {
        return redstoneLevel > 0;
    }

    public boolean emitRedstone(final boolean state) {
        return state ? emitRedstoneWhenOn() : emitRedstoneWhenOff();
    }

    public boolean emitRedstoneWhenOff() {
        return emitRedstone() && !hasActiveState();
    }

    public boolean emitRedstoneWhenOn() {
        return emitRedstone() && hasActiveState();
    }

    public float opacity() {
        if (opacityDirty) {
            opacityDirty = false;
            opacity = Math.min(computeApproximateOpacity(stateOn), computeApproximateOpacity(stateOff));
        }
        return opacity;
    }

    public void load(final CompoundTag tag) {
        label = tag.contains("label") ? tag.getString("label") : null;
        tooltip = tag.contains("tooltip") ? tag.getString("tooltip") : null;
        buttonMode = tag.getBoolean("isButtonMode");
        setRedstoneLevel(tag.getInt("redstoneLevel"));
        if (tag.getBoolean("emitRedstone")) {
            redstoneLevel = 15;
        }
        pressurePlate = tag.getBoolean("pressurePlate");
        stateOff.clear();
        loadShapes(tag, "stateOff", stateOff);
        stateOn.clear();
        loadShapes(tag, "stateOn", stateOn);
        beaconBase = tag.getBoolean("isBeaconBase");
        setLightLevel(tag.getByte("lightLevel") & 0xFF);
        noclipOff = tag.getBoolean("noclipOff");
        noclipOn = tag.getBoolean("noclipOn");
        opacityDirty = true;
    }

    public void load(final ItemStack stack) {
        if (stack == null) {
            return;
        }
        final CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            load(customData.copyTag());
        }
    }

    public void save(final CompoundTag tag) {
        if (label != null) {
            tag.putString("label", label);
        }
        if (tooltip != null) {
            tag.putString("tooltip", tooltip);
        }
        tag.putBoolean("isButtonMode", buttonMode);
        tag.putInt("redstoneLevel", redstoneLevel);
        tag.putBoolean("pressurePlate", pressurePlate);
        saveShapes(tag, "stateOff", stateOff);
        saveShapes(tag, "stateOn", stateOn);
        tag.putBoolean("isBeaconBase", beaconBase);
        tag.putByte("lightLevel", (byte) lightLevel);
        tag.putBoolean("noclipOff", noclipOff);
        tag.putBoolean("noclipOn", noclipOn);
    }

    public void save(final ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        final CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        final CompoundTag tag = customData == null ? new CompoundTag() : customData.copyTag();
        save(tag);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public ItemStack createItemStack() {
        final ItemStack stack = new ItemStack(ModItems.PRINT.get());
        save(stack);
        return stack;
    }

    public static float computeApproximateOpacity(final Iterable<Shape> shapes) {
        final List<Shape> shapeList = new ArrayList<>();
        shapes.forEach(shapeList::add);
        float volume = 1F;
        if (!shapeList.isEmpty()) {
            for (int x = 0; x < 16 / STEPPING; x++) {
                for (int y = 0; y < 16 / STEPPING; y++) {
                    for (int z = 0; z < 16 / STEPPING; z++) {
                        final AABB bounds = new AABB(
                            x * STEP,
                            y * STEP,
                            z * STEP,
                            (x + 1) * STEP,
                            (y + 1) * STEP,
                            (z + 1) * STEP);
                        if (shapeList.stream().noneMatch(shape -> shape.bounds().intersects(bounds))) {
                            volume -= INV_MAX_VOLUME;
                        }
                    }
                }
            }
        }
        return volume;
    }

    public static Optional<Costs> computeCosts(final PrintData data) {
        final int totalVolume = data.stateOn.stream().mapToInt(shape -> volume(shape.bounds())).sum()
            + data.stateOff.stream().mapToInt(shape -> volume(shape.bounds())).sum();
        final int totalSurface = data.stateOn.stream().mapToInt(shape -> surface(shape.bounds())).sum()
            + data.stateOff.stream().mapToInt(shape -> surface(shape.bounds())).sum();
        if (totalVolume <= 0) {
            return Optional.empty();
        }
        final int baseMaterialRequired = Math.max(totalVolume / 2, 1);
        final int materialRequired = data.redstoneLevel > 0 && data.redstoneLevel < 15
            ? baseMaterialRequired + ModSettings.printerCustomRedstoneCost()
            : baseMaterialRequired;
        final double multiplier = data.noclipOff || data.noclipOn ? ModSettings.printerNoclipMultiplier() : 1D;
        return Optional.of(new Costs((int) (materialRequired * multiplier), Math.max(totalSurface / 6, 1)));
    }

    public static void addInkProvider(final Method provider) {
        InkProviders.add(provider);
    }

    public static int inkValue(final ItemStack stack) {
        return InkProviders.inkValue(stack);
    }

    public static int materialValue(final ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        if (stack.is(ModItems.CHAMELIUM.get())) {
            return ModSettings.printerMaterialValue();
        }
        if (stack.is(ModItems.PRINT.get())) {
            final PrintData data = new PrintData(stack);
            return computeCosts(data)
                .map(costs -> (int) (costs.material() * ModSettings.printerRecycleRate()))
                .orElse(0);
        }
        return 0;
    }

    public static Shape nbtToShape(final CompoundTag tag) {
        final AABB bounds;
        if (tag.contains("minX")) {
            bounds = new AABB(
                tag.getByte("minX") / 16D,
                tag.getByte("minY") / 16D,
                tag.getByte("minZ") / 16D,
                tag.getByte("maxX") / 16D,
                tag.getByte("maxY") / 16D,
                tag.getByte("maxZ") / 16D);
        } else {
            final byte[] raw = tag.getByteArray("bounds");
            final byte[] values = new byte[6];
            System.arraycopy(raw, 0, values, 0, Math.min(raw.length, values.length));
            bounds = new AABB(
                values[0] / 16D,
                values[1] / 16D,
                values[2] / 16D,
                values[3] / 16D,
                values[4] / 16D,
                values[5] / 16D);
        }
        return new Shape(bounds, tag.getString("texture"), tag.contains("tint") ? tag.getInt("tint") : null);
    }

    public static CompoundTag shapeToNbt(final Shape shape) {
        final CompoundTag tag = new CompoundTag();
        tag.putByteArray("bounds", new byte[]{
            (byte) Math.round(shape.bounds().minX * 16D),
            (byte) Math.round(shape.bounds().minY * 16D),
            (byte) Math.round(shape.bounds().minZ * 16D),
            (byte) Math.round(shape.bounds().maxX * 16D),
            (byte) Math.round(shape.bounds().maxY * 16D),
            (byte) Math.round(shape.bounds().maxZ * 16D)
        });
        tag.putString("texture", shape.texture());
        if (shape.tint() != null) {
            tag.putInt("tint", shape.tint());
        }
        return tag;
    }

    private static void loadShapes(final CompoundTag tag, final String name, final Set<Shape> shapes) {
        final ListTag list = tag.getList(name, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            shapes.add(nbtToShape(list.getCompound(i)));
        }
    }

    private static void saveShapes(final CompoundTag tag, final String name, final Set<Shape> shapes) {
        final ListTag list = new ListTag();
        shapes.stream().sorted(SHAPE_ORDER).map(PrintData::shapeToNbt).forEach(list::add);
        tag.put(name, list);
    }

    private static int volume(final AABB bounds) {
        return sizeX(bounds) * sizeY(bounds) * sizeZ(bounds);
    }

    private static int surface(final AABB bounds) {
        final int sizeX = sizeX(bounds);
        final int sizeY = sizeY(bounds);
        final int sizeZ = sizeZ(bounds);
        return sizeX * sizeY * 2 + sizeX * sizeZ * 2 + sizeY * sizeZ * 2;
    }

    private static int sizeX(final AABB bounds) {
        return (int) Math.round((bounds.maxX - bounds.minX) * 16D);
    }

    private static int sizeY(final AABB bounds) {
        return (int) Math.round((bounds.maxY - bounds.minY) * 16D);
    }

    private static int sizeZ(final AABB bounds) {
        return (int) Math.round((bounds.maxZ - bounds.minZ) * 16D);
    }

    public record Shape(AABB bounds, String texture, Integer tint) {
    }

    public record Costs(int material, int ink) {
    }
}
