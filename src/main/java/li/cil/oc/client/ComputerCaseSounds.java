package li.cil.oc.client;

import li.cil.oc.common.ModSounds;
import li.cil.oc.common.blockentity.ComputerCaseBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class ComputerCaseSounds {
    private static final Map<BlockPos, ComputerRunningSound> RUNNING_SOUNDS = new HashMap<>();

    private ComputerCaseSounds() {
    }

    public static void update(final ComputerCaseBlockEntity computer) {
        if (computer == null) {
            return;
        }
        final Level level = computer.getLevel();
        if (level == null || !level.isClientSide) {
            return;
        }

        cleanupStoppedSounds();

        final BlockPos pos = computer.getBlockPos().immutable();
        final ComputerRunningSound sound = RUNNING_SOUNDS.get(pos);
        if (!computer.isClientRunning() || computer.isRemoved()) {
            stop(pos, sound);
            return;
        }
        if (sound == null || sound.isStopped()) {
            final ComputerRunningSound newSound = new ComputerRunningSound(computer);
            RUNNING_SOUNDS.put(pos, newSound);
            Minecraft.getInstance().getSoundManager().play(newSound);
        }
    }

    public static void tick() {
        cleanupStoppedSounds();
    }

    private static void stop(final BlockPos pos, final ComputerRunningSound sound) {
        if (sound != null) {
            sound.stopSound();
            RUNNING_SOUNDS.remove(pos);
        }
    }

    private static void cleanupStoppedSounds() {
        for (final Iterator<Map.Entry<BlockPos, ComputerRunningSound>> iterator = RUNNING_SOUNDS.entrySet().iterator(); iterator.hasNext();) {
            if (iterator.next().getValue().isStopped()) {
                iterator.remove();
            }
        }
    }

    private static final class ComputerRunningSound extends AbstractTickableSoundInstance {
        private final ComputerCaseBlockEntity computer;

        private ComputerRunningSound(final ComputerCaseBlockEntity computer) {
            super(ModSounds.soundEvent(ModSounds.COMPUTER_RUNNING_ID), SoundSource.BLOCKS, RandomSource.create());
            this.computer = computer;
            looping = true;
            volume = 0.5F;
            pitch = 1.0F;
            updatePosition();
        }

        @Override
        public void tick() {
            if (computer.isRemoved() || !computer.isClientRunning()) {
                stopSound();
                return;
            }
            updatePosition();
        }

        private void stopSound() {
            stop();
        }

        private void updatePosition() {
            final BlockPos pos = computer.getBlockPos();
            x = pos.getX() + 0.5D;
            y = pos.getY() + 0.5D;
            z = pos.getZ() + 0.5D;
        }
    }
}
