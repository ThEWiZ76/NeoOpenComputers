package li.cil.oc.common;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.common.menu.AdapterMenu;
import li.cil.oc.common.menu.AssemblerMenu;
import li.cil.oc.common.menu.ChargerMenu;
import li.cil.oc.common.menu.ComputerCaseMenu;
import li.cil.oc.common.menu.DisassemblerMenu;
import li.cil.oc.common.menu.DiskDriveMenu;
import li.cil.oc.common.menu.DriveMenu;
import li.cil.oc.common.menu.MicrocontrollerMenu;
import li.cil.oc.common.menu.PrinterMenu;
import li.cil.oc.common.menu.RackMenu;
import li.cil.oc.common.menu.RaidMenu;
import li.cil.oc.common.menu.RelayMenu;
import li.cil.oc.common.menu.RobotMenu;
import li.cil.oc.common.menu.ServerRackMenu;
import li.cil.oc.common.menu.TerminalMenu;
import li.cil.oc.common.menu.WaypointMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, NeoOpenComputers.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<AdapterMenu>> ADAPTER = MENUS.register(
        ModContentIds.ADAPTER_MENU,
        () -> new MenuType<>(AdapterMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<ComputerCaseMenu>> COMPUTER_CASE = MENUS.register(
        ModContentIds.COMPUTER_CASE_MENU,
        () -> IMenuTypeExtension.create(ComputerCaseMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<MicrocontrollerMenu>> MICROCONTROLLER = MENUS.register(
        ModContentIds.MICROCONTROLLER_MENU,
        () -> IMenuTypeExtension.create(MicrocontrollerMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<RobotMenu>> ROBOT = MENUS.register(
        ModContentIds.ROBOT_MENU,
        () -> IMenuTypeExtension.create(RobotMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<AssemblerMenu>> ASSEMBLER = MENUS.register(
        ModContentIds.ASSEMBLER_MENU,
        () -> new MenuType<>(AssemblerMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<ChargerMenu>> CHARGER = MENUS.register(
        ModContentIds.CHARGER_MENU,
        () -> new MenuType<>(ChargerMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<DiskDriveMenu>> DISK_DRIVE = MENUS.register(
        ModContentIds.DISK_DRIVE_MENU,
        () -> new MenuType<>(DiskDriveMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<DriveMenu>> DRIVE = MENUS.register(
        ModContentIds.DRIVE_MENU,
        () -> new MenuType<>(DriveMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<DisassemblerMenu>> DISASSEMBLER = MENUS.register(
        ModContentIds.DISASSEMBLER_MENU,
        () -> new MenuType<>(DisassemblerMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<PrinterMenu>> PRINTER = MENUS.register(
        ModContentIds.PRINTER_MENU,
        () -> new MenuType<>(PrinterMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<RackMenu>> RACK = MENUS.register(
        ModContentIds.RACK_MENU,
        () -> new MenuType<>(RackMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<RaidMenu>> RAID = MENUS.register(
        ModContentIds.RAID_MENU,
        () -> new MenuType<>(RaidMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<ServerRackMenu>> SERVER_RACK = MENUS.register(
        ModContentIds.SERVER_RACK_MENU,
        () -> new MenuType<>(ServerRackMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<RelayMenu>> RELAY = MENUS.register(
        ModContentIds.RELAY_MENU,
        () -> new MenuType<>(RelayMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<TerminalMenu>> TERMINAL = MENUS.register(
        ModContentIds.TERMINAL_MENU,
        () -> new MenuType<>(TerminalMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<WaypointMenu>> WAYPOINT = MENUS.register(
        ModContentIds.WAYPOINT_MENU,
        () -> IMenuTypeExtension.create(WaypointMenu::new));

    public static void register(final IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }

    private ModMenus() {
    }
}
