package li.cil.oc.common.menu;

import li.cil.oc.common.ModMenus;
import li.cil.oc.common.blockentity.ScreenItemEnvironment;
import li.cil.oc.common.item.TabletRuntime;
import li.cil.oc.common.item.TabletRuntimeRegistry;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

public final class TabletTerminalMenu extends TerminalMenu {
    private final TabletRuntime runtime;

    public TabletTerminalMenu(int containerId, Inventory inventory, TabletRuntime runtime) {
        super(ModMenus.TERMINAL.get(), containerId, inventory);
        this.runtime = runtime;
        updateSnapshot(runtime.screen().terminalSnapshot());
    }

    @Override public ScreenItemEnvironment itemScreen() { return runtime.screen(); }
    @Override public boolean supportsMouseInput() { return true; }
    @Override public boolean stillValid(Player player) {
        return player == runtime.player() && !runtime.isClosed() && player.level() == runtime.world()
            && TabletRuntimeRegistry.isCarried(player, runtime.stack()) && runtime.machine().canInteract(player.getGameProfile().getName());
    }
    @Override public boolean acceptsInput(Player player) { return stillValid(player) && runtime.screen().hasKeyboard(); }
}
