package li.cil.oc.common.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.event.SignChangeEvent;
import li.cil.oc.api.internal.Rotatable;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.neoforged.neoforge.common.NeoForge;

import java.util.Arrays;
import java.util.Map;

public class SignUpgradeEnvironment extends AbstractManagedEnvironment implements DeviceInfo {
    private static final String COMPONENT_NAME = "sign";
    private static final int LINE_COUNT = 4;
    private static final int MAX_LINE_LENGTH = 15;
    private static final Map<String, String> DEVICE_INFO = Map.of(
        DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Generic,
        DeviceInfo.DeviceAttribute.Description, "Sign upgrade",
        DeviceInfo.DeviceAttribute.Vendor, "MightyPirates",
        DeviceInfo.DeviceAttribute.Product, "Labelizer Deluxe"
    );

    private final EnvironmentHost host;
    private final Rotatable rotatable;

    public SignUpgradeEnvironment(final EnvironmentHost host) {
        this(host, null);
    }

    public SignUpgradeEnvironment(final EnvironmentHost host, final Rotatable rotatable) {
        this.host = host;
        this.rotatable = rotatable;
        final var builder = Network.newNode(this, Visibility.Network);
        if (builder != null) {
            setNode(builder.withComponent(COMPONENT_NAME, Visibility.Network).withConnector().create());
        }
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return DEVICE_INFO;
    }

    @Override
    public void onMessage(final Message message) {
        super.onMessage(message);
        if (message == null || !"tablet.use".equals(message.name())) {
            return;
        }
        final Object[] data = message.data();
        if (data.length < 4 || !(data[0] instanceof CompoundTag nbt) || !(data[3] instanceof BlockPos blockPos)) {
            return;
        }
        if (host.world().getBlockEntity(blockPos) instanceof SignBlockEntity sign) {
            nbt.putString("signText", text(sign));
        }
    }

    @Callback(doc = "function():string -- Get the text on the sign in front of the host.")
    public Object[] getValue(final Context context, final Arguments arguments) {
        final SignBlockEntity sign = findSign();
        if (sign == null) {
            return new Object[]{null, "no sign"};
        }
        return new Object[]{text(sign)};
    }

    @Callback(doc = "function(value:string):string -- Set the text on the sign in front of the host.")
    public Object[] setValue(final Context context, final Arguments arguments) {
        final SignBlockEntity sign = findSign();
        if (sign == null) {
            return new Object[]{null, "no sign"};
        }

        final String[] lines = normalizedLines(arguments.checkString(0));
        final SignChangeEvent.Pre pre = new SignChangeEvent.Pre(sign, lines);
        NeoForge.EVENT_BUS.post(pre);
        if (pre.isCanceled()) {
            return new Object[]{null, "not allowed"};
        }

        SignText text = sign.getFrontText();
        for (int i = 0; i < LINE_COUNT; i++) {
            text = text.setMessage(i, Component.literal(lines[i]));
        }
        sign.setText(text, true);
        NeoForge.EVENT_BUS.post(new SignChangeEvent.Post(sign, lines));
        host.markChanged();
        return new Object[]{text(sign)};
    }

    private SignBlockEntity findSign() {
        final BlockPos hostPos = hostPosition();
        if (host.world().getBlockEntity(hostPos) instanceof SignBlockEntity sign) {
            return sign;
        }
        if (rotatable != null) {
            final BlockPos frontPos = hostPos.relative(rotatable.facing());
            if (host.world().getBlockEntity(frontPos) instanceof SignBlockEntity sign) {
                return sign;
            }
        } else {
            for (final Direction direction : Direction.values()) {
                final BlockPos adjacentPos = hostPos.relative(direction);
                if (host.world().getBlockEntity(adjacentPos) instanceof SignBlockEntity sign) {
                    return sign;
                }
            }
        }
        return null;
    }

    private BlockPos hostPosition() {
        return BlockPos.containing(host.xPosition(), host.yPosition(), host.zPosition());
    }

    private static String text(final SignBlockEntity sign) {
        return Arrays.stream(sign.getFrontText().getMessages(false))
            .map(Component::getString)
            .reduce((left, right) -> left + "\n" + right)
            .orElse("");
    }

    private static String[] normalizedLines(final String value) {
        final String[] input = value.split("\\R", -1);
        final String[] lines = new String[LINE_COUNT];
        for (int i = 0; i < LINE_COUNT; i++) {
            final String line = i < input.length ? input[i] : "";
            lines[i] = line.length() > MAX_LINE_LENGTH ? line.substring(0, MAX_LINE_LENGTH) : line;
        }
        return lines;
    }
}
