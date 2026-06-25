package li.cil.oc.api;

import com.typesafe.config.Config;
import li.cil.oc.api.detail.DriverAPI;
import li.cil.oc.api.detail.FileSystemAPI;
import li.cil.oc.api.detail.ItemAPI;
import li.cil.oc.api.detail.MachineAPI;
import li.cil.oc.api.detail.ManualAPI;
import li.cil.oc.api.detail.NanomachinesAPI;
import li.cil.oc.api.detail.NetworkAPI;

/**
 * Central service references for the NeoOpenComputers API.
 */
public final class API {
    public static final String ID_OWNER = "neoopencomputers|core";
    public static final String VERSION = "0.1.0";

    /**
     * Placeholder for the loaded mod config.
     */
    public static Config config = null;

    public static boolean isPowerEnabled = false;

    public static DriverAPI driver = null;
    public static FileSystemAPI fileSystem = null;
    public static ItemAPI items = null;
    public static MachineAPI machine = null;
    public static ManualAPI manual = null;
    public static NanomachinesAPI nanomachines = null;
    public static NetworkAPI network = null;

    private API() {
    }
}
