package li.cil.oc.common.gametest;

import li.cil.oc.api.Network;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.block.KeyboardBlock;
import li.cil.oc.common.blockentity.KeyboardBlockEntity;
import li.cil.oc.common.blockentity.ScreenBlockEntity;
import li.cil.oc.common.blockentity.ScreenItemEnvironment;
import li.cil.oc.common.component.KeyboardItemEnvironment;
import li.cil.oc.common.menu.TerminalMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@GameTestHolder("neoopencomputers")
@PrefixGameTestTemplate(false)
public final class ScreenKeyboardRoutingGameTests {
    private static ScreenBlockEntity screen(GameTestHelper h, int x, int y) {
        final var pos = h.absolutePos(new BlockPos(x, y, 2));
        h.getLevel().setBlockAndUpdate(pos, ModBlocks.SCREEN_TIER2.get().defaultBlockState());
        return (ScreenBlockEntity) h.getLevel().getBlockEntity(pos);
    }
    private static KeyboardBlockEntity keyboard(GameTestHelper h, int x, int y) {
        final var pos = h.absolutePos(new BlockPos(x, y, 3));
        h.getLevel().setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        h.getLevel().setBlockAndUpdate(pos, ModBlocks.KEYBOARD.get().defaultBlockState().setValue(KeyboardBlock.FACING, Direction.NORTH));
        return (KeyboardBlockEntity) h.getLevel().getBlockEntity(pos);
    }
    private static final class Probe implements Environment, AutoCloseable {
        private final Node node = Network.newNode(this, Visibility.Network).create();
        private final List<String> signals = new ArrayList<>();
        @Override public Node node() { return node; }
        @Override public void onConnect(Node n) { }
        @Override public void onDisconnect(Node n) { }
        @Override public void onMessage(Message m) {
            if (m.name().equals("computer.checked_signal")) signals.add(m.source().address() + ":" + m.data()[1]);
        }
        @Override public void close() { node.remove(); }
    }
    private static void assertSignals(GameTestHelper h, Probe probe, Node keyboard) {
        h.assertTrue(probe.signals.equals(List.of(keyboard.address() + ":key_down", keyboard.address() + ":key_up", keyboard.address() + ":clipboard")),
            "Input reached an unrelated keyboard or was duplicated/lost: " + probe.signals);
    }

    @GameTest(template = "ae2_export")
    public static void physicalScreenInputDoesNotReachOtherNetworkKeyboards(GameTestHelper h) {
        final var first = screen(h, 2, 2); final var second = screen(h, 6, 2);
        final var local = keyboard(h, 2, 2); final var remote = keyboard(h, 6, 2);
        try (var probe = new Probe()) {
            probe.node.connect(first.node()); probe.node.connect(second.node());
            h.assertTrue(!first.node().isNeighborOf(remote.node()) && first.node().network() == remote.node().network(), "Fixture lacks a reachable non-neighbor keyboard");
            final var player = h.makeMockPlayer(GameType.SURVIVAL);
            player.setPos(local.getBlockPos().getX() + 0.5, local.getBlockPos().getY() + 0.5, local.getBlockPos().getZ() + 0.5);
            h.assertTrue(local.isUsableByPlayer(player) && remote.isUsableByPlayer(player), "Both keyboards must be in range to test routing");
            first.keyDown('a', 30, player); first.keyUp('a', 30, player); first.clipboard("text", player);
            assertSignals(h, probe, local.node());
        }
        h.succeed();
    }

    @GameTest(template = "ae2_export")
    public static void itemScreenInputDoesNotReachOtherNetworkKeyboards(GameTestHelper h) {
        final var screen = new ScreenItemEnvironment(null, 0);
        final var local = new KeyboardItemEnvironment(); final var remote = new KeyboardItemEnvironment();
        try (var probe = new Probe()) {
            Network.joinNewNetwork(screen.node());
            screen.node().connect(local.node()); screen.node().connect(probe.node); probe.node.connect(remote.node());
            h.assertTrue(screen.node().network() == remote.node().network() && !screen.node().isNeighborOf(remote.node()), "Item fixture lacks a reachable non-neighbor keyboard");
            screen.keyDown('a', 30, null); screen.keyUp('a', 30, null); screen.clipboard("text", null);
            assertSignals(h, probe, local.node());
        } finally { screen.node().remove(); local.node().remove(); remote.node().remove(); }
        h.succeed();
    }

    @GameTest(template = "ae2_export")
    public static void multiblockListsAndUsesAllMemberKeyboards(GameTestHelper h) {
        final var first = screen(h, 2, 2); final var second = screen(h, 3, 2);
        final var left = keyboard(h, 2, 2); final var right = keyboard(h, 3, 2);
        h.assertTrue(first.originScreen() == second.originScreen(), "Fixture screens did not merge");
        final Set<String> expected = Set.of(left.node().address(), right.node().address());
        h.assertTrue(new HashSet<>(Arrays.asList((String[]) first.getKeyboards(null, null)[0])).equals(expected), "Screen did not list all keyboards of its merged group");
        h.assertTrue(new HashSet<>(Arrays.asList((String[]) second.getKeyboards(null, null)[0])).equals(expected), "Non-origin screen did not list the same merged-group keyboards");
        try (var probe = new Probe()) {
            probe.node.connect(first.node());
            final var player = h.makeMockPlayer(GameType.SURVIVAL);
            player.setPos(left.getBlockPos().getX() + 0.5, left.getBlockPos().getY() + 0.5, left.getBlockPos().getZ() + 0.5);
            second.keyDown('a', 30, player); second.keyUp('a', 30, player); second.clipboard("text", player);
            for (String address : expected) for (String event : List.of("key_down", "key_up", "clipboard"))
                h.assertTrue(java.util.Collections.frequency(probe.signals, address + ":" + event) == 1, "Merged-group keyboard did not receive each event exactly once: " + probe.signals);
            h.assertTrue(probe.signals.size() == 6, "Unexpected merged-group input signals");
        }
        h.succeed();
    }

    @GameTest(template = "ae2_export")
    public static void touchingScreenGroupsDoNotShareKeyboardPresence(GameTestHelper h) {
        final var first = screen(h, 2, 2); screen(h, 2, 3); final var other = screen(h, 3, 2);
        keyboard(h, 3, 2);
        h.assertTrue(first.originScreen() != other.originScreen(), "L-shaped fixture did not form separate rectangular groups");
        h.assertTrue(other.hasKeyboard(null), "Keyboard-owning group lost its keyboard");
        h.assertTrue(!first.hasKeyboard(null), "Another merged group supplied keyboard presence");
        h.succeed();
    }

    @GameTest(template = "ae2_export")
    public static void physicalTerminalRejectsAnotherDimension(GameTestHelper h) {
        final var screen = screen(h, 2, 2); keyboard(h, 2, 2);
        final var player = h.makeMockServerPlayerInLevel();
        try {
            final var menu = new TerminalMenu(42, player.getInventory(), screen.terminalSnapshot(), screen);
            h.assertTrue(menu.stillValid(player), "Current physical terminal is invalid");
            player.setServerLevel(h.getLevel().getServer().getLevel(Level.NETHER));
            h.assertTrue(!menu.stillValid(player) && !menu.acceptsInput(player), "Old physical terminal accepts another dimension");
        } finally { player.setServerLevel(h.getLevel()); h.getLevel().getServer().getPlayerList().remove(player); }
        h.succeed();
    }

    @GameTest(template = "ae2_export")
    public static void physicalTerminalRejectsStaleHostAndMissingPlayer(GameTestHelper h) {
        final var screen = screen(h, 2, 2); keyboard(h, 2, 2);
        final var player = h.makeMockPlayer(GameType.SURVIVAL);
        final var menu = new TerminalMenu(42, player.getInventory(), screen.terminalSnapshot(), screen);
        h.assertTrue(menu.stillValid(player), "Current physical terminal is invalid");
        h.getLevel().removeBlockEntity(screen.getBlockPos()); screen.clearRemoved();
        h.getLevel().setBlockEntity(new ScreenBlockEntity(screen.getBlockPos(), screen.getBlockState()));
        try {
            h.assertTrue(!menu.stillValid(player) && !menu.acceptsInput(player), "Old terminal accepts a replaced screen");
            h.assertTrue(!menu.stillValid(null), "Bound physical terminal accepts no player");
        } finally { screen.setRemoved(); }
        h.succeed();
    }
}
