package li.cil.oc.common.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.machine.Value;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import net.minecraft.nbt.CompoundTag;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public class DataCardEnvironment extends AbstractManagedEnvironment implements DeviceInfo {
    private static final String COMPONENT_NAME = "data";
    private static final int LIMIT = 8192;
    private static final int MAX_RANDOM_SIZE = 1024;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Map<String, String> TIER1_DEVICE_INFO = Map.of(
        DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Processor,
        DeviceInfo.DeviceAttribute.Description, "Data processor card",
        DeviceInfo.DeviceAttribute.Vendor, "S.C. Ltd.",
        DeviceInfo.DeviceAttribute.Product, "SC01D H45h3r",
        DeviceInfo.DeviceAttribute.Capacity, Integer.toString(LIMIT)
    );
    private static final Map<String, String> TIER2_DEVICE_INFO = Map.of(
        DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Processor,
        DeviceInfo.DeviceAttribute.Description, "Data processor card",
        DeviceInfo.DeviceAttribute.Vendor, "S.C. Ltd.",
        DeviceInfo.DeviceAttribute.Product, "SC02D Cryptic",
        DeviceInfo.DeviceAttribute.Capacity, Integer.toString(LIMIT)
    );
    private static final Map<String, String> TIER3_DEVICE_INFO = Map.of(
        DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Processor,
        DeviceInfo.DeviceAttribute.Description, "Data processor card",
        DeviceInfo.DeviceAttribute.Vendor, "S.C. Ltd.",
        DeviceInfo.DeviceAttribute.Product, "SC03D Signer",
        DeviceInfo.DeviceAttribute.Capacity, Integer.toString(LIMIT)
    );

    private final int tier;

    public DataCardEnvironment(final int tier) {
        this.tier = Math.max(0, Math.min(2, tier));
        final var builder = Network.newNode(this, Visibility.Network);
        if (builder != null) {
            setNode(builder.withComponent(COMPONENT_NAME, Visibility.Neighbors).create());
        }
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return switch (tier) {
            case 0 -> TIER1_DEVICE_INFO;
            case 1 -> TIER2_DEVICE_INFO;
            default -> TIER3_DEVICE_INFO;
        };
    }

    @Callback(direct = true, doc = "function():number -- Gets the maximum input size in bytes.")
    public Object[] getLimit(final Context context, final Arguments args) {
        return new Object[]{LIMIT};
    }

    @Callback(direct = true, doc = "function(data:string):string -- Encodes bytes as base64.")
    public Object[] encode64(final Context context, final Arguments args) {
        return new Object[]{Base64.getEncoder().encode(checkData(args, 0))};
    }

    @Callback(direct = true, doc = "function(data:string):string -- Decodes base64 bytes.")
    public Object[] decode64(final Context context, final Arguments args) {
        return new Object[]{Base64.getDecoder().decode(checkData(args, 0))};
    }

    @Callback(doc = "function(data:string):string -- Compresses bytes using zlib deflate.")
    public Object[] deflate(final Context context, final Arguments args) {
        try {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (DeflaterOutputStream deflater = new DeflaterOutputStream(out)) {
                deflater.write(checkData(args, 0));
            }
            return new Object[]{out.toByteArray()};
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid deflate input", e);
        }
    }

    @Callback(doc = "function(data:string):string -- Decompresses zlib deflate bytes.")
    public Object[] inflate(final Context context, final Arguments args) {
        try (InflaterInputStream inflater = new InflaterInputStream(new ByteArrayInputStream(checkData(args, 0)))) {
            return new Object[]{inflater.readAllBytes()};
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid deflate stream", e);
        }
    }

    @Callback(direct = true, doc = "function(data:string):string -- Computes CRC32.")
    public Object[] crc32(final Context context, final Arguments args) {
        final CRC32 crc = new CRC32();
        crc.update(checkData(args, 0));
        final long value = crc.getValue();
        return new Object[]{new byte[]{
            (byte) ((value >>> 24) & 0xFF),
            (byte) ((value >>> 16) & 0xFF),
            (byte) ((value >>> 8) & 0xFF),
            (byte) (value & 0xFF)
        }};
    }

    @Callback(direct = true, doc = "function(data:string[, key:string]):string -- Computes MD5 or HMAC-MD5.")
    public Object[] md5(final Context context, final Arguments args) {
        return new Object[]{hash("MD5", "HmacMD5", args)};
    }

    @Callback(direct = true, doc = "function(data:string[, key:string]):string -- Computes SHA-256 or HMAC-SHA256.")
    public Object[] sha256(final Context context, final Arguments args) {
        return new Object[]{hash("SHA-256", "HmacSHA256", args)};
    }

    @Callback(doc = "function(data:string, key:string, iv:string):string -- Encrypts bytes using AES/CBC/PKCS5Padding.")
    public Object[] encrypt(final Context context, final Arguments args) {
        requireTier(1);
        return new Object[]{aes(Cipher.ENCRYPT_MODE, args)};
    }

    @Callback(doc = "function(data:string, key:string, iv:string):string -- Decrypts bytes using AES/CBC/PKCS5Padding.")
    public Object[] decrypt(final Context context, final Arguments args) {
        requireTier(1);
        return new Object[]{aes(Cipher.DECRYPT_MODE, args)};
    }

    @Callback(direct = true, doc = "function(length:number):string -- Returns cryptographically random bytes.")
    public Object[] random(final Context context, final Arguments args) {
        requireTier(1);
        final int length = Math.max(0, Math.min(MAX_RANDOM_SIZE, args.checkInteger(0)));
        final byte[] data = new byte[length];
        RANDOM.nextBytes(data);
        return new Object[]{data};
    }

    @Callback(direct = true, doc = "function([bits:number]):userdata, userdata -- Generates an EC public/private key pair.")
    public Object[] generateKeyPair(final Context context, final Arguments args) {
        requireTier(2);
        final int bits = args.optInteger(0, 384);
        if (bits != 256 && bits != 384) {
            throw new IllegalArgumentException("invalid key length, must be 256 or 384");
        }
        try {
            final KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(bits, RANDOM);
            final KeyPair keyPair = generator.generateKeyPair();
            return new Object[]{new ECKey(keyPair.getPublic()), new ECKey(keyPair.getPrivate())};
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    @Callback(direct = true, doc = "function(data:string, type:string):userdata -- Restores an EC key from its binary representation.")
    public Object[] deserializeKey(final Context context, final Arguments args) {
        requireTier(2);
        return new Object[]{new ECKey(deserializeKey(args.checkString(1), checkData(args, 0)))};
    }

    @Callback(direct = true, doc = "function(private:userdata, public:userdata):string -- Generates an ECDH shared secret.")
    public Object[] ecdh(final Context context, final Arguments args) {
        requireTier(2);
        final PrivateKey privateKey = checkKey(args, 0, false).privateKey();
        final PublicKey publicKey = checkKey(args, 1, true).publicKey();
        try {
            final KeyAgreement agreement = KeyAgreement.getInstance("ECDH");
            agreement.init(privateKey);
            agreement.doPhase(publicKey, true);
            return new Object[]{agreement.generateSecret()};
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("invalid ec key", e);
        }
    }

    @Callback(direct = true, doc = "function(data:string, key:userdata[, signature:string]):string or boolean -- Signs or verifies data using ECDSA.")
    public Object[] ecdsa(final Context context, final Arguments args) {
        requireTier(2);
        final byte[] data = checkData(args, 0);
        final ECKey key = checkKey(args, 1, null);
        final byte[] signatureBytes = args.optByteArray(2, null);
        try {
            final Signature signature = Signature.getInstance("SHA256withECDSA");
            if (signatureBytes == null) {
                signature.initSign(key.privateKey());
                signature.update(data);
                return new Object[]{signature.sign()};
            }
            signature.initVerify(key.publicKey());
            signature.update(data);
            return new Object[]{signature.verify(signatureBytes)};
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("invalid ec signature input", e);
        }
    }

    private byte[] hash(final String digestAlgorithm, final String hmacAlgorithm, final Arguments args) {
        final byte[] data = checkData(args, 0);
        try {
            if (args.count() > 1) {
                requireTier(1);
                final Mac mac = Mac.getInstance(hmacAlgorithm);
                mac.init(new SecretKeySpec(checkData(args, 1), hmacAlgorithm));
                return mac.doFinal(data);
            }
            return MessageDigest.getInstance(digestAlgorithm).digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("invalid hash key", e);
        }
    }

    private byte[] aes(final int mode, final Arguments args) {
        try {
            final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(mode, new SecretKeySpec(checkData(args, 1), "AES"), new IvParameterSpec(checkData(args, 2)));
            return cipher.doFinal(checkData(args, 0));
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("invalid aes input", e);
        }
    }

    private byte[] checkData(final Arguments args, final int index) {
        final byte[] data = args.checkByteArray(index);
        if (data.length > LIMIT) {
            throw new IllegalArgumentException("data size limit exceeded");
        }
        return data;
    }

    private void requireTier(final int minimumTier) {
        if (tier < minimumTier) {
            throw new UnsupportedOperationException("unsupported data card tier");
        }
    }

    private static ECKey checkKey(final Arguments args, final int index, final Boolean expectedPublic) {
        final Object value = args.checkAny(index);
        if (!(value instanceof ECKey key)) {
            throw new IllegalArgumentException("bad argument #" + (index + 1) + " (userdata expected)");
        }
        if (expectedPublic != null && key.isPublicKey() != expectedPublic) {
            throw new IllegalArgumentException((expectedPublic ? "public" : "private") + " key expected at " + (index + 1));
        }
        return key;
    }

    private static Key deserializeKey(final String type, final byte[] data) {
        try {
            final KeyFactory factory = KeyFactory.getInstance("EC");
            return switch (type) {
                case ECKey.PUBLIC_TYPE -> factory.generatePublic(new X509EncodedKeySpec(data));
                case ECKey.PRIVATE_TYPE -> factory.generatePrivate(new PKCS8EncodedKeySpec(data));
                default -> throw new IllegalArgumentException("invalid key type, must be ec-public or ec-private");
            };
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("invalid ec key data", e);
        }
    }

    public static final class ECKey implements Value {
        public static final String PUBLIC_TYPE = "ec-public";
        public static final String PRIVATE_TYPE = "ec-private";
        private static final String TYPE_TAG = "Type";
        private static final String DATA_TAG = "Data";

        private Key key;

        public ECKey() {
        }

        public ECKey(final Key key) {
            this.key = key;
        }

        @Callback(direct = true, doc = "function():boolean -- Returns whether this key is public.")
        public Object[] isPublic(final Context context, final Arguments args) {
            return new Object[]{isPublicKey()};
        }

        @Callback(direct = true, doc = "function():string -- Returns the key type.")
        public Object[] keyType(final Context context, final Arguments args) {
            return new Object[]{keyType()};
        }

        @Callback(direct = true, doc = "function():string -- Returns the binary encoded key.")
        public Object[] serialize(final Context context, final Arguments args) {
            return new Object[]{key.getEncoded()};
        }

        @Override
        public Object apply(final Context context, final Arguments arguments) {
            return this;
        }

        @Override
        public void unapply(final Context context, final Arguments arguments) {
        }

        @Override
        public Object[] call(final Context context, final Arguments arguments) {
            return new Object[]{this};
        }

        @Override
        public void dispose(final Context context) {
        }

        @Override
        public void load(final CompoundTag nbt) {
            key = deserializeKey(nbt.getString(TYPE_TAG), nbt.getByteArray(DATA_TAG));
        }

        @Override
        public void save(final CompoundTag nbt) {
            nbt.putString(TYPE_TAG, keyType());
            nbt.putByteArray(DATA_TAG, key.getEncoded());
        }

        private boolean isPublicKey() {
            return key instanceof ECPublicKey;
        }

        private String keyType() {
            return isPublicKey() ? PUBLIC_TYPE : PRIVATE_TYPE;
        }

        private PrivateKey privateKey() {
            if (key instanceof PrivateKey privateKey) {
                return privateKey;
            }
            throw new IllegalArgumentException("private key expected");
        }

        private PublicKey publicKey() {
            if (key instanceof PublicKey publicKey) {
                return publicKey;
            }
            throw new IllegalArgumentException("public key expected");
        }
    }
}
