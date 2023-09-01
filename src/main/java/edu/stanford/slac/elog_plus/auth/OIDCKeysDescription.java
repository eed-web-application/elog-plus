package edu.stanford.slac.elog_plus.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;

@Data
public class OIDCKeysDescription {
    @JsonProperty("keys")
    private List<Key> keys;

    @Getter
    static public class Key {
        @JsonProperty("kid")
        private String keyId;
        @JsonProperty("kty")
        private String keyType;
        @JsonProperty("alg")
        private String algorithm;
        @JsonProperty("n")
        private String modulus;

        @JsonProperty("e")
        private String exponent;

        public PublicKey getKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
            byte[] modulusBytes = Base64.getUrlDecoder().decode(modulus);
            byte[] exponentBytes = Base64.getUrlDecoder().decode(exponent);
            return KeyFactory.getInstance("RSA")
                    .generatePublic(
                            new RSAPublicKeySpec(
                                    new java.math.BigInteger(1, modulusBytes),
                                    new java.math.BigInteger(1, exponentBytes)
                            )
                    );
        }
    }
}

