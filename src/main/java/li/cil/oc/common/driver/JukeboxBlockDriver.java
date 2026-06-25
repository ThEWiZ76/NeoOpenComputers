package li.cil.oc.common.driver;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DriverBlock;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;

import java.util.Map;
import java.util.Optional;

public final class JukeboxBlockDriver implements DriverBlock {
    @Override
    public boolean worksWith(final Level world, final BlockPos pos, final Direction side) {
        return world != null && pos != null && world.getBlockEntity(pos) instanceof JukeboxBlockEntity;
    }

    @Override
    public ManagedEnvironment createEnvironment(final Level world, final BlockPos pos, final Direction side) {
        if (world != null && pos != null && world.getBlockEntity(pos) instanceof JukeboxBlockEntity jukebox) {
            return new Environment(jukebox);
        }
        return null;
    }

    public static final class Environment extends AbstractManagedEnvironment implements NamedBlock, DeviceInfo {
        private static final Map<String, String> DEVICE_INFO = Map.of(
            DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Generic,
            DeviceInfo.DeviceAttribute.Description, "Jukebox",
            DeviceInfo.DeviceAttribute.Vendor, "Minecraft",
            DeviceInfo.DeviceAttribute.Product, "Jukebox"
        );

        private final JukeboxBlockEntity jukebox;

        public Environment(final JukeboxBlockEntity jukebox) {
            this.jukebox = jukebox;
            setNode(Network.newNode(this, Visibility.Network)
                .withComponent("jukebox", Visibility.Network)
                .create());
        }

        @Override
        public String preferredName() {
            return "jukebox";
        }

        @Override
        public int priority() {
            return 0;
        }

        @Override
        public Map<String, String> getDeviceInfo() {
            return DEVICE_INFO;
        }

        @Callback(doc = "function():string -- Get the title of the record currently in the jukebox.")
        public Object[] getRecord(final Context context, final Arguments arguments) {
            final Optional<Holder<JukeboxSong>> song = song();
            return song.<Object[]>map(jukeboxSong -> new Object[]{jukeboxSong.value().description().getString()}).orElse(null);
        }

        @Callback(doc = "function() -- Start playing the record currently in the jukebox.")
        public Object[] play(final Context context, final Arguments arguments) {
            final Level level = jukebox.getLevel();
            final Optional<Holder<JukeboxSong>> song = song();
            if (level == null || song.isEmpty()) {
                return null;
            }
            jukebox.getSongPlayer().play(level, song.get());
            return new Object[]{true};
        }

        @Callback(doc = "function() -- Stop playing the record currently in the jukebox.")
        public Object[] stop(final Context context, final Arguments arguments) {
            final Level level = jukebox.getLevel();
            if (level != null) {
                jukebox.getSongPlayer().stop(level, jukebox.getBlockState());
            }
            return null;
        }

        private Optional<Holder<JukeboxSong>> song() {
            final Level level = jukebox.getLevel();
            return level == null ? Optional.empty() : JukeboxSong.fromStack(level.registryAccess(), jukebox.getTheItem());
        }
    }
}
