package li.cil.oc.common;

import li.cil.oc.api.API;
import li.cil.oc.common.item.PresentItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import java.time.Clock;
import java.time.LocalDate;
import java.util.function.DoubleSupplier;

public final class PresentHandler {
    private final Clock clock;
    private final DoubleSupplier chance;

    public PresentHandler(final Clock clock, final DoubleSupplier chance) {
        this.clock = clock;
        this.chance = chance;
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(new PresentHandler(Clock.systemDefaultZone(), ModSettings::presentChance)::onCraft);
    }

    public static boolean isHoliday(final LocalDate date) {
        final int day = date.getDayOfMonth();
        return switch (date.getMonth()) {
            case DECEMBER -> day > 24 || day == 14;
            case JANUARY -> day < 7;
            case FEBRUARY -> day == 14;
            case APRIL -> day == 22;
            case MAY -> day == 1;
            case OCTOBER -> day == 3;
            default -> false;
        };
    }

    public void onCraft(final PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player instanceof FakePlayer) return;
        final var output = event.getCrafting();
        final double probability = chance.getAsDouble();
        // Upstream excludes all outputs handled by its recraft callback, including initial navigation crafting.
        if (probability <= 0 || recraftOutput(output) || output.isEmpty() || API.items.get(output) == null
            || player.getRandom().nextFloat() >= probability || !isHoliday(LocalDate.now(clock))) return;
        player.level().playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.MASTER, 0.2F, 1);
        PresentItem.deliver(player, new ItemStack(ModItems.PRESENT.get()));
    }

    private static boolean recraftOutput(final ItemStack stack) {
        return stack.is(ModItems.NAVIGATION_UPGRADE.get()) || stack.is(ModItems.DRONE.get()) || stack.is(ModItems.ROBOT.get())
            || stack.is(ModItems.TABLET.get()) || stack.is(ModItems.MICROCONTROLLER_TIER1.get())
            || stack.is(ModItems.MICROCONTROLLER_TIER2.get()) || stack.is(ModItems.MICROCONTROLLER_CREATIVE.get());
    }
}
