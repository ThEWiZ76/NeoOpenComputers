package li.cil.oc.common.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.internal.TextBuffer;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.machine.LimitReachedException;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.ModSettings;
import li.cil.oc.common.OpenComputersApi;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GraphicsCardEnvironmentTest {
    @Test
    void exposesGpuCallbacks() throws NoSuchMethodException {
        assertCallback("bind");
        assertCallback("getScreen");
        assertCallback("getResolution");
        assertCallback("setResolution");
        assertCallback("maxResolution");
        assertCallback("getViewport");
        assertCallback("setViewport");
        assertCallback("getBackground");
        assertCallback("setBackground");
        assertCallback("getForeground");
        assertCallback("setForeground");
        assertCallback("getPaletteColor");
        assertCallback("setPaletteColor");
        assertCallback("getActiveBuffer");
        assertCallback("setActiveBuffer");
        assertCallback("buffers");
        assertCallback("allocateBuffer");
        assertCallback("freeBuffer");
        assertCallback("freeAllBuffers");
        assertCallback("totalMemory");
        assertCallback("freeMemory");
        assertCallback("getBufferSize");
        assertCallback("getDepth");
        assertCallback("setDepth");
        assertCallback("maxDepth");
        assertCallback("get");
        assertCallback("set");
        assertCallback("copy");
        assertCallback("fill");
        assertCallback("bitblt");
    }

    @Test
    void createsGpuComponentConnectorNode() {
        OpenComputersApi.initialize();

        GraphicsCardEnvironment gpu = new GraphicsCardEnvironment(0);

        assertNotNull(gpu.node());
        assertInstanceOf(ComponentConnector.class, gpu.node());
    }

    @Test
    void exposesDeviceInfoMetadata() {
        OpenComputersApi.initialize();
        GraphicsCardEnvironment gpu = new GraphicsCardEnvironment(0);

        DeviceInfo info = assertInstanceOf(DeviceInfo.class, gpu);
        Map<String, String> metadata = info.getDeviceInfo();

        assertEquals(DeviceInfo.DeviceClass.Display, metadata.get(DeviceInfo.DeviceAttribute.Class));
        assertEquals("Graphics controller", metadata.get(DeviceInfo.DeviceAttribute.Description));
        assertEquals("MPG1000 GTZ", metadata.get(DeviceInfo.DeviceAttribute.Product));
        assertEquals("800", metadata.get(DeviceInfo.DeviceAttribute.Capacity));
        assertEquals("1", metadata.get(DeviceInfo.DeviceAttribute.Width));
    }

    @Test
    void gpuLimitsUseConfiguredScreenTierSettings() throws Exception {
        withCachedConfig(ModSettings.SCREEN_WIDTHS_BY_TIER, java.util.List.of(11, 13, 17), () ->
            withCachedConfig(ModSettings.SCREEN_HEIGHTS_BY_TIER, java.util.List.of(3, 5, 7), () ->
                withCachedConfig(ModSettings.SCREEN_DEPTHS_BY_TIER, java.util.List.of(1, 1, 4), () -> {
                    OpenComputersApi.initialize();
                    GraphicsCardEnvironment gpu = new GraphicsCardEnvironment(1);

                    assertEquals(13 * 5 * 2, ((Number) gpu.totalMemory(null, new TestArguments())[0]).intValue());
                    assertArrayEquals(new Object[]{1}, gpu.allocateBuffer(null, new TestArguments()));
                    assertArrayEquals(new Object[]{13, 5}, gpu.getBufferSize(null, new TestArguments(1)));
                    assertArrayEquals(new Object[]{0}, gpu.setActiveBuffer(null, new TestArguments(1)));
                    assertArrayEquals(new Object[]{1}, gpu.maxDepth(null, new TestArguments()));

                    DeviceInfo info = assertInstanceOf(DeviceInfo.class, gpu);
                    assertEquals("65", info.getDeviceInfo().get(DeviceInfo.DeviceAttribute.Capacity));
                    assertEquals("1", info.getDeviceInfo().get(DeviceInfo.DeviceAttribute.Width));
                })));
    }

    @Test
    void totalVideoMemoryUsesUpstreamTierMultipliers() {
        OpenComputersApi.initialize();

        assertEquals(50 * 16, ((Number) new GraphicsCardEnvironment(0).totalMemory(null, new TestArguments())[0]).intValue());
        assertEquals(80 * 25 * 2, ((Number) new GraphicsCardEnvironment(1).totalMemory(null, new TestArguments())[0]).intValue());
        assertEquals(160 * 50 * 3, ((Number) new GraphicsCardEnvironment(2).totalMemory(null, new TestArguments())[0]).intValue());
    }

    @Test
    void reportsNoScreenBeforeBind() {
        OpenComputersApi.initialize();
        GraphicsCardEnvironment gpu = new GraphicsCardEnvironment(0);

        assertArrayEquals(new Object[]{null, "no screen"}, gpu.getScreen(null, new TestArguments()));
    }

    @Test
    void bindsConnectedScreenByAddress() {
        OpenComputersApi.initialize();
        GraphicsCardEnvironment gpu = new GraphicsCardEnvironment(0);
        FakeTextBuffer screen = new FakeTextBuffer();
        Network.joinNewNetwork(gpu.node());
        gpu.node().connect(screen.node());

        assertArrayEquals(new Object[]{true}, gpu.bind(null, new TestArguments(screen.node().address(), true)));
        assertArrayEquals(new Object[]{screen.node().address()}, gpu.getScreen(null, new TestArguments()));
        assertArrayEquals(new Object[]{50, 16}, gpu.getResolution(null, new TestArguments()));
    }

    @Test
    void bindsPhysicalMultiblockScreensThroughOriginBuffer() throws Exception {
        final String source = Files.readString(Path.of("src/main/java/li/cil/oc/common/component/GraphicsCardEnvironment.java"));

        assertTrue(source.contains("originScreen()"), "Physical screen binding must normalize multiblock child screens to the origin screen");
        assertTrue(source.contains("bindingTarget"), "GPU binding should share the same origin normalization for explicit and automatic binds");
    }

    @Test
    void leavesNonPhysicalScreenBindingsUntouched() {
        FakeTextBuffer screen = new FakeTextBuffer();

        assertEquals(screen, GraphicsCardEnvironment.bindingTarget(screen));
    }

    @Test
    void bindsReachableScreenWhenComputerStarts() {
        OpenComputersApi.initialize();
        GraphicsCardEnvironment gpu = new GraphicsCardEnvironment(0);
        FakeEnvironment computer = new FakeEnvironment();
        FakeTextBuffer screen = new FakeTextBuffer();
        Network.joinNewNetwork(computer.node());
        computer.node().connect(gpu.node());
        computer.node().connect(screen.node());

        gpu.onMessage(new TestMessage(computer.node(), "computer.started", new Object[0]));

        assertArrayEquals(new Object[]{screen.node().address()}, gpu.getScreen(null, new TestArguments()));
        assertArrayEquals(new Object[]{50, 16}, gpu.getResolution(null, new TestArguments()));
    }

    @Test
    void setResolutionUsesUpstreamAreaLimitInsteadOfGpuMaxHeight() {
        OpenComputersApi.initialize();
        GraphicsCardEnvironment gpu = new GraphicsCardEnvironment(0);
        FakeTextBuffer screen = new FakeTextBuffer();
        Network.joinNewNetwork(gpu.node());
        gpu.node().connect(screen.node());
        gpu.bind(null, new TestArguments(screen.node().address(), true));

        assertArrayEquals(new Object[]{true}, gpu.setResolution(null, new TestArguments(20, 20)));
        assertArrayEquals(new Object[]{20, 20}, gpu.getResolution(null, new TestArguments()));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
            () -> gpu.setResolution(null, new TestArguments(41, 20)));
        assertEquals("unsupported resolution", error.getMessage());
    }

    @Test
    void computerStoppedMessageKeepsBindingAndResetsScreen() {
        OpenComputersApi.initialize();
        GraphicsCardEnvironment gpu = new GraphicsCardEnvironment(0);
        FakeTextBuffer screen = new FakeTextBuffer();
        Network.joinNewNetwork(gpu.node());
        gpu.node().connect(screen.node());
        gpu.bind(null, new TestArguments(screen.node().address(), true));
        screen.setResolution(10, 5);
        screen.setForegroundColor(0x112233);
        screen.setBackgroundColor(0x445566);
        gpu.allocateBuffer(null, new TestArguments(2, 1));
        gpu.setActiveBuffer(null, new TestArguments(1));

        gpu.onMessage(new TestMessage(screen.node(), "computer.stopped", new Object[0]));

        assertArrayEquals(new Object[]{screen.node().address()}, gpu.getScreen(null, new TestArguments()));
        assertArrayEquals(new Object[]{0}, gpu.getActiveBuffer(null, new TestArguments()));
        assertArrayEquals(new int[0], (int[]) gpu.buffers(null, new TestArguments())[0]);
        assertArrayEquals(new Object[]{50, 16}, gpu.getResolution(null, new TestArguments()));
        assertArrayEquals(new Object[]{0xFFFFFF, false}, gpu.getForeground(null, new TestArguments()));
        assertArrayEquals(new Object[]{0x000000, false}, gpu.getBackground(null, new TestArguments()));
    }

    @Test
    void delegatesPaletteColorCallbacksToScreen() throws Exception {
        OpenComputersApi.initialize();
        GraphicsCardEnvironment gpu = new GraphicsCardEnvironment(0);
        FakeTextBuffer screen = new FakeTextBuffer();
        Network.joinNewNetwork(gpu.node());
        gpu.node().connect(screen.node());
        gpu.bind(null, new TestArguments(screen.node().address(), true));
        screen.setPaletteColor(2, 0x112233);

        assertArrayEquals(new Object[]{0x112233}, gpu.getPaletteColor(null, new TestArguments(2)));
        assertArrayEquals(new Object[]{0x112233}, gpu.setPaletteColor(null, new TestArguments(2, 0x445566)));
        assertArrayEquals(new Object[]{0x445566}, gpu.getPaletteColor(null, new TestArguments(2)));
    }

    @Test
    void rejectsInvalidPaletteColorIndexes() {
        OpenComputersApi.initialize();
        GraphicsCardEnvironment gpu = new GraphicsCardEnvironment(0);
        gpu.allocateBuffer(null, new TestArguments(2, 1));
        gpu.setActiveBuffer(null, new TestArguments(1));

        IllegalArgumentException getError = assertThrows(IllegalArgumentException.class,
            () -> gpu.getPaletteColor(null, new TestArguments(16)));
        IllegalArgumentException setError = assertThrows(IllegalArgumentException.class,
            () -> gpu.setPaletteColor(null, new TestArguments(-1, 0x112233)));

        assertEquals("invalid palette index", getError.getMessage());
        assertEquals("invalid palette index", setError.getMessage());
    }

    @Test
    void screenMutationsConsumeCallBudget() throws Exception {
        OpenComputersApi.initialize();
        GraphicsCardEnvironment gpu = new GraphicsCardEnvironment(0);
        FakeTextBuffer screen = new FakeTextBuffer();
        RecordingContext context = new RecordingContext();
        Network.joinNewNetwork(gpu.node());
        gpu.node().connect(screen.node());
        gpu.bind(null, new TestArguments(screen.node().address(), true));

        gpu.setBackground(context, new TestArguments(0x111111));
        gpu.setForeground(context, new TestArguments(0x222222));
        gpu.setPaletteColor(context, new TestArguments(2, 0x333333));
        gpu.set(context, new TestArguments(1, 1, "ABC"));
        gpu.copy(context, new TestArguments(1, 1, 2, 3, 1, 0));
        gpu.fill(context, new TestArguments(1, 1, 4, 5, " "));

        assertEquals(0.671875D, context.callBudget, 0.0000001D);
    }

    @Test
    void videoBufferMutationsDoNotConsumeScreenCallBudget() throws Exception {
        OpenComputersApi.initialize();
        GraphicsCardEnvironment gpu = new GraphicsCardEnvironment(0);
        RecordingContext context = new RecordingContext();
        gpu.allocateBuffer(null, new TestArguments(4, 2));
        gpu.setActiveBuffer(null, new TestArguments(1));

        gpu.setBackground(context, new TestArguments(0x111111));
        gpu.setForeground(context, new TestArguments(0x222222));
        gpu.setPaletteColor(context, new TestArguments(2, 0x333333));
        gpu.set(context, new TestArguments(1, 1, "ABC"));
        gpu.copy(context, new TestArguments(1, 1, 2, 1, 1, 0));
        gpu.fill(context, new TestArguments(1, 1, 2, 1, " "));

        assertEquals(0D, context.callBudget, 0.0000001D);
    }

    @Test
    void paletteColorMutationPausesOnlyWhenWritingScreen() throws Exception {
        OpenComputersApi.initialize();
        GraphicsCardEnvironment gpu = new GraphicsCardEnvironment(0);
        FakeTextBuffer screen = new FakeTextBuffer();
        RecordingContext context = new RecordingContext();
        Network.joinNewNetwork(gpu.node());
        gpu.node().connect(screen.node());
        gpu.bind(null, new TestArguments(screen.node().address(), true));

        gpu.setPaletteColor(context, new TestArguments(2, 0x333333));

        assertEquals(0.1D, context.pauseSeconds, 0.000_001D);

        context.pauseSeconds = -1D;
        gpu.allocateBuffer(null, new TestArguments(2, 1));
        gpu.setActiveBuffer(null, new TestArguments(1));
        gpu.setPaletteColor(context, new TestArguments(2, 0x444444));

        assertEquals(-1D, context.pauseSeconds, 0.000_001D);
    }

    @Test
    void screenMutationsConsumeConfiguredEnergy() throws Exception {
        withCachedConfig(ModSettings.GPU_SET_COST, 80D, () ->
            withCachedConfig(ModSettings.GPU_COPY_COST, 20D, () ->
                withCachedConfig(ModSettings.GPU_FILL_COST, 40D, () ->
                    withCachedConfig(ModSettings.GPU_CLEAR_COST, 8D, () -> {
                        OpenComputersApi.initialize();
                        GraphicsCardEnvironment gpu = new GraphicsCardEnvironment(0);
                        FakeTextBuffer screen = new FakeTextBuffer();
                        ComponentConnector connector = assertInstanceOf(ComponentConnector.class, gpu.node());
                        connector.setLocalBufferSize(10D);
                        connector.changeBuffer(10D);
                        Network.joinNewNetwork(gpu.node());
                        gpu.node().connect(screen.node());
                        gpu.bind(null, new TestArguments(screen.node().address(), true));

                        gpu.set(null, new TestArguments(1, 1, "ABC"));
                        gpu.copy(null, new TestArguments(1, 1, 2, 3, 1, 0));
                        gpu.fill(null, new TestArguments(1, 1, 4, 5, "Z"));
                        gpu.fill(null, new TestArguments(1, 1, 2, 5, " "));

                        assertEquals(8.45D, connector.localBuffer(), 0.000_001D);
                    }))));
    }

    @Test
    void bitbltToScreenConsumesConfiguredEnergy() throws Exception {
        withCachedConfig(ModSettings.GPU_COPY_COST, 120D, () -> {
            OpenComputersApi.initialize();
            GraphicsCardEnvironment gpu = new GraphicsCardEnvironment(0);
            FakeTextBuffer screen = new FakeTextBuffer();
            ComponentConnector connector = assertInstanceOf(ComponentConnector.class, gpu.node());
            connector.setLocalBufferSize(1D);
            connector.changeBuffer(1D);
            Network.joinNewNetwork(gpu.node());
            gpu.node().connect(screen.node());
            gpu.bind(null, new TestArguments(screen.node().address(), true));
            gpu.allocateBuffer(null, new TestArguments(3, 2));
            gpu.setActiveBuffer(null, new TestArguments(1));

            assertArrayEquals(new Object[]{true}, gpu.bitblt(null, new TestArguments(0, 1, 1, 3, 2, 1, 1, 1)));

            assertEquals(0.94D, connector.localBuffer(), 0.000_001D);
        });
    }

    @Test
    void bitbltDirtyVideoBufferToScreenConsumesConfiguredBudget() throws Exception {
        withCachedConfig(ModSettings.GPU_BITBLT_COST, 8D, () -> {
            OpenComputersApi.initialize();
            GraphicsCardEnvironment gpu = new GraphicsCardEnvironment(0);
            FakeTextBuffer screen = new FakeTextBuffer();
            ComponentConnector connector = assertInstanceOf(ComponentConnector.class, gpu.node());
            RecordingContext context = new RecordingContext();
            connector.setLocalBufferSize(1D);
            connector.changeBuffer(1D);
            Network.joinNewNetwork(gpu.node());
            gpu.node().connect(screen.node());
            gpu.bind(null, new TestArguments(screen.node().address(), true));
            gpu.allocateBuffer(null, new TestArguments(10, 5));
            gpu.setActiveBuffer(null, new TestArguments(1));
            gpu.set(null, new TestArguments(1, 1, "A"));

            assertArrayEquals(new Object[]{true}, gpu.bitblt(context, new TestArguments(0, 1, 1, 10, 5, 1, 1, 1)));

            assertEquals(0.5D, context.callBudget, 0.000_001D);
        });
    }

    @Test
    void bitbltIntoVideoBufferMarksItDirtyForScreenBudget() throws Exception {
        withCachedConfig(ModSettings.GPU_BITBLT_COST, 8D, () -> {
            OpenComputersApi.initialize();
            GraphicsCardEnvironment gpu = new GraphicsCardEnvironment(0);
            FakeTextBuffer screen = new FakeTextBuffer();
            ComponentConnector connector = assertInstanceOf(ComponentConnector.class, gpu.node());
            RecordingContext context = new RecordingContext();
            connector.setLocalBufferSize(1D);
            connector.changeBuffer(1D);
            Network.joinNewNetwork(gpu.node());
            gpu.node().connect(screen.node());
            gpu.bind(null, new TestArguments(screen.node().address(), true));
            gpu.allocateBuffer(null, new TestArguments(10, 5));
            gpu.save(new CompoundTag());

            assertArrayEquals(new Object[]{true}, gpu.bitblt(null, new TestArguments(1, 1, 1, 10, 5, 0, 1, 1)));
            assertArrayEquals(new Object[]{true}, gpu.bitblt(context, new TestArguments(0, 1, 1, 10, 5, 1, 1, 1)));

            assertEquals(0.5D, context.callBudget, 0.000_001D);
        });
    }

    @Test
    void expensiveBitbltThrowsOnceThenPausesAndSkipsBudgetCost() throws Exception {
        withCachedConfig(ModSettings.GPU_BITBLT_COST, 32D, () -> {
            OpenComputersApi.initialize();
            GraphicsCardEnvironment gpu = new GraphicsCardEnvironment(0);
            FakeTextBuffer screen = new FakeTextBuffer();
            ComponentConnector connector = assertInstanceOf(ComponentConnector.class, gpu.node());
            RecordingContext context = new RecordingContext();
            connector.setLocalBufferSize(20D);
            connector.changeBuffer(20D);
            Network.joinNewNetwork(gpu.node());
            gpu.node().connect(screen.node());
            gpu.bind(null, new TestArguments(screen.node().address(), true));
            gpu.allocateBuffer(null, new TestArguments(10, 5));
            gpu.setActiveBuffer(null, new TestArguments(1));
            gpu.set(null, new TestArguments(1, 1, "A"));

            assertThrows(LimitReachedException.class,
                () -> gpu.bitblt(context, new TestArguments(0, 1, 1, 10, 5, 1, 1, 1)));

            assertEquals(0D, context.callBudget, 0.000_001D);
            assertEquals(-1D, context.pauseSeconds, 0.000_001D);

            assertArrayEquals(new Object[]{true}, gpu.bitblt(context, new TestArguments(0, 1, 1, 10, 5, 1, 1, 1)));

            assertEquals(0D, context.callBudget, 0.000_001D);
            assertEquals(0.1D, context.pauseSeconds, 0.000_001D);
        });
    }

    @Test
    void colorSettersReturnPreviousColorAndPaletteIndex() throws Exception {
        OpenComputersApi.initialize();
        GraphicsCardEnvironment gpu = new GraphicsCardEnvironment(0);
        FakeTextBuffer screen = new FakeTextBuffer();
        Network.joinNewNetwork(gpu.node());
        gpu.node().connect(screen.node());
        gpu.bind(null, new TestArguments(screen.node().address(), true));
        screen.setPaletteColor(3, 0x112233);
        screen.setForegroundColor(3, true);
        screen.setPaletteColor(4, 0x445566);
        screen.setBackgroundColor(4, true);

        assertArrayEquals(new Object[]{0x112233, 3}, gpu.setForeground(null, new TestArguments(0xAAAAAA)));
        assertArrayEquals(new Object[]{0x445566, 4}, gpu.setBackground(null, new TestArguments(0xBBBBBB)));
        assertArrayEquals(new Object[]{0xAAAAAA, null}, gpu.setForeground(null, new TestArguments(0xCCCCCC)));
        assertArrayEquals(new Object[]{0xBBBBBB, null}, gpu.setBackground(null, new TestArguments(0xDDDDDD)));
    }

    @Test
    void getReturnsResolvedPaletteColorsAndIndices() throws Exception {
        OpenComputersApi.initialize();
        GraphicsCardEnvironment gpu = new GraphicsCardEnvironment(0);
        gpu.allocateBuffer(null, new TestArguments(2, 1));
        gpu.setActiveBuffer(null, new TestArguments(1));
        gpu.setPaletteColor(null, new TestArguments(3, 0x112233));
        gpu.setPaletteColor(null, new TestArguments(4, 0x445566));
        gpu.setForeground(null, new TestArguments(3, true));
        gpu.setBackground(null, new TestArguments(4, true));
        gpu.set(null, new TestArguments(1, 1, "A"));

        assertArrayEquals(new Object[]{"A", 0x112233, 0x445566, 3, 4}, gpu.get(null, new TestArguments(1, 1)));

        gpu.setForeground(null, new TestArguments(0xABCDEF));
        gpu.setBackground(null, new TestArguments(0x123456));
        gpu.set(null, new TestArguments(2, 1, "B"));

        assertArrayEquals(new Object[]{"A", 0x112233, 0x445566, 3, 4}, gpu.get(null, new TestArguments(1, 1)));
        assertArrayEquals(new Object[]{"B", 0xABCDEF, 0x123456, null, null}, gpu.get(null, new TestArguments(2, 1)));
    }

    @Test
    void videoBufferSetAndFillRespectWideCharactersLikeUpstream() throws Exception {
        OpenComputersApi.initialize();
        GraphicsCardEnvironment gpu = new GraphicsCardEnvironment(0);
        String wide = new String(Character.toChars(0x6c34));
        gpu.allocateBuffer(null, new TestArguments(4, 1));
        gpu.setActiveBuffer(null, new TestArguments(1));

        assertArrayEquals(new Object[]{true}, gpu.set(null, new TestArguments(1, 1, wide + "B")));
        assertArrayEquals(new Object[]{true}, gpu.set(null, new TestArguments(4, 1, wide)));

        assertArrayEquals(new Object[]{wide, 0xFFFFFF, 0x000000, null, null}, gpu.get(null, new TestArguments(1, 1)));
        assertArrayEquals(new Object[]{" ", 0xFFFFFF, 0x000000, null, null}, gpu.get(null, new TestArguments(2, 1)));
        assertArrayEquals(new Object[]{"B", 0xFFFFFF, 0x000000, null, null}, gpu.get(null, new TestArguments(3, 1)));
        assertArrayEquals(new Object[]{" ", 0xFFFFFF, 0x000000, null, null}, gpu.get(null, new TestArguments(4, 1)));

        assertArrayEquals(new Object[]{true}, gpu.fill(null, new TestArguments(1, 1, 4, 1, wide)));

        assertArrayEquals(new Object[]{wide, 0xFFFFFF, 0x000000, null, null}, gpu.get(null, new TestArguments(1, 1)));
        assertArrayEquals(new Object[]{" ", 0xFFFFFF, 0x000000, null, null}, gpu.get(null, new TestArguments(2, 1)));
        assertArrayEquals(new Object[]{wide, 0xFFFFFF, 0x000000, null, null}, gpu.get(null, new TestArguments(3, 1)));
        assertArrayEquals(new Object[]{" ", 0xFFFFFF, 0x000000, null, null}, gpu.get(null, new TestArguments(4, 1)));
    }

    @Test
    void managesVideoBuffersWithoutBoundScreen() throws Exception {
        OpenComputersApi.initialize();
        GraphicsCardEnvironment gpu = new GraphicsCardEnvironment(0);

        assertArrayEquals(new Object[]{0}, gpu.getActiveBuffer(null, new TestArguments()));
        int initialFreeMemory = ((Number) gpu.freeMemory(null, new TestArguments())[0]).intValue();

        assertArrayEquals(new Object[]{1}, gpu.allocateBuffer(null, new TestArguments(4, 2)));
        assertArrayEquals(new int[]{1}, (int[]) gpu.buffers(null, new TestArguments())[0]);
        assertEquals(initialFreeMemory - 8, ((Number) gpu.freeMemory(null, new TestArguments())[0]).intValue());
        assertArrayEquals(new Object[]{4, 2}, gpu.getBufferSize(null, new TestArguments(1)));

        assertArrayEquals(new Object[]{0}, gpu.setActiveBuffer(null, new TestArguments(1)));
        assertArrayEquals(new Object[]{1}, gpu.getActiveBuffer(null, new TestArguments()));
        assertArrayEquals(new Object[]{true}, gpu.set(null, new TestArguments(1, 1, "A")));
        assertArrayEquals(new Object[]{"A", 0xFFFFFF, 0x000000, null, null}, gpu.get(null, new TestArguments(1, 1)));
        assertArrayEquals(new Object[]{false}, gpu.setResolution(null, new TestArguments(5, 2)));
        assertArrayEquals(new Object[]{4, 2}, gpu.getBufferSize(null, new TestArguments(1)));

        assertArrayEquals(new Object[]{true}, gpu.freeBuffer(null, new TestArguments(1)));
        assertArrayEquals(new Object[]{0}, gpu.getActiveBuffer(null, new TestArguments()));
        assertEquals(initialFreeMemory, ((Number) gpu.freeMemory(null, new TestArguments())[0]).intValue());
    }

    @Test
    void videoBufferViewportMatchesUpstreamVramBehavior() {
        OpenComputersApi.initialize();
        GraphicsCardEnvironment gpu = new GraphicsCardEnvironment(0);

        assertArrayEquals(new Object[]{1}, gpu.allocateBuffer(null, new TestArguments(4, 2)));
        assertArrayEquals(new Object[]{0}, gpu.setActiveBuffer(null, new TestArguments(1)));

        assertArrayEquals(new Object[]{2, 4}, gpu.getViewport(null, new TestArguments()));
        assertArrayEquals(new Object[]{false}, gpu.setViewport(null, new TestArguments(3, 1)));
        assertArrayEquals(new Object[]{2, 4}, gpu.getViewport(null, new TestArguments()));
    }

    @Test
    void rejectsInvalidVideoBufferRequests() {
        OpenComputersApi.initialize();
        GraphicsCardEnvironment gpu = new GraphicsCardEnvironment(0);

        assertArrayEquals(new Object[]{null, "invalid page dimensions: must be greater than zero"}, gpu.allocateBuffer(null, new TestArguments(0, 2)));
        assertArrayEquals(new Object[]{null, "invalid buffer index"}, gpu.setActiveBuffer(null, new TestArguments(1)));
        assertArrayEquals(new Object[]{null, "no buffer at index"}, gpu.freeBuffer(null, new TestArguments(1)));
    }

    @Test
    void allocateBufferRejectsDisconnectedGpuLikeUpstream() {
        OpenComputersApi.initialize();
        DisconnectableGraphicsCardEnvironment gpu = new DisconnectableGraphicsCardEnvironment(0);

        gpu.disconnectNode();

        assertArrayEquals(new Object[]{null, "graphics card appears disconnected"}, gpu.allocateBuffer(null, new TestArguments(2, 2)));
        assertArrayEquals(new int[0], (int[]) gpu.buffers(null, new TestArguments())[0]);
    }

    @Test
    void bitbltCopiesBetweenVideoBuffers() throws Exception {
        OpenComputersApi.initialize();
        GraphicsCardEnvironment gpu = new GraphicsCardEnvironment(0);
        gpu.allocateBuffer(null, new TestArguments(3, 2));
        gpu.allocateBuffer(null, new TestArguments(3, 2));
        gpu.setActiveBuffer(null, new TestArguments(1));
        gpu.set(null, new TestArguments(1, 1, "AB"));
        gpu.set(null, new TestArguments(1, 2, "CD"));

        assertArrayEquals(new Object[]{true}, gpu.bitblt(null, new TestArguments(2, 1, 1, 2, 2, 1, 1, 1)));

        gpu.setActiveBuffer(null, new TestArguments(2));
        assertArrayEquals(new Object[]{"A", 0xFFFFFF, 0x000000, null, null}, gpu.get(null, new TestArguments(1, 1)));
        assertArrayEquals(new Object[]{"B", 0xFFFFFF, 0x000000, null, null}, gpu.get(null, new TestArguments(2, 1)));
        assertArrayEquals(new Object[]{"C", 0xFFFFFF, 0x000000, null, null}, gpu.get(null, new TestArguments(1, 2)));
        assertArrayEquals(new Object[]{"D", 0xFFFFFF, 0x000000, null, null}, gpu.get(null, new TestArguments(2, 2)));
    }

    @Test
    void bitbltCopiesPaletteFlagsBetweenVideoBuffersLikeUpstream() throws Exception {
        OpenComputersApi.initialize();
        GraphicsCardEnvironment gpu = new GraphicsCardEnvironment(0);
        gpu.allocateBuffer(null, new TestArguments(2, 1));
        gpu.allocateBuffer(null, new TestArguments(2, 1));
        gpu.setActiveBuffer(null, new TestArguments(1));
        gpu.setPaletteColor(null, new TestArguments(2, 0x223344));
        gpu.setPaletteColor(null, new TestArguments(3, 0x556677));
        gpu.setForeground(null, new TestArguments(2, true));
        gpu.setBackground(null, new TestArguments(3, true));
        gpu.set(null, new TestArguments(1, 1, "A"));
        gpu.setActiveBuffer(null, new TestArguments(2));
        gpu.setPaletteColor(null, new TestArguments(2, 0x223344));
        gpu.setPaletteColor(null, new TestArguments(3, 0x556677));
        gpu.setActiveBuffer(null, new TestArguments(1));

        assertArrayEquals(new Object[]{true}, gpu.bitblt(null, new TestArguments(2, 1, 1, 1, 1, 1, 1, 1)));

        gpu.setActiveBuffer(null, new TestArguments(2));
        assertArrayEquals(new Object[]{"A", 0x223344, 0x556677, 2, 3}, gpu.get(null, new TestArguments(1, 1)));
    }

    @Test
    void bitbltClipsSourceAndDestinationLikeUpstream() throws Exception {
        OpenComputersApi.initialize();
        GraphicsCardEnvironment gpu = new GraphicsCardEnvironment(0);
        gpu.allocateBuffer(null, new TestArguments(3, 1));
        gpu.allocateBuffer(null, new TestArguments(3, 1));
        gpu.setActiveBuffer(null, new TestArguments(1));
        gpu.set(null, new TestArguments(1, 1, "ABC"));
        gpu.setActiveBuffer(null, new TestArguments(2));
        gpu.set(null, new TestArguments(1, 1, "xyz"));

        assertArrayEquals(new Object[]{true}, gpu.bitblt(null, new TestArguments(2, 1, 1, 3, 1, 1, 0, 1)));

        assertArrayEquals(new Object[]{"x", 0xFFFFFF, 0x000000, null, null}, gpu.get(null, new TestArguments(1, 1)));
        assertArrayEquals(new Object[]{"A", 0xFFFFFF, 0x000000, null, null}, gpu.get(null, new TestArguments(2, 1)));
        assertArrayEquals(new Object[]{"B", 0xFFFFFF, 0x000000, null, null}, gpu.get(null, new TestArguments(3, 1)));
    }

    @Test
    void persistsVideoBuffersAndActiveIndex() throws Exception {
        OpenComputersApi.initialize();
        GraphicsCardEnvironment gpu = new GraphicsCardEnvironment(0);
        gpu.allocateBuffer(null, new TestArguments(3, 2));
        gpu.setActiveBuffer(null, new TestArguments(1));
        gpu.setPaletteColor(null, new TestArguments(2, 0x223344));
        gpu.setPaletteColor(null, new TestArguments(3, 0x556677));
        gpu.setForeground(null, new TestArguments(2, true));
        gpu.setBackground(null, new TestArguments(3, true));
        gpu.setForeground(null, new TestArguments(0x112233));
        gpu.setBackground(null, new TestArguments(0x445566));
        gpu.set(null, new TestArguments(1, 1, "XY"));
        gpu.setForeground(null, new TestArguments(2, true));
        gpu.setBackground(null, new TestArguments(3, true));
        gpu.set(null, new TestArguments(3, 1, "Z"));
        CompoundTag tag = new CompoundTag();

        gpu.save(tag);
        GraphicsCardEnvironment restored = new GraphicsCardEnvironment(0);
        restored.load(tag);

        assertArrayEquals(new Object[]{1}, restored.getActiveBuffer(null, new TestArguments()));
        assertArrayEquals(new int[]{1}, (int[]) restored.buffers(null, new TestArguments())[0]);
        assertArrayEquals(new Object[]{"X", 0x112233, 0x445566, null, null}, restored.get(null, new TestArguments(1, 1)));
        assertArrayEquals(new Object[]{"Y", 0x112233, 0x445566, null, null}, restored.get(null, new TestArguments(2, 1)));
        assertArrayEquals(new Object[]{"Z", 0x223344, 0x556677, 2, 3}, restored.get(null, new TestArguments(3, 1)));
    }

    private static void assertCallback(final String methodName) throws NoSuchMethodException {
        Method method = GraphicsCardEnvironment.class.getMethod(methodName, li.cil.oc.api.machine.Context.class, Arguments.class);
        assertTrue(method.isAnnotationPresent(Callback.class));
    }

    private static <T> void withCachedConfig(final ModConfigSpec.ConfigValue<T> value, final T override, final ThrowingRunnable action) throws Exception {
        final Field cachedValue = ModConfigSpec.ConfigValue.class.getDeclaredField("cachedValue");
        cachedValue.setAccessible(true);
        final Object previous = cachedValue.get(value);
        cachedValue.set(value, override);
        try {
            action.run();
        } finally {
            cachedValue.set(value, previous);
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static final class DisconnectableGraphicsCardEnvironment extends GraphicsCardEnvironment {
        private DisconnectableGraphicsCardEnvironment(final int tier) {
            super(tier);
        }

        private void disconnectNode() {
            setNode(null);
        }
    }

    private static final class FakeEnvironment extends AbstractManagedEnvironment {
        private FakeEnvironment() {
            setNode(Network.newNode(this, li.cil.oc.api.network.Visibility.Network).create());
        }
    }

    private static final class RecordingContext implements Context {
        private double callBudget;
        private double pauseSeconds = -1D;

        @Override public Node node() { return null; }
        @Override public boolean canInteract(final String player) { return true; }
        @Override public boolean isRunning() { return true; }
        @Override public boolean isPaused() { return false; }
        @Override public boolean start() { return true; }
        @Override public boolean pause(final double seconds) { pauseSeconds = seconds; return true; }
        @Override public boolean stop() { return true; }
        @Override public void consumeCallBudget(final double callCost) { callBudget += callCost; }
        @Override public boolean signal(final String name, final Object... args) { return true; }
    }

    private record TestMessage(Node source, String name, Object[] data) implements Message {
        @Override
        public void cancel() {
        }
    }

    private static final class FakeTextBuffer extends AbstractManagedEnvironment implements TextBuffer {
        private int width = 40;
        private int height = 16;
        private int viewportWidth = 40;
        private int viewportHeight = 16;
        private int foreground = 0xFFFFFF;
        private int background = 0x000000;
        private boolean foregroundFromPalette;
        private boolean backgroundFromPalette;
        private ColorDepth depth = ColorDepth.OneBit;
        private final int[] palette = new int[16];

        private FakeTextBuffer() {
            setNode(ScreenEnvironment.createNode(this));
        }

        @Override
        public void setEnergyCostPerTick(final double value) {
        }

        @Override
        public double getEnergyCostPerTick() {
            return 0;
        }

        @Override
        public void setPowerState(final boolean value) {
        }

        @Override
        public boolean getPowerState() {
            return true;
        }

        @Override
        public void setMaximumResolution(final int width, final int height) {
        }

        @Override
        public int getMaximumWidth() {
            return 80;
        }

        @Override
        public int getMaximumHeight() {
            return 25;
        }

        @Override
        public void setAspectRatio(final double width, final double height) {
        }

        @Override
        public double getAspectRatio() {
            return 1;
        }

        @Override
        public boolean setResolution(final int width, final int height) {
            this.width = width;
            this.height = height;
            viewportWidth = Math.min(viewportWidth, width);
            viewportHeight = Math.min(viewportHeight, height);
            return true;
        }

        @Override
        public int getWidth() {
            return width;
        }

        @Override
        public int getHeight() {
            return height;
        }

        @Override
        public boolean setViewport(final int width, final int height) {
            viewportWidth = width;
            viewportHeight = height;
            return true;
        }

        @Override
        public int getViewportWidth() {
            return viewportWidth;
        }

        @Override
        public int getViewportHeight() {
            return viewportHeight;
        }

        @Override
        public void setMaximumColorDepth(final ColorDepth depth) {
        }

        @Override
        public ColorDepth getMaximumColorDepth() {
            return ColorDepth.OneBit;
        }

        @Override
        public boolean setColorDepth(final ColorDepth depth) {
            this.depth = depth;
            return true;
        }

        @Override
        public ColorDepth getColorDepth() {
            return depth;
        }

        @Override
        public void setPaletteColor(final int index, final int color) {
            if (index >= 0 && index < palette.length) {
                palette[index] = color;
            }
        }

        @Override
        public int getPaletteColor(final int index) {
            return index >= 0 && index < palette.length ? palette[index] : 0;
        }

        @Override
        public void setForegroundColor(final int color) {
            foregroundFromPalette = false;
            foreground = color;
        }

        @Override
        public void setForegroundColor(final int color, final boolean isFromPalette) {
            foreground = color;
            foregroundFromPalette = isFromPalette;
        }

        @Override
        public int getForegroundColor() {
            return foreground;
        }

        @Override
        public boolean isForegroundFromPalette() {
            return foregroundFromPalette;
        }

        @Override
        public void setBackgroundColor(final int color) {
            backgroundFromPalette = false;
            background = color;
        }

        @Override
        public void setBackgroundColor(final int color, final boolean isFromPalette) {
            background = color;
            backgroundFromPalette = isFromPalette;
        }

        @Override
        public int getBackgroundColor() {
            return background;
        }

        @Override
        public boolean isBackgroundFromPalette() {
            return backgroundFromPalette;
        }

        @Override
        public void copy(final int column, final int row, final int width, final int height, final int horizontalTranslation, final int verticalTranslation) {
        }

        @Override
        public void fill(final int column, final int row, final int width, final int height, final char value) {
        }

        @Override
        public void fill(final int column, final int row, final int width, final int height, final int value) {
        }

        @Override
        public void set(final int column, final int row, final String value, final boolean vertical) {
        }

        @Override
        public char get(final int column, final int row) {
            return ' ';
        }

        @Override
        public int getCodePoint(final int column, final int row) {
            return ' ';
        }

        @Override
        public int getForegroundColor(final int column, final int row) {
            return foreground;
        }

        @Override
        public boolean isForegroundFromPalette(final int column, final int row) {
            return foregroundFromPalette;
        }

        @Override
        public int getBackgroundColor(final int column, final int row) {
            return background;
        }

        @Override
        public boolean isBackgroundFromPalette(final int column, final int row) {
            return backgroundFromPalette;
        }

        @Override
        public void rawSetText(final int column, final int row, final char[][] text) {
        }

        @Override
        public void rawSetText(final int column, final int row, final int[][] text) {
        }

        @Override
        public void rawSetForeground(final int column, final int row, final int[][] color) {
        }

        @Override
        public void rawSetBackground(final int column, final int row, final int[][] color) {
        }

        @Override
        public boolean renderText() {
            return true;
        }

        @Override
        public int renderWidth() {
            return viewportWidth;
        }

        @Override
        public int renderHeight() {
            return viewportHeight;
        }

        @Override
        public void setRenderingEnabled(final boolean enabled) {
        }

        @Override
        public boolean isRenderingEnabled() {
            return true;
        }

        @Override
        public void keyDown(final char character, final int code, final Player player) {
        }

        @Override
        public void keyUp(final char character, final int code, final Player player) {
        }

        @Override
        public void clipboard(final String value, final Player player) {
        }

        @Override
        public void mouseDown(final double x, final double y, final int button, final Player player) {
        }

        @Override
        public void mouseDrag(final double x, final double y, final int button, final Player player) {
        }

        @Override
        public void mouseUp(final double x, final double y, final int button, final Player player) {
        }

        @Override
        public void mouseScroll(final double x, final double y, final int delta, final Player player) {
        }
    }

    private record TestArguments(Object... values) implements Arguments {
        @Override
        public int count() {
            return values.length;
        }

        @Override
        public Object checkAny(final int index) {
            return values[index];
        }

        @Override
        public boolean checkBoolean(final int index) {
            return (Boolean) values[index];
        }

        @Override
        public int checkInteger(final int index) {
            return ((Number) values[index]).intValue();
        }

        @Override
        public long checkLong(final int index) {
            return ((Number) values[index]).longValue();
        }

        @Override
        public double checkDouble(final int index) {
            return ((Number) values[index]).doubleValue();
        }

        @Override
        public String checkString(final int index) {
            return (String) values[index];
        }

        @Override
        public byte[] checkByteArray(final int index) {
            return (byte[]) values[index];
        }

        @Override
        public Map checkTable(final int index) {
            return (Map) values[index];
        }

        @Override
        public ItemStack checkItemStack(final int index) {
            return (ItemStack) values[index];
        }

        @Override
        public Object optAny(final int index, final Object def) {
            return index < values.length ? values[index] : def;
        }

        @Override
        public boolean optBoolean(final int index, final boolean def) {
            return index < values.length ? checkBoolean(index) : def;
        }

        @Override
        public int optInteger(final int index, final int def) {
            return index < values.length ? checkInteger(index) : def;
        }

        @Override
        public long optLong(final int index, final long def) {
            return index < values.length ? checkLong(index) : def;
        }

        @Override
        public double optDouble(final int index, final double def) {
            return index < values.length ? checkDouble(index) : def;
        }

        @Override
        public String optString(final int index, final String def) {
            return index < values.length ? checkString(index) : def;
        }

        @Override
        public byte[] optByteArray(final int index, final byte[] def) {
            return index < values.length ? checkByteArray(index) : def;
        }

        @Override
        public Map optTable(final int index, final Map def) {
            return index < values.length ? checkTable(index) : def;
        }

        @Override
        public ItemStack optItemStack(final int index, final ItemStack def) {
            return index < values.length ? checkItemStack(index) : def;
        }

        @Override
        public boolean isBoolean(final int index) {
            return values[index] instanceof Boolean;
        }

        @Override
        public boolean isInteger(final int index) {
            return values[index] instanceof Integer;
        }

        @Override
        public boolean isLong(final int index) {
            return values[index] instanceof Long;
        }

        @Override
        public boolean isDouble(final int index) {
            return values[index] instanceof Double;
        }

        @Override
        public boolean isString(final int index) {
            return values[index] instanceof String;
        }

        @Override
        public boolean isByteArray(final int index) {
            return values[index] instanceof byte[];
        }

        @Override
        public boolean isTable(final int index) {
            return values[index] instanceof Map;
        }

        @Override
        public boolean isItemStack(final int index) {
            return values[index] instanceof ItemStack;
        }

        @Override
        public Object[] toArray() {
            return values;
        }

        @Override
        public Iterator<Object> iterator() {
            return java.util.Arrays.asList(values).iterator();
        }
    }
}
