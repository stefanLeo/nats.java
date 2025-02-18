// Copyright 2018 The NATS Authors
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at:
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package io.nats.client;

import net.i2p.crypto.eddsa.EdDSAEngine;
import net.i2p.crypto.eddsa.EdDSAPrivateKey;
import net.i2p.crypto.eddsa.EdDSAPublicKey;
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec;
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.*;
import java.util.Arrays;

import static io.nats.client.support.Encoding.base32Decode;
import static io.nats.client.support.Encoding.base32Encode;
import static io.nats.client.support.RandomUtils.PRAND;
import static io.nats.client.support.RandomUtils.SRAND;
import static io.nats.nkey.NKeyConstants.*;

class DecodedSeed {
    int prefix;
    byte[] bytes;
}

/**
 * @deprecated This class has been extracted to the <a href="https://github.com/nats-io/nkeys.java">nkeys.java</a> library.
 * It is left here for full backward compatibility.
 * <p>
 * The NATS ecosystem will be moving to Ed25519 keys for identity,
 * authentication and authorization for entities such as Accounts, Users,
 * Servers and Clusters.
 * </p>
 * <p>
 * NKeys are based on the Ed25519 standard. This signing algorithm provides for
 * the use of public and private keys to sign and verify data. NKeys is designed
 * to formulate keys in a much friendlier fashion referencing work done in
 * cryptocurrencies, specifically Stellar. Bitcoin and others use a form of
 * Base58 (or Base58Check) to encode raw keys. Stellar utilizes a more
 * traditional Base32 with a CRC16 and a version or prefix byte. NKeys utilizes
 * a similar format with one or two prefix bytes. The base32 encoding of these
 * prefixes will yield friendly human readable prefixes, e.g. 'N' = server, 'C'
 * = cluster, 'O' = operator, 'A' = account, and 'U' = user to help developers
 * and administrators quickly identify key types.
 * </p>
 * <p>
 * Each NKey is generated from 32 bytes. These bytes are called the seed and are
 * encoded, in the NKey world, into a string starting with the letter 'S', with
 * a second character indicating the key’s type, e.g. "SU" is a seed for a u
 * er key pair, "SA" is a seed for an account key pair. The seed can be used t
 *  create the Ed25519 public/private key pair and should be protected as a p
 * ivate key. It is equivalent to the private key for a PGP key pair, or the m
 * ster password for your password vault.
 * </p>
 * <p>
 * Ed25519 uses the seed bytes to generate a key pair. The pair contains a
 * private key, which can be used to sign data, and a public key which can be
 * used to verify a signature. The public key can be distributed, and is not
 * considered secret.
 * </p>
 * <p>
 * The NKey libraries encode 32 byte public keys using Base32 and a CRC16
 * checksum plus a prefix based on the key type, e.g. U for a user key.
 * </p>
 * <p>
 * The NKey libraries have support for exporting a 64 byte private key. This
 * data is encoded into a string starting with the prefix ‘P’ for private. The
 * 64 bytes in a private key consists of the 32 bytes of the seed followed by 
 * he 32 bytes of the public key. Essentially, the private key is redundant sin
 * e you can get it back from the seed alone. The NATS team recommends sto
 * ing the 32 byte seed and letting the NKey library regenerate anything els
 *  it needs for signing.
 * </p>
 * <p>
 * The existence of both a seed and a private key can result in confusion. It is
 * reasonable to simply think of Ed25519 as having a public key and a private
 * seed, and ignore the longer private key concept. In fact, the NKey libraries
 * generally expect you to create an NKey from either a public key, to use for
 * verification, or a seed, to use for signing.
 * </p>
 * <p>
 * The NATS system will utilize public NKeys for identification, the NATS system
 * will never store or even have access to any private keys or seeds.
 * Authentication will utilize a challenge-response mechanism based on a
 * collection of random bytes called a nonce.
 * </p>
 * <p>
 * Version note - 2.2.0 provided string arguments for seeds, this is not as safe
 * as char arrays, so in 2.3.0 we have included a breaking change to char arrays.
 * While this is not the proper version choice, NKeys aren't widely used, if at all yet,
 * so we are making the change on a minor jump.
 * </p>
 */
@Deprecated
public class NKey {

    /**
     * NKeys use a prefix byte to indicate their intended owner: 'N' = server, 'C' =
     * cluster, 'A' = account, and 'U' = user. 'P' is used for private keys. The
     * NKey class formalizes these into the enum NKey.Type.
     */
    public enum Type {
        /** A user NKey. */
        USER(PREFIX_BYTE_USER),
        /** An account NKey. */
        ACCOUNT(PREFIX_BYTE_ACCOUNT),
        /** A server NKey. */
        SERVER(PREFIX_BYTE_SERVER),
        /** An operator NKey. */
        OPERATOR(PREFIX_BYTE_OPERATOR),
        /** A cluster NKey. */
        CLUSTER(PREFIX_BYTE_CLUSTER),
        /** A private NKey. */
        PRIVATE(PREFIX_BYTE_PRIVATE);

        private final int prefix;

        Type(int prefix) {
            this.prefix = prefix;
        }

        public static Type fromPrefix(int prefix) {
            if (prefix == PREFIX_BYTE_ACCOUNT) {
                return ACCOUNT;
            } else if (prefix == PREFIX_BYTE_SERVER) {
                return SERVER;
            } else if (prefix == PREFIX_BYTE_USER) {
                return USER;
            } else if (prefix == PREFIX_BYTE_CLUSTER) {
                return CLUSTER;
            } else if (prefix == PREFIX_BYTE_PRIVATE) {
                return ACCOUNT;
            } else if (prefix == PREFIX_BYTE_OPERATOR) {
                return OPERATOR;
            }

            throw new IllegalArgumentException("Unknown prefix");
        }
    }

    static int crc16(byte[] bytes) {
        int crc = 0;

        for (byte b : bytes) {
            crc = ((crc << 8) & 0xffff) ^ CRC_16_TABLE[((crc >> 8) ^ (b & 0xFF)) & 0x00FF];
        }

        return crc;
    }


    private static boolean checkValidPublicPrefixByte(int prefix) {
        switch (prefix) {
            case PREFIX_BYTE_SERVER:
            case PREFIX_BYTE_CLUSTER:
            case PREFIX_BYTE_OPERATOR:
            case PREFIX_BYTE_ACCOUNT:
            case PREFIX_BYTE_USER:
                return true;
        }
        return false;
    }

    static char[] removePaddingAndClear(char[] withPad) {
        int i;

        for (i=withPad.length-1;i>=0;i--) {
            if (withPad[i] != '=') {
                break;
            }
        }
        char[] withoutPad = new char[i+1];
        System.arraycopy(withPad, 0, withoutPad, 0, withoutPad.length);

        for (int j=0;j<withPad.length;j++) {
            withPad[j] = '\0';
        }

        return withoutPad;
    }

    static char[] encode(Type type, byte[] src) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        bytes.write(type.prefix);
        bytes.write(src);

        int crc = crc16(bytes.toByteArray());
        byte[] littleEndian = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort((short) crc).array();

        bytes.write(littleEndian);

        char[] withPad = base32Encode(bytes.toByteArray());
        return removePaddingAndClear(withPad);
    }

    static char[] encodeSeed(Type type, byte[] src) throws IOException {
        if (src.length != ED25519_PRIVATE_KEYSIZE && src.length != ED25519_SEED_SIZE) {
            throw new IllegalArgumentException("Source is not the correct size for an ED25519 seed");
        }

        // In order to make this human printable for both bytes, we need to do a little
        // bit manipulation to setup for base32 encoding which takes 5 bits at a time.
        int b1 = PREFIX_BYTE_SEED | (type.prefix >> 5);
        int b2 = (type.prefix & 31) << 3; // 31 = 00011111

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        bytes.write(b1);
        bytes.write(b2);
        bytes.write(src);

        int crc = crc16(bytes.toByteArray());
        byte[] littleEndian = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort((short) crc).array();

        bytes.write(littleEndian);

        char[] withPad = base32Encode(bytes.toByteArray());
        return removePaddingAndClear(withPad);
    }

    static byte[] decode(char[] src) {
        byte[] raw = base32Decode(src);

        if (raw == null || raw.length < 4) {
            throw new IllegalArgumentException("Invalid encoding for source string");
        }

        byte[] crcBytes = Arrays.copyOfRange(raw, raw.length - 2, raw.length);
        byte[] dataBytes = Arrays.copyOfRange(raw, 0, raw.length - 2);

        int crc = ByteBuffer.wrap(crcBytes).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
        int actual = crc16(dataBytes);

        if (actual != crc) {
            throw new IllegalArgumentException("CRC is invalid");
        }

        return dataBytes;
    }

    static byte[] decode(Type expectedType, char[] src, boolean safe) {
        byte[] raw = decode(src);
        byte[] dataBytes = Arrays.copyOfRange(raw, 1, raw.length);
        Type type = NKey.Type.fromPrefix(raw[0] & 0xFF);

        if (type != expectedType) {
            if (safe) {
                return null;
            }
            throw new IllegalArgumentException("Unexpected type");
        }

        return dataBytes;
    }

    static DecodedSeed decodeSeed(char[] seed) {
        byte[] raw = decode(seed);

        // Need to do the reverse here to get back to internal representation.
        int b1 = raw[0] & 248; // 248 = 11111000
        int b2 = (raw[0] & 7) << 5 | ((raw[1] & 248) >> 3); // 7 = 00000111

        if (b1 != PREFIX_BYTE_SEED) {
            throw new IllegalArgumentException("Invalid encoding");
        }

        if (!checkValidPublicPrefixByte(b2)) {
            throw new IllegalArgumentException("Invalid encoded prefix byte");
        }

        byte[] dataBytes = Arrays.copyOfRange(raw, 2, raw.length);
        DecodedSeed retVal = new DecodedSeed();
        retVal.prefix = b2;
        retVal.bytes = dataBytes;
        return retVal;
    }

    private static NKey createPair(Type type, SecureRandom random)
            throws IOException, NoSuchProviderException, NoSuchAlgorithmException {
        if (random == null) {
            random = SRAND;
        }

        byte[] seed = new byte[ED_25519.getCurve().getField().getb() / 8];
        random.nextBytes(seed);

        return createPair(type, seed);
    }

    private static NKey createPair(Type type, byte[] seed)
            throws IOException, NoSuchProviderException, NoSuchAlgorithmException {
        EdDSAPrivateKeySpec privKeySpec = new EdDSAPrivateKeySpec(seed, ED_25519);
        EdDSAPrivateKey privKey = new EdDSAPrivateKey(privKeySpec);
        EdDSAPublicKeySpec pubKeySpec = new EdDSAPublicKeySpec(privKey.getA(), ED_25519);
        EdDSAPublicKey pubKey = new EdDSAPublicKey(pubKeySpec);
        byte[] pubBytes = pubKey.getAbyte();

        byte[] bytes = new byte[pubBytes.length + seed.length];
        System.arraycopy(seed, 0, bytes, 0, seed.length);
        System.arraycopy(pubBytes, 0, bytes, seed.length, pubBytes.length);

        char[] encoded = encodeSeed(type, bytes);
        return new NKey(type, null, encoded);
    }

    /**
     * Create an Account NKey from the provided random number generator.
     * 
     * If no random is provided, SecureRandom() will be used to create one.
     * 
     * The new NKey contains the private seed, which should be saved in a secure location.
     * 
     * @param random A secure random provider
     * @return the new Nkey
     * @throws IOException if the seed cannot be encoded to a string
     * @throws NoSuchProviderException if the default secure random cannot be created
     * @throws NoSuchAlgorithmException if the default secure random cannot be created
     */
    public static NKey createAccount(SecureRandom random)
            throws IOException, NoSuchProviderException, NoSuchAlgorithmException {
        return createPair(Type.ACCOUNT, random);
    }

    /**
     * Create a Cluster NKey from the provided random number generator.
     * 
     * If no random is provided, SecureRandom() will be used to create one.
     * 
     * The new NKey contains the private seed, which should be saved in a secure location.
     * 
     * @param random A secure random provider
     * @return the new Nkey
     * @throws IOException if the seed cannot be encoded to a string
     * @throws NoSuchProviderException if the default secure random cannot be created
     * @throws NoSuchAlgorithmException if the default secure random cannot be created
     */
    public static NKey createCluster(SecureRandom random)
            throws IOException, NoSuchProviderException, NoSuchAlgorithmException {
        return createPair(Type.CLUSTER, random);
    }

    /**
     * Create an Operator NKey from the provided random number generator.
     * 
     * If no random is provided, SecureRandom() will be used to create one.
     * 
     * The new NKey contains the private seed, which should be saved in a secure location.
     * 
     * @param random A secure random provider
     * @return the new Nkey
     * @throws IOException if the seed cannot be encoded to a string
     * @throws NoSuchProviderException if the default secure random cannot be created
     * @throws NoSuchAlgorithmException if the default secure random cannot be created
     */
    public static NKey createOperator(SecureRandom random)
            throws IOException, NoSuchProviderException, NoSuchAlgorithmException {
        return createPair(Type.OPERATOR, random);
    }

    /**
     * Create a Server NKey from the provided random number generator.
     * 
     * If no random is provided, SecureRandom() will be used to create one.
     * 
     * The new NKey contains the private seed, which should be saved in a secure location.
     * 
     * @param random A secure random provider
     * @return the new Nkey
     * @throws IOException if the seed cannot be encoded to a string
     * @throws NoSuchProviderException if the default secure random cannot be created
     * @throws NoSuchAlgorithmException if the default secure random cannot be created
     */
    public static NKey createServer(SecureRandom random)
            throws IOException, NoSuchProviderException, NoSuchAlgorithmException {
        return createPair(Type.SERVER, random);
    }

    /**
     * Create a User NKey from the provided random number generator.
     * 
     * If no random is provided, SecureRandom() will be used to create one.
     * 
     * The new NKey contains the private seed, which should be saved in a secure location.
     * 
     * @param random A secure random provider
     * @return the new Nkey
     * @throws IOException if the seed cannot be encoded to a string
     * @throws NoSuchProviderException if the default secure random cannot be created
     * @throws NoSuchAlgorithmException if the default secure random cannot be created
     */
    public static NKey createUser(SecureRandom random)
            throws IOException, NoSuchProviderException, NoSuchAlgorithmException {
        return createPair(Type.USER, random);
    }

    /**
     * Create an NKey object from the encoded public key. This NKey can be used for verification but not for signing.
     * 
     * @param publicKey the string encoded public key
     * @return the new Nkey
     */
    public static NKey fromPublicKey(char[] publicKey) {
        byte[] raw = decode(publicKey);
        int prefix = raw[0] & 0xFF;

        if (!checkValidPublicPrefixByte(prefix)) {
            throw new IllegalArgumentException("Not a valid public NKey");
        }

        Type type = NKey.Type.fromPrefix(prefix);
        return new NKey(type, publicKey, null);
    }

    /**
     * Creates an NKey object from a string encoded seed. This NKey can be used to sign or verify.
     * 
     * @param seed the string encoded seed, see {@link NKey#getSeed() getSeed()}
     * @return the Nkey
     */
    public static NKey fromSeed(char[] seed) {
        DecodedSeed decoded = decodeSeed(seed); // Should throw on bad seed

        if (decoded.bytes.length == ED25519_PRIVATE_KEYSIZE) {
            return new NKey(Type.fromPrefix(decoded.prefix), null, seed);
        } else {
            try {
                return createPair(Type.fromPrefix(decoded.prefix), decoded.bytes);
            } catch (Exception e) {
                throw new IllegalArgumentException("Bad seed value", e);
            }
        }
    }

    /**
     * @param src the encoded public key
     * @return true if the public key is an account public key
     */
    public static boolean isValidPublicAccountKey(char[] src) {
        return decode(Type.ACCOUNT, src, true) != null;
    }

    /**
     * @param src the encoded public key
     * @return true if the public key is a cluster public key
     */
    public static boolean isValidPublicClusterKey(char[] src) {
        return decode(Type.CLUSTER, src, true) != null;
    }

    /**
     * @param src the encoded public key
     * @return true if the public key is an operator public key
     */
    public static boolean isValidPublicOperatorKey(char[] src) {
        return decode(Type.OPERATOR, src, true) != null;
    }

    /**
     * @param src the encoded public key
     * @return true if the public key is a server public key
     */
    public static boolean isValidPublicServerKey(char[] src) {
        return decode(Type.SERVER, src, true) != null;
    }

    /**
     * @param src the encoded public key
     * @return true if the public key is a user public key
     */
    public static boolean isValidPublicUserKey(char[] src) {
        return decode(Type.USER, src, true) != null;
    }

    /**
     * The seed or private key per the Ed25519 spec, encoded with encodeSeed.
     */
    private char[] privateKeyAsSeed;

    /**
     * The public key, maybe null. Used for public only NKeys.
     */
    private char[] publicKey;

    private Type type;

    private NKey(Type t, char[] publicKey, char[] privateKey) {
        this.type = t;
        this.privateKeyAsSeed = privateKey;
        this.publicKey = publicKey;
    }

    /**
     * Clear the seed and public key char arrays by filling them
     * with random bytes then zero-ing them out.
     * 
     * The nkey is unusable after this operation.
     */
    public void clear() {
        if (privateKeyAsSeed != null) {
            for (int i=0; i< privateKeyAsSeed.length ; i++) {
                privateKeyAsSeed[i] = (char)(PRAND.nextInt(26) + 'a');
            }
            Arrays.fill(privateKeyAsSeed, '\0');
        }
        if (publicKey != null) {
            for (int i=0; i< publicKey.length ; i++) {
                publicKey[i] = (char)(PRAND.nextInt(26) + 'a');
            }
            Arrays.fill(publicKey, '\0');
        }
    }

    /**
     * @return the string encoded seed for this NKey
     */
    public char[] getSeed() {
        if (privateKeyAsSeed == null) {
            throw new IllegalStateException("Public-only NKey");
        }
        DecodedSeed decoded = decodeSeed(privateKeyAsSeed);
        byte[] seedBytes = new byte[ED25519_SEED_SIZE];
        System.arraycopy(decoded.bytes, 0, seedBytes, 0, seedBytes.length);
        try {
            return encodeSeed(Type.fromPrefix(decoded.prefix), seedBytes);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to create seed.", e);
        }
    }

    /**
     * @return the encoded public key for this NKey
     * 
     * @throws GeneralSecurityException if there is an encryption problem
     * @throws IOException              if there is a problem encoding the public
     *                                  key
     */
    public char[] getPublicKey() throws GeneralSecurityException, IOException {
        if (publicKey != null) {
            return publicKey;
        }

        KeyPair keys = getKeyPair();
        EdDSAPublicKey pubKey = (EdDSAPublicKey) keys.getPublic();
        byte[] pubBytes = pubKey.getAbyte();

        return encode(this.type, pubBytes);
    }

    /**
     * @return the encoded private key for this NKey
     * 
     * @throws GeneralSecurityException if there is an encryption problem
     * @throws IOException              if there is a problem encoding the key
     */
    public char[] getPrivateKey() throws GeneralSecurityException, IOException {
        if (privateKeyAsSeed == null) {
            throw new IllegalStateException("Public-only NKey");
        }

        DecodedSeed decoded = decodeSeed(privateKeyAsSeed);
        return encode(Type.PRIVATE, decoded.bytes);
    }

    /**
     * @return A Java security keypair that represents this NKey in Java security
     *         form.
     * 
     * @throws GeneralSecurityException if there is an encryption problem
     * @throws IOException              if there is a problem encoding or decoding
     */
    public KeyPair getKeyPair() throws GeneralSecurityException, IOException {
        if (privateKeyAsSeed == null) {
            throw new IllegalStateException("Public-only NKey");
        }

        DecodedSeed decoded = decodeSeed(privateKeyAsSeed);
        byte[] seedBytes = new byte[ED25519_SEED_SIZE];
        byte[] pubBytes = new byte[ED25519_PUBLIC_KEYSIZE];

        System.arraycopy(decoded.bytes, 0, seedBytes, 0, seedBytes.length);
        System.arraycopy(decoded.bytes, seedBytes.length, pubBytes, 0, pubBytes.length);

        EdDSAPrivateKeySpec privKeySpec = new EdDSAPrivateKeySpec(seedBytes, ED_25519);
        EdDSAPrivateKey privKey = new EdDSAPrivateKey(privKeySpec);
        EdDSAPublicKeySpec pubKeySpec = new EdDSAPublicKeySpec(pubBytes, ED_25519);
        EdDSAPublicKey pubKey = new EdDSAPublicKey(pubKeySpec);

        return new KeyPair(pubKey, privKey);
    }

    /**
     * @return the Type of this NKey
     */
    public Type getType() {
        return type;
    }

    /**
     * Sign aribitrary binary input.
     * 
     * @param input the bytes to sign
     * @return the signature for the input from the NKey
     * 
     * @throws GeneralSecurityException if there is an encryption problem
     * @throws IOException              if there is a problem reading the data
     */
    public byte[] sign(byte[] input) throws GeneralSecurityException, IOException {
        Signature sgr = new EdDSAEngine(MessageDigest.getInstance(ED_25519.getHashAlgorithm()));
        PrivateKey sKey = getKeyPair().getPrivate();

        sgr.initSign(sKey);
        sgr.update(input);

        return sgr.sign();
    }

    /**
     * Verify a signature.
     * 
     * @param input     the bytes that were signed
     * @param signature the bytes for the signature
     * @return true if the signature matches this keys signature for the input.
     * 
     * @throws GeneralSecurityException if there is an encryption problem
     * @throws IOException              if there is a problem reading the data
     */
    public boolean verify(byte[] input, byte[] signature) throws GeneralSecurityException, IOException {
        Signature sgr = new EdDSAEngine(MessageDigest.getInstance(ED_25519.getHashAlgorithm()));
        PublicKey sKey = null;

        if (privateKeyAsSeed != null) {
            sKey = getKeyPair().getPublic();
        } else {
            char[] encodedPublicKey = getPublicKey();
            byte[] decodedPublicKey = decode(this.type, encodedPublicKey, false);
            EdDSAPublicKeySpec pubKeySpec = new EdDSAPublicKeySpec(decodedPublicKey, ED_25519);
            sKey = new EdDSAPublicKey(pubKeySpec);
        }

        sgr.initVerify(sKey);
        sgr.update(input);

        return sgr.verify(signature);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof NKey)) {
            return false;
        }

        NKey otherNKey = (NKey) o;

        if (this.type != otherNKey.type) {
            return false;
        }

        if (this.privateKeyAsSeed == null) {
            return Arrays.equals(this.publicKey, otherNKey.publicKey);
        }

        return Arrays.equals(this.privateKeyAsSeed, otherNKey.privateKeyAsSeed);
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + this.type.prefix;

        if (this.privateKeyAsSeed == null) {
            result = 31 * result + Arrays.hashCode(this.publicKey);
        } else {
            result = 31 * result + Arrays.hashCode(this.privateKeyAsSeed);
        }
        return result;
    }

}