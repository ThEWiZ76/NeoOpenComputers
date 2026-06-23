package li.cil.oc.common.network;

public final class DebugClipboardState {
    private static String pendingClipboard;

    public static void accept(final DebugClipboardPayload payload) {
        pendingClipboard = payload == null ? "" : payload.value();
    }

    public static String consumePendingClipboard() {
        final String value = pendingClipboard;
        pendingClipboard = null;
        return value;
    }

    private DebugClipboardState() {
    }
}
