package li.cil.oc.common.item;

import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Analyzable;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.SidedEnvironment;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AnalyzerItem extends Item {
    public AnalyzerItem(final Properties properties) {
        super(properties);
    }

    public static List<net.minecraft.network.chat.Component> describe(final Object target) {
        return describe(target, Direction.DOWN);
    }

    public static List<net.minecraft.network.chat.Component> describe(final Object target, final Direction side) {
        final ArrayList<net.minecraft.network.chat.Component> lines = new ArrayList<>();
        for (Node node : nodes(target, side)) {
            if (node != null) {
                describeNode(node, lines);
            }
        }
        return lines;
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        final Level level = context.getLevel();
        final Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        final BlockEntity blockEntity = level.getBlockEntity(context.getClickedPos());
        final List<net.minecraft.network.chat.Component> lines = describe(blockEntity, context.getClickedFace());
        if (lines.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            for (net.minecraft.network.chat.Component line : lines) {
                player.sendSystemMessage(line);
            }
        }
        return InteractionResult.CONSUME;
    }

    private static Iterable<Node> nodes(final Object target, final Direction side) {
        if (target instanceof Analyzable analyzable) {
            final Node[] nodes = analyzable.onAnalyze(null, side, 0, 0, 0);
            if (nodes == null) {
                return List.of();
            }
            final ArrayList<Node> nonNullNodes = new ArrayList<>(nodes.length);
            for (final Node node : nodes) {
                if (node != null) {
                    nonNullNodes.add(node);
                }
            }
            return nonNullNodes;
        }
        if (target instanceof SidedEnvironment sidedEnvironment) {
            final Node node = sidedEnvironment.sidedNode(side);
            return node == null ? List.of() : List.of(node);
        }
        if (target instanceof Environment environment) {
            final Node node = environment.node();
            return node == null ? List.of() : List.of(node);
        }
        return List.of();
    }

    private static void describeNode(final Node node, final List<net.minecraft.network.chat.Component> lines) {
        if (node.host() instanceof Machine machine) {
            if (machine.lastError() != null && !machine.lastError().isEmpty()) {
                lines.add(line("Last error: " + machine.lastError()));
            }
            lines.add(line("Components: " + machine.componentCount() + "/" + machine.maxComponents()));
            final String[] users = machine.users();
            if (users.length > 0) {
                lines.add(line("Users: " + String.join(", ", users)));
            }
        }
        if (node instanceof Connector connector) {
            if (connector.localBufferSize() > 0D) {
                lines.add(line("Stored energy: " + format(connector.localBuffer()) + "/" + format(connector.localBufferSize())));
            }
            lines.add(line("Total energy: " + format(connector.globalBuffer()) + "/" + format(connector.globalBufferSize())));
        }
        if (node.host() instanceof DeviceInfo deviceInfo) {
            describeDeviceInfo(deviceInfo.getDeviceInfo(), lines);
        }
        if (node instanceof Component component) {
            lines.add(line("Component: " + component.name()));
        }
        final String address = node.address();
        if (address != null && !address.isEmpty()) {
            lines.add(line("Address: " + address));
        }
    }

    private static void describeDeviceInfo(final Map<String, String> deviceInfo, final List<net.minecraft.network.chat.Component> lines) {
        if (deviceInfo == null || deviceInfo.isEmpty()) {
            return;
        }
        final List<String> orderedKeys = List.of(
            DeviceInfo.DeviceAttribute.Class,
            DeviceInfo.DeviceAttribute.Description,
            DeviceInfo.DeviceAttribute.Vendor,
            DeviceInfo.DeviceAttribute.Product,
            DeviceInfo.DeviceAttribute.Version,
            DeviceInfo.DeviceAttribute.Serial,
            DeviceInfo.DeviceAttribute.Capacity,
            DeviceInfo.DeviceAttribute.Size,
            DeviceInfo.DeviceAttribute.Clock,
            DeviceInfo.DeviceAttribute.Width);
        for (String key : orderedKeys) {
            addDeviceInfoLine(key, deviceInfo.get(key), lines);
        }
        deviceInfo.entrySet().stream()
            .filter(entry -> !orderedKeys.contains(entry.getKey()))
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> addDeviceInfoLine(entry.getKey(), entry.getValue(), lines));
    }

    private static void addDeviceInfoLine(final String key, final String value, final List<net.minecraft.network.chat.Component> lines) {
        if (value == null || value.isEmpty()) {
            return;
        }
        lines.add(line(deviceInfoLabel(key) + ": " + value));
    }

    private static String deviceInfoLabel(final String key) {
        return switch (key) {
            case DeviceInfo.DeviceAttribute.Class -> "Device class";
            case DeviceInfo.DeviceAttribute.Description -> "Description";
            case DeviceInfo.DeviceAttribute.Vendor -> "Vendor";
            case DeviceInfo.DeviceAttribute.Product -> "Product";
            case DeviceInfo.DeviceAttribute.Version -> "Version";
            case DeviceInfo.DeviceAttribute.Serial -> "Serial";
            case DeviceInfo.DeviceAttribute.Capacity -> "Capacity";
            case DeviceInfo.DeviceAttribute.Size -> "Size";
            case DeviceInfo.DeviceAttribute.Clock -> "Clock";
            case DeviceInfo.DeviceAttribute.Width -> "Width";
            default -> key;
        };
    }

    private static MutableComponent line(final String value) {
        return net.minecraft.network.chat.Component.literal(value);
    }

    private static String format(final double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
