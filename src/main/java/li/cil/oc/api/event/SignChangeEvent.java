package li.cil.oc.api.event;

import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public abstract class SignChangeEvent extends Event {
    public final SignBlockEntity sign;
    public final String[] lines;

    private SignChangeEvent(final SignBlockEntity sign, final String[] lines) {
        this.sign = sign;
        this.lines = lines;
    }

    public static class Pre extends SignChangeEvent implements ICancellableEvent {
        public Pre(final SignBlockEntity sign, final String[] lines) {
            super(sign, lines);
        }
    }

    public static class Post extends SignChangeEvent {
        public Post(final SignBlockEntity sign, final String[] lines) {
            super(sign, lines);
        }
    }
}
