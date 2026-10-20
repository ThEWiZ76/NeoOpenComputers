package li.cil.oc.common.gametest;

import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.item.MutableProcessor;
import li.cil.oc.api.network.Connector;
import li.cil.oc.common.ItemRegistry;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.blockentity.ComputerCaseBlockEntity;
import li.cil.oc.common.blockentity.RackBlockEntity;
import li.cil.oc.common.blockentity.AdapterBlockEntity;
import li.cil.oc.common.blockentity.RobotBlockEntity;
import li.cil.oc.common.item.ItemDriverData;
import li.cil.oc.common.machine.NativeLua53Architecture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@GameTestHolder("neoopencomputers")
public final class AddonLifecycleGameTests {
    private static final List<String> INITIALIZED = List.of("Constructing", "load", "Initializing", "Initialized");
    private static final List<String> DISPOSED = List.of("Constructing", "load", "Initializing", "Initialized", "Disposing", "Disposed");

    @GameTestGenerator
    public static Collection<TestFunction> tests() {
        if (!Boolean.getBoolean("neoopencomputers.testApiSmokeAddon")) return List.of();
        return List.of(new TestFunction("addonLifecycle", "addon_lifecycle_case_reload", "neoopencomputers:empty", 400, 0, true, AddonLifecycleGameTests::caseReload),
            new TestFunction("addonLifecycle", "addon_lifecycle_nodeless_update_and_save", "neoopencomputers:empty", 400, 0, true, AddonLifecycleGameTests::noNode),
            new TestFunction("addonLifecycle", "addon_lifecycle_rack_bus_and_removal", "neoopencomputers:empty", 400, 0, true, AddonLifecycleGameTests::rack),
            new TestFunction("addonLifecycle", "addon_lifecycle_adapter_nodeless", "neoopencomputers:empty", 400, 0, true, AddonLifecycleGameTests::adapter),
            new TestFunction("addonLifecycle", "addon_lifecycle_robot_runtime_unload", "neoopencomputers:empty", 400, 0, true, AddonLifecycleGameTests::robotRuntime));
    }

    static ItemStack card(String id, boolean noNode) {
        final var stack = new ItemStack(Items.PAPER); final var tag = new CompoundTag();
        tag.putBoolean("oc_api_smoke", true); tag.putString("oc_api_smoke_lifecycle_id", id); tag.putBoolean("oc_api_smoke_no_node", noNode);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag)); return stack;
    }

    private static void hardware(Container items, int cpuSlot, int ramSlot, int eepromSlot, String id) {
        final var cpu = new ItemStack(ModItems.CPU_TIER1.get());
        ((MutableProcessor) Driver.driverFor(cpu)).setArchitecture(cpu, NativeLua53Architecture.class);
        items.setItem(cpuSlot, cpu); items.setItem(ramSlot, new ItemStack(ModItems.MEMORY_TIER1.get()));
        items.setItem(eepromSlot, RobotMovementPersistenceGameTests.eeprom("""
            local c=component.proxy(assert(component.list('api_lifecycle')()))
            assert(c.getPhase()=='Initialized', 'addon lifecycle not initialized')
            assert(c.setValue(731)==731)
            local eeprom=component.proxy(component.list('eeprom')()); eeprom.setData('lifecycle-ready')
            repeat until computer.pullSignal()=='lifecycle-resume'
            assert(c.getPhase()=='Initialized' and c.getValue()==731, 'lifecycle/state lost')
            eeprom.setData('lifecycle-verified')
            while true do computer.pullSignal() end
            """));
    }

    private static ComputerCaseBlockEntity computer(GameTestHelper helper, String id, boolean noNode) {
        final var pos=helper.absolutePos(new BlockPos(1,1,1));
        helper.getLevel().setBlockAndUpdate(pos, ModBlocks.COMPUTER_CASE_TIER1.get().defaultBlockState());
        final var computer=(ComputerCaseBlockEntity) helper.getLevel().getBlockEntity(pos);
        hardware(computer,4,2,6,id); computer.setItem(0,card(id,noNode));
        initialized(helper,id); return computer;
    }

    private static void caseReload(GameTestHelper helper) {
        final String id="case-lifecycle"; final ComputerCaseBlockEntity[] active={computer(helper,id,false)};
        ((Connector) active[0].machine().node()).changeBuffer(10000); helper.assertTrue(active[0].toggleMachine(),"Lifecycle case did not start");
        helper.startSequence().thenWaitUntil(()->helper.assertTrue(marker(active[0],6).equals("lifecycle-ready"),"Lifecycle program failed: "+active[0].machine().lastError()))
            .thenExecute(()->{
                final var original=active[0]; original.onChunkUnloaded(); disposed(helper,id);
                final var saved=original.saveWithFullMetadata(helper.getLevel().registryAccess()); original.setRemoved(); disposed(helper,id);
                active[0]=(ComputerCaseBlockEntity) BlockEntity.loadStatic(original.getBlockPos(),original.getBlockState(),saved,helper.getLevel().registryAccess());
                helper.getLevel().setBlockEntity(active[0]); active[0].onLoad(); initialized(helper,id);
                helper.assertTrue(active[0].machine().signal("lifecycle-resume"),"Lifecycle resume rejected");
            }).thenWaitUntil(()->helper.assertTrue(marker(active[0],6).equals("lifecycle-verified"),"Lifecycle continuation failed: "+active[0].machine().lastError()))
            .thenExecute(()->{active[0].setItem(0,ItemStack.EMPTY); disposed(helper,id);}).thenSucceed();
    }

    private static void noNode(GameTestHelper helper) {
        final String id="nodeless-lifecycle"; final var computer=computer(helper,id,true);
        invoke("setTrackedValue",new Class<?>[]{String.class,int.class},id,731);
        helper.startSequence().thenIdle(3).thenExecute(()->{
            helper.assertTrue(Boolean.TRUE.equals(observe(id).get("updated")),"Node-less component was not updated while stopped");
            final var removed=computer.removeItem(0,1); disposed(helper,id);
            helper.assertTrue(ItemDriverData.dataTag(removed).getInt("trackedValue")==731,"Node-less component state was not saved");
            computer.setItem(0,removed.copy()); initialized(helper,id);
            helper.assertTrue(((Number)observe(id).get("value")).intValue()==731,"Node-less component state was not reloaded");
        }).thenIdle(3).thenExecute(()->{
            helper.assertTrue(Boolean.TRUE.equals(observe(id).get("updated")),"Reloaded node-less component was not updated");
            computer.onChunkUnloaded(); disposed(helper,id); computer.setRemoved(); disposed(helper,id);
        }).thenSucceed();
    }

    private static void rack(GameTestHelper helper) {
        final String id="rack-lifecycle"; final var pos=helper.absolutePos(new BlockPos(1,1,1));
        helper.getLevel().setBlockAndUpdate(pos,ModBlocks.RACK.get().defaultBlockState());
        final var rack=(RackBlockEntity)helper.getLevel().getBlockEntity(pos); rack.setItem(0,new ItemStack(ModItems.SERVER_TIER1.get()));
        final var server=(li.cil.oc.api.internal.Server)rack.getMountable(0); final var items=(Container)server;
        hardware(items,2,4,8,id); items.setItem(0,card(id,false)); initialized(helper,id);
        ((Connector)server.machine().node()).changeBuffer(10000); helper.assertTrue(server.machine().start(),"Lifecycle rack did not start");
        helper.startSequence().thenWaitUntil(()->helper.assertTrue(marker(items,8).equals("lifecycle-ready"),"Rack lifecycle program failed: "+server.machine().lastError()))
            .thenExecute(()->{
                rack.connect(0,-1,Direction.EAST); rack.connect(0,-1,Direction.WEST); rack.connect(0,-1,null);
                initialized(helper,id); helper.assertTrue(server.machine().signal("lifecycle-resume"),"Rack bus remap stopped the VM");
            }).thenWaitUntil(()->helper.assertTrue(marker(items,8).equals("lifecycle-verified"),"Rack lifecycle continuation failed: "+server.machine().lastError()))
            .thenExecute(()->{rack.setItem(0,ItemStack.EMPTY); disposed(helper,id);}).thenSucceed();
    }

    private static void adapter(GameTestHelper helper) {
        final String id = "adapter-lifecycle";
        final var pos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlockAndUpdate(pos, ModBlocks.ADAPTER.get().defaultBlockState());
        final var adapter = (AdapterBlockEntity) helper.getLevel().getBlockEntity(pos);
        adapter.onLoad();
        final var stack = card(id, true);
        final var tag = stack.get(DataComponents.CUSTOM_DATA).copyTag();
        tag.putBoolean("oc_api_smoke_upgrade", true);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        ItemDriverData.dataTag(stack).putInt("trackedValue", 123);
        helper.assertTrue(adapter.canPlaceItem(0, stack), "Addon upgrade rejected by adapter slot");
        adapter.setItem(0, stack); initialized(helper, id);
        helper.assertTrue(((Number) observe(id).get("value")).intValue() == 123, "Adapter did not load item data");
        invoke("setTrackedValue", new Class<?>[]{String.class, int.class}, id, 731);
        helper.startSequence().thenIdle(3).thenExecute(() -> {
            helper.assertTrue(Boolean.TRUE.equals(observe(id).get("updated")), "Node-less adapter upgrade did not update");
            final var removed = adapter.removeItem(0, 1); disposed(helper, id);
            helper.assertTrue(ItemDriverData.dataTag(removed).getInt("trackedValue") == 731, "Adapter lost removed addon data");
            adapter.setItem(0, removed); initialized(helper, id);
            helper.assertTrue(((Number) observe(id).get("value")).intValue() == 731, "Adapter did not restore addon data");
            adapter.onChunkUnloaded(); disposed(helper, id); adapter.setRemoved(); disposed(helper, id);
        }).thenSucceed();
    }

    private static void robotRuntime(GameTestHelper helper) {
        final String id = "robot-runtime-lifecycle";
        helper.setBlock(new BlockPos(1, 1, 1), ModBlocks.ROBOT.get());
        final RobotBlockEntity original = helper.getBlockEntity(new BlockPos(1, 1, 1));
        original.setTier(2);
        RobotMovementPersistenceGameTests.installHardware(helper, original, List.of(new ItemStack(ModItems.UPGRADE_CONTAINER_TIER2.get())));
        original.onLoad();
        final var stack = card(id, true);
        final var tag = stack.get(DataComponents.CUSTOM_DATA).copyTag();
        tag.putBoolean("oc_api_smoke_upgrade", true);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        helper.assertTrue(original.canPlaceItem(1, stack), "Robot rejected addon runtime upgrade");
        original.setItem(1, stack); initialized(helper, id);
        invoke("setTrackedValue", new Class<?>[]{String.class, int.class}, id, 731);
        final RobotBlockEntity[] active = {original};
        helper.startSequence().thenIdle(3).thenExecute(() -> {
            helper.assertTrue(Boolean.TRUE.equals(observe(id).get("updated")), "Robot runtime addon did not update");
            original.onChunkUnloaded(); disposed(helper, id);
            final var saved = original.saveWithFullMetadata(helper.getLevel().registryAccess());
            original.setRemoved(); disposed(helper, id);
            active[0] = (RobotBlockEntity) BlockEntity.loadStatic(original.getBlockPos(), original.getBlockState(), saved, helper.getLevel().registryAccess());
            helper.getLevel().setBlockEntity(active[0]); active[0].onLoad(); initialized(helper, id);
            helper.assertTrue(((Number) observe(id).get("value")).intValue() == 731, "Robot runtime addon lost saved value");
        }).thenIdle(3).thenExecute(() -> {
            helper.assertTrue(Boolean.TRUE.equals(observe(id).get("updated")), "Reloaded robot runtime addon did not update");
            final var removed = active[0].removeItemNoUpdate(1); disposed(helper, id);
            helper.assertTrue(ItemDriverData.dataTag(removed).getInt("trackedValue") == 731, "Removed robot addon lost value");
        }).thenSucceed();
    }

    @SuppressWarnings("unchecked") private static Map<String,Object> observe(String id) { return (Map<String,Object>)invoke("observe",new Class<?>[]{String.class},id); }
    private static Object invoke(String name,Class<?>[] types,Object... args) {
        try {return Class.forName("example.ocapi.ApiSmokeAddon").getMethod(name,types).invoke(null,args);}
        catch(ReflectiveOperationException e){throw new IllegalStateException("Standalone lifecycle fixture is unavailable",e);}
    }
    private static void initialized(GameTestHelper helper,String id) {
        final var data=observe(id);
        helper.assertTrue(INITIALIZED.equals(data.get("trace")) && Boolean.TRUE.equals(data.get("loaded")),"Missing/incorrect lifecycle initialization: "+data);
        helper.assertTrue(!Boolean.TRUE.equals(data.get("updatedOutsideLifetime")),"Component updated outside its lifetime");
    }
    @SuppressWarnings("unchecked") private static void disposed(GameTestHelper helper,String id) {
        final var history=(List<Map<String,Object>>)invoke("history",new Class<?>[]{String.class},id);
        helper.assertTrue(!history.isEmpty(),"Lifecycle fixture never existed");
        for(final var data:history) helper.assertTrue(DISPOSED.equals(data.get("trace")) && !Boolean.TRUE.equals(data.get("updatedOutsideLifetime")),"Missing/duplicate lifecycle disposal: "+data);
    }
    private static String marker(Container items,int slot) {return new String(ItemDriverData.dataTag(items.getItem(slot)).getByteArray(ItemRegistry.EEPROM_DATA_SECTION_TAG),StandardCharsets.UTF_8);}
}
