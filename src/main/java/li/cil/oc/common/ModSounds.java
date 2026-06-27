package li.cil.oc.common;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.network.EnvironmentHost;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
    public static final String COMPUTER_RUNNING = "computer_running";
    public static final String FLOPPY_ACCESS = "floppy_access";
    public static final String FLOPPY_EJECT = "floppy_eject";
    public static final String FLOPPY_INSERT = "floppy_insert";
    public static final String HDD_ACCESS = "hdd_access";

    public static final String COMPUTER_RUNNING_ID = soundId(COMPUTER_RUNNING);
    public static final String FLOPPY_ACCESS_ID = soundId(FLOPPY_ACCESS);
    public static final String FLOPPY_EJECT_ID = soundId(FLOPPY_EJECT);
    public static final String FLOPPY_INSERT_ID = soundId(FLOPPY_INSERT);
    public static final String HDD_ACCESS_ID = soundId(HDD_ACCESS);

    private ModSounds() {
    }

    public static void register(final IEventBus eventBus) {
        RegistryEvents.SOUND_EVENTS.register(eventBus);
    }

    public static SoundEvent soundEvent(final String id) {
        final ResourceLocation location = ResourceLocation.parse(id);
        if (NeoOpenComputers.MODID.equals(location.getNamespace())) {
            return switch (location.getPath()) {
                case COMPUTER_RUNNING -> RegistryEvents.COMPUTER_RUNNING_EVENT.get();
                case FLOPPY_ACCESS -> RegistryEvents.FLOPPY_ACCESS_EVENT.get();
                case FLOPPY_EJECT -> RegistryEvents.FLOPPY_EJECT_EVENT.get();
                case FLOPPY_INSERT -> RegistryEvents.FLOPPY_INSERT_EVENT.get();
                case HDD_ACCESS -> RegistryEvents.HDD_ACCESS_EVENT.get();
                default -> SoundEvent.createVariableRangeEvent(location);
            };
        }
        return SoundEvent.createVariableRangeEvent(location);
    }

    public static void playDiskInsert(final EnvironmentHost host) {
        play(host, RegistryEvents.FLOPPY_INSERT_EVENT.get());
    }

    public static void playDiskEject(final EnvironmentHost host) {
        play(host, RegistryEvents.FLOPPY_EJECT_EVENT.get());
    }

    public static void play(final EnvironmentHost host, final SoundEvent sound) {
        if (host == null) {
            return;
        }
        play(host.world(), host.xPosition(), host.yPosition(), host.zPosition(), sound);
    }

    public static void play(final Level level, final double x, final double y, final double z, final SoundEvent sound) {
        if (level == null || level.isClientSide || sound == null) {
            return;
        }
        level.playSound(null, BlockPos.containing(x, y, z), sound, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    private static ResourceLocation id(final String name) {
        return ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, name);
    }

    private static String soundId(final String name) {
        return NeoOpenComputers.MODID + ":" + name;
    }

    private static final class RegistryEvents {
        private static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, NeoOpenComputers.MODID);

        private static final DeferredHolder<SoundEvent, SoundEvent> COMPUTER_RUNNING_EVENT = register(COMPUTER_RUNNING);
        private static final DeferredHolder<SoundEvent, SoundEvent> FLOPPY_ACCESS_EVENT = register(FLOPPY_ACCESS);
        private static final DeferredHolder<SoundEvent, SoundEvent> FLOPPY_EJECT_EVENT = register(FLOPPY_EJECT);
        private static final DeferredHolder<SoundEvent, SoundEvent> FLOPPY_INSERT_EVENT = register(FLOPPY_INSERT);
        private static final DeferredHolder<SoundEvent, SoundEvent> HDD_ACCESS_EVENT = register(HDD_ACCESS);

        private RegistryEvents() {
        }

        private static DeferredHolder<SoundEvent, SoundEvent> register(final String name) {
            return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(id(name)));
        }
    }
}
