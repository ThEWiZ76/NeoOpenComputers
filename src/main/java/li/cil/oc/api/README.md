# NeoOpenComputers API

This package is the public Java API for NeoOpenComputers addons. It keeps the OpenComputers package shape where practical, while using Minecraft 1.21.1 and NeoForge types.

Use it to integrate blocks, items, component callbacks, file systems, manual pages, machine hosts, rack mountables, and network nodes without depending on NeoOpenComputers internals.

## Expose A Block Component

For simple blocks, implement `li.cil.oc.api.network.SimpleComponent` on the block entity and annotate callable methods with `li.cil.oc.api.machine.Callback`.

```java
public final class ExampleBlockEntity extends BlockEntity implements SimpleComponent {
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

If the block needs full network control, implement `Environment` or extend `li.cil.oc.api.prefab.TileEntityEnvironment`, then create a node with `Network.newNode(this, Visibility.Network).create()`.

## Add Item Or Block Drivers

Use `Driver.add(...)` during mod setup to register item drivers, block drivers, converters, environment providers, or inventory providers.

```java
Driver.add(new MyDriverItem());
Driver.add(new MyDriverBlock());
```

For item components such as cards, disks, or upgrades, implement `li.cil.oc.api.driver.DriverItem` or extend `li.cil.oc.api.prefab.DriverItem`.

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
