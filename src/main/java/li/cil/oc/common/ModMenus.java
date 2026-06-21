package li.cil.oc.common;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.common.menu.ComputerCaseMenu;
import li.cil.oc.common.menu.DiskDriveMenu;
import li.cil.oc.common.menu.RackMenu;
import li.cil.oc.common.menu.RaidMenu;
import li.cil.oc.common.menu.RelayMenu;
import li.cil.oc.common.menu.TerminalMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, NeoOpenComputers.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<ComputerCaseMenu>> COMPUTER_CASE = MENUS.register(
        ModContentIds.COMPUTER_CASE_MENU,
        () -> new MenuType<>(ComputerCaseMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<DiskDriveMenu>> DISK_DRIVE = MENUS.register(
        ModContentIds.DISK_DRIVE_MENU,
        () -> new MenuType<>(DiskDriveMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<RackMenu>> RACK = MENUS.register(
        ModContentIds.RACK_MENU,
        () -> new MenuType<>(RackMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<RaidMenu>> RAID = MENUS.register(
        ModContentIds.RAID_MENU,
        () -> new MenuType<>(RaidMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<RelayMenu>> RELAY = MENUS.register(
        ModContentIds.RELAY_MENU,
        () -> new MenuType<>(RelayMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<TerminalMenu>> TERMINAL = MENUS.register(
        ModContentIds.TERMINAL_MENU,
        () -> new MenuType<>(TerminalMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static void register(final IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }

    private ModMenus() {
    }
}
