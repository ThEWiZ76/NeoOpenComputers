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
import li.cil.oc.mixin.BaseSpawnerAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;

import javax.annotation.Nullable;
import java.util.Map;

public final class MobSpawnerBlockDriver implements DriverBlock {
    @Override
    public boolean worksWith(final Level world, final BlockPos pos, final Direction side) {
        return world != null && pos != null && world.getBlockEntity(pos) instanceof SpawnerBlockEntity;
    }

    @Override
    public ManagedEnvironment createEnvironment(final Level world, final BlockPos pos, final Direction side) {
        if (world != null && pos != null && world.getBlockEntity(pos) instanceof SpawnerBlockEntity spawner) {
            return new Environment(spawner);
        }
        return null;
    }

    public static final class Environment extends AbstractManagedEnvironment implements NamedBlock, DeviceInfo {
        private static final Map<String, String> DEVICE_INFO = Map.of(
            DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Generic,
            DeviceInfo.DeviceAttribute.Description, "Mob Spawner",
            DeviceInfo.DeviceAttribute.Vendor, "Minecraft",
            DeviceInfo.DeviceAttribute.Product, "Mob Spawner"
        );

        private final SpawnerBlockEntity spawner;

        public Environment(final SpawnerBlockEntity spawner) {
            this.spawner = spawner;
            setNode(Network.newNode(this, Visibility.Network)
                .withComponent("mob_spawner", Visibility.Network)
                .create());
        }

        @Override
        public String preferredName() {
            return "mob_spawner";
        }

        @Override
        public int priority() {
            return 0;
        }

        @Override
        public Map<String, String> getDeviceInfo() {
            return DEVICE_INFO;
        }

        @Callback(doc = "function():string -- Get the name of the entity that is being spawned by this spawner.")
        public Object[] getSpawningMobName(final Context context, final Arguments arguments) {
            return new Object[]{spawningMobName(spawner)};
        }

        @Nullable
        private static String spawningMobName(final SpawnerBlockEntity spawner) {
            final SpawnData data = ((BaseSpawnerAccessor) spawner.getSpawner()).neoopencomputers$getNextSpawnData();
            if (data == null) {
                return null;
            }
            final CompoundTag entity = data.getEntityToSpawn();
            return entity.contains("id", Tag.TAG_STRING) ? entity.getString("id") : null;
        }
    }
}
