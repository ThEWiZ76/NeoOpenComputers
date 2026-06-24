package li.cil.oc.client;

import li.cil.oc.common.menu.RackMenu;
import li.cil.oc.common.network.RackControlPayload;
import li.cil.oc.common.network.RackOpenServerPayload;
import net.minecraft.core.Direction;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class RackScreen extends AbstractContainerScreen<RackMenu> {
    private static final int FIRST_SLOT_X = 53;
    private static final int SLOT_Y = 26;
    private static final int SLOT_SPACING = 18;
    private static final int CONTROL_Y = 50;
    private static final int CONTROL_SIZE = 10;
    private static final int RELAY_X = 151;
    private static final int RELAY_Y = 50;
    private static final int RELAY_SIZE = 10;
    private static final int MAPPING_Y = 64;
    private static final int MAPPING_CELL_SIZE = 3;
    private static final int MAPPING_BUS_STEP = 3;
    private static final int MAPPING_ROW_STEP = 3;
    private static final int BUS_LABEL_X = 122;
    private static final int BUS_LABEL_Y = 20;
    private static final int BUS_LABEL_WIDTH = 36;
    private static final int BUS_LABEL_STEP = 11;
    private static final Direction DEFAULT_FRONT = Direction.NORTH;
    private static final int BUS_SIDE_COUNT = Direction.values().length - 1;

    public RackScreen(final RackMenu menu, final Inventory playerInventory, final Component title) {
        super(menu, playerInventory, title);
        imageHeight = 166;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void renderBg(final GuiGraphics guiGraphics, final float partialTick, final int mouseX, final int mouseY) {
        final int left = leftPos;
        final int top = topPos;
        guiGraphics.fill(left, top, left + imageWidth, top + imageHeight, 0xFF2E3440);
        guiGraphics.fill(left + 7, top + 16, left + 169, top + 76, 0xFF3B4252);
        drawRelayControl(guiGraphics, left + RELAY_X, top + RELAY_Y, menu.rackRelayEnabled());
        for (int slot = 0; slot < RackMenu.RACK_SLOT_COUNT; slot++) {
            drawSlot(guiGraphics, left + FIRST_SLOT_X - 1 + slot * SLOT_SPACING, top + SLOT_Y - 1);
            drawControl(guiGraphics, left + FIRST_SLOT_X + 3 + slot * SLOT_SPACING, top + CONTROL_Y, controlColor(menu.rackState(slot)));
            drawMappingControls(guiGraphics, menu, left, top, slot);
        }
    }

    @Override
    public void render(final GuiGraphics guiGraphics, final int mouseX, final int mouseY, final float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
        final List<Component> mappingTooltip = mappingTooltip(menu, mappingControlAt(mouseX, mouseY, leftPos, topPos));
        if (!mappingTooltip.isEmpty()) {
            guiGraphics.renderComponentTooltip(font, mappingTooltip, mouseX, mouseY);
        }
        if (relayControlAt(mouseX, mouseY, leftPos, topPos)) {
            guiGraphics.renderComponentTooltip(font, relayTooltip(menu), mouseX, mouseY);
        }
        if (busLabelAt(mouseX, mouseY, leftPos, topPos) >= 0) {
            guiGraphics.renderComponentTooltip(font, orientationTooltip(), mouseX, mouseY);
        }
        final int slot = controlSlotAt(mouseX, mouseY, leftPos, topPos);
        if (slot >= 0) {
            guiGraphics.renderComponentTooltip(font, controlTooltip(menu.rackState(slot), menu.rackMissingRequirements(slot)), mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(final GuiGraphics guiGraphics, final int mouseX, final int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        for (int busIndex = 0; busIndex < BUS_SIDE_COUNT; busIndex++) {
            guiGraphics.drawString(
                font,
                busLabel(menu.rackFacing(), busIndex),
                BUS_LABEL_X,
                BUS_LABEL_Y + busIndex * BUS_LABEL_STEP,
                0x404040,
                false);
        }
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (button == 0) {
            if (relayControlAt(mouseX, mouseY, leftPos, topPos)) {
                PacketDistributor.sendToServer(relayPayload(menu));
                return true;
            }
            final MappingControl mappingControl = mappingControlAt(mouseX, mouseY, leftPos, topPos);
            final RackControlPayload payload = mappingPayload(menu, mappingControl);
            if (payload != null) {
                PacketDistributor.sendToServer(payload);
                return true;
            }
        }
        final int slot = controlSlotAt(mouseX, mouseY, leftPos, topPos);
        if (button == 0 && slot >= 0) {
            PacketDistributor.sendToServer(controlPayload(menu, slot, RackControlPayload.TOGGLE));
            return true;
        }
        if (button == 1 && slot >= 0) {
            PacketDistributor.sendToServer(openServerPayload(menu, slot));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    static RackControlPayload controlPayload(final RackMenu menu, final int slot, final int action) {
        return new RackControlPayload(menu.containerId, slot, action);
    }

    static RackControlPayload mappingPayload(final RackMenu menu, final int slot, final int connectableIndex, final Direction side) {
        return RackControlPayload.map(menu.containerId, slot, connectableIndex, side);
    }

    static RackControlPayload mappingPayload(final RackMenu menu, final MappingControl control) {
        if (control == null || !menu.rackNodePresent(control.slot(), control.connectableIndex())) {
            return null;
        }
        final Direction side = busSide(menu.rackFacing(), control.busIndex());
        if (side == null) {
            return null;
        }
        final int selectedSide = menu.rackNodeMapping(control.slot(), control.connectableIndex());
        return mappingPayload(
            menu,
            control.slot(),
            control.connectableIndex() - 1,
            selectedSide == side.ordinal() ? null : side);
    }

    static RackOpenServerPayload openServerPayload(final RackMenu menu, final int slot) {
        return new RackOpenServerPayload(menu.containerId, slot);
    }

    static RackControlPayload relayPayload(final RackMenu menu) {
        return RackControlPayload.relay(menu.containerId, !menu.rackRelayEnabled());
    }

    static boolean relayControlAt(final double mouseX, final double mouseY, final int left, final int top) {
        final int x = left + RELAY_X;
        final int y = top + RELAY_Y;
        return mouseX >= x && mouseX < x + RELAY_SIZE && mouseY >= y && mouseY < y + RELAY_SIZE;
    }

    static MappingControl mappingControlAt(final double mouseX, final double mouseY, final int left, final int top) {
        for (int slot = 0; slot < RackMenu.RACK_SLOT_COUNT; slot++) {
            for (int connectableIndex = 0; connectableIndex < 4; connectableIndex++) {
                for (int busIndex = 0; busIndex < BUS_SIDE_COUNT; busIndex++) {
                    final int x = left + FIRST_SLOT_X + slot * SLOT_SPACING + busIndex * MAPPING_BUS_STEP;
                    final int y = top + MAPPING_Y + connectableIndex * MAPPING_ROW_STEP;
                    if (mouseX >= x && mouseX < x + MAPPING_CELL_SIZE && mouseY >= y && mouseY < y + MAPPING_CELL_SIZE) {
                        return new MappingControl(slot, connectableIndex, busIndex);
                    }
                }
            }
        }
        return null;
    }

    static int busLabelAt(final double mouseX, final double mouseY, final int left, final int top) {
        final int x = left + BUS_LABEL_X;
        final int y = top + BUS_LABEL_Y;
        if (mouseX < x || mouseX >= x + BUS_LABEL_WIDTH || mouseY < y || mouseY >= y + BUS_SIDE_COUNT * BUS_LABEL_STEP) {
            return -1;
        }
        return (int) ((mouseY - y) / BUS_LABEL_STEP);
    }

    static Direction busSide(final int busIndex) {
        return busSide(DEFAULT_FRONT, busIndex);
    }

    static Direction busSide(final Direction front, final int busIndex) {
        if (busIndex < 0 || busIndex >= BUS_SIDE_COUNT) {
            return null;
        }
        final Direction skippedFront = front == null ? DEFAULT_FRONT : front;
        int index = 0;
        for (final Direction side : Direction.values()) {
            if (side == skippedFront) {
                continue;
            }
            if (index == busIndex) {
                return side;
            }
            index++;
        }
        return null;
    }

    static int busIndex(final Direction side) {
        return busIndex(DEFAULT_FRONT, side);
    }

    static int busIndex(final Direction front, final Direction side) {
        if (side == null || side == front) {
            return -1;
        }
        for (int index = 0; index < BUS_SIDE_COUNT; index++) {
            if (busSide(front, index) == side) {
                return index;
            }
        }
        return -1;
    }

    static int controlSlotAt(final double mouseX, final double mouseY, final int left, final int top) {
        for (int slot = 0; slot < RackMenu.RACK_SLOT_COUNT; slot++) {
            final int x = left + FIRST_SLOT_X + 3 + slot * SLOT_SPACING;
            final int y = top + CONTROL_Y;
            if (mouseX >= x && mouseX < x + CONTROL_SIZE && mouseY >= y && mouseY < y + CONTROL_SIZE) {
                return slot;
            }
        }
        return -1;
    }

    static int controlColor(final int state) {
        return switch (state) {
            case RackMenu.STATE_READY -> 0xFFA3BE8C;
            case RackMenu.STATE_RUNNING -> 0xFF88C0D0;
            case RackMenu.STATE_INCOMPLETE -> 0xFFD08770;
            default -> 0xFF4C566A;
        };
    }

    static Component stateLabel(final int state) {
        return switch (state) {
            case RackMenu.STATE_READY -> Component.translatable("gui.neoopencomputers.rack.state.ready");
            case RackMenu.STATE_RUNNING -> Component.translatable("gui.neoopencomputers.rack.state.running");
            case RackMenu.STATE_INCOMPLETE -> Component.translatable("gui.neoopencomputers.rack.state.incomplete");
            default -> Component.translatable("gui.neoopencomputers.rack.state.empty");
        };
    }

    static List<Component> controlTooltip(final int state) {
        return controlTooltip(state, 0);
    }

    static List<Component> controlTooltip(final int state, final int missingRequirements) {
        final List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.translatable("gui.neoopencomputers.rack.control"));
        tooltip.add(stateLabel(state));
        if ((missingRequirements & RackMenu.MISSING_CPU) != 0) {
            tooltip.add(Component.translatable("gui.neoopencomputers.rack.missing.cpu"));
        }
        if ((missingRequirements & RackMenu.MISSING_MEMORY) != 0) {
            tooltip.add(Component.translatable("gui.neoopencomputers.rack.missing.memory"));
        }
        if ((missingRequirements & RackMenu.MISSING_EEPROM) != 0) {
            tooltip.add(Component.translatable("gui.neoopencomputers.rack.missing.eeprom"));
        }
        return tooltip;
    }

    static List<Component> mappingTooltip(final RackMenu menu, final MappingControl control) {
        final List<Component> tooltip = new ArrayList<>();
        if (control == null || !menu.rackNodePresent(control.slot(), control.connectableIndex())) {
            return tooltip;
        }
        final Direction side = busSide(menu.rackFacing(), control.busIndex());
        if (side == null) {
            return tooltip;
        }
        tooltip.add(Component.translatable("gui.neoopencomputers.rack.bus"));
        tooltip.add(sideLabel(menu.rackFacing(), side));
        tooltip.add(Component.translatable(menu.rackNodeMapping(control.slot(), control.connectableIndex()) == side.ordinal()
            ? "gui.neoopencomputers.rack.bus.clear"
            : "gui.neoopencomputers.rack.bus.map"));
        return tooltip;
    }

    static List<Component> relayTooltip(final RackMenu menu) {
        final List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.translatable("gui.neoopencomputers.rack.relay"));
        tooltip.add(Component.translatable(menu.rackRelayEnabled()
            ? "gui.neoopencomputers.rack.relay.enabled"
            : "gui.neoopencomputers.rack.relay.disabled"));
        return tooltip;
    }

    static List<Component> orientationTooltip() {
        final List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.translatable("gui.neoopencomputers.rack.orientation.line1"));
        tooltip.add(Component.translatable("gui.neoopencomputers.rack.orientation.line2"));
        tooltip.add(Component.translatable("gui.neoopencomputers.rack.orientation.line3"));
        tooltip.add(Component.translatable("gui.neoopencomputers.rack.orientation.line4"));
        return tooltip;
    }

    static Component sideLabel(final Direction side) {
        return sideLabel(DEFAULT_FRONT, side);
    }

    static Component sideLabel(final Direction front, final Direction side) {
        return Component.translatable("gui.neoopencomputers.rack.bus.side." + localSideKey(front, side));
    }

    static Component busLabel(final Direction front, final int busIndex) {
        return Component.translatable("gui.neoopencomputers.rack.bus.label." + localSideKey(front, busSide(front, busIndex)));
    }

    private static String localSideKey(final Direction front, final Direction side) {
        if (side == Direction.UP) {
            return "top";
        }
        if (side == Direction.DOWN) {
            return "bottom";
        }

        final Direction horizontalFront = front == null || front.getAxis().isVertical() ? DEFAULT_FRONT : front;
        if (side == horizontalFront) {
            return "front";
        }
        if (side == horizontalFront.getOpposite()) {
            return "back";
        }
        if (side == horizontalFront.getClockWise()) {
            return "left";
        }
        if (side == horizontalFront.getCounterClockWise()) {
            return "right";
        }
        return side == null ? "unknown" : side.getSerializedName();
    }

    private static void drawSlot(final GuiGraphics guiGraphics, final int left, final int top) {
        guiGraphics.fill(left - 1, top - 1, left + 17, top + 17, 0xFF1F232B);
        guiGraphics.fill(left, top, left + 16, top + 16, 0xFF4C566A);
    }

    private static void drawControl(final GuiGraphics guiGraphics, final int left, final int top, final int color) {
        guiGraphics.fill(left, top, left + CONTROL_SIZE, top + CONTROL_SIZE, 0xFF1F232B);
        guiGraphics.fill(left + 3, top + 2, left + 5, top + 8, color);
        guiGraphics.fill(left + 5, top + 3, left + 7, top + 7, color);
        guiGraphics.fill(left + 7, top + 4, left + 8, top + 6, color);
    }

    private static void drawRelayControl(final GuiGraphics guiGraphics, final int left, final int top, final boolean enabled) {
        final int color = enabled ? 0xFFA3BE8C : 0xFF6C7480;
        guiGraphics.fill(left, top, left + RELAY_SIZE, top + RELAY_SIZE, 0xFF1F232B);
        guiGraphics.fill(left + 2, top + 4, left + 8, top + 6, color);
        guiGraphics.fill(left + 4, top + 2, left + 6, top + 8, color);
    }

    private static void drawMappingControls(final GuiGraphics guiGraphics, final RackMenu menu, final int left, final int top, final int slot) {
        for (int connectableIndex = 0; connectableIndex < 4; connectableIndex++) {
            if (!menu.rackNodePresent(slot, connectableIndex)) {
                continue;
            }
            final int selectedSide = menu.rackNodeMapping(slot, connectableIndex);
            for (int busIndex = 0; busIndex < BUS_SIDE_COUNT; busIndex++) {
                final int x = left + FIRST_SLOT_X + slot * SLOT_SPACING + busIndex * MAPPING_BUS_STEP;
                final int y = top + MAPPING_Y + connectableIndex * MAPPING_ROW_STEP;
                final Direction side = busSide(menu.rackFacing(), busIndex);
                final int color = side != null && selectedSide == side.ordinal() ? 0xFFA3BE8C : 0xFF6C7480;
                guiGraphics.fill(x, y, x + MAPPING_CELL_SIZE, y + MAPPING_CELL_SIZE, color);
            }
        }
    }

    record MappingControl(int slot, int connectableIndex, int busIndex) {
    }
}
