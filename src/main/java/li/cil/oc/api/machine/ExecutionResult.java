package li.cil.oc.api.machine;

/**
 * Result of one machine execution step.
 *
 * <p>These nested classes mirror the public OpenComputers API shape so
 * architectures can communicate scheduler decisions without depending on
 * concrete machine internals.</p>
 */
public abstract class ExecutionResult {
    public static final class Sleep extends ExecutionResult {
        public final int ticks;

        public Sleep(final int ticks) {
            this.ticks = ticks;
        }
    }

    public static final class Shutdown extends ExecutionResult {
        public final boolean reboot;

        public Shutdown(final boolean reboot) {
            this.reboot = reboot;
        }
    }

    public static final class SynchronizedCall extends ExecutionResult {
    }

    public static final class Error extends ExecutionResult {
        public final String message;

        public Error(final String message) {
            this.message = message;
        }
    }
}
