package li.cil.oc.common.menu;

import li.cil.oc.common.ModMenus;
import li.cil.oc.common.blockentity.WaypointBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

public class WaypointMenu extends AbstractContainerMenu {
    private final WaypointBlockEntity waypoint;
    private final BlockPos pos;
    private String label;

    public WaypointMenu(final int containerId, final Inventory playerInventory) {
        this(containerId, playerInventory, null, BlockPos.ZERO, "");
    }

    public WaypointMenu(final int containerId, final Inventory playerInventory, final RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory, null, extraData.readBlockPos(), extraData.readUtf(32767));
    }

    public WaypointMenu(final int containerId, final Inventory playerInventory, final WaypointBlockEntity waypoint) {
        this(containerId, playerInventory, waypoint, waypoint == null ? BlockPos.ZERO : waypoint.getBlockPos(), waypoint == null ? "" : waypoint.label());
    }

    private WaypointMenu(
        final int containerId,
        final Inventory playerInventory,
        final WaypointBlockEntity waypoint,
        final BlockPos pos,
        final String label) {
        super(ModMenus.WAYPOINT.get(), containerId);
        this.waypoint = waypoint;
        this.pos = pos == null ? BlockPos.ZERO : pos;
        this.label = WaypointBlockEntity.truncateLabel(label);
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(final Player player) {
        if (waypoint == null || player == null) {
            return true;
        }
        if (waypoint.isRemoved() || waypoint.getLevel() == null) {
            return false;
        }
        return player.distanceToSqr(
            waypoint.getBlockPos().getX() + 0.5D,
            waypoint.getBlockPos().getY() + 0.5D,
            waypoint.getBlockPos().getZ() + 0.5D) <= 64.0D;
    }

    public void setLabel(final String value) {
        label = WaypointBlockEntity.truncateLabel(value);
        if (waypoint != null) {
            waypoint.setLabelValue(label);
            label = waypoint.label();
        }
    }

    public String label() {
        return label;
    }

    public BlockPos pos() {
        return pos;
    }
}
