package li.cil.oc.common;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.event.FileSystemAccessEvent;
import li.cil.oc.api.fs.Label;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.common.NeoForge;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

public final class DriveEnvironment extends AbstractManagedEnvironment implements DeviceInfo {
    private static final String DATA_TAG = "data";
    private static final String HEAD_POS_TAG = "headPos";
    private static final String UNMANAGED_TAG = "oc:unmanaged";
    private static final String LOCK_TAG = "oc:lock";
    private static final int SECTOR_SIZE = 512;
    private static final int SECTOR_SEEK_THRESHOLD = 8;
    private static final double SECTOR_SEEK_TIME = 0.1D;
    private static final double[] READ_SECTOR_COSTS = {1.0D / 10.0D, 1.0D / 20.0D, 1.0D / 30.0D, 1.0D / 40.0D, 1.0D / 50.0D, 1.0D / 60.0D};
    private static final double[] WRITE_SECTOR_COSTS = {1.0D / 5.0D, 1.0D / 10.0D, 1.0D / 15.0D, 1.0D / 20.0D, 1.0D / 25.0D, 1.0D / 30.0D};
    private static final double[] READ_BYTE_COSTS = {1.0D / 48.0D, 1.0D / 64.0D, 1.0D / 80.0D, 1.0D / 96.0D, 1.0D / 112.0D, 1.0D / 128.0D};
    private static final double[] WRITE_BYTE_COSTS = {1.0D / 24.0D, 1.0D / 32.0D, 1.0D / 40.0D, 1.0D / 48.0D, 1.0D / 56.0D, 1.0D / 64.0D};

    private final byte[] data;
    private final int platterCount;
    private final Label label;
    private final Optional<EnvironmentHost> host;
    private final Optional<String> accessSound;
    private final int costIndex;
    private final String lockInfo;
    private int headPos;

    public DriveEnvironment(final int capacity, final int platterCount, final Label label, final EnvironmentHost host, final String accessSound, final int speed, final String lockInfo) {
        this.data = new byte[Math.max(0, capacity)];
        this.platterCount = Math.max(1, platterCount);
        this.label = label;
        this.host = Optional.ofNullable(host);
        this.accessSound = Optional.ofNullable(accessSound);
        this.costIndex = Math.max(0, Math.min(speed, READ_SECTOR_COSTS.length - 1));
        this.lockInfo = lockInfo == null ? "" : lockInfo;
        setNode(Network.newNode(this, Visibility.Network)
            .withComponent("drive", Visibility.Neighbors)
            .withConnector()
            .create());
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        final int capacity = data.length;
        return Map.of(
            DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Disk,
            DeviceInfo.DeviceAttribute.Description, "Hard disk drive",
            DeviceInfo.DeviceAttribute.Vendor, "MightyPirates GmbH & Co. KG",
            DeviceInfo.DeviceAttribute.Product, "MPD" + (capacity / 1024) + "L" + platterCount,
            DeviceInfo.DeviceAttribute.Capacity, Long.toString((long) (capacity * 1.024D)),
            DeviceInfo.DeviceAttribute.Size, Integer.toString(capacity),
            DeviceInfo.DeviceAttribute.Clock, clock()
        );
    }

    @Callback(direct = true, doc = "function():string -- Get the current label of the drive.")
    public Object[] getLabel(final Context context, final Arguments arguments) {
        return label == null ? null : new Object[]{label.getLabel()};
    }

    @Callback(doc = "function(value:string):string -- Sets the label of the drive. Returns the new value, which may be truncated.")
    public Object[] setLabel(final Context context, final Arguments arguments) throws Exception {
        if (locked()) {
            throw new Exception("drive is read only");
        }
        if (label == null) {
            throw new Exception("drive does not support labeling");
        }
        final Object value = arguments.checkAny(0);
        label.setLabel(value == null ? null : arguments.checkString(0));
        return new Object[]{label.getLabel()};
    }

    @Callback(direct = true, doc = "function():number -- Returns the total capacity of the drive, in bytes.")
    public Object[] getCapacity(final Context context, final Arguments arguments) {
        return new Object[]{data.length};
    }

    @Callback(direct = true, doc = "function():number -- Returns the size of a single sector on the drive, in bytes.")
    public Object[] getSectorSize(final Context context, final Arguments arguments) {
        return new Object[]{SECTOR_SIZE};
    }

    @Callback(direct = true, doc = "function():number -- Returns the number of platters in the drive.")
    public Object[] getPlatterCount(final Context context, final Arguments arguments) {
        return new Object[]{platterCount};
    }

    @Callback(direct = true, doc = "function(sector:number):string -- Read the current contents of the specified sector.")
    public Object[] readSector(final Context context, final Arguments arguments) {
        consumeCallBudget(context, READ_SECTOR_COSTS[costIndex]);
        final int sector = moveToSector(context, checkSector(arguments.checkInteger(0) - 1));
        diskActivity();
        return new Object[]{Arrays.copyOfRange(data, sectorOffset(sector), sectorOffset(sector) + SECTOR_SIZE)};
    }

    @Callback(direct = true, doc = "function(sector:number,value:string) -- Write the specified contents to the specified sector.")
    public Object[] writeSector(final Context context, final Arguments arguments) throws Exception {
        if (locked()) {
            throw new Exception("drive is read only");
        }
        consumeCallBudget(context, WRITE_SECTOR_COSTS[costIndex]);
        final int sector = moveToSector(context, checkSector(arguments.checkInteger(0) - 1));
        final byte[] sectorData = arguments.checkByteArray(1);
        diskActivity();
        System.arraycopy(sectorData, 0, data, sectorOffset(sector), Math.min(SECTOR_SIZE, sectorData.length));
        return null;
    }

    @Callback(direct = true, doc = "function(offset:number):number -- Read a single byte at the specified offset.")
    public Object[] readByte(final Context context, final Arguments arguments) {
        consumeCallBudget(context, READ_BYTE_COSTS[costIndex]);
        final int offset = arguments.checkInteger(0) - 1;
        moveToSector(context, checkSector(offsetSector(offset)));
        diskActivity();
        return new Object[]{(int) data[offset]};
    }

    @Callback(direct = true, doc = "function(offset:number,value:number) -- Write a single byte to the specified offset.")
    public Object[] writeByte(final Context context, final Arguments arguments) throws Exception {
        if (locked()) {
            throw new Exception("drive is read only");
        }
        consumeCallBudget(context, WRITE_BYTE_COSTS[costIndex]);
        final int offset = arguments.checkInteger(0) - 1;
        final byte value = (byte) arguments.checkInteger(1);
        moveToSector(context, checkSector(offsetSector(offset)));
        diskActivity();
        data[offset] = value;
        return null;
    }

    @Override
    public void load(final CompoundTag nbt) {
        super.load(nbt);
        if (nbt.contains(DATA_TAG, Tag.TAG_BYTE_ARRAY)) {
            final byte[] saved = nbt.getByteArray(DATA_TAG);
            System.arraycopy(saved, 0, data, 0, Math.min(saved.length, data.length));
        }
        headPos = Math.max(0, Math.min(nbt.getInt(HEAD_POS_TAG), sectorToHeadPos(sectorCount())));
        if (label != null) {
            label.load(nbt);
        }
    }

    @Override
    public void save(final CompoundTag nbt) {
        super.save(nbt);
        nbt.putBoolean(UNMANAGED_TAG, true);
        nbt.putString(LOCK_TAG, lockInfo);
        nbt.put(DATA_TAG, new ByteArrayTag(data));
        nbt.putInt(HEAD_POS_TAG, headPos);
        if (label != null) {
            label.save(nbt);
        }
    }

    private int checkSector(final int sector) {
        if (sector < 0 || sector >= sectorCount()) {
            throw new IllegalArgumentException("invalid offset, not in a usable sector");
        }
        return sector;
    }

    private boolean locked() {
        return !lockInfo.isEmpty();
    }

    private int moveToSector(final Context context, final int sector) {
        final int newHeadPos = sectorToHeadPos(sector);
        if (headPos != newHeadPos) {
            final int delta = Math.abs(headPos - newHeadPos);
            if (delta > SECTOR_SEEK_THRESHOLD && context != null) {
                context.pause(SECTOR_SEEK_TIME);
            }
            headPos = newHeadPos;
        }
        return sector;
    }

    private int sectorCount() {
        return data.length / SECTOR_SIZE;
    }

    private int sectorsPerPlatter() {
        return Math.max(1, sectorCount() / platterCount);
    }

    private int sectorToHeadPos(final int sector) {
        return sector % sectorsPerPlatter();
    }

    private int sectorOffset(final int sector) {
        return sector * SECTOR_SIZE;
    }

    private int offsetSector(final int offset) {
        return offset / SECTOR_SIZE;
    }

    private void consumeCallBudget(final Context context, final double cost) {
        if (context != null) {
            context.consumeCallBudget(cost);
        }
    }

    private void diskActivity() {
        if (accessSound.isEmpty() || host.isEmpty() || node() == null) {
            return;
        }
        final EnvironmentHost environmentHost = host.get();
        NeoForge.EVENT_BUS.post(new FileSystemAccessEvent.Server(
            accessSound.get(),
            environmentHost.world(),
            environmentHost.xPosition(),
            environmentHost.yPosition(),
            environmentHost.zPosition(),
            node()));
    }

    private String clock() {
        return clock(READ_SECTOR_COSTS[costIndex]) + "/"
            + clock(WRITE_SECTOR_COSTS[costIndex]) + "/"
            + clock(READ_BYTE_COSTS[costIndex]) + "/"
            + clock(WRITE_BYTE_COSTS[costIndex]);
    }

    private static int clock(final double cost) {
        return (int) (2000.0D / cost) / 100;
    }
}
