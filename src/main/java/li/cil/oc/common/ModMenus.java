package li.cil.oc.common;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.common.menu.ComputerCaseMenu;
import li.cil.oc.common.menu.DiskDriveMenu;
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

    public static void register(final IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }

    private ModMenus() {
    }
}
