package li.cil.oc.common.integration.forestry;

import forestry.api.IForestryApi;
import forestry.api.apiculture.ForestryActivityTypes;
import forestry.api.apiculture.genetics.IBee;
import forestry.api.arboriculture.genetics.ITree;
import forestry.api.core.genetics.IGenome;
import forestry.api.core.genetics.IIndividual;
import forestry.api.core.genetics.IIndividualLiving;
import forestry.api.core.genetics.IMutation;
import forestry.api.core.genetics.ISpecies;
import forestry.api.core.genetics.alleles.BeeChromosomes;
import forestry.api.core.genetics.alleles.ButterflyChromosomes;
import forestry.api.core.genetics.alleles.IChromosome;
import forestry.api.core.genetics.alleles.TreeChromosomes;
import forestry.api.core.genetics.capability.IIndividualHandlerItem;
import forestry.api.lepidopterology.genetics.IButterfly;
import li.cil.oc.api.driver.Converter;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ForestryConverter implements Converter {
    public static String name(ISpecies<?> species) { return species.getDisplayName().getString(); }

    public static double percentChance(IMutation<?> mutation) {
        // Forestry CE stores probabilities, while the legacy OC API exposed percent units.
        return mutation.getChance() * 100F;
    }

    @Override
    public void convert(Object value, Map<Object, Object> output) {
        if (value instanceof ISpecies<?> species) {
            output.put("name", name(species));
            output.put("uid", species.id().toString());
        } else if (value instanceof IMutation<?> mutation) {
            output.put("allele1", mutation.getFirstParent());
            output.put("allele2", mutation.getSecondParent());
            output.put("chance", percentChance(mutation));
            output.put("specialConditions", mutation.getSpecialConditions().stream().map(Component::getString).toArray(String[]::new));
        } else if (value instanceof IIndividual individual) {
            output.put("displayName", individual instanceof IButterfly butterfly ? butterfly.getDisplayName().getString() : name(individual.getSpecies()));
            output.put("ident", individual.getSpecies().id().toString());
            output.put("isAnalyzed", individual.isAnalyzed());
            output.put("isSecret", individual.isSecret());
            if (individual instanceof IIndividualLiving living) {
                output.put("health", living.getHealth());
                output.put("maxHealth", living.getMaxHealth());
            }
            if (individual instanceof IBee bee) {
                output.put("type", "bee");
                output.put("canSpawn", bee.getMate() != null);
                output.put("generation", bee.getGeneration());
                output.put("hasEffect", bee.hasGlint());
                output.put("isAlive", bee.isAlive());
                output.put("isNatural", bee.isPristine());
            } else if (individual instanceof IButterfly butterfly) {
                output.put("type", "butterfly");
                output.put("size", butterfly.getGenome().getActiveValue(ButterflyChromosomes.SIZE));
            } else if (individual instanceof ITree) {
                output.put("type", "tree");
                output.put("plantType", name(individual.getSpecies()));
            }
            if (individual.isAnalyzed() && (individual instanceof IBee || individual instanceof IButterfly || individual instanceof ITree)) {
                output.put("active", genome(individual, false));
                output.put("inactive", genome(individual, true));
            }
        } else if (value instanceof ItemStack stack) {
            final var individual = IIndividualHandlerItem.getIndividual(stack);
            if (individual != null) output.put("individual", individual);
            final var board = IForestryApi.INSTANCE.getCircuitManager().getCircuitBoard(stack);
            if (board != null) {
                final var circuits = java.util.Arrays.stream(board.getCircuits()).filter(java.util.Objects::nonNull)
                    .map(forestry.api.core.circuits.ICircuit::getId).toArray(String[]::new);
                if (circuits.length > 0) output.put("circuits", circuits);
            }
        }
    }

    private static Map<String, Object> genome(IIndividual individual, boolean inactive) {
        final var genome = individual.getGenome();
        final var values = new LinkedHashMap<String, Object>();
        if (individual instanceof IBee) {
            put(values, genome, inactive, "species", BeeChromosomes.SPECIES);
            put(values, genome, inactive, "speed", BeeChromosomes.SPEED);
            put(values, genome, inactive, "lifespan", BeeChromosomes.LIFESPAN);
            put(values, genome, inactive, "fertility", BeeChromosomes.FERTILITY);
            put(values, genome, inactive, "temperatureTolerance", BeeChromosomes.TEMPERATURE_TOLERANCE);
            final var activity = genome.getAllelePair(BeeChromosomes.ACTIVITY);
            values.put("neverSleeps", ForestryActivityTypes.METATURNAL.equals((inactive ? activity.inactive() : activity.active()).value()));
            put(values, genome, inactive, "humidityTolerance", BeeChromosomes.HUMIDITY_TOLERANCE);
            put(values, genome, inactive, "toleratesRain", BeeChromosomes.TOLERATES_RAIN);
            put(values, genome, inactive, "caveDwelling", BeeChromosomes.CAVE_DWELLING);
            put(values, genome, inactive, "flowerProvider", BeeChromosomes.FLOWER_TYPE);
            put(values, genome, inactive, "flowering", BeeChromosomes.POLLINATION);
            put(values, genome, inactive, "effect", BeeChromosomes.EFFECT);
            put(values, genome, inactive, "territory", BeeChromosomes.TERRITORY);
        } else if (individual instanceof IButterfly) {
            put(values, genome, inactive, "species", ButterflyChromosomes.SPECIES);
            put(values, genome, inactive, "size", ButterflyChromosomes.SIZE);
            put(values, genome, inactive, "speed", ButterflyChromosomes.SPEED);
            put(values, genome, inactive, "lifespan", ButterflyChromosomes.LIFESPAN);
            put(values, genome, inactive, "metabolism", ButterflyChromosomes.METABOLISM);
            put(values, genome, inactive, "fertility", ButterflyChromosomes.FERTILITY);
            put(values, genome, inactive, "temperatureTolerance", ButterflyChromosomes.TEMPERATURE_TOLERANCE);
            put(values, genome, inactive, "humidityTolerance", ButterflyChromosomes.HUMIDITY_TOLERANCE);
            put(values, genome, inactive, "nocturnal", ButterflyChromosomes.NEVER_SLEEPS);
            put(values, genome, inactive, "tolerantFlyer", ButterflyChromosomes.TOLERATES_RAIN);
            put(values, genome, inactive, "fireResist", ButterflyChromosomes.FIREPROOF);
            put(values, genome, inactive, "flowerProvider", ButterflyChromosomes.FLOWER_TYPE);
            put(values, genome, inactive, "effect", ButterflyChromosomes.EFFECT);
            put(values, genome, inactive, "cocoon", ButterflyChromosomes.COCOON);
        } else if (individual instanceof ITree) {
            put(values, genome, inactive, "species", TreeChromosomes.SPECIES);
            put(values, genome, inactive, "fireproof", TreeChromosomes.FIREPROOF);
            put(values, genome, inactive, "height", TreeChromosomes.HEIGHT);
            put(values, genome, inactive, "fertility", TreeChromosomes.SAPLINGS);
            put(values, genome, inactive, "fruits", TreeChromosomes.FRUIT);
            put(values, genome, inactive, "yield", TreeChromosomes.YIELD);
            put(values, genome, inactive, "sappiness", TreeChromosomes.SAPPINESS);
            put(values, genome, inactive, "effect", TreeChromosomes.EFFECT);
            put(values, genome, inactive, "maturation", TreeChromosomes.MATURATION);
            put(values, genome, inactive, "girth", TreeChromosomes.GIRTH);
        }
        return values;
    }

    private static <V> void put(Map<String, Object> output, IGenome genome, boolean inactive, String name, IChromosome<V> chromosome) {
        final var pair = genome.getChromosomes().get(chromosome);
        if (pair == null) { output.put(name, "missing"); return; }
        @SuppressWarnings("unchecked") final V value = (V) (inactive ? pair.inactive() : pair.active()).value();
        if (value instanceof Number || value instanceof Boolean) output.put(name, value);
        else if (value instanceof Vec3i vector) output.put(name, new Integer[]{vector.getX(), vector.getY(), vector.getZ()});
        else output.put(name, Component.translatable(chromosome.translationKey(value)).getString());
    }
}
