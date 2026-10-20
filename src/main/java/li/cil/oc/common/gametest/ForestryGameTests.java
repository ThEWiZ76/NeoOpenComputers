package li.cil.oc.common.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@GameTestHolder("neoopencomputers")
public final class ForestryGameTests {
    @GameTestGenerator
    public static Collection<TestFunction> tests() {
        if (!net.neoforged.fml.ModList.get().isLoaded("forestry")) {
            if (Boolean.getBoolean("neoopencomputers.testForestry")) throw new IllegalStateException("Requested Forestry fixture is missing");
            return List.of();
        }
        if (Boolean.getBoolean("neoopencomputers.testForestryButterflies") && !net.neoforged.fml.ModList.get().isLoaded("forestrybutterflies")) {
            throw new IllegalStateException("Requested Forestry Butterflies fixture is missing");
        }
        return List.of(
            new TestFunction("forestry", "forestry_real_block_callbacks", "neoopencomputers:empty", 200, 0, true, Loaded::blockCallbacks),
            new TestFunction("forestry", "forestry_real_individual_conversion", "neoopencomputers:empty", 200, 0, true, Loaded::individualConversion),
            new TestFunction("forestry", "forestry_native_adapter_and_powered_analyzer", "neoopencomputers:empty", 1000, 0, true, Loaded::nativeAnalyzer));
    }

    private static final class Loaded {
        static void blockCallbacks(GameTestHelper helper) {
            try {
                for (String kind : List.of("apiary", "bee_house", "analyzer")) {
                    final var pos = helper.absolutePos(new BlockPos(kind.equals("analyzer") ? 3 : 1, 1, 1));
                    final var block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("forestry", kind));
                    helper.assertTrue(block != net.minecraft.world.level.block.Blocks.AIR, "Published Forestry block missing: " + kind);
                    helper.getLevel().setBlockAndUpdate(pos, block.defaultBlockState());
                    final var driver = li.cil.oc.api.Driver.driverFor(helper.getLevel(), pos, Direction.NORTH);
                    helper.assertTrue(driver != null, "No driver for real Forestry " + kind);
                    final var environment = driver.createEnvironment(helper.getLevel(), pos, Direction.NORTH);
                    try {
                        final var component = (li.cil.oc.api.network.Component) environment.node();
                        final var names = kind.equals("analyzer") ? List.of("isWorking", "getProgress", "getIndividualOnDisplay")
                            : List.of("canBreed", "getDrone", "getQueen", "getBeeBreedingData", "listAllSpecies", "getBeeParents");
                        helper.assertTrue(component.methods().containsAll(names), "Missing Forestry callbacks on " + kind + ": " + component.methods());
                        helper.assertTrue(component.name().equals(kind.equals("analyzer") ? "forestry_analyzer" : "bee_housing"), "Legacy Forestry component name changed");
                        if (kind.equals("analyzer")) {
                            helper.assertTrue(component.invoke("isWorking", null)[0].equals(Boolean.FALSE), "Empty analyzer reports work");
                            final var analyzer = (forestry.core.content.analyzer.TileAnalyzer) helper.getLevel().getBlockEntity(pos);
                            helper.assertTrue(((Number) component.invoke("getProgress", null)[0]).doubleValue() == 1.0 - analyzer.getProgressScaled(100) / 100.0, "Legacy analyzer progress direction/scale changed");
                            final var displayed = component.invoke("getIndividualOnDisplay", null);
                            helper.assertTrue(displayed != null && displayed.length == 1 && displayed[0] == null, "Empty analyzer must return one nil");
                        } else {
                            helper.assertTrue(component.invoke("canBreed", null)[0].equals(Boolean.FALSE), "Empty housing reports breeding work");
                            for (String method : List.of("getDrone", "getQueen")) {
                                final var empty = component.invoke(method, null);
                                helper.assertTrue(empty != null && empty.length == 1 && empty[0] == null, "Empty bee housing must return one nil: " + method);
                            }
                            final var mutations = (Object[]) li.cil.oc.common.OpenComputersApi.convert(component.invoke("getBeeBreedingData", null))[0];
                            helper.assertTrue(mutations.length > 0 && mutations[0] instanceof Map, "Real registered mutations were not converted");
                            final var species = (Object[]) li.cil.oc.common.OpenComputersApi.convert(component.invoke("listAllSpecies", null))[0];
                            helper.assertTrue(species.length > 0 && species[0] instanceof Map && ((Map<?, ?>) species[0]).get("uid") instanceof String, "Mutation-output species lost identities");
                            final var parents = (Object[]) li.cil.oc.common.OpenComputersApi.convert(component.invoke("getBeeParents", null, "FORESTRY:ABYSSAL"))[0];
                            boolean percent = false;
                            for (Object parent : parents) {
                                final var row = (Map<?, ?>) parent;
                                final var first = (Map<?, ?>) row.get("allele1");
                                final var second = (Map<?, ?>) row.get("allele2");
                                if (first.get("uid").equals("forestry:pirate") && second.get("uid").equals("forestry:ended")) {
                                    helper.assertTrue(((Number) row.get("chance")).doubleValue() == 40.0, "Modern probability leaked instead of legacy percent");
                                    percent = true;
                                }
                            }
                            helper.assertTrue(percent, "Case-insensitive parent lookup missed published Abyssal mutation");
                            final var missing = (Object[]) li.cil.oc.common.OpenComputersApi.convert(component.invoke("getBeeParents", null, "oc:no_such_species"))[0];
                            helper.assertTrue(missing.length == 0, "Unknown species should return an empty table");
                            boolean rejected = false;
                            try { component.invoke("getBeeParents", null, true); }
                            catch (IllegalArgumentException expected) { rejected = true; }
                            helper.assertTrue(rejected, "Parent callback accepts a non-string argument");
                        }
                        final var stale = helper.getLevel().getBlockEntity(pos);
                        helper.getLevel().removeBlockEntity(pos);
                        helper.getLevel().setBlockEntity(((net.minecraft.world.level.block.EntityBlock) block).newBlockEntity(pos, block.defaultBlockState()));
                        stale.clearRemoved();
                        boolean rejected = false;
                        try { component.invoke(kind.equals("analyzer") ? "getProgress" : "getBeeBreedingData", null); }
                        catch (IllegalStateException expected) { rejected = expected.getMessage().contains("no longer present"); }
                        helper.assertTrue(rejected, "Forestry environment reused a replaced same-kind host");
                    } finally { environment.node().remove(); }
                }
                helper.succeed();
            } catch (Exception exception) { throw new IllegalStateException(exception); }
        }

        static void individualConversion(GameTestHelper helper) {
            final var manager = forestry.api.IForestryApi.INSTANCE.getGeneticManager();
            final var type = manager.getSpeciesType(forestry.api.core.genetics.ForestrySpeciesTypes.BEE);
            final var base = type.getDefaultSpecies().getDefaultGenome();
            final Map<forestry.api.core.genetics.alleles.IChromosome<?>, forestry.api.core.genetics.alleles.AllelePair<?>> pairs = new java.util.LinkedHashMap<>();
            pairs.put(forestry.api.core.genetics.alleles.BeeChromosomes.SPEED, new forestry.api.core.genetics.alleles.AllelePair<>(new forestry.api.core.genetics.alleles.Allele<>(0.625F, true), new forestry.api.core.genetics.alleles.Allele<>(1.25F, false)));
            pairs.put(forestry.api.core.genetics.alleles.BeeChromosomes.TERRITORY, new forestry.api.core.genetics.alleles.AllelePair<>(new forestry.api.core.genetics.alleles.Allele<>(new net.minecraft.core.Vec3i(3, 5, 7), true), new forestry.api.core.genetics.alleles.Allele<>(new net.minecraft.core.Vec3i(9, 11, 13), false)));
            pairs.put(forestry.api.core.genetics.alleles.BeeChromosomes.ACTIVITY, new forestry.api.core.genetics.alleles.AllelePair<>(new forestry.api.core.genetics.alleles.Allele<>(forestry.api.apiculture.ForestryActivityTypes.METATURNAL, true), new forestry.api.core.genetics.alleles.Allele<>(forestry.api.apiculture.ForestryActivityTypes.DIURNAL, false)));
            final var individual = type.getDefaultSpecies().createIndividual(base.copyWithPairs(pairs));
            final Object converted = li.cil.oc.common.OpenComputersApi.convert(new Object[]{individual})[0];
            helper.assertTrue(converted instanceof Map, "Real Forestry individual has no OC converter");
            final var hidden = (Map<?, ?>) converted;
            helper.assertTrue("bee".equals(hidden.get("type")) && Boolean.FALSE.equals(hidden.get("isAnalyzed")), "Unanalyzed bee metadata changed");
            helper.assertTrue(!hidden.containsKey("active") && !hidden.containsKey("inactive") && !individual.isAnalyzed(), "Description reveals or analyzes hidden genetics");
            helper.assertTrue(Boolean.FALSE.equals(hidden.get("canSpawn")), "Unmated bee canSpawn must be false");
            individual.setMate(base);
            ((forestry.api.apiculture.genetics.IBee) individual).setPristine(false);
            individual.analyze();
            final var analyzed = (Map<?, ?>) li.cil.oc.common.OpenComputersApi.convert(new Object[]{individual})[0];
            helper.assertTrue(analyzed.get("active") instanceof Map && analyzed.get("inactive") instanceof Map, "Analyzed genome lacks active/inactive data");
            final var active = (Map<?, ?>) analyzed.get("active");
            helper.assertTrue(active.get("speed") instanceof Number && active.get("fertility") instanceof Number
                && active.get("neverSleeps") instanceof Boolean && active.get("territory") instanceof Object[], "Genome loses numeric/boolean/vector types");
            final var inactive = (Map<?, ?>) analyzed.get("inactive");
            helper.assertTrue(((Number) active.get("speed")).doubleValue() == 0.625 && ((Number) inactive.get("speed")).doubleValue() == 1.25, "Active/inactive numeric alleles were interchanged");
            helper.assertTrue(Boolean.TRUE.equals(active.get("neverSleeps")) && Boolean.FALSE.equals(inactive.get("neverSleeps")), "Metaturnal activity did not preserve neverSleeps");
            helper.assertTrue(java.util.Arrays.equals((Object[]) active.get("territory"), new Integer[]{3,5,7}) && java.util.Arrays.equals((Object[]) inactive.get("territory"), new Integer[]{9,11,13}), "Territory vector axes/alleles changed");
            helper.assertTrue(Boolean.TRUE.equals(analyzed.get("canSpawn")) && Boolean.FALSE.equals(analyzed.get("isNatural")), "Mated/pristine bee compatibility fields changed");
            final var stack = individual.createStack(forestry.api.apiculture.genetics.BeeLifeStage.QUEEN);
            final var described = (Map<?, ?>) li.cil.oc.common.OpenComputersApi.convert(new Object[]{stack})[0];
            helper.assertTrue(described.get("individual") instanceof Map && ((Map<?, ?>) described.get("individual")).get("active") instanceof Map, "Bee item description lacks analyzed genetics");
            final var tree = manager.getSpeciesType(forestry.api.core.genetics.ForestrySpeciesTypes.TREE).getDefaultSpecies().createIndividual();
            tree.analyze();
            final var treeData = (Map<?, ?>) li.cil.oc.common.OpenComputersApi.convert(new Object[]{tree})[0];
            helper.assertTrue("tree".equals(treeData.get("type")) && ((Map<?, ?>) treeData.get("active")).keySet().containsAll(List.of("species","fireproof","height","fertility","fruits","yield","sappiness","effect","maturation","girth")), "Tree genome fields missing");
            final var circuits = forestry.api.IForestryApi.INSTANCE.getCircuitManager();
            final var holder = circuits.getCircuitHolders().iterator().next();
            final var layout = circuits.getLayout(holder.layoutId());
            final var board = forestry.core.engine.circuits.ItemCircuitBoard.createCircuitboard(forestry.core.engine.circuits.EnumCircuitBoardType.BASIC, layout, new forestry.api.core.circuits.ICircuit[]{holder.circuit()});
            final var boardData = (Map<?, ?>) li.cil.oc.common.OpenComputersApi.convert(new Object[]{board})[0];
            helper.assertTrue(java.util.Arrays.equals((Object[]) boardData.get("circuits"), new String[]{holder.circuit().getId()}), "Circuit board description lost circuit IDs");
            final var butterflies = manager.getSpeciesTypeSafe(forestry.api.core.genetics.ForestrySpeciesTypes.BUTTERFLY);
            if (Boolean.getBoolean("neoopencomputers.testForestryButterflies")) helper.assertTrue(butterflies != null, "Butterfly provider loaded without its species type");
            if (butterflies != null) {
                final var butterfly = butterflies.getDefaultSpecies().createIndividual();
                final var hiddenButterfly = (Map<?, ?>) li.cil.oc.common.OpenComputersApi.convert(new Object[]{butterfly})[0];
                helper.assertTrue("butterfly".equals(hiddenButterfly.get("type")) && hiddenButterfly.get("size") instanceof Number && !hiddenButterfly.containsKey("active"), "Unanalyzed butterfly visibility/size changed");
                butterfly.analyze();
                final var butterflyData = (Map<?, ?>) li.cil.oc.common.OpenComputersApi.convert(new Object[]{butterfly})[0];
                helper.assertTrue(((Map<?, ?>) butterflyData.get("active")).keySet().containsAll(List.of("species","size","speed","lifespan","metabolism","fertility","temperatureTolerance","humidityTolerance","nocturnal","tolerantFlyer","fireResist","flowerProvider","effect","cocoon")), "Butterfly genome contract missing fields");
                final var butterflyItem = butterfly.createStack(forestry.api.lepidopterology.genetics.ButterflyLifeStage.BUTTERFLY);
                final var butterflyItemData = (Map<?, ?>) li.cil.oc.common.OpenComputersApi.convert(new Object[]{butterflyItem})[0];
                helper.assertTrue("butterfly".equals(((Map<?, ?>) butterflyItemData.get("individual")).get("type")), "Actual butterfly item lost individual data");
            }
            helper.succeed();
        }

        static void nativeAnalyzer(GameTestHelper helper) {
            final var apiaryPos = new BlockPos(1,1,1);
            final var analyzerPos = new BlockPos(3,1,1);
            helper.setBlock(apiaryPos, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("forestry", "apiary")));
            helper.setBlock(analyzerPos, BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("forestry", "analyzer")));
            helper.setBlock(new BlockPos(2,1,1), li.cil.oc.common.ModBlocks.ADAPTER.get());
            helper.setBlock(new BlockPos(2,1,2), li.cil.oc.common.ModBlocks.COMPUTER_CASE_TIER1.get());
            final forestry.core.content.analyzer.TileAnalyzer analyzer = helper.getBlockEntity(analyzerPos);
            final forestry.api.apiculture.IBeeHousing housing = (forestry.api.apiculture.IBeeHousing) helper.getBlockEntity(apiaryPos);
            final var type = forestry.api.IForestryApi.INSTANCE.getGeneticManager().getSpeciesType(forestry.api.core.genetics.ForestrySpeciesTypes.BEE);
            final var drone = type.getDefaultSpecies().createIndividual(); drone.analyze();
            final var queen = type.getDefaultSpecies().createIndividual(); queen.analyze(); queen.setMate(drone.getGenome());
            housing.getBeeInventory().setDrone(drone.createStack(forestry.api.apiculture.genetics.BeeLifeStage.DRONE));
            housing.getBeeInventory().setQueen(queen.createStack(forestry.api.apiculture.genetics.BeeLifeStage.QUEEN));
            final var butterflies = forestry.api.IForestryApi.INSTANCE.getGeneticManager().getSpeciesTypeSafe(forestry.api.core.genetics.ForestrySpeciesTypes.BUTTERFLY);
            final var specimen = Boolean.getBoolean("neoopencomputers.testForestryButterflies")
                ? butterflies.getDefaultSpecies().createIndividual().createStack(forestry.api.lepidopterology.genetics.ButterflyLifeStage.BUTTERFLY)
                : type.getDefaultSpecies().createIndividual().createStack(forestry.api.apiculture.genetics.BeeLifeStage.DRONE);
            analyzer.setItem(forestry.core.platform.inventory.InventoryAnalyzer.SLOT_INPUT_1, specimen);
            final var honey = BuiltInRegistries.FLUID.get(ResourceLocation.fromNamespaceAndPath("forestry", "honey"));
            helper.assertTrue(analyzer.getTankManager().fill(new net.neoforged.neoforge.fluids.FluidStack(honey, 1000), net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE) == 1000, "Analyzer rejected real honey");
            helper.onEachTick(() -> analyzer.getEnergyManager().receiveEnergy(800, false));
            final li.cil.oc.common.blockentity.ComputerCaseBlockEntity computer = helper.getBlockEntity(new BlockPos(2,1,2));
            final var cpu = new net.minecraft.world.item.ItemStack(li.cil.oc.common.ModItems.CPU_TIER1.get());
            ((li.cil.oc.api.driver.item.MutableProcessor) li.cil.oc.api.Driver.driverFor(cpu)).setArchitecture(cpu, li.cil.oc.common.machine.NativeLuaArchitecture.class);
            computer.setItem(4,cpu); computer.setItem(2,new net.minecraft.world.item.ItemStack(li.cil.oc.common.ModItems.MEMORY_TIER1.get()));
            computer.setItem(6,RobotMovementPersistenceGameTests.eeprom("""
                local h,a
                repeat
                  local hid,aid=component.list('bee_housing')(),component.list('forestry_analyzer')()
                  if hid and aid then h,a=component.proxy(hid),component.proxy(aid) else computer.pullSignal(0.05) end
                until h and a
                assert(type(h.canBreed())=='boolean')
                local d,q=h.getDrone(),h.getQueen()
                assert(d.type=='bee' and d.active and q.canSpawn and q.active)
                assert(#h.getBeeBreedingData()>0 and #h.listAllSpecies()>0 and #h.getBeeParents('FORESTRY:ABYSSAL')>0)
                assert(not pcall(h.getBeeParents,true))
                assert(a.isWorking())
                local sample=a.getIndividualOnDisplay()
                assert(sample and not sample.isAnalyzed and sample.active==nil)
                local progress=false
                repeat
                  local p=a.getProgress()
                  assert(type(p)=='number' and p>=0 and p<=1)
                  if p>0 and p<1 then progress=true end
                  computer.pullSignal(0.1)
                until not a.isWorking()
                assert(progress and select('#',a.getIndividualOnDisplay())==1 and a.getIndividualOnDisplay()==nil)
                component.proxy(component.list('eeprom')()).setData('forestry-native-ok')
                while true do computer.pullSignal() end
                """));
            final li.cil.oc.common.blockentity.AdapterBlockEntity adapter = helper.getBlockEntity(new BlockPos(2,1,1));
            computer.onLoad(); adapter.onLoad(); adapter.refreshNeighbors();
            li.cil.oc.api.Network.joinOrCreateNetwork(adapter); li.cil.oc.api.Network.joinOrCreateNetwork(computer);
            ((li.cil.oc.api.network.Connector) computer.machine().node()).changeBuffer(10000);
            helper.assertTrue(computer.machine().start(), "Native Forestry computer failed to start");
            helper.startSequence().thenWaitUntil(() -> {
                final var marker = new String(li.cil.oc.common.item.ItemDriverData.dataTag(computer.getItem(6)).getByteArray(li.cil.oc.common.ItemRegistry.EEPROM_DATA_SECTION_TAG), java.nio.charset.StandardCharsets.UTF_8);
                helper.assertTrue(marker.equals("forestry-native-ok"), "Native Forestry callbacks/analyzer incomplete: " + computer.machine().lastError());
                final var result = analyzer.getItem(forestry.core.platform.inventory.InventoryAnalyzer.SLOT_OUTPUT_1);
                helper.assertTrue(!result.isEmpty() && forestry.api.core.genetics.capability.IIndividualHandlerItem.getIndividual(result).isAnalyzed(), "Powered analyzer did not produce an analyzed specimen");
                helper.assertTrue(analyzer.getTankManager().getFluidInTank(0).getAmount() == 900, "Actual analyzer honey consumption changed");
            }).thenExecute(() -> {
                computer.machine().stop();
                housing.getBeeInventory().setQueen(net.minecraft.world.item.ItemStack.EMPTY);
                housing.getBeeInventory().setDrone(net.minecraft.world.item.ItemStack.EMPTY);
                helper.setBlock(apiaryPos, net.minecraft.world.level.block.Blocks.AIR);
                helper.setBlock(analyzerPos, net.minecraft.world.level.block.Blocks.AIR);
            }).thenSucceed();
        }
    }
}
