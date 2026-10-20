package li.cil.oc.common.integration.forestry;

import forestry.api.IForestryApi;
import forestry.api.apiculture.IBeeHousing;
import forestry.api.apiculture.genetics.IBeeSpeciesType;
import forestry.api.core.genetics.ForestrySpeciesTypes;
import forestry.api.core.genetics.capability.IIndividualHandlerItem;
import forestry.core.content.analyzer.TileAnalyzer;
import li.cil.oc.api.driver.DriverBlock;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;

public final class ForestryBlockDriver implements DriverBlock {
    @Override
    public boolean worksWith(Level world, BlockPos pos, Direction side) {
        if (world == null || pos == null || !world.hasChunkAt(pos)) return false;
        final var tile = world.getBlockEntity(pos);
        return tile instanceof IBeeHousing || tile instanceof TileAnalyzer;
    }

    @Override
    public ManagedEnvironment createEnvironment(Level world, BlockPos pos, Direction side) {
        if (!worksWith(world, pos, side)) return null;
        final var tile = world.getBlockEntity(pos);
        return tile instanceof TileAnalyzer ? new AnalyzerEnvironment(tile) : new HousingEnvironment(tile);
    }

    public abstract static class Environment extends AbstractManagedEnvironment implements NamedBlock {
        private final BlockEntity tile;
        private final String name;

        Environment(BlockEntity tile, String name) {
            this.tile = tile;
            this.name = name;
            setNode(li.cil.oc.api.Network.newNode(this, Visibility.Network).withComponent(name, Visibility.Network).create());
        }

        @Override public String preferredName() { return name; }
        @Override public int priority() { return 0; }

        protected BlockEntity tile() {
            final var world = tile.getLevel();
            if (tile.isRemoved() || world == null || !world.hasChunkAt(tile.getBlockPos()) || world.getBlockEntity(tile.getBlockPos()) != tile) {
                throw new IllegalStateException("Forestry block is no longer present");
            }
            return tile;
        }
    }

    public static final class AnalyzerEnvironment extends Environment {
        AnalyzerEnvironment(BlockEntity tile) { super(tile, "forestry_analyzer"); }

        @Callback(doc = "function():boolean -- Get whether the analyzer can work.")
        public Object[] isWorking(Context context, Arguments args) { return new Object[]{((TileAnalyzer) tile()).hasWork()}; }

        @Callback(doc = "function():number -- Get the progress of the current operation.")
        public Object[] getProgress(Context context, Arguments args) { return new Object[]{1.0 - ((TileAnalyzer) tile()).getProgressScaled(100) / 100.0}; }

        @Callback(doc = "function():table -- Get info on the currently present individual.")
        public Object[] getIndividualOnDisplay(Context context, Arguments args) {
            return new Object[]{IIndividualHandlerItem.getIndividual(((TileAnalyzer) tile()).getIndividualOnDisplay())};
        }
    }

    public static final class HousingEnvironment extends Environment {
        HousingEnvironment(BlockEntity tile) { super(tile, "bee_housing"); }

        @Callback(doc = "function():boolean -- Can the bees breed?")
        public Object[] canBreed(Context context, Arguments args) { return new Object[]{((IBeeHousing) tile()).getBeekeepingLogic().canWork()}; }

        @Callback(doc = "function():table -- Get the drone.")
        public Object[] getDrone(Context context, Arguments args) {
            final var stack = ((IBeeHousing) tile()).getBeeInventory().getDrone();
            return stack == null ? null : new Object[]{IIndividualHandlerItem.getIndividual(stack)};
        }

        @Callback(doc = "function():table -- Get the queen.")
        public Object[] getQueen(Context context, Arguments args) {
            final var stack = ((IBeeHousing) tile()).getBeeInventory().getQueen();
            return stack == null ? null : new Object[]{IIndividualHandlerItem.getIndividual(stack)};
        }

        @Callback(doc = "function():table -- Get all bee mutations and their conditions.")
        public Object[] getBeeBreedingData(Context context, Arguments args) {
            tile();
            final var type = bees();
            if (type == null) return null;
            final var rows = new LinkedHashSet<java.util.Map<String, Object>>();
            for (final var mutation : type.getMutations().getAllMutations()) {
                final var row = new LinkedHashMap<String, Object>();
                row.put("allele1", ForestryConverter.name(mutation.getFirstParent()));
                row.put("allele2", ForestryConverter.name(mutation.getSecondParent()));
                row.put("chance", ForestryConverter.percentChance(mutation));
                row.put("specialConditions", mutation.getSpecialConditions().stream().map(net.minecraft.network.chat.Component::getString).toArray(String[]::new));
                row.put("result", ForestryConverter.name(mutation.getResult()));
                rows.add(row);
            }
            return new Object[]{rows};
        }

        @Callback(doc = "function():table -- Get all species produced by known bee mutations.")
        public Object[] listAllSpecies(Context context, Arguments args) {
            tile();
            final var type = bees();
            if (type == null) return null;
            final var species = new LinkedHashSet<forestry.api.apiculture.genetics.IBeeSpecies>();
            for (final var mutation : type.getMutations().getAllMutations()) species.add(mutation.getResult());
            return new Object[]{species};
        }

        @Callback(doc = "function(beeName:string):table -- Get mutations producing a bee by species ID or localized name.")
        public Object[] getBeeParents(Context context, Arguments args) {
            tile();
            final var type = bees();
            if (type == null) return null;
            final String child = args.checkString(0).toLowerCase(Locale.ROOT);
            final var mutations = new LinkedHashSet<forestry.api.core.genetics.IMutation<forestry.api.apiculture.genetics.IBeeSpecies>>();
            for (final var mutation : type.getMutations().getAllMutations()) {
                final var species = mutation.getResult();
                if (species.id().toString().toLowerCase(Locale.ROOT).equals(child) || ForestryConverter.name(species).toLowerCase(Locale.ROOT).equals(child)) mutations.add(mutation);
            }
            return new Object[]{mutations};
        }

        private static IBeeSpeciesType bees() {
            final var type = IForestryApi.INSTANCE.getGeneticManager().getSpeciesTypeSafe(ForestrySpeciesTypes.BEE);
            return type instanceof IBeeSpeciesType bees ? bees : null;
        }
    }
}
