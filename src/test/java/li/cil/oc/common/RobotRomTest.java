package li.cil.oc.common;

import li.cil.oc.api.fs.FileSystem;
import li.cil.oc.api.fs.Mode;
import org.junit.jupiter.api.Test;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

final class RobotRomTest {
    @Test
    void robotRomIsReadOnlyAndContainsOpenOsEntrypoints() {
        final FileSystem rom = rom();
        assertTrue(rom.isReadOnly());
        for (final String path : new String[]{".autorun.lua", ".prop", "lib/robot.lua", "bin/go.lua"}) {
            assertTrue(rom.exists(path), path);
            assertTrue(rom.size(path) > 0, path);
        }
        assertFalse(rom.exists("init.lua"), "Component ROM must not compete with the boot disk");
    }

    @Test
    void requireRobotAndGoForwardUseRobotComponentDirections() throws IOException {
        final Globals lua = JsePlatform.standardGlobals();
        lua.load("""
            moves = {}
            turns = {}
            package.loaded.component = {robot = {
              move = function(side) table.insert(moves, side); return true end,
              turn = function(clockwise) table.insert(turns, clockwise); return true end
            }}
            package.loaded.sides = {front=3, back=2, up=1, down=0}
            package.loaded.shell = {parse = function(...) return {...} end}
            """).call();
        lua.get("package").get("preload").set("robot", lua.load(read("lib/robot.lua"), "robot.lua"));
        lua.load("assert(require('robot').forward()); assert(require('robot').up())").call();
        lua.load(read("bin/go.lua"), "go.lua").invoke(LuaValue.varargsOf(new LuaValue[]{LuaValue.valueOf("forward"), LuaValue.valueOf("3")}));
        lua.load(read("bin/go.lua"), "go.lua").invoke(LuaValue.varargsOf(new LuaValue[]{LuaValue.valueOf("left"), LuaValue.valueOf("2")}));
        assertEquals(5, lua.get("moves").length());
        assertEquals(3, lua.get("moves").get(1).toint());
        assertEquals(1, lua.get("moves").get(2).toint());
        for (int i = 3; i <= 5; i++) {
            assertEquals(3, lua.get("moves").get(i).toint());
        }
        assertEquals(2, lua.get("turns").length());
        assertFalse(lua.get("turns").get(1).toboolean());
        assertFalse(lua.get("turns").get(2).toboolean());
    }

    @Test
    void autorunInstallsAndRemovesOnlyItsOwnLinks() throws IOException {
        final Globals lua = JsePlatform.standardGlobals();
        lua.load("""
            links = {['lib/existing.lua'] = '/existing.lua'}
            package.loaded.event = {listen = function(name, callback)
              assert(name == 'component_removed'); removed = callback
            end}
            package.loaded.process = {running = function() return '/mnt/rom/.autorun.lua' end}
            package.loaded.filesystem = {
              path = function() return '/mnt/rom' end,
              concat = function(...) return table.concat({...}, '/') end,
              list = function(path)
                local files = path == '/mnt/rom/lib' and {'robot.lua','existing.lua'}
                  or path == '/mnt/rom/bin' and {'go.lua'} or {}
                local index = 0
                return function() index=index+1; return files[index] end
              end,
              link = function(source, target)
                if links[target] then return false end
                links[target]=source; return true
              end,
              remove = function(target) links[target]=nil end
            }
            """).call();
        lua.load(read(".autorun.lua"), ".autorun.lua").call(lua.load("return {address='rom-address'}").call());
        assertEquals("/mnt/rom/lib/robot.lua", lua.get("links").get("lib/robot.lua").tojstring());
        assertEquals("/mnt/rom/bin/go.lua", lua.get("links").get("bin/go.lua").tojstring());
        lua.get("removed").call(LuaValue.valueOf("component_removed"), LuaValue.valueOf("another-disk"));
        assertFalse(lua.get("links").get("lib/robot.lua").isnil());
        assertFalse(lua.get("removed").call(LuaValue.valueOf("component_removed"), LuaValue.valueOf("rom-address")).toboolean());
        assertTrue(lua.get("links").get("lib/robot.lua").isnil());
        assertTrue(lua.get("links").get("bin/go.lua").isnil());
        assertEquals("/existing.lua", lua.get("links").get("lib/existing.lua").tojstring());
    }

    private static FileSystem rom() {
        final FileSystem rom = new FileSystemRegistry().fromClass(RobotRomTest.class, "neoopencomputers", "lua/component/robot");
        assertNotNull(rom);
        return rom;
    }

    private static String read(final String path) throws IOException {
        final FileSystem rom = rom();
        final var handle = rom.getHandle(rom.open(path, Mode.Read));
        try {
            final byte[] content = new byte[(int) rom.size(path)];
            assertEquals(content.length, handle.read(content));
            return new String(content, StandardCharsets.UTF_8);
        } finally {
            handle.close();
            rom.close();
        }
    }
}
