package li.cil.oc.api;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.InterModComms;
import org.apache.commons.lang3.tuple.Pair;

public final class IMC {
    public static final String REGISTER_ASSEMBLER_FILTER = "registerAssemblerFilter";
    public static final String REGISTER_ASSEMBLER_TEMPLATE = "registerAssemblerTemplate";
    public static final String REGISTER_DISASSEMBLER_TEMPLATE = "registerDisassemblerTemplate";
    public static final String REGISTER_TOOL_DURABILITY_PROVIDER = "registerToolDurabilityProvider";
    public static final String REGISTER_WRENCH_TOOL = "registerWrenchTool";
    public static final String REGISTER_WRENCH_TOOL_CHECK = "registerWrenchToolCheck";
    public static final String REGISTER_ITEM_CHARGE = "registerItemCharge";
    public static final String REGISTER_INK_PROVIDER = "registerInkProvider";
    public static final String BLACKLIST_PERIPHERAL = "blacklistPeripheral";
    public static final String BLACKLIST_HOST = "blacklistHost";
    public static final String REGISTER_CUSTOM_POWER_SYSTEM = "registerCustomPowerSystem";
    public static final String REGISTER_PROGRAM_DISK_LABEL = "registerProgramDiskLabel";

    private static final String MOD_ID = "neoopencomputers";

    public static void registerAssemblerFilter(final String callback) {
        send(REGISTER_ASSEMBLER_FILTER, callback);
    }

    public static void registerAssemblerTemplate(final String name, final String select, final String validate, final String assemble, final Class host, final int[] containerTiers, final int[] upgradeTiers, final Iterable<Pair<String, Integer>> componentSlots) {
        CompoundTag nbt = new CompoundTag();
        if (name != null) {
            nbt.putString("name", name);
        }
        nbt.putString("select", select);
        nbt.putString("validate", validate);
        nbt.putString("assemble", assemble);
        if (host != null) {
            nbt.putString("hostClass", host.getName());
        }
        putTierSlots(nbt, "containerSlots", containerTiers);
        putTierSlots(nbt, "upgradeSlots", upgradeTiers);
        putComponentSlots(nbt, componentSlots);
        send(REGISTER_ASSEMBLER_TEMPLATE, nbt);
    }

    public static void registerDisassemblerTemplate(final String name, final String select, final String disassemble) {
        CompoundTag nbt = new CompoundTag();
        if (name != null) {
            nbt.putString("name", name);
        }
        nbt.putString("select", select);
        nbt.putString("disassemble", disassemble);
        send(REGISTER_DISASSEMBLER_TEMPLATE, nbt);
    }

    public static void registerToolDurabilityProvider(final String callback) {
        send(REGISTER_TOOL_DURABILITY_PROVIDER, callback);
    }

    public static void registerWrenchTool(final String callback) {
        send(REGISTER_WRENCH_TOOL, callback);
    }

    public static void registerWrenchToolCheck(final String callback) {
        send(REGISTER_WRENCH_TOOL_CHECK, callback);
    }

    public static void registerItemCharge(final String name, final String canCharge, final String charge) {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("name", name);
        nbt.putString("canCharge", canCharge);
        nbt.putString("charge", charge);
        send(REGISTER_ITEM_CHARGE, nbt);
    }

    public static void registerInkProvider(final String callback) {
        send(REGISTER_INK_PROVIDER, callback);
    }

    public static void blacklistPeripheral(final Class peripheral) {
        send(BLACKLIST_PERIPHERAL, peripheral.getName());
    }

    public static void blacklistHost(final String name, final Class host, final ItemStack stack) {
        send(BLACKLIST_HOST, blacklistHostPayload(name, host, stack));
    }

    public static void registerProgramDiskLabel(final String programName, final String diskLabel, final String... architectures) {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("program", programName);
        nbt.putString("label", diskLabel);
        if (architectures != null && architectures.length > 0) {
            ListTag architectureTags = new ListTag();
            for (String architecture : architectures) {
                architectureTags.add(StringTag.valueOf(architecture));
            }
            nbt.put("architectures", architectureTags);
        }
        send(REGISTER_PROGRAM_DISK_LABEL, nbt);
    }

    private static void putTierSlots(final CompoundTag nbt, final String key, final int[] tiers) {
        if (tiers == null || tiers.length == 0) {
            return;
        }
        ListTag tags = new ListTag();
        for (int tier : tiers) {
            CompoundTag slot = new CompoundTag();
            slot.putInt("tier", tier);
            tags.add(slot);
        }
        nbt.put(key, tags);
    }

    private static void putComponentSlots(final CompoundTag nbt, final Iterable<Pair<String, Integer>> componentSlots) {
        if (componentSlots == null) {
            return;
        }
        ListTag tags = new ListTag();
        for (Pair<String, Integer> slot : componentSlots) {
            CompoundTag slotTag = new CompoundTag();
            if (slot != null) {
                slotTag.putString("type", slot.getLeft());
                slotTag.putInt("tier", slot.getRight());
            }
            tags.add(slotTag);
        }
        if (!tags.isEmpty()) {
            nbt.put("componentSlots", tags);
        }
    }

    static CompoundTag blacklistHostPayload(final String name, final Class host, final ItemStack stack) {
        CompoundTag nbt = new CompoundTag();
        if (name != null) {
            nbt.putString("name", name);
        }
        if (host != null) {
            nbt.putString("host", host.getName());
        }
        nbt.put("item", encodeStack(stack));
        return nbt;
    }

    private static CompoundTag encodeStack(final ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return new CompoundTag();
        }
        Tag tag = ItemStack.OPTIONAL_CODEC.encodeStart(NbtOps.INSTANCE, stack).result().orElseGet(CompoundTag::new);
        return tag instanceof CompoundTag compoundTag ? compoundTag : new CompoundTag();
    }

    private static void send(final String key, final Object payload) {
        InterModComms.sendTo(MOD_ID, key, () -> payload);
    }

    private IMC() {
    }
}
