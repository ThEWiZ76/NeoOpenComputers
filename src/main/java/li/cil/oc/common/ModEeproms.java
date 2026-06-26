package li.cil.oc.common;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.API;
import li.cil.oc.api.Items;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.io.InputStream;

public final class ModEeproms {
    static final String LUA_BIOS_NAME = "luabios";
    static final String LUA_BIOS_PATH = "/assets/" + NeoOpenComputers.MODID + "/lua/bios.lua";
    static final String LUA_BIOS_LABEL = "EEPROM (Lua BIOS)";

    public static void registerDefaults() {
        final byte[] code = luaBiosCode();
        if (code.length > 0) {
            final ItemStack stack = Items.registerEEPROM(LUA_BIOS_LABEL, code, new byte[0], true);
            if (API.items instanceof ItemRegistry registry && stack != null && !stack.isEmpty()) {
                registry.registerStack(LUA_BIOS_NAME, stack::copy);
            }
        }
    }

    public static byte[] luaBiosCode() {
        try (InputStream stream = ModEeproms.class.getResourceAsStream(LUA_BIOS_PATH)) {
            return stream == null ? new byte[0] : stream.readAllBytes();
        } catch (IOException e) {
            return new byte[0];
        }
    }

    private ModEeproms() {
    }
}
