// Copyright 2021-2023 The NATS Authors
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

package io.nats.client.support;

import io.nats.client.NKey;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.List;

import static io.nats.client.support.JsonWriteUtils.beginJson;
import static io.nats.client.support.JsonWriteUtils.endJson;

/**
 * Implements <a href="https://github.com/nats-io/nats-architecture-and-design/blob/main/adr/ADR-14.md">ADR-14</a>
 * @deprecated This class has been extracted to the io.nats.jwt.JwtUtils class in the <a href="https://github.com/nats-io/jwt.java">jwt.java</a> library.
 */
@Deprecated
public abstract class JwtUtils {

    private JwtUtils() {} /* ensures cannot be constructed */

    /**
     * Format string with `%s` placeholder for the JWT token followed
     * by the user NKey seed. This can be directly used as such:
     * 
     * <pre>
     * NKey userKey = NKey.createUser(new SecureRandom());
     * NKey signingKey = loadFromSecretStore();
     * String jwt = issueUserJWT(signingKey, accountId, new String(userKey.getPublicKey()));
     * String.format(NATS_USER_JWT_FORMAT, jwt, new String(userKey.getSeed()));
     * </pre>
     */
    @Deprecated
    public static final String NATS_USER_JWT_FORMAT = io.nats.jwt.JwtUtils.NATS_USER_JWT_FORMAT;

    /**
     * Get the current time in seconds since epoch. Used for issue time.
     * @return the time
     */
    @Deprecated
    public static long currentTimeSeconds() {
        return System.currentTimeMillis() / 1000;
    }

    /**
     * Issue a user JWT from a scoped signing key. See <a href="https://docs.nats.io/nats-tools/nsc/signing_keys">Signing Keys</a>
     * @param signingKey a mandatory account nkey pair to sign the generated jwt.
     * @param accountId a mandatory public account nkey. Will throw error when not set or not account nkey.
     * @param publicUserKey a mandatory public user nkey. Will throw error when not set or not user nkey.
     * @throws IllegalArgumentException if the accountId or publicUserKey is not a valid public key of the proper type
     * @throws NullPointerException if signingKey, accountId, or publicUserKey are null.
     * @throws GeneralSecurityException if SHA-256 MessageDigest is missing, or if the signingKey can not be used for signing.
     * @throws IOException if signingKey sign method throws this exception.
     * @return a JWT
     */
    @Deprecated
    public static String issueUserJWT(NKey signingKey, String accountId, String publicUserKey) throws GeneralSecurityException, IOException {
        return io.nats.jwt.JwtUtils.issueUserJWT(signingKey, publicUserKey, null, null, io.nats.jwt.JwtUtils.currentTimeSeconds(), null, new io.nats.jwt.UserClaim(accountId));
    }

    /**
     * Issue a user JWT from a scoped signing key. See <a href="https://docs.nats.io/nats-tools/nsc/signing_keys">Signing Keys</a>
     * @param signingKey a mandatory account nkey pair to sign the generated jwt.
     * @param accountId a mandatory public account nkey. Will throw error when not set or not account nkey.
     * @param publicUserKey a mandatory public user nkey. Will throw error when not set or not user nkey.
     * @param name optional human-readable name. When absent, default to publicUserKey.
     * @throws IllegalArgumentException if the accountId or publicUserKey is not a valid public key of the proper type
     * @throws NullPointerException if signingKey, accountId, or publicUserKey are null.
     * @throws GeneralSecurityException if SHA-256 MessageDigest is missing, or if the signingKey can not be used for signing.
     * @throws IOException if signingKey sign method throws this exception.
     * @return a JWT
     */
    @Deprecated
    public static String issueUserJWT(NKey signingKey, String accountId, String publicUserKey, String name) throws GeneralSecurityException, IOException {
        return io.nats.jwt.JwtUtils.issueUserJWT(signingKey, publicUserKey, name, null, io.nats.jwt.JwtUtils.currentTimeSeconds(), null, new io.nats.jwt.UserClaim(accountId));
    }

    /**
     * Issue a user JWT from a scoped signing key. See <a href="https://docs.nats.io/nats-tools/nsc/signing_keys">Signing Keys</a>
     * @param signingKey a mandatory account nkey pair to sign the generated jwt.
     * @param accountId a mandatory public account nkey. Will throw error when not set or not account nkey.
     * @param publicUserKey a mandatory public user nkey. Will throw error when not set or not user nkey.
     * @param name optional human-readable name. When absent, default to publicUserKey.
     * @param expiration optional but recommended duration, when the generated jwt needs to expire. If not set, JWT will not expire.
     * @param tags optional list of tags to be included in the JWT.
     * @throws IllegalArgumentException if the accountId or publicUserKey is not a valid public key of the proper type
     * @throws NullPointerException if signingKey, accountId, or publicUserKey are null.
     * @throws GeneralSecurityException if SHA-256 MessageDigest is missing, or if the signingKey can not be used for signing.
     * @throws IOException if signingKey sign method throws this exception.
     * @return a JWT
     */
    @Deprecated
    public static String issueUserJWT(NKey signingKey, String accountId, String publicUserKey, String name, Duration expiration, String... tags) throws GeneralSecurityException, IOException {
        return io.nats.jwt.JwtUtils.issueUserJWT(signingKey, accountId, publicUserKey, name, expiration, tags, null, null);
    }

    /**
     * Issue a user JWT from a scoped signing key. See <a href="https://docs.nats.io/nats-tools/nsc/signing_keys">Signing Keys</a>
     * @param signingKey a mandatory account nkey pair to sign the generated jwt.
     * @param accountId a mandatory public account nkey. Will throw error when not set or not account nkey.
     * @param publicUserKey a mandatory public user nkey. Will throw error when not set or not user nkey.
     * @param name optional human-readable name. When absent, default to publicUserKey.
     * @param expiration optional but recommended duration, when the generated jwt needs to expire. If not set, JWT will not expire.
     * @param tags optional list of tags to be included in the JWT.
     * @param issuedAt the current epoch seconds.
     * @throws IllegalArgumentException if the accountId or publicUserKey is not a valid public key of the proper type
     * @throws NullPointerException if signingKey, accountId, or publicUserKey are null.
     * @throws GeneralSecurityException if SHA-256 MessageDigest is missing, or if the signingKey can not be used for signing.
     * @throws IOException if signingKey sign method throws this exception.
     * @return a JWT
     */
    @Deprecated
    public static String issueUserJWT(NKey signingKey, String accountId, String publicUserKey, String name, Duration expiration, String[] tags, long issuedAt) throws GeneralSecurityException, IOException {
        return io.nats.jwt.JwtUtils.issueUserJWT(signingKey, accountId, publicUserKey, name, expiration, tags, issuedAt, null);
    }

    /**
     * Issue a user JWT from a scoped signing key. See <a href="https://docs.nats.io/nats-tools/nsc/signing_keys">Signing Keys</a>
     * @param signingKey a mandatory account nkey pair to sign the generated jwt.
     * @param accountId a mandatory public account nkey. Will throw error when not set or not account nkey.
     * @param publicUserKey a mandatory public user nkey. Will throw error when not set or not user nkey.
     * @param name optional human-readable name. When absent, default to publicUserKey.
     * @param expiration optional but recommended duration, when the generated jwt needs to expire. If not set, JWT will not expire.
     * @param tags optional list of tags to be included in the JWT.
     * @param issuedAt the current epoch seconds.
     * @param audience the optional audience
     * @throws IllegalArgumentException if the accountId or publicUserKey is not a valid public key of the proper type
     * @throws NullPointerException if signingKey, accountId, or publicUserKey are null.
     * @throws GeneralSecurityException if SHA-256 MessageDigest is missing, or if the signingKey can not be used for signing.
     * @throws IOException if signingKey sign method throws this exception.
     * @return a JWT
     */
    @Deprecated
    public static String issueUserJWT(NKey signingKey, String accountId, String publicUserKey, String name, Duration expiration, String[] tags, long issuedAt, String audience) throws GeneralSecurityException, IOException {
        return io.nats.jwt.JwtUtils.issueUserJWT(signingKey, accountId, publicUserKey, name, expiration, tags, issuedAt, audience);
    }

    /**
     * Issue a user JWT from a scoped signing key. See <a href="https://docs.nats.io/nats-tools/nsc/signing_keys">Signing Keys</a>
     * @param signingKey a mandatory account nkey pair to sign the generated jwt.
     * @param publicUserKey a mandatory public user nkey. Will throw error when not set or not user nkey.
     * @param name optional human-readable name. When absent, default to publicUserKey.
     * @param expiration optional but recommended duration, when the generated jwt needs to expire. If not set, JWT will not expire.
     * @param issuedAt the current epoch seconds.
     * @param nats the user claim
     * @throws IllegalArgumentException if the accountId or publicUserKey is not a valid public key of the proper type
     * @throws NullPointerException if signingKey, accountId, or publicUserKey are null.
     * @throws GeneralSecurityException if SHA-256 MessageDigest is missing, or if the signingKey can not be used for signing.
     * @throws IOException if signingKey sign method throws this exception.
     * @return a JWT
     */
    @Deprecated
    public static String issueUserJWT(NKey signingKey, String publicUserKey, String name, Duration expiration, long issuedAt, UserClaim nats) throws GeneralSecurityException, IOException {
        return issueUserJWT(signingKey, publicUserKey, name, expiration, issuedAt, null, nats);
    }

    /**
     * Issue a user JWT from a scoped signing key. See <a href="https://docs.nats.io/nats-tools/nsc/signing_keys">Signing Keys</a>
     * @param signingKey a mandatory account nkey pair to sign the generated jwt.
     * @param publicUserKey a mandatory public user nkey. Will throw error when not set or not user nkey.
     * @param name optional human-readable name. When absent, default to publicUserKey.
     * @param expiration optional but recommended duration, when the generated jwt needs to expire. If not set, JWT will not expire.
     * @param issuedAt the current epoch seconds.
     * @param audience the optional audience
     * @param nats the user claim
     * @throws IllegalArgumentException if the accountId or publicUserKey is not a valid public key of the proper type
     * @throws NullPointerException if signingKey, accountId, or publicUserKey are null.
     * @throws GeneralSecurityException if SHA-256 MessageDigest is missing, or if the signingKey can not be used for signing.
     * @throws IOException if signingKey sign method throws this exception.
     * @return a JWT
     */
    @Deprecated
    public static String issueUserJWT(NKey signingKey, String publicUserKey, String name, Duration expiration, long issuedAt, String audience, UserClaim nats) throws GeneralSecurityException, IOException {
        // Validate the signingKey:
        if (signingKey.getType() != NKey.Type.ACCOUNT) {
            throw new IllegalArgumentException("issueUserJWT requires an account key for the signingKey parameter, but got " + signingKey.getType());
        }
        // Validate the accountId:
        NKey accountKey = NKey.fromPublicKey(nats.issuerAccount.toCharArray());
        if (accountKey.getType() != NKey.Type.ACCOUNT) {
            throw new IllegalArgumentException("issueUserJWT requires an account key for the accountId parameter, but got " + accountKey.getType());
        }
        // Validate the publicUserKey:
        NKey userKey = NKey.fromPublicKey(publicUserKey.toCharArray());
        if (userKey.getType() != NKey.Type.USER) {
            throw new IllegalArgumentException("issueUserJWT requires a user key for the publicUserKey parameter, but got " + userKey.getType());
        }
        String accSigningKeyPub = new String(signingKey.getPublicKey());

        String claimName = Validator.nullOrEmpty(name) ? publicUserKey : name;

        return io.nats.jwt.JwtUtils.issueJWT(signingKey, publicUserKey, claimName, expiration, issuedAt, accSigningKeyPub, audience, nats);
    }

    /**
     * Issue a JWT
     * @param signingKey account nkey pair to sign the generated jwt.
     * @param publicUserKey a mandatory public user nkey.
     * @param name optional human-readable name.
     * @param expiration optional but recommended duration, when the generated jwt needs to expire. If not set, JWT will not expire.
     * @param issuedAt the current epoch seconds.
     * @param accSigningKeyPub the account signing key
     * @param nats the generic nats claim
     * @throws GeneralSecurityException if SHA-256 MessageDigest is missing, or if the signingKey can not be used for signing.
     * @throws IOException if signingKey sign method throws this exception.
     * @return a JWT
     */
    @Deprecated
    public static String issueJWT(NKey signingKey, String publicUserKey, String name, Duration expiration, long issuedAt, String accSigningKeyPub, JsonSerializable nats) throws GeneralSecurityException, IOException {
        return io.nats.jwt.JwtUtils.issueJWT(signingKey, publicUserKey, name, expiration, issuedAt, accSigningKeyPub, null, nats);
    }

    /**
     * Issue a JWT
     *
     * @param signingKey       account nkey pair to sign the generated jwt.
     * @param publicUserKey    a mandatory public user nkey.
     * @param name             optional human-readable name.
     * @param expiration       optional but recommended duration, when the generated jwt needs to expire. If not set, JWT will not expire.
     * @param issuedAt         the current epoch seconds.
     * @param accSigningKeyPub the account signing key
     * @param audience         the optional audience
     * @param nats             the generic nats claim
     * @return a JWT
     * @throws GeneralSecurityException if SHA-256 MessageDigest is missing, or if the signingKey can not be used for signing.
     * @throws IOException              if signingKey sign method throws this exception.
     */
    @Deprecated
    public static String issueJWT(NKey signingKey, String publicUserKey, String name, Duration expiration, long issuedAt, String accSigningKeyPub, String audience, JsonSerializable nats) throws GeneralSecurityException, IOException {
        return io.nats.jwt.JwtUtils.issueJWT(signingKey, publicUserKey, name, expiration, issuedAt, accSigningKeyPub, audience, nats);
    }

    /**
     * Get the claim body from a JWT
     * @param jwt the encoded jwt
     * @return the claim body json
     */
    @Deprecated
    public static String getClaimBody(String jwt) {
        return io.nats.jwt.JwtUtils.getClaimBody(jwt);
    }

    @Deprecated
    public static class UserClaim implements JsonSerializable {
        public String issuerAccount;            // User
        public String[] tags;                   // User/GenericFields
        public String type = "user";            // User/GenericFields
        public int version = 2;                 // User/GenericFields
        public Permission pub;                  // User/UserPermissionLimits/Permissions
        public Permission sub;                  // User/UserPermissionLimits/Permissions
        public ResponsePermission resp;         // User/UserPermissionLimits/Permissions
        public String[] src;                    // User/UserPermissionLimits/Limits/UserLimits
        public List<TimeRange> times;           // User/UserPermissionLimits/Limits/UserLimits
        public String locale;                   // User/UserPermissionLimits/Limits/UserLimits
        public long subs = -1;                  // User/UserPermissionLimits/Limits/NatsLimits
        public long data = -1;                  // User/UserPermissionLimits/Limits/NatsLimits
        public long payload = -1;               // User/UserPermissionLimits/Limits/NatsLimits
        public boolean bearerToken;             // User/UserPermissionLimits
        public String[] allowedConnectionTypes; // User/UserPermissionLimits

        public UserClaim(String issuerAccount) {
            this.issuerAccount = issuerAccount;
        }

        @Override
        public String toJson() {
            StringBuilder sb = beginJson();
            JsonWriteUtils.addField(sb, "issuer_account", issuerAccount);
            JsonWriteUtils.addStrings(sb, "tags", tags);
            JsonWriteUtils.addField(sb, "type", type);
            JsonWriteUtils.addField(sb, "version", version);
            JsonWriteUtils.addField(sb, "pub", pub);
            JsonWriteUtils.addField(sb, "sub", sub);
            JsonWriteUtils.addField(sb, "resp", resp);
            JsonWriteUtils.addStrings(sb, "src", src);
            JsonWriteUtils.addJsons(sb, "times", times);
            JsonWriteUtils.addField(sb, "times_location", locale);
            JsonWriteUtils.addFieldWhenGteMinusOne(sb, "subs", subs);
            JsonWriteUtils.addFieldWhenGteMinusOne(sb, "data", data);
            JsonWriteUtils.addFieldWhenGteMinusOne(sb, "payload", payload);
            JsonWriteUtils.addFldWhenTrue(sb, "bearer_token", bearerToken);
            JsonWriteUtils.addStrings(sb, "allowed_connection_types", allowedConnectionTypes);
            return endJson(sb).toString();
        }

        public UserClaim tags(String... tags) {
            this.tags = tags;
            return this;
        }

        public UserClaim pub(Permission pub) {
            this.pub = pub;
            return this;
        }

        public UserClaim sub(Permission sub) {
            this.sub = sub;
            return this;
        }

        public UserClaim resp(ResponsePermission resp) {
            this.resp = resp;
            return this;
        }

        public UserClaim src(String... src) {
            this.src = src;
            return this;
        }

        public UserClaim times(List<TimeRange> times) {
            this.times = times;
            return this;
        }

        public UserClaim locale(String locale) {
            this.locale = locale;
            return this;
        }

        public UserClaim subs(long subs) {
            this.subs = subs;
            return this;
        }

        public UserClaim data(long data) {
            this.data = data;
            return this;
        }

        public UserClaim payload(long payload) {
            this.payload = payload;
            return this;
        }

        public UserClaim bearerToken(boolean bearerToken) {
            this.bearerToken = bearerToken;
            return this;
        }

        public UserClaim allowedConnectionTypes(String... allowedConnectionTypes) {
            this.allowedConnectionTypes = allowedConnectionTypes;
            return this;
        }
    }

    @Deprecated
    public static class TimeRange implements JsonSerializable {
        public String start;
        public String end;

        public TimeRange(String start, String end) {
            this.start = start;
            this.end = end;
        }

        @Override
        public String toJson() {
            StringBuilder sb = beginJson();
            JsonWriteUtils.addField(sb, "start", start);
            JsonWriteUtils.addField(sb, "end", end);
            return endJson(sb).toString();
        }
    }

    @Deprecated
    public static class ResponsePermission implements JsonSerializable {
        public int maxMsgs;
        public Duration expires;

        public ResponsePermission maxMsgs(int maxMsgs) {
            this.maxMsgs = maxMsgs;
            return this;
        }

        public ResponsePermission expires(Duration expires) {
            this.expires = expires;
            return this;
        }

        public ResponsePermission expires(long expiresMillis) {
            this.expires = Duration.ofMillis(expiresMillis);
            return this;
        }

        @Override
        public String toJson() {
            StringBuilder sb = beginJson();
            JsonWriteUtils.addField(sb, "max", maxMsgs);
            JsonWriteUtils.addFieldAsNanos(sb, "ttl", expires);
            return endJson(sb).toString();
        }
    }

    public static class Permission implements JsonSerializable {
        public String[] allow;
        public String[] deny;

        public Permission allow(String... allow) {
            this.allow = allow;
            return this;
        }

        public Permission deny(String... deny) {
            this.deny = deny;
            return this;
        }

        @Override
        public String toJson() {
            StringBuilder sb = beginJson();
            JsonWriteUtils.addStrings(sb, "allow", allow);
            JsonWriteUtils.addStrings(sb, "deny", deny);
            return endJson(sb).toString();
        }
    }

    @Deprecated
    static class Claim implements JsonSerializable {
        String aud;
        String jti;
        long iat;
        String iss;
        String name;
        String sub;
        Duration exp;
        JsonSerializable nats;

        @Override
        public String toJson() {
            StringBuilder sb = beginJson();
            JsonWriteUtils.addField(sb, "aud", aud);
            JsonWriteUtils.addFieldEvenEmpty(sb, "jti", jti);
            JsonWriteUtils.addField(sb, "iat", iat);
            JsonWriteUtils.addField(sb, "iss", iss);
            JsonWriteUtils.addField(sb, "name", name);
            JsonWriteUtils.addField(sb, "sub", sub);

            if (exp != null && !exp.isZero() && !exp.isNegative()) {
                long seconds = exp.toMillis() / 1000;
                JsonWriteUtils.addField(sb, "exp", iat + seconds); // relative to the iat
            }

            JsonWriteUtils.addField(sb, "nats", nats);
            return endJson(sb).toString();
        }
    }
}
