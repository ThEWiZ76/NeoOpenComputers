package li.cil.oc.client;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PrinterBlockEntityRendererShapeTest {
    @Test
    void printerRendererClassExists() throws ClassNotFoundException {
        assertEquals("li.cil.oc.client.PrinterBlockEntityRenderer", Class.forName("li.cil.oc.client.PrinterBlockEntityRenderer").getName());
    }

    @Test
    void clientRegistersPrinterRenderer() throws Exception {
        final String client = Files.readString(Path.of("src/main/java/li/cil/oc/client/NeoOpenComputersClient.java"));

        assertTrue(client.contains("ModBlockEntities.PRINTER.get()"));
        assertTrue(client.contains("PrinterBlockEntityRenderer::new"));
    }

    @Test
    void rendererUsesUpstreamPreviewTransformAndPrintStack() throws Exception {
        final String renderer = Files.readString(Path.of("src/main/java/li/cil/oc/client/PrinterBlockEntityRenderer.java"));

        assertTrue(renderer.contains("previewStack()"));
        assertTrue(renderer.contains("0.5D, 0.8D, 0.5D"));
        assertTrue(renderer.contains("rotationDegrees"));
        assertTrue(renderer.contains("0.75F, 0.75F, 0.75F"));
        assertTrue(renderer.contains("ItemDisplayContext.FIXED"));
    }
}
