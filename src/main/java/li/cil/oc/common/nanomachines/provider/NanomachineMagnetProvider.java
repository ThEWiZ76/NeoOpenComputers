package li.cil.oc.common.nanomachines.provider;

import li.cil.oc.api.Nanomachines;
import li.cil.oc.api.nanomachines.Behavior;
import li.cil.oc.api.nanomachines.Controller;
import li.cil.oc.api.nanomachines.DisableReason;
import li.cil.oc.api.prefab.AbstractProvider;
import li.cil.oc.common.ModSettings;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class NanomachineMagnetProvider extends AbstractProvider {
    private static final String PROVIDER_ID = "9324d5ec-71f1-41c2-b51c-406e527668fc";

    public NanomachineMagnetProvider() {
        super(PROVIDER_ID);
    }

    @Override
    public Iterable<Behavior> createBehaviors(final Player player) {
        return java.util.List.of(new MagnetBehavior(player));
    }

    @Override
    protected void writeBehaviorToNBT(final Behavior behavior, final CompoundTag nbt) {
    }

    @Override
    protected Behavior readBehaviorFromNBT(final Player player, final CompoundTag nbt) {
        return new MagnetBehavior(player);
    }

    private record MagnetBehavior(Player player) implements Behavior {
        @Override
        public String getNameHint() {
            return "magnet";
        }

        @Override
        public void onEnable() {
        }

        @Override
        public void onDisable(final DisableReason reason) {
        }

        @Override
        public void update() {
            if (player == null || player.level().isClientSide()) {
                return;
            }
            final Controller controller = Nanomachines.getController(player);
            if (controller == null) {
                return;
            }
            final double range = ModSettings.nanomachinesMagnetRange() * controller.getInputCount(this);
            if (range <= 0D) {
                return;
            }
            for (final ItemEntity item : player.level().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(range))) {
                if (!canPull(item)) {
                    continue;
                }
                final Vec3 delta = player.position().subtract(item.position());
                if (delta.lengthSqr() > 0D) {
                    item.push(delta.normalize().scale(0.1D));
                }
            }
        }

        private boolean canPull(final ItemEntity item) {
            if (item == null || item.hasPickUpDelay() || item.getItem().isEmpty()) {
                return false;
            }
            final ItemStack pulledStack = item.getItem();
            for (final ItemStack inventoryStack : player.getInventory().items) {
                if (inventoryStack.isEmpty()) {
                    return true;
                }
                if (inventoryStack.getCount() < inventoryStack.getMaxStackSize()
                    && ItemStack.isSameItemSameComponents(inventoryStack, pulledStack)) {
                    return true;
                }
            }
            return false;
        }
    }
}
