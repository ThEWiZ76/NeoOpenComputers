package li.cil.oc.common;

import li.cil.oc.api.Network;
import li.cil.oc.api.fs.FileSystem;
import li.cil.oc.api.fs.Handle;
import li.cil.oc.api.fs.Label;
import li.cil.oc.api.fs.Mode;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.machine.LimitReachedException;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.api.prefab.AbstractValue;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

final class FileSystemEnvironment extends AbstractManagedEnvironment implements DeviceInfo {
    private static final String FILE_SYSTEM_TAG = "fs";
    private static final String OWNERS_TAG = "owners";
    private static final String ADDRESS_TAG = "address";
    private static final String HANDLES_TAG = "handles";
    private static final double HDD_READ_COST = 0.1D / 1024.0D;
    private static final double HDD_WRITE_COST = 0.25D / 1024.0D;
    private static final double[] READ_COSTS = {1.0D / 1.0D, 1.0D / 4.0D, 1.0D / 7.0D, 1.0D / 10.0D, 1.0D / 13.0D, 1.0D / 15.0D};
    private static final double[] SEEK_COSTS = {1.0D / 1.0D, 1.0D / 4.0D, 1.0D / 7.0D, 1.0D / 10.0D, 1.0D / 13.0D, 1.0D / 15.0D};
    private static final double[] WRITE_COSTS = {1.0D / 1.0D, 1.0D / 2.0D, 1.0D / 3.0D, 1.0D / 4.0D, 1.0D / 5.0D, 1.0D / 6.0D};

    private final FileSystem fileSystem;
    private final Label label;
    private final Optional<EnvironmentHost> host;
    private final Optional<String> accessSound;
    private final int speed;
    private final int costIndex;
    private final Map<String, Set<Integer>> owners = new LinkedHashMap<>();
    private boolean saving;

    FileSystemEnvironment(final FileSystem fileSystem, final Label label, final EnvironmentHost host, final String accessSound, final int speed) {
        this.fileSystem = fileSystem;
        this.label = label;
        this.host = Optional.ofNullable(host);
        this.accessSound = Optional.ofNullable(accessSound);
        this.speed = Math.max(1, Math.min(speed, 6));
        this.costIndex = Math.max(0, Math.min(this.speed, READ_COSTS.length - 1));
        setNode(Network.newNode(this, Visibility.Network)
            .withComponent("filesystem", Visibility.Neighbors)
            .withConnector()
            .create());
    }

    FileSystem fileSystem() {
        return fileSystem;
    }

    Label label() {
        return label;
    }

    Optional<EnvironmentHost> host() {
        return host;
    }

    Optional<String> accessSound() {
        return accessSound;
    }

    int speed() {
        return speed;
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        final long spaceTotal = fileSystem.spaceTotal();
        return Map.of(
            DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Volume,
            DeviceInfo.DeviceAttribute.Description, "Filesystem",
            DeviceInfo.DeviceAttribute.Vendor, "MightyPirates",
            DeviceInfo.DeviceAttribute.Product, "MPFS.21.6",
            DeviceInfo.DeviceAttribute.Capacity, Long.toString((long) (spaceTotal * 1.024D)),
            DeviceInfo.DeviceAttribute.Size, Long.toString(spaceTotal),
            DeviceInfo.DeviceAttribute.Clock, clock(costIndex)
        );
    }

    @Callback(direct = true, doc = "function():string -- Get the current label of the drive.")
    public Object[] getLabel(final Context context, final Arguments arguments) {
        return new Object[]{label == null ? null : label.getLabel()};
    }

    @Callback(doc = "function(value:string):string -- Sets the label of the drive. Returns the new value, which may be truncated.")
    public Object[] setLabel(final Context context, final Arguments arguments) throws Exception {
        if (label == null) {
            throw new Exception("drive does not support labeling");
        }
        final Object value = arguments.checkAny(0);
        label.setLabel(value == null ? null : arguments.checkString(0));
        return new Object[]{label.getLabel()};
    }

    @Callback(direct = true, doc = "function():boolean -- Returns whether the file system is read-only.")
    public Object[] isReadOnly(final Context context, final Arguments arguments) {
        return new Object[]{fileSystem.isReadOnly()};
    }

    @Callback(direct = true, doc = "function():number -- The overall capacity of the file system, in bytes.")
    public Object[] spaceTotal(final Context context, final Arguments arguments) {
        final long space = fileSystem.spaceTotal();
        if (space < 0) {
            return new Object[]{Double.POSITIVE_INFINITY};
        }
        return new Object[]{space};
    }

    @Callback(direct = true, doc = "function():number -- The currently used capacity of the file system, in bytes.")
    public Object[] spaceUsed(final Context context, final Arguments arguments) {
        return new Object[]{fileSystem.spaceUsed()};
    }

    @Callback(direct = true, doc = "function(path:string):boolean -- Returns whether an object exists at the specified absolute path.")
    public Object[] exists(final Context context, final Arguments arguments) {
        return new Object[]{fileSystem.exists(clean(arguments.checkString(0)))};
    }

    @Callback(direct = true, doc = "function(path:string):number -- Returns the size of the object at the specified path.")
    public Object[] size(final Context context, final Arguments arguments) {
        return new Object[]{fileSystem.size(clean(arguments.checkString(0)))};
    }

    @Callback(direct = true, doc = "function(path:string):boolean -- Returns whether the object at the specified path is a directory.")
    public Object[] isDirectory(final Context context, final Arguments arguments) {
        return new Object[]{fileSystem.isDirectory(clean(arguments.checkString(0)))};
    }

    @Callback(direct = true, doc = "function(path:string):number -- Returns the timestamp of when the object at the path was modified.")
    public Object[] lastModified(final Context context, final Arguments arguments) {
        return new Object[]{fileSystem.lastModified(clean(arguments.checkString(0)))};
    }

    @Callback(doc = "function(path:string):table -- Returns names of objects in the directory at the specified path.")
    public Object[] list(final Context context, final Arguments arguments) {
        final String[] contents = fileSystem.list(clean(arguments.checkString(0)));
        return contents == null ? null : new Object[]{contents};
    }

    @Callback(doc = "function(path:string):boolean -- Creates a directory at the specified path, including parent directories.")
    public Object[] makeDirectory(final Context context, final Arguments arguments) {
        return new Object[]{makeDirectory(clean(arguments.checkString(0)))};
    }

    @Callback(doc = "function(path:string):boolean -- Removes the object at the specified path.")
    public Object[] remove(final Context context, final Arguments arguments) {
        return new Object[]{remove(clean(arguments.checkString(0)))};
    }

    @Callback(doc = "function(from:string,to:string):boolean -- Renames or moves an object.")
    public Object[] rename(final Context context, final Arguments arguments) throws java.io.FileNotFoundException {
        return new Object[]{fileSystem.rename(clean(arguments.checkString(0)), clean(arguments.checkString(1)))};
    }

    @Callback(direct = true, limit = 4, doc = "function(path:string[,mode:string='r']):userdata -- Opens a file handle.")
    public Object[] open(final Context context, final Arguments arguments) throws IOException {
        checkHandleLimit(context);
        final int handle = fileSystem.open(clean(arguments.checkString(0)), parseMode(arguments.optString(1, "r")));
        rememberOwner(context, handle);
        return new Object[]{new FileHandleValue(this, handle)};
    }

    @Callback(direct = true, doc = "function(handle:userdata) -- Closes an open file handle.")
    public Object[] close(final Context context, final Arguments arguments) throws IOException {
        close(context, checkHandle(arguments, 0));
        return null;
    }

    @Callback(direct = true, limit = 15, doc = "function(handle:userdata,count:number):string -- Reads up to count bytes from a file handle.")
    public Object[] read(final Context context, final Arguments arguments) throws IOException, LimitReachedException {
        consumeCallBudget(context, READ_COSTS[costIndex]);
        final int handleId = checkHandle(arguments, 0);
        checkOwner(context, handleId);
        final Handle handle = getHandle(handleId);
        final byte[] buffer = new byte[Math.min(ModSettings.maxReadBuffer(), Math.max(0, arguments.checkInteger(1)))];
        final int read = handle.read(buffer);
        if (read < 0) {
            return new Object[]{null};
        }
        consumeEnergy(context, HDD_READ_COST * read);
        if (read == buffer.length) {
            return new Object[]{buffer};
        }
        final byte[] bytes = new byte[read];
        System.arraycopy(buffer, 0, bytes, 0, read);
        return new Object[]{bytes};
    }

    @Callback(direct = true, doc = "function(handle:userdata,whence:string,offset:number):number -- Seeks in a file handle.")
    public Object[] seek(final Context context, final Arguments arguments) throws IOException, LimitReachedException {
        consumeCallBudget(context, SEEK_COSTS[costIndex]);
        final int handleId = checkHandle(arguments, 0);
        checkOwner(context, handleId);
        final Handle handle = getHandle(handleId);
        final String whence = arguments.checkString(1);
        final long offset = arguments.checkLong(2);
        final long position = switch (whence) {
            case "cur" -> handle.seek(handle.position() + offset);
            case "set" -> handle.seek(offset);
            case "end" -> handle.seek(handle.length() + offset);
            default -> throw new IllegalArgumentException("invalid mode");
        };
        return new Object[]{position};
    }

    @Callback(direct = true, doc = "function(handle:userdata,value:string):boolean -- Writes bytes to a file handle.")
    public Object[] write(final Context context, final Arguments arguments) throws IOException, LimitReachedException {
        consumeCallBudget(context, WRITE_COSTS[costIndex]);
        final int handleId = checkHandle(arguments, 0);
        checkOwner(context, handleId);
        final byte[] value = arguments.checkByteArray(1);
        consumeEnergy(context, HDD_WRITE_COST * value.length);
        getHandle(handleId).write(value);
        return new Object[]{true};
    }

    @Override
    public void onDisconnect(final Node node) {
        if (saving && node == node()) {
            return;
        }
        if (node == node()) {
            owners.clear();
            fileSystem.close();
        } else {
            closeOwner(node);
        }
    }

    @Override
    public void onMessage(final Message message) {
        if ("computer.stopped".equals(message.name()) || "computer.started".equals(message.name())) {
            if (message.source() == null) {
                owners.clear();
                fileSystem.close();
            } else {
                closeOwner(message.source());
            }
        }
    }

    @Override
    public void load(final CompoundTag nbt) {
        super.load(nbt);
        owners.clear();
        final ListTag ownersTag = nbt.getList(OWNERS_TAG, Tag.TAG_COMPOUND);
        for (int index = 0; index < ownersTag.size(); index++) {
            final CompoundTag ownerTag = ownersTag.getCompound(index);
            final String address = ownerTag.getString(ADDRESS_TAG);
            if (!address.isEmpty()) {
                final Set<Integer> handles = new LinkedHashSet<>();
                for (int handle : ownerTag.getIntArray(HANDLES_TAG)) {
                    handles.add(handle);
                }
                if (!handles.isEmpty()) {
                    owners.put(address, handles);
                }
            }
        }
        if (label != null) {
            label.load(nbt);
        }
        if (nbt.contains(FILE_SYSTEM_TAG)) {
            fileSystem.load(nbt.getCompound(FILE_SYSTEM_TAG));
        }
    }

    @Override
    public void save(final CompoundTag nbt) {
        final Map<String, Set<Integer>> savedOwners = snapshotOwners();
        if (label != null) {
            label.save(nbt);
        }
        CompoundTag fileSystemTag = new CompoundTag();
        fileSystem.save(fileSystemTag);
        nbt.put(FILE_SYSTEM_TAG, fileSystemTag);
        saving = true;
        try {
            super.save(nbt);
        } finally {
            saving = false;
        }
        final ListTag ownersTag = new ListTag();
        for (Map.Entry<String, Set<Integer>> owner : savedOwners.entrySet()) {
            if (!owner.getValue().isEmpty()) {
                final CompoundTag ownerTag = new CompoundTag();
                ownerTag.putString(ADDRESS_TAG, owner.getKey());
                ownerTag.put(HANDLES_TAG, new IntArrayTag(owner.getValue().stream().mapToInt(Integer::intValue).toArray()));
                ownersTag.add(ownerTag);
            }
        }
        nbt.put(OWNERS_TAG, ownersTag);
    }

    private Map<String, Set<Integer>> snapshotOwners() {
        final Map<String, Set<Integer>> snapshot = new LinkedHashMap<>();
        for (Map.Entry<String, Set<Integer>> owner : owners.entrySet()) {
            snapshot.put(owner.getKey(), new LinkedHashSet<>(owner.getValue()));
        }
        return snapshot;
    }

    private String clean(final String path) {
        if (path == null || path.isEmpty() || "/".equals(path) || ".".equals(path)) {
            return "";
        }
        final String[] rawSegments = path.replace('\\', '/').split("/");
        final java.util.ArrayDeque<String> segments = new java.util.ArrayDeque<>();
        for (String segment : rawSegments) {
            if (segment.isEmpty() || ".".equals(segment)) {
                continue;
            }
            if ("..".equals(segment)) {
                if (segments.isEmpty()) {
                    throw new IllegalArgumentException("path escapes file system root");
                }
                segments.removeLast();
            } else {
                segments.addLast(segment);
            }
        }
        return String.join("/", segments);
    }

    private boolean makeDirectory(final String path) {
        if (path.isEmpty() || fileSystem.exists(path)) {
            return false;
        }
        final int separator = path.lastIndexOf('/');
        if (separator > 0) {
            final String parent = path.substring(0, separator);
            if (!fileSystem.exists(parent) && !makeDirectory(parent)) {
                return false;
            }
        }
        return fileSystem.makeDirectory(path);
    }

    private boolean remove(final String path) {
        if (fileSystem.isDirectory(path)) {
            final String[] children = fileSystem.list(path);
            if (children != null) {
                for (String child : children) {
                    final String childPath = path.isEmpty() ? child : path + "/" + child;
                    if (!remove(clean(childPath))) {
                        return false;
                    }
                }
            }
        }
        return fileSystem.delete(path);
    }

    private Mode parseMode(final String mode) {
        return switch (mode) {
            case "r", "rb" -> Mode.Read;
            case "w", "wb" -> Mode.Write;
            case "a", "ab" -> Mode.Append;
            default -> throw new IllegalArgumentException("unsupported mode");
        };
    }

    private int checkHandle(final Arguments arguments, final int index) throws IOException {
        final Object value = arguments.checkAny(index);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof Map<?, ?> table && table.get("handle") instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof FileHandleValue handleValue && handleValue.owner == this) {
            return handleValue.handle;
        }
        throw new IOException("bad file descriptor");
    }

    private Handle getHandle(final int handle) throws IOException {
        final Handle file = fileSystem.getHandle(handle);
        if (file == null) {
            throw new IOException("bad file descriptor");
        }
        return file;
    }

    private void close(final Context context, final int handle) throws IOException {
        checkOwner(context, handle);
        getHandle(handle).close();
        forgetOwner(context, handle);
    }

    private void rememberOwner(final Context context, final int handle) {
        final String address = ownerAddress(context);
        if (address != null) {
            owners.computeIfAbsent(address, ignored -> new LinkedHashSet<>()).add(handle);
        }
    }

    private void checkHandleLimit(final Context context) throws IOException {
        final String address = ownerAddress(context);
        if (address == null) {
            return;
        }
        final Set<Integer> handles = owners.get(address);
        if (handles != null && handles.size() >= ModSettings.maxHandles()) {
            throw new IOException("too many open handles");
        }
    }

    private void forgetOwner(final Context context, final int handle) {
        final String address = ownerAddress(context);
        if (address == null) {
            return;
        }
        final Set<Integer> handles = owners.get(address);
        if (handles != null) {
            handles.remove(handle);
            if (handles.isEmpty()) {
                owners.remove(address);
            }
        }
    }

    private void checkOwner(final Context context, final int handle) throws IOException {
        final String address = ownerAddress(context);
        if (address == null) {
            return;
        }
        final Set<Integer> handles = owners.get(address);
        if (handles == null || !handles.contains(handle)) {
            throw new IOException("bad file descriptor");
        }
    }

    private void closeOwner(final Node node) {
        if (node == null || node.address() == null) {
            return;
        }
        final Set<Integer> handles = owners.remove(node.address());
        if (handles == null) {
            return;
        }
        for (int handle : handles) {
            final Handle file = fileSystem.getHandle(handle);
            if (file != null) {
                file.close();
            }
        }
    }

    private static String ownerAddress(final Context context) {
        if (context == null || context.node() == null) {
            return null;
        }
        return context.node().address();
    }

    private static void consumeCallBudget(final Context context, final double cost) throws LimitReachedException {
        if (context != null) {
            context.consumeCallBudget(cost);
        }
    }

    private static void consumeEnergy(final Context context, final double cost) throws IOException {
        if (context != null && context.node() instanceof Connector connector && !connector.tryChangeBuffer(-cost)) {
            throw new IOException("not enough energy");
        }
    }

    private static String clock(final int costIndex) {
        return clock(READ_COSTS[costIndex]) + "/" + clock(SEEK_COSTS[costIndex]) + "/" + clock(WRITE_COSTS[costIndex]);
    }

    private static int clock(final double cost) {
        return (int) (2000.0D / cost) / 100;
    }

    private static final class FileHandleValue extends AbstractValue {
        private final FileSystemEnvironment owner;
        private final int handle;

        private FileHandleValue(final FileSystemEnvironment owner, final int handle) {
            this.owner = owner;
            this.handle = handle;
        }

        @Override
        public void dispose(final Context context) {
            try {
                owner.close(context, handle);
            } catch (IOException ignored) {
                // Already closed.
            }
        }

        @Override
        public String toString() {
            return Integer.toString(handle);
        }
    }
}
