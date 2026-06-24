package li.cil.oc.common.component;

import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.ModSettings;
import li.cil.oc.common.OpenComputersApi;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.CRC32;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DataCardEnvironmentTest {
    @Test
    void exposesTierOneCallbacks() throws NoSuchMethodException {
        assertCallback("getLimit");
        assertDirectCallback("encode64", 32);
        assertDirectCallback("decode64", 32);
        assertDirectCallback("deflate", 4);
        assertDirectCallback("inflate", 4);
        assertDirectCallback("crc32", 32);
        assertDirectCallback("md5", 8);
        assertDirectCallback("sha256", 4);
    }

    @Test
    void exposesTierTwoCallbacks() throws NoSuchMethodException {
        assertDirectCallback("encrypt", 8);
        assertDirectCallback("decrypt", 8);
        assertDirectCallback("random", 4);
    }

    @Test
    void reportsTieredDeviceInfo() {
        OpenComputersApi.initialize();

        assertEquals("SC01D H45h3r", assertInstanceOf(DeviceInfo.class, new DataCardEnvironment(0)).getDeviceInfo().get(DeviceInfo.DeviceAttribute.Product));
        assertEquals("SC02D Cryptic", assertInstanceOf(DeviceInfo.class, new DataCardEnvironment(1)).getDeviceInfo().get(DeviceInfo.DeviceAttribute.Product));
        assertEquals("SC03D Signer", assertInstanceOf(DeviceInfo.class, new DataCardEnvironment(2)).getDeviceInfo().get(DeviceInfo.DeviceAttribute.Product));
    }

    @Test
    void createsNeighborReachableNodeLikeUpstream() {
        OpenComputersApi.initialize();

        DataCardEnvironment card = new DataCardEnvironment(0);

        assertEquals(Visibility.Neighbors, card.node().reachability());
        assertEquals(Visibility.Neighbors, assertInstanceOf(li.cil.oc.api.network.Component.class, card.node()).visibility());
    }

    @Test
    void exposesOnlyCallbacksAvailableForTierLikeUpstreamSubclasses() {
        OpenComputersApi.initialize();

        li.cil.oc.api.network.Component tierOne = assertInstanceOf(li.cil.oc.api.network.Component.class, new DataCardEnvironment(0).node());
        li.cil.oc.api.network.Component tierTwo = assertInstanceOf(li.cil.oc.api.network.Component.class, new DataCardEnvironment(1).node());
        li.cil.oc.api.network.Component tierThree = assertInstanceOf(li.cil.oc.api.network.Component.class, new DataCardEnvironment(2).node());

        assertTrue(tierOne.methods().contains("encode64"));
        assertTrue(tierOne.methods().contains("sha256"));
        assertFalse(tierOne.methods().contains("encrypt"));
        assertFalse(tierOne.methods().contains("random"));
        assertFalse(tierOne.methods().contains("generateKeyPair"));

        assertTrue(tierTwo.methods().contains("encrypt"));
        assertTrue(tierTwo.methods().contains("random"));
        assertFalse(tierTwo.methods().contains("generateKeyPair"));

        assertTrue(tierThree.methods().contains("encrypt"));
        assertTrue(tierThree.methods().contains("random"));
        assertTrue(tierThree.methods().contains("generateKeyPair"));
    }

    @Test
    void tierOneEncodesHashesAndCompressesData() throws Exception {
        OpenComputersApi.initialize();
        DataCardEnvironment card = new DataCardEnvironment(0);
        charge(card, 1000D);
        byte[] data = "hello world".getBytes(StandardCharsets.UTF_8);

        assertArrayEquals(Base64.getEncoder().encode(data), (byte[]) card.encode64(null, new TestArguments(data))[0]);
        assertArrayEquals(data, (byte[]) card.decode64(null, new TestArguments(Base64.getEncoder().encode(data)))[0]);
        byte[] compressed = (byte[]) card.deflate(null, new TestArguments(data))[0];
        assertArrayEquals(data, (byte[]) card.inflate(null, new TestArguments(compressed))[0]);
        assertArrayEquals(crc32(data), (byte[]) card.crc32(null, new TestArguments(data))[0]);
        assertArrayEquals(MessageDigest.getInstance("MD5").digest(data), (byte[]) card.md5(null, new TestArguments(data))[0]);
        assertArrayEquals(MessageDigest.getInstance("SHA-256").digest(data), (byte[]) card.sha256(null, new TestArguments(data))[0]);
    }

    @Test
    void operationsConsumeEnergyAndPauseAboveSoftLimit() throws Exception {
        OpenComputersApi.initialize();
        DataCardEnvironment card = new DataCardEnvironment(0);
        ComponentConnector connector = assertInstanceOf(ComponentConnector.class, card.node());
        connector.setLocalBufferSize(100);
        TestEnvironment machineEnvironment = new TestEnvironment();
        Node machineNode = Network.newNode(machineEnvironment, li.cil.oc.api.network.Visibility.None).withConnector(100D).create();
        machineEnvironment.node = machineNode;
        Connector machineConnector = assertInstanceOf(Connector.class, machineNode);
        machineConnector.changeBuffer(100D);
        RecordingContext context = new RecordingContext(machineNode);

        Exception error = assertThrows(Exception.class, () -> card.encode64(context, new TestArguments("hello")));
        assertEquals("not enough energy", error.getMessage());
        error = assertThrows(Exception.class, () -> card.deflate(context, new TestArguments("hello")));
        assertEquals("not enough energy", error.getMessage());

        connector.changeBuffer(50);
        byte[] large = new byte[8193];
        Arrays.fill(large, (byte) 'x');
        assertArrayEquals(Base64.getEncoder().encode(large), (byte[]) card.encode64(context, new TestArguments(large))[0]);
        assertEquals(1.0D, context.pauseSeconds, 0.000_001D);
        assertEquals(8.835D, connector.localBuffer(), 0.000_001D);
        assertEquals(100D, machineConnector.localBuffer(), 0.000_001D);
        assertArrayEquals(new Object[]{1048576}, card.getLimit(null, new TestArguments()));
    }

    @Test
    void usesConfiguredLimitsAndEnergyCostsLikeUpstream() throws Exception {
        OpenComputersApi.initialize();
        withCachedConfig(ModSettings.DATA_CARD_HARD_LIMIT, 4, () ->
            withCachedConfig(ModSettings.DATA_CARD_TRIVIAL, 1.5D, () ->
                withCachedConfig(ModSettings.DATA_CARD_TRIVIAL_BYTE, 0.25D, () -> {
                    DataCardEnvironment card = new DataCardEnvironment(0);
                    ComponentConnector connector = assertInstanceOf(ComponentConnector.class, card.node());
                    connector.setLocalBufferSize(10);
                    connector.changeBuffer(10);

                    assertArrayEquals(new Object[]{4}, card.getLimit(null, new TestArguments()));
                    assertArrayEquals(Base64.getEncoder().encode(bytes("abc")), (byte[]) card.encode64(null, new TestArguments(bytes("abc")))[0]);
                    assertEquals(7.75D, connector.localBuffer(), 0.000_001D);

                    IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> card.encode64(null, new TestArguments(bytes("abcde"))));
                    assertEquals("data size limit exceeded", error.getMessage());
                })));
    }

    @Test
    void tierTwoAddsHmacAesAndRandomData() throws Exception {
        OpenComputersApi.initialize();
        DataCardEnvironment card = new DataCardEnvironment(1);
        charge(card, 1000D);
        byte[] data = "hello world".getBytes(StandardCharsets.UTF_8);
        byte[] key = "0123456789abcdef".getBytes(StandardCharsets.UTF_8);
        byte[] iv = "abcdef0123456789".getBytes(StandardCharsets.UTF_8);

        assertArrayEquals(hmac("HmacMD5", key, data), (byte[]) card.md5(null, new TestArguments(data, key))[0]);
        assertArrayEquals(hmac("HmacSHA256", key, data), (byte[]) card.sha256(null, new TestArguments(data, key))[0]);
        byte[] encrypted = (byte[]) card.encrypt(null, new TestArguments(data, key, iv))[0];
        assertArrayEquals(aes(Cipher.ENCRYPT_MODE, data, key, iv), encrypted);
        assertArrayEquals(data, (byte[]) card.decrypt(null, new TestArguments(encrypted, key, iv))[0]);
        assertEquals(16, ((byte[]) card.random(null, new TestArguments(16))[0]).length);
    }

    @Test
    void tierTwoRejectsInvalidAesKeyAndIvLengths() {
        OpenComputersApi.initialize();
        DataCardEnvironment card = new DataCardEnvironment(1);
        charge(card, 100D);
        byte[] data = "hello world".getBytes(StandardCharsets.UTF_8);
        byte[] key = "too short".getBytes(StandardCharsets.UTF_8);
        byte[] iv = "abcdef0123456789".getBytes(StandardCharsets.UTF_8);

        IllegalArgumentException keyError = assertThrows(IllegalArgumentException.class, () -> card.encrypt(null, new TestArguments(data, key, iv)));
        assertEquals("expected a 128-bit AES key", keyError.getMessage());

        byte[] validKey = "0123456789abcdef".getBytes(StandardCharsets.UTF_8);
        byte[] shortIv = "short".getBytes(StandardCharsets.UTF_8);
        IllegalArgumentException ivError = assertThrows(IllegalArgumentException.class, () -> card.encrypt(null, new TestArguments(data, validKey, shortIv)));
        assertEquals("expected a 128-bit AES IV", ivError.getMessage());
    }

    @Test
    void exposesTierThreeCallbacks() throws NoSuchMethodException {
        assertDirectCallback("generateKeyPair", 1);
        assertDirectCallback("deserializeKey", 8);
        assertDirectCallback("ecdh", 1);
        assertDirectCallback("ecdsa", 1);
    }

    @Test
    void tierThreeAddsEllipticCurveOperations() throws Exception {
        OpenComputersApi.initialize();
        DataCardEnvironment alice = new DataCardEnvironment(2);
        DataCardEnvironment bob = new DataCardEnvironment(2);
        charge(alice, 1000D);
        charge(bob, 1000D);
        Object[] aliceKeys = alice.generateKeyPair(null, new TestArguments(256));
        Object[] bobKeys = bob.generateKeyPair(null, new TestArguments(256));
        DataCardEnvironment.ECKey alicePublic = assertInstanceOf(DataCardEnvironment.ECKey.class, aliceKeys[0]);
        DataCardEnvironment.ECKey alicePrivate = assertInstanceOf(DataCardEnvironment.ECKey.class, aliceKeys[1]);
        DataCardEnvironment.ECKey bobPublic = assertInstanceOf(DataCardEnvironment.ECKey.class, bobKeys[0]);
        DataCardEnvironment.ECKey bobPrivate = assertInstanceOf(DataCardEnvironment.ECKey.class, bobKeys[1]);

        assertEquals(true, alicePublic.isPublic(null, new TestArguments())[0]);
        assertEquals(false, alicePrivate.isPublic(null, new TestArguments())[0]);
        assertEquals("ec-public", alicePublic.keyType(null, new TestArguments())[0]);
        assertEquals("ec-private", alicePrivate.keyType(null, new TestArguments())[0]);
        assertArrayEquals(
            (byte[]) alice.ecdh(null, new TestArguments(alicePrivate, bobPublic))[0],
            (byte[]) bob.ecdh(null, new TestArguments(bobPrivate, alicePublic))[0]);

        byte[] data = "signed payload".getBytes(StandardCharsets.UTF_8);
        byte[] signature = (byte[]) alice.ecdsa(null, new TestArguments(data, alicePrivate))[0];
        assertEquals(true, alice.ecdsa(null, new TestArguments(data, alicePublic, signature))[0]);
        assertEquals(false, alice.ecdsa(null, new TestArguments("tampered".getBytes(StandardCharsets.UTF_8), alicePublic, signature))[0]);

        byte[] serializedPublic = (byte[]) alicePublic.serialize(null, new TestArguments())[0];
        DataCardEnvironment.ECKey restoredPublic = assertInstanceOf(
            DataCardEnvironment.ECKey.class,
            alice.deserializeKey(null, new TestArguments(serializedPublic, "ec-public"))[0]);
        assertEquals(true, alice.ecdsa(null, new TestArguments(data, restoredPublic, signature))[0]);
    }

    @Test
    void ecKeySerializeUsesUpstreamCallbackLimit() throws NoSuchMethodException {
        Method method = DataCardEnvironment.ECKey.class.getMethod("serialize", Context.class, Arguments.class);
        Callback callback = method.getAnnotation(Callback.class);

        assertTrue(callback.direct());
        assertEquals(4, callback.limit());
    }

    private static void assertCallback(final String methodName) throws NoSuchMethodException {
        Method method = DataCardEnvironment.class.getMethod(methodName, li.cil.oc.api.machine.Context.class, Arguments.class);
        assertTrue(method.isAnnotationPresent(Callback.class));
    }

    private static void assertDirectCallback(final String methodName) throws NoSuchMethodException {
        Method method = DataCardEnvironment.class.getMethod(methodName, li.cil.oc.api.machine.Context.class, Arguments.class);
        Callback callback = method.getAnnotation(Callback.class);
        assertTrue(callback.direct());
    }

    private static void assertDirectCallback(final String methodName, final int limit) throws NoSuchMethodException {
        Method method = DataCardEnvironment.class.getMethod(methodName, li.cil.oc.api.machine.Context.class, Arguments.class);
        Callback callback = method.getAnnotation(Callback.class);
        assertTrue(callback.direct());
        assertEquals(limit, callback.limit());
    }

    private static byte[] crc32(final byte[] data) {
        CRC32 crc = new CRC32();
        crc.update(data);
        long value = crc.getValue();
        return new byte[]{
            (byte) (value & 0xFF),
            (byte) ((value >>> 8) & 0xFF),
            (byte) ((value >>> 16) & 0xFF),
            (byte) ((value >>> 24) & 0xFF)
        };
    }

    private static byte[] hmac(final String algorithm, final byte[] key, final byte[] data) throws Exception {
        Mac mac = Mac.getInstance(algorithm);
        mac.init(new SecretKeySpec(key, algorithm));
        return mac.doFinal(data);
    }

    private static byte[] aes(final int mode, final byte[] data, final byte[] key, final byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(mode, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
        return cipher.doFinal(data);
    }

    private static void charge(final DataCardEnvironment card, final double amount) {
        ComponentConnector connector = assertInstanceOf(ComponentConnector.class, card.node());
        connector.setLocalBufferSize(amount);
        connector.changeBuffer(amount);
    }

    private static <T> void withCachedConfig(final ModConfigSpec.ConfigValue<T> value, final T override, final ThrowingRunnable action) throws Exception {
        final Field cachedValue = ModConfigSpec.ConfigValue.class.getDeclaredField("cachedValue");
        cachedValue.setAccessible(true);
        final Object previous = cachedValue.get(value);
        cachedValue.set(value, override);
        try {
            action.run();
        } finally {
            cachedValue.set(value, previous);
        }
    }

    private static byte[] bytes(final String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static final class RecordingContext implements Context {
        private final Node node;
        private double pauseSeconds = -1D;

        private RecordingContext(final Node node) {
            this.node = node;
        }

        @Override public Node node() { return node; }
        @Override public boolean canInteract(final String player) { return true; }
        @Override public boolean isRunning() { return true; }
        @Override public boolean isPaused() { return false; }
        @Override public boolean start() { return true; }
        @Override public boolean pause(final double seconds) { pauseSeconds = seconds; return true; }
        @Override public boolean stop() { return true; }
        @Override public void consumeCallBudget(final double callCost) { }
        @Override public boolean signal(final String name, final Object... args) { return true; }
    }

    private static final class TestEnvironment implements Environment {
        private Node node;

        @Override public Node node() { return node; }
        @Override public void onConnect(final Node node) { }
        @Override public void onDisconnect(final Node node) { }
        @Override public void onMessage(final Message message) { }
    }

    private record TestArguments(Object... values) implements Arguments {
        @Override public int count() { return values.length; }
        @Override public Object checkAny(final int index) { return values[index]; }
        @Override public boolean checkBoolean(final int index) { return (Boolean) values[index]; }
        @Override public int checkInteger(final int index) { return ((Number) values[index]).intValue(); }
        @Override public long checkLong(final int index) { return ((Number) values[index]).longValue(); }
        @Override public double checkDouble(final int index) { return ((Number) values[index]).doubleValue(); }
        @Override public String checkString(final int index) { return (String) values[index]; }
        @Override public byte[] checkByteArray(final int index) {
            Object value = values[index];
            return value instanceof byte[] bytes ? bytes : ((String) value).getBytes(StandardCharsets.UTF_8);
        }
        @Override public Map checkTable(final int index) { return (Map) values[index]; }
        @Override public ItemStack checkItemStack(final int index) { return (ItemStack) values[index]; }
        @Override public Object optAny(final int index, final Object def) { return index < values.length ? values[index] : def; }
        @Override public boolean optBoolean(final int index, final boolean def) { return index < values.length ? checkBoolean(index) : def; }
        @Override public int optInteger(final int index, final int def) { return index < values.length ? checkInteger(index) : def; }
        @Override public long optLong(final int index, final long def) { return index < values.length ? checkLong(index) : def; }
        @Override public double optDouble(final int index, final double def) { return index < values.length ? checkDouble(index) : def; }
        @Override public String optString(final int index, final String def) { return index < values.length ? checkString(index) : def; }
        @Override public byte[] optByteArray(final int index, final byte[] def) { return index < values.length ? checkByteArray(index) : def; }
        @Override public Map optTable(final int index, final Map def) { return index < values.length ? checkTable(index) : def; }
        @Override public ItemStack optItemStack(final int index, final ItemStack def) { return index < values.length ? checkItemStack(index) : def; }
        @Override public boolean isBoolean(final int index) { return values[index] instanceof Boolean; }
        @Override public boolean isInteger(final int index) { return values[index] instanceof Integer; }
        @Override public boolean isLong(final int index) { return values[index] instanceof Long; }
        @Override public boolean isDouble(final int index) { return values[index] instanceof Double; }
        @Override public boolean isString(final int index) { return values[index] instanceof String; }
        @Override public boolean isByteArray(final int index) { return values[index] instanceof byte[]; }
        @Override public boolean isTable(final int index) { return values[index] instanceof Map; }
        @Override public boolean isItemStack(final int index) { return values[index] instanceof ItemStack; }
        @Override public Object[] toArray() { return Arrays.copyOf(values, values.length); }
        @Override public Iterator<Object> iterator() { return Arrays.asList(values).iterator(); }
    }
}
