package li.cil.oc.common.item;

import li.cil.oc.api.internal.Adapter;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.common.component.MfuEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class MfuItem extends BasicUpgradeItem {
    public static final String COORD_TAG = "oc:coord";

    public MfuItem(final Properties properties) {
        super(properties, 2);
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        final Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        final Integer dimension = legacyDimensionId(context.getLevel());
        if (dimension == null) {
            return InteractionResult.PASS;
        }
        if (!context.getLevel().isClientSide()) {
            final BlockPos target = context.getClickedPos();
            dataTag(context.getItemInHand()).putIntArray(COORD_TAG, new int[]{
                target.getX(),
                target.getY(),
                target.getZ(),
                dimension,
                context.getClickedFace().ordinal()
            });
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide());
    }

    @Override
    public boolean worksWith(final ItemStack stack, final Class<? extends EnvironmentHost> host) {
        return worksWith(stack) && Adapter.class.isAssignableFrom(host);
    }

    @Override
    public ManagedEnvironment createEnvironment(final ItemStack stack, final EnvironmentHost host) {
        if (host != null && host.world() != null && host.world().isClientSide()) {
            return null;
        }
        if (!(host instanceof Adapter)) {
            return null;
        }
        final int[] coord = dataTag(stack).getIntArray(COORD_TAG);
        if (coord.length < MfuEnvironment.LEGACY_TARGET_TAG_LENGTH) {
            return null;
        }
        final int sideIndex;
        if (coord.length >= MfuEnvironment.TARGET_TAG_LENGTH) {
            if (!matchesLegacyDimension(host.world(), coord[3])) {
                return null;
            }
            sideIndex = coord[4];
        } else {
            sideIndex = coord[3];
        }
        final Direction side = Direction.values()[Math.floorMod(sideIndex, Direction.values().length)];
        return new MfuEnvironment(host, new BlockPos(coord[0], coord[1], coord[2]), side);
    }

    @Override
    @SuppressWarnings("deprecation")
    public CompoundTag dataTag(final ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return new CompoundTag();
        }
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(new CompoundTag()));
            customData = stack.get(DataComponents.CUSTOM_DATA);
        }
        return customData.getUnsafe();
    }

    private static boolean matchesLegacyDimension(final Level level, final int dimension) {
        final Integer currentDimension = legacyDimensionId(level);
        return currentDimension != null && currentDimension == dimension;
    }

    private static Integer legacyDimensionId(final Level level) {
        if (level == null) {
            return null;
        }
        if (level.dimension().equals(Level.OVERWORLD)) {
            return 0;
        }
        if (level.dimension().equals(Level.NETHER)) {
            return -1;
        }
        if (level.dimension().equals(Level.END)) {
            return 1;
        }
        return null;
    }
}
