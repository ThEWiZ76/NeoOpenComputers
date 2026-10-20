package li.cil.oc.common.integration.ae2;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** World-owned requests outlive the Lua heap that originally submitted them. */
final class CraftingJournal extends SavedData {
    static final String FILE = "neoopencomputers_ae2_requests";
    static final Factory<CraftingJournal> FACTORY = new Factory<>(CraftingJournal::new, CraftingJournal::load, null);
    private final Map<UUID, CompoundTag> requests = new LinkedHashMap<>();

    static boolean terminal(final CompoundTag tag) {
        return tag.getBoolean("done") || tag.getBoolean("canceled") || tag.contains("failed");
    }

    synchronized void record(final CompoundTag state) {
        final UUID id = state.getUUID("id");
        final CompoundTag stored;
        if (terminal(state)) {
            // Retain small outcomes: an unloaded host can restore an arbitrarily old pre-completion snapshot.
            stored = new CompoundTag(); stored.putUUID("id", id);
            stored.putBoolean("done", state.getBoolean("done")); stored.putBoolean("canceled", state.getBoolean("canceled"));
            if (state.contains("failed")) stored.putString("failed", state.getString("failed"));
        } else stored = state.copy();
        if (!stored.equals(requests.get(id))) { requests.put(id, stored); setDirty(); }
    }

    synchronized CompoundTag lookup(final UUID id) {
        final var state = requests.get(id);
        return state == null ? null : state.copy();
    }

    synchronized List<CompoundTag> active() {
        return requests.values().stream().filter(state -> !terminal(state)).map(CompoundTag::copy).toList();
    }

    @Override public synchronized CompoundTag save(final CompoundTag tag, final HolderLookup.Provider registries) {
        final var list = new ListTag();
        requests.values().forEach(state -> list.add(state.copy()));
        tag.put("requests", list);
        return tag;
    }

    static CraftingJournal load(final CompoundTag tag, final HolderLookup.Provider registries) {
        final var journal = new CraftingJournal();
        for (final var entry : tag.getList("requests", Tag.TAG_COMPOUND)) {
            final var state = (CompoundTag) entry;
            if (state.hasUUID("id")) journal.requests.put(state.getUUID("id"), state.copy());
        }
        return journal;
    }
}
