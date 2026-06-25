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
import li.cil.oc.common.ModSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BaseCommandBlock;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

public final class CommandBlockDriver implements DriverBlock {
    @Override
    public boolean worksWith(final Level world, final BlockPos pos, final Direction side) {
        return ModSettings.enableCommandBlockDriver()
            && world != null
            && pos != null
            && world.getBlockEntity(pos) instanceof CommandBlockEntity;
    }

    @Override
    public ManagedEnvironment createEnvironment(final Level world, final BlockPos pos, final Direction side) {
        if (worksWith(world, pos, side) && world.getBlockEntity(pos) instanceof CommandBlockEntity commandBlock) {
            return new Environment(commandBlock);
        }
        return null;
    }

    public static final class Environment extends AbstractManagedEnvironment implements NamedBlock, DeviceInfo {
        private static final Map<String, String> DEVICE_INFO = Map.of(
            DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Generic,
            DeviceInfo.DeviceAttribute.Description, "Command block",
            DeviceInfo.DeviceAttribute.Vendor, "Minecraft",
            DeviceInfo.DeviceAttribute.Product, "Command block"
        );

        private final CommandBlockEntity commandBlock;

        public Environment(final CommandBlockEntity commandBlock) {
            this.commandBlock = commandBlock;
            setNode(Network.newNode(this, Visibility.Network)
                .withComponent("command_block", Visibility.Network)
                .create());
        }

        @Override
        public String preferredName() {
            return "command_block";
        }

        @Override
        public int priority() {
            return 0;
        }

        @Override
        public Map<String, String> getDeviceInfo() {
            return DEVICE_INFO;
        }

        @Callback(direct = true, doc = "function():string -- Get the command currently set in this command block.")
        public Object[] getCommand(final Context context, final Arguments arguments) {
            return new Object[]{commandBlock.getCommandBlock().getCommand()};
        }

        @Callback(doc = "function(value:string) -- Set the specified command for the command block.")
        public Object[] setCommand(final Context context, final Arguments arguments) {
            commandBlock.getCommandBlock().setCommand(arguments.checkString(0));
            final Level level = commandBlock.getLevel();
            if (level != null) {
                final BlockState state = commandBlock.getBlockState();
                level.sendBlockUpdated(commandBlock.getBlockPos(), state, state, 3);
            }
            return new Object[]{true};
        }

        @Callback(doc = "function():number -- Execute the currently set command. This has a slight delay to allow the command block to properly update.")
        public Object[] executeCommand(final Context context, final Arguments arguments) {
            context.pause(0.1);
            final Level level = commandBlock.getLevel();
            if (!(level instanceof ServerLevel serverLevel) || !serverLevel.getServer().isCommandBlockEnabled()) {
                return new Object[]{null, "command blocks are disabled"};
            }
            final BaseCommandBlock command = commandBlock.getCommandBlock();
            command.performCommand(level);
            final Component output = command.getLastOutput();
            return new Object[]{command.getSuccessCount(), output.getString()};
        }
    }
}
