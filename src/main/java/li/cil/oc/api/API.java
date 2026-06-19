package li.cil.oc.api;

import li.cil.oc.api.detail.DriverAPI;
import li.cil.oc.api.detail.FileSystemAPI;
import li.cil.oc.api.detail.ItemAPI;
import li.cil.oc.api.detail.MachineAPI;
import li.cil.oc.api.detail.ManualAPI;
import li.cil.oc.api.detail.NanomachinesAPI;

/**
 * Central service references for the NeoOpenComputers API.
 */
public final class API {
    public static final String ID_OWNER = "neoopencomputers|core";
    public static final String VERSION = "0.1.0";

    /**
     * Placeholder for the loaded mod config. Kept as Object to avoid forcing a
     * specific config library on API consumers during the Java-first port.
     */
    public static Object config = null;

    public static boolean isPowerEnabled = false;

    public static DriverAPI driver = null;
    public static FileSystemAPI fileSystem = null;
    public static ItemAPI items = null;
    public static MachineAPI machine = null;
    public static ManualAPI manual = null;
    public static NanomachinesAPI nanomachines = null;

    private API() {
    }
}
