package li.cil.oc.common.gametest;

import dan200.computercraft.api.lua.IDynamicLuaObject;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.IDynamicPeripheral;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.apis.handles.ArrayByteChannel;
import dan200.computercraft.core.apis.handles.ReadHandle;
import net.minecraft.server.level.ServerLevel;

/** Test capability only: returns actual CC handles as well as dynamic/annotated objects. */
final class TestComputerCraftLuaObjectPeripheral implements IDynamicPeripheral, AutoCloseable {
    private final ServerLevel world;
    private final ArrayByteChannel channel = new ArrayByteChannel(new byte[]{0, (byte) 128, (byte) 255, 42});
    private final ReadHandle handle = new ReadHandle(channel, true);
    private final ComputerCraftLuaObjectGameTests.AnnotatedObject annotated;
    private final Target target = new Target();
    private int tasks;
    private int tails;
    private boolean closed;
    TestComputerCraftLuaObjectPeripheral(ServerLevel world) { this.world = world; annotated = new ComputerCraftLuaObjectGameTests.AnnotatedObject(world.getServer()); }
    @Override public String getType() { return "oc_lua_object_probe"; }
    @Override public String[] getMethodNames() { return new String[]{"object", "handle", "annotated", "issue", "callback", "ordered", "tailCount"}; }
    @Override public boolean equals(IPeripheral other) { return other == this; }
    @Override public MethodResult callMethod(dan200.computercraft.api.peripheral.IComputerAccess computer, ILuaContext context, int method, IArguments args) throws LuaException {
        if (closed) throw new LuaException("object peripheral is closed");
        return switch (method) {
            case 0 -> MethodResult.of(target);
            case 1 -> MethodResult.of(handle);
            case 2 -> MethodResult.of(annotated);
            case 3 -> MethodResult.of(context.issueMainThreadTask(() -> { tasks++; return new Object[]{"peripheral-task"}; }));
            case 4 -> {
                final long id = context.issueMainThreadTask(() -> new Object[]{collection(), "callback-results"});
                yield MethodResult.pullEvent("task_complete", event -> {
                    if (((Number) event[1]).longValue() != id || !Boolean.TRUE.equals(event[2]) || event.length != 5)
                        throw new LuaException("peripheral callback result payload missing");
                    return MethodResult.of(event[3], event[4]);
                });
            }
            case 5 -> {
                final long id = context.issueMainThreadTask(() -> new Object[]{"ordered-first"});
                context.issueMainThreadTask(() -> { tails++; return new Object[]{"ordered-tail"}; });
                yield MethodResult.pullEvent("task_complete", event -> {
                    if (((Number) event[1]).longValue() != id || tails != 0) throw new LuaException("peripheral callback task order changed");
                    return MethodResult.of(event[3]);
                });
            }
            case 6 -> MethodResult.of(tails);
            default -> throw new LuaException("unknown probe method");
        };
    }
    @Override public void close() { closed = true; channel.close(); }

    private static Object collection() {
        final var bytes = java.nio.ByteBuffer.wrap(new byte[]{9, 0, (byte) 128, (byte) 255, 42, 9});
        bytes.position(1); bytes.limit(5);
        return java.util.List.of(bytes, java.util.Map.of("inner", java.util.List.of(bytes)));
    }

    private final class Target implements IDynamicLuaObject {
        private int calls;
        @Override public String[] getMethodNames() { return new String[]{"echo", "binary", "sync", "async", "fail", "collection"}; }
        @Override public MethodResult callMethod(ILuaContext context, int method, IArguments args) throws LuaException {
            if (closed) throw new LuaException("object peripheral is closed");
            return switch (method) {
                case 0 -> MethodResult.of(args.getString(0));
                case 1 -> MethodResult.of(args.getBytes(0));
                case 2 -> context.executeMainThreadTask(() -> {
                    if (!world.getServer().isSameThread()) throw new LuaException("object task ran off-thread");
                    return new Object[]{++calls};
                });
                case 3 -> MethodResult.of(context.issueMainThreadTask(() -> { calls++; return new Object[]{"object-task", new byte[]{0, (byte) 255}}; }));
                case 4 -> throw new LuaException("native object failure");
                case 5 -> MethodResult.of(collection());
                default -> throw new LuaException("unknown object method");
            };
        }
    }
}
