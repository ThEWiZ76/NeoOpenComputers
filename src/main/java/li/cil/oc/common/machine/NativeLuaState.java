package li.cil.oc.common.machine;

import li.cil.repack.com.naef.jnlua.LuaState;
import li.cil.repack.com.naef.jnlua.LuaStateFiveThree;
import li.cil.repack.com.naef.jnlua.LuaStateFiveFour;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.EnumSet;
import java.util.Locale;

/** Native VM ownership and library setup. Machine scheduling and persistence are separate. */
public final class NativeLuaState implements AutoCloseable {
    public enum Version { LUA52, LUA53, LUA54 }

    private static final EnumSet<Version> LOADED = EnumSet.noneOf(Version.class);
    private static final String HOST_LIBRARY_PREFIX = "neoopencomputers.host.";
    private final LuaState lua;
    private final Version version;

    private NativeLuaState(final LuaState lua, final Version version) {
        this.lua = lua;
        this.version = version;
    }

    public static NativeLuaState create(final Version version, final int memoryLimitBytes) throws IOException {
        if (memoryLimitBytes <= 0) throw new IllegalArgumentException("A positive native memory limit is required");
        loadLibrary(version);
        final LuaState lua = switch (version) {
            case LUA52 -> new LuaState(memoryLimitBytes);
            case LUA53 -> new LuaStateFiveThree(memoryLimitBytes);
            case LUA54 -> new LuaStateFiveFour(memoryLimitBytes);
        };
        try {
            for (final LuaState.Library library : new LuaState.Library[]{LuaState.Library.BASE,
                LuaState.Library.COROUTINE, LuaState.Library.DEBUG, LuaState.Library.ERIS,
                LuaState.Library.MATH, LuaState.Library.STRING, LuaState.Library.TABLE,
                version == Version.LUA52 ? LuaState.Library.BIT32 : LuaState.Library.UTF8}) {
                lua.openLib(library);
                lua.pop(1);
            }
            // Only trusted host code may use debug hooks and raw Eris deserialization.
            for (final String name : new String[]{"debug", "eris"}) {
                lua.getGlobal(name);
                lua.setField(lua.getRegistryIndex(), HOST_LIBRARY_PREFIX + name);
            }
            for (final String name : new String[]{"debug", "eris", "dofile", "loadfile"}) {
                lua.pushNil();
                lua.setGlobal(name);
            }
            lua.load("""
                local textLoad = load
                load = function(source, name, mode, environment)
                    return textLoad(source, name, "t", environment or _G)
                end
                """, "=native-sandbox");
            lua.call(0, 0);
            return new NativeLuaState(lua, version);
        } catch (RuntimeException | Error failure) {
            lua.close();
            throw failure;
        }
    }

    public Version version() {
        return version;
    }

    public LuaState state() {
        return lua;
    }

    public void pushHostLibrary(final String name) {
        if (!name.equals("debug") && !name.equals("eris")) throw new IllegalArgumentException("Unknown host library");
        lua.getField(lua.getRegistryIndex(), HOST_LIBRARY_PREFIX + name);
    }

    @Override
    public void close() {
        if (lua.isOpen()) lua.close();
    }

    private static synchronized void loadLibrary(final Version version) throws IOException {
        if (LOADED.contains(version)) return;
        final String filename = libraryName(version, System.getProperty("os.name"), System.getProperty("os.arch"));
        final String resource = "/assets/neoopencomputers/lib/" + filename;
        try (final var input = NativeLuaState.class.getResourceAsStream(resource)) {
            if (input == null) throw new IOException("Native Lua is unavailable for this platform: " + filename);
            final Path extracted = Files.createTempFile("neoopencomputers-", "-" + filename);
            extracted.toFile().deleteOnExit();
            Files.copy(input, extracted, StandardCopyOption.REPLACE_EXISTING);
            System.load(extracted.toAbsolutePath().toString());
            LOADED.add(version);
        }
    }

    static String libraryName(final Version version, final String osName, final String osArch) {
        final String os = osName.toLowerCase(Locale.ROOT);
        final String platform;
        final String extension;
        if (os.startsWith("windows")) { platform = "windows"; extension = ".dll"; }
        else if (os.equals("mac os x") || os.equals("darwin")) { platform = "darwin"; extension = ".dylib"; }
        else if (os.equals("linux")) { platform = "linux"; extension = ".so"; }
        else if (os.equals("freebsd")) { platform = "freebsd"; extension = ".so"; }
        else throw new IllegalArgumentException("Unsupported native Lua operating system: " + osName);
        final String arch = switch (osArch.toLowerCase(Locale.ROOT)) {
            case "amd64", "x86_64", "x64" -> "x86_64";
            case "x86", "i386", "i486", "i586", "i686" -> "x86";
            case "aarch64", "arm64" -> "aarch64";
            case "arm", "armv7", "armv7l" -> "arm";
            default -> throw new IllegalArgumentException("Unsupported native Lua architecture: " + osArch);
        };
        final String luaVersion = switch (version) {
            case LUA52 -> "52";
            case LUA53 -> "53";
            case LUA54 -> "54";
        };
        return "libjnlua" + luaVersion + "-" + platform + "-" + arch + extension;
    }
}
