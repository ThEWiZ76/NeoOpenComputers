# NeoOpenComputers API

This package is the public Java API for NeoOpenComputers addons. It keeps the OpenComputers package shape where practical, while using Minecraft 1.21.1 and NeoForge types.

Use it to integrate blocks, items, component callbacks, file systems, manual pages, machine hosts, rack mountables, and network nodes without depending on NeoOpenComputers internals.

## Expose A Block Component

For simple blocks, implement `li.cil.oc.api.network.SimpleComponent` on the block entity and annotate callable methods with `li.cil.oc.api.machine.Callback`.
If the block needs its own managed node, extend `li.cil.oc.api.prefab.AbstractManagedEnvironment` or `li.cil.oc.api.prefab.TileEntityEnvironment` and create the node with `Network.newNode(...)`.

```java
public final class ExampleEnvironment extends AbstractManagedEnvironment implements SimpleComponent {
    public ExampleEnvironment() {
        setNode(Network.newNode(this, Visibility.Network).create());
    }

    @Override
    public String getComponentName() {
        return "example";
    }

    @Callback
    public Object[] greet(final Context context, final Arguments args) {
        return new Object[]{"Hello, " + args.checkString(0)};
    }
}
```

The API jar compile smoke test builds addon-style source against the generated `apiJar` output to keep this example shape honest.

## Add Item Or Block Drivers

Use `Driver.add(...)` during mod setup to register item drivers, block drivers, converters, environment providers, or inventory providers.
Use enqueued `FMLCommonSetupEvent` work; registration stays open through setup and IMC processing and closes at load completion. Runtime registration is rejected.

```java
Driver.add(new MyDriverItem());
Driver.add(new MyDriverBlock());
```

For item components such as cards, disks, or upgrades, implement `li.cil.oc.api.driver.DriverItem` or extend `li.cil.oc.api.prefab.DriverItem`.
The host loads the environment from the driver's item data before connecting its node and saves it back before copying/removing hardware or saving the host. Override `load`/`save` and call the prefab superclass to retain node identity; a driver does not need to capture the item stack in its environment for persistence.
`DriverItem.dataTag` may return `null` to use the default `oc:data` child. A non-null result must refer to the desired child in that item's custom data; the host preserves this explicit choice.
The host supplies an empty tag to `save` and replaces only the selected child after saving. Write all owned state to that tag; omitted fields are removed. Loading uses a copy of stored state, and sibling item metadata is preserved through modern data-component publication.
Synchronous item `load`/`save` serialization and nonfatal linkage errors are logged and isolated; a failed save retains the last published item data. Fatal VM/thread failures and explicit `UncheckedIOException`/`CompletionException` propagate so a failed durable or asynchronous write cannot be treated as a successful host snapshot. Factory, update, callback and lifecycle methods are outside this persistence boundary.

Managed components may implement `li.cil.oc.api.util.Lifecycle` to receive `Constructing`, `Initializing`, `Initialized`, `Disposing`, and `Disposed`. Loading occurs after `Constructing`; node connection occurs after `Initializing`; regular updates begin after `Initialized`. Disposal belongs to removal of the owned component, rather than a temporary network remap. A managed item without a node still loads, saves and updates. Client-side addon lifetimes remain a separate acceptance gate.

For neighboring blocks, register a `DriverBlock` with `Driver.add`. An adapter keeps its environment while the matched driver set and physical target remain unchanged, retains saved side data for reload, and starts fresh state when an active matched driver set changes. Returned environments may implement `NamedBlock` to choose the compound component's preferred name and priority. A node-less child can still update and persist; naming it alone does not make it network-visible.

`tools/api-smoke-addon` contains a standalone NeoForge fixture compiled against the API JAR without OC implementation output. Build it with `gradlew.bat apiSmokeAddonJar` and include it in developer runs with `-PwithApiSmokeAddon`. It is separate from OC's published artifacts. The opt-in server tests exercise real LuaJ/native callbacks and native userdata/component persistence across normal process restarts; this is not a claim that every addon or old binary is compatible.

## Attach Files

Use `FileSystem.asManagedEnvironment(...)` to expose bundled Lua or data files to computers through the component network.

```java
final var fs = FileSystem.asManagedEnvironment(
    FileSystem.fromClass(getClass(), "assets/myaddon/lua"),
    "myaddon");
node.connect(fs.node());
```

## Compatibility Notes

The API intentionally preserves common OpenComputers names such as `Driver`, `Network`, `Machine`, `SimpleComponent`, `RackMountable`, and `FileSystem`. Some signatures use modern Minecraft and NeoForge classes instead of old Forge 1.12 classes.

Do not implement internal NeoOpenComputers classes directly. Prefer interfaces under `li.cil.oc.api` and prefab base classes under `li.cil.oc.api.prefab`.
