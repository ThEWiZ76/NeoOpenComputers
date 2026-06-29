package li.cil.oc.common;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class AlphaSmokeMatrixTest {
    @Test
    void alphaSmokeMatrixMapsChecklistToEvidence() throws IOException {
        final Path matrixPath = Path.of("ALPHA_SMOKE_MATRIX.md");
        final String readme = Files.readString(Path.of("README.md"));

        assertTrue(Files.isRegularFile(matrixPath), "Alpha smoke matrix must exist before first alpha sharing");
        assertTrue(readme.contains("[Alpha Smoke Matrix](ALPHA_SMOKE_MATRIX.md)"),
            "README must link alpha smoke matrix");

        final String matrix = Files.readString(matrixPath);
        for (final String requiredArea : new String[]{
            "OpenOS boot",
            "Computer case storage persistence",
            "Disk-drive floppy persistence",
            "Redstone",
            "Modem",
            "Inventory",
            "Tank",
            "Transposer",
            "Printer and print",
            "Manual and packaging"
        }) {
            assertTrue(matrix.contains(requiredArea), "Alpha smoke matrix missing area: " + requiredArea);
        }

        assertTrue(matrix.contains("Automated evidence"), "Matrix must distinguish automated evidence");
        assertTrue(matrix.contains("Manual alpha smoke"), "Matrix must distinguish manual alpha smoke");
        assertTrue(matrix.contains("Screen world rendering stays frozen"),
            "Matrix must keep the screen loop-control rule visible");
    }
}
