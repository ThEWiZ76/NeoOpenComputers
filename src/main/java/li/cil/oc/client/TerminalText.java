package li.cil.oc.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

final class TerminalText {
    private TerminalText() {
    }

    static ResourceLocation font() {
        return Minecraft.UNIFORM_FONT;
    }

    static MutableComponent cell(final String text) {
        return Component.literal(text).withStyle(style -> style.withFont(font()));
    }
}
