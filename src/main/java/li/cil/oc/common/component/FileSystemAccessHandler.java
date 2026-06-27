package li.cil.oc.common.component;

import li.cil.oc.api.component.RackMountable;
import li.cil.oc.api.event.FileSystemAccessEvent;
import li.cil.oc.common.ModSounds;
import li.cil.oc.common.blockentity.RackBlockEntity;
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
        final RackBlockEntity rack = rackFor(event);
        if (rack == null) {
            return;
        }
        final long timestamp = System.currentTimeMillis();
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

    private static RackBlockEntity rackFor(final FileSystemAccessEvent.Server event) {
        final BlockEntity blockEntity = event.getTileEntity();
        if (blockEntity instanceof RackBlockEntity rack) {
            return rack;
        }
        if (event.getWorld() == null) {
            return null;
        }
        final BlockPos pos = BlockPos.containing(event.getX(), event.getY(), event.getZ());
        return event.getWorld().getBlockEntity(pos) instanceof RackBlockEntity rack ? rack : null;
    }
}
