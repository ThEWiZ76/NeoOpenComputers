package li.cil.oc.common.item;

import li.cil.oc.common.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.util.RandomSource;
import java.util.HashSet;
import java.util.List;
import java.util.function.Supplier;

public final class PresentLoot {
    public record Entry(Supplier<? extends Item> item, int weight) {}
    // Original Present.scala order and weights; modern registrations replace the old item descriptors.
    public static final List<Entry> ENTRIES = List.of(
        new Entry(ModItems.ARROW_KEYS, 520),
        new Entry(ModItems.BUTTON_GROUP, 460),
        new Entry(ModItems.NUM_PAD, 410),
        new Entry(ModItems.DISK_PLATTER, 370),
        new Entry(ModItems.TRANSISTOR, 350),
        new Entry(ModItems.FLOPPY, 340),
        new Entry(ModItems.PRINTED_CIRCUIT_BOARD, 320),
        new Entry(ModItems.MICROCHIP_TIER1, 290),
        new Entry(ModItems.EEPROM, 250),
        new Entry(ModItems.INTERWEB, 220),
        new Entry(ModItems.CARD, 190),
        new Entry(ModItems.ANALYZER, 170),
        new Entry(ModItems.SIGN_UPGRADE, 150),
        new Entry(ModItems.INVENTORY_UPGRADE, 130),
        new Entry(ModItems.CRAFTING_UPGRADE, 110),
        new Entry(ModItems.TANK_UPGRADE, 90),
        new Entry(ModItems.PISTON_UPGRADE, 80),
        new Entry(ModItems.LEASH_UPGRADE, 70),
        new Entry(ModItems.ANGEL_UPGRADE, 55),
        new Entry(ModItems.REDSTONE_CARD, 50),
        new Entry(ModItems.MEMORY_TIER1, 48),
        new Entry(ModItems.CONTROL_UNIT, 46),
        new Entry(ModItems.ALU, 45),
        new Entry(ModItems.BATTERY_UPGRADE_TIER1, 43),
        new Entry(ModItems.NETWORK_CARD, 38),
        new Entry(ModItems.WIRELESS_NETWORK_CARD_TIER1, 37),
        new Entry(ModItems.HDD_TIER1, 36),
        new Entry(ModItems.GENERATOR_UPGRADE, 35),
        new Entry(ModItems.CPU_TIER1, 31),
        new Entry(ModItems.MICROCONTROLLER_CASE_TIER1, 30),
        new Entry(ModItems.DRONE_CASE_TIER1, 25),
        new Entry(ModItems.UPGRADE_CONTAINER_TIER1, 23),
        new Entry(ModItems.CARD_CONTAINER_TIER1, 23),
        new Entry(ModItems.GRAPHICS_CARD_TIER1, 19),
        new Entry(() -> upstreamItem("redstonecard2"), 17),
        new Entry(ModItems.MEMORY_TIER2, 15),
        new Entry(ModItems.DATABASE_UPGRADE_TIER1, 15),
        new Entry(ModItems.MICROCHIP_TIER2, 15),
        new Entry(ModItems.COMPONENT_BUS_TIER1, 13),
        new Entry(ModItems.BATTERY_UPGRADE_TIER2, 12),
        new Entry(ModItems.WIRELESS_NETWORK_CARD_TIER2, 11),
        new Entry(ModItems.MEMORY_TIER3, 10),
        new Entry(ModItems.SERVER_TIER1, 10),
        new Entry(ModItems.INTERNET_CARD, 9),
        new Entry(ModItems.TERMINAL, 9),
        new Entry(ModItems.SOLAR_GENERATOR_UPGRADE, 9),
        new Entry(ModItems.HDD_TIER2, 7),
        new Entry(ModItems.NAVIGATION_UPGRADE, 7),
        new Entry(ModItems.INVENTORY_CONTROLLER_UPGRADE, 7),
        new Entry(ModItems.TANK_CONTROLLER_UPGRADE, 7),
        new Entry(ModItems.CPU_TIER2, 6),
        new Entry(ModItems.MICROCONTROLLER_CASE_TIER2, 6),
        new Entry(ModItems.COMPONENT_BUS_TIER2, 6),
        new Entry(ModItems.TABLET_CASE_TIER1, 5),
        new Entry(ModItems.UPGRADE_CONTAINER_TIER2, 5),
        new Entry(ModItems.CARD_CONTAINER_TIER2, 5),
        new Entry(ModItems.GRAPHICS_CARD_TIER2, 4),
        new Entry(ModItems.MEMORY_TIER4, 4),
        new Entry(ModItems.DRONE_CASE_TIER2, 4),
        new Entry(ModItems.DATABASE_UPGRADE_TIER2, 4),
        new Entry(ModItems.SERVER_TIER2, 4),
        new Entry(ModItems.MICROCHIP_TIER3, 3),
        new Entry(ModItems.COMPONENT_BUS_TIER3, 3),
        new Entry(ModItems.TRACTOR_BEAM_UPGRADE, 3),
        new Entry(ModItems.BATTERY_UPGRADE_TIER3, 3),
        new Entry(ModItems.EXPERIENCE_UPGRADE, 2),
        new Entry(ModItems.MEMORY_TIER5, 2),
        new Entry(ModItems.UPGRADE_CONTAINER_TIER3, 2),
        new Entry(ModItems.CARD_CONTAINER_TIER3, 2),
        new Entry(ModItems.TABLET_CASE_TIER2, 1),
        new Entry(ModItems.HDD_TIER3, 1),
        new Entry(ModItems.CHUNKLOADER_UPGRADE, 1),
        new Entry(ModItems.CPU_TIER3, 1),
        new Entry(ModItems.GRAPHICS_CARD_TIER3, 1),
        new Entry(ModItems.SERVER_TIER3, 1),
        new Entry(ModItems.DATABASE_UPGRADE_TIER3, 1),
        new Entry(ModItems.MEMORY_TIER6, 1)
    );

    private PresentLoot() {}

    private static Item upstreamItem(final String name) {
        final var info = li.cil.oc.api.API.items.get(name);
        return info == null ? null : info.item();
    }

    public static List<Entry> eligible(final Level level) {
        final var craftable = new HashSet<Item>();
        for (final var recipe : level.getRecipeManager().getRecipes()) {
            if ((recipe.value() instanceof ShapedRecipe || recipe.value() instanceof ShapelessRecipe)
                && !recipe.value().getIngredients().isEmpty()) {
                final var result = recipe.value().getResultItem(level.registryAccess());
                if (!result.isEmpty()) craftable.add(result.getItem());
            }
        }
        return ENTRIES.stream().filter(entry -> craftable.contains(entry.item().get())).toList();
    }

    public static ItemStack select(final List<Entry> pool, final int roll) {
        if (roll < 0) throw new IllegalArgumentException("negative present roll");
        int remaining = roll;
        for (final var entry : pool) {
            if (remaining < entry.weight()) return new ItemStack(entry.item().get());
            remaining -= entry.weight();
        }
        throw new IllegalArgumentException("present roll outside pool");
    }

    public static ItemStack next(final Level level, final RandomSource random) {
        final var pool = eligible(level);
        final int total = pool.stream().mapToInt(Entry::weight).sum();
        return total == 0 ? ItemStack.EMPTY : select(pool, random.nextInt(total));
    }
}
