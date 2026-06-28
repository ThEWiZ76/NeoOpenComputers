package li.cil.oc.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RackMountableRenderHandlerShapeTest {
    @Test
    void rackMountableRenderHandlerClassExists() throws ClassNotFoundException {
        assertEquals("li.cil.oc.client.RackMountableRenderHandler", Class.forName("li.cil.oc.client.RackMountableRenderHandler").getName());
    }

    @Test
    void clientRegistersRackMountableRenderHandler() throws IOException {
        final String client = Files.readString(Path.of("src/main/java/li/cil/oc/client/NeoOpenComputersClient.java"));

        assertTrue(client.contains("NeoForge.EVENT_BUS.register(RackMountableRenderHandler.class)"));
    }

    @Test
    void handlerUsesUpstreamRackTextures() throws IOException {
        final String handler = Files.readString(Path.of("src/main/java/li/cil/oc/client/RackMountableRenderHandler.java"));

        assertTrue(handler.contains("block/rack_disk_drive"));
        assertTrue(handler.contains("block/rack_server"));
        assertTrue(handler.contains("block/rack_terminal_server"));
        assertTrue(handler.contains("block/overlay/rack_disk_drive_activity"));
        assertTrue(handler.contains("block/overlay/rack_server_on"));
        assertTrue(handler.contains("block/overlay/rack_server_error"));
        assertTrue(handler.contains("block/overlay/rack_server_activity"));
        assertTrue(handler.contains("block/overlay/rack_server_network_activity"));
        assertTrue(handler.contains("block/overlay/rack_terminal_server_on"));
        assertTrue(handler.contains("block/overlay/rack_terminal_server_presence"));
    }

    @Test
    void handlerChecksUpstreamDataKeys() throws IOException {
        final String handler = Files.readString(Path.of("src/main/java/li/cil/oc/client/RackMountableRenderHandler.java"));

        assertTrue(handler.contains("\"lastAccess\""));
        assertTrue(handler.contains("\"isRunning\""));
        assertTrue(handler.contains("\"hasErrored\""));
        assertTrue(handler.contains("\"lastFileSystemAccess\""));
        assertTrue(handler.contains("\"lastNetworkActivity\""));
        assertTrue(handler.contains("\"keys\""));
    }
}
