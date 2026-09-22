package li.cil.oc.common.component;

import li.cil.oc.api.component.RackMountable;
import li.cil.oc.api.event.FileSystemAccessEvent;
import li.cil.oc.common.ModSounds;
import li.cil.oc.common.blockentity.ComputerCaseBlockEntity;
import li.cil.oc.common.blockentity.DiskDriveBlockEntity;
import li.cil.oc.common.blockentity.RackBlockEntity;
import li.cil.oc.common.blockentity.RaidBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.NeoForge;

public final class FileSystemAccessHandler {
    private FileSystemAccessHandler() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(FileSystemAccessHandler::onFileSystemAccess);
    }

    private static void onFileSystemAccess(final FileSystemAccessEvent.Server event) {
        ModSounds.play(event.getWorld(), event.getX(), event.getY(), event.getZ(), ModSounds.soundEvent(event.getSound()));
        final long timestamp = System.currentTimeMillis();
        recordComputerCaseAccess(event, timestamp);
        recordDiskDriveAccess(event, timestamp);
        recordRaidAccess(event, timestamp);
        final RackBlockEntity rack = rackFor(event);
        if (rack == null) {
            return;
        }
        for (int slot = 0; slot < RackBlockEntity.CONTAINER_SIZE; slot++) {
            final RackMountable mountable = rack.getMountable(slot);
            if (recordsFileSystemAccess(mountable, event, timestamp)) {
                rack.markChanged(slot);
            }
        }
    }

    private static boolean recordsFileSystemAccess(final RackMountable mountable, final FileSystemAccessEvent.Server event, final long timestamp) {
        if (mountable instanceof ServerRackMountableEnvironment server) {
            return server.recordFileSystemAccess(event.getNode(), timestamp);
        }
        return mountable instanceof DiskDriveMountableEnvironment diskDrive
            && diskDrive.recordFileSystemAccess(event.getNode(), timestamp);
    }

    private static boolean recordDiskDriveAccess(final FileSystemAccessEvent.Server event, final long timestamp) {
        final DiskDriveBlockEntity diskDrive = blockEntityFor(event, DiskDriveBlockEntity.class);
        return diskDrive != null && diskDrive.recordFileSystemAccess(event.getNode(), timestamp);
    }

    private static boolean recordComputerCaseAccess(final FileSystemAccessEvent.Server event, final long timestamp) {
        final ComputerCaseBlockEntity computerCase = blockEntityFor(event, ComputerCaseBlockEntity.class);
        return computerCase != null && computerCase.recordFileSystemAccess(event.getNode(), timestamp);
    }

    private static boolean recordRaidAccess(final FileSystemAccessEvent.Server event, final long timestamp) {
        final RaidBlockEntity raid = blockEntityFor(event, RaidBlockEntity.class);
        return raid != null && raid.recordFileSystemAccess(event.getNode(), timestamp);
    }

    private static RackBlockEntity rackFor(final FileSystemAccessEvent.Server event) {
        return blockEntityFor(event, RackBlockEntity.class);
    }

    private static <T extends BlockEntity> T blockEntityFor(final FileSystemAccessEvent.Server event, final Class<T> type) {
        if (type.isInstance(event.getTileEntity())) {
            return type.cast(event.getTileEntity());
        }
        if (event.getWorld() == null) {
            return null;
        }
        final BlockPos pos = BlockPos.containing(event.getX(), event.getY(), event.getZ());
        final BlockEntity blockEntity = event.getWorld().getBlockEntity(pos);
        return type.isInstance(blockEntity) ? type.cast(blockEntity) : null;
    }
}
