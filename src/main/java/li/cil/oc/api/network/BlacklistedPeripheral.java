package li.cil.oc.api.network;

/**
 * Lets a peripheral opt out of OpenComputers wrapping.
 */
@FunctionalInterface
public interface BlacklistedPeripheral {
    boolean isPeripheralBlacklisted();
}
