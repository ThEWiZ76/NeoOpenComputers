package li.cil.oc.common.component;

import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.common.OpenComputersApi;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DataCardEnvironmentTest {
    @Test
    void exposesTierOneCallbacks() throws NoSuchMethodException {
        assertCallback("getLimit");
        assertCallback("encode64");
        assertCallback("decode64");
        assertCallback("deflate");
        assertCallback("inflate");
        assertCallback("crc32");
        assertCallback("md5");
        assertCallback("sha256");
    }

    @Test
    void exposesTierTwoCallbacks() throws NoSuchMethodException {
        assertCallback("encrypt");
        assertCallback("decrypt");
        assertCallback("random");
    }

    @Test
    void reportsTieredDeviceInfo() {
        OpenComputersApi.initialize();

        assertEquals("SC01D H45h3r", assertInstanceOf(DeviceInfo.class, new DataCardEnvironment(0)).getDeviceInfo().get(DeviceInfo.DeviceAttribute.Product));
        assertEquals("SC02D Cryptic", assertInstanceOf(DeviceInfo.class, new DataCardEnvironment(1)).getDeviceInfo().get(DeviceInfo.DeviceAttribute.Product));
    }

    @Test
    void tierOneEncodesHashesAndCompressesData() throws Exception {
        OpenComputersApi.initialize();
        DataCardEnvironment card = new DataCardEnvironment(0);
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
    void tierTwoAddsHmacAesAndRandomData() throws Exception {
        OpenComputersApi.initialize();
        DataCardEnvironment card = new DataCardEnvironment(1);
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

    private static void assertCallback(final String methodName) throws NoSuchMethodException {
        Method method = DataCardEnvironment.class.getMethod(methodName, li.cil.oc.api.machine.Context.class, Arguments.class);
        assertTrue(method.isAnnotationPresent(Callback.class));
    }

    private static byte[] crc32(final byte[] data) {
        CRC32 crc = new CRC32();
        crc.update(data);
        long value = crc.getValue();
        return new byte[]{
            (byte) ((value >>> 24) & 0xFF),
            (byte) ((value >>> 16) & 0xFF),
            (byte) ((value >>> 8) & 0xFF),
            (byte) (value & 0xFF)
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
