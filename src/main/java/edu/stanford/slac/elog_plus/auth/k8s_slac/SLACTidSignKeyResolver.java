package edu.stanford.slac.elog_plus.auth.k8s_slac;

import edu.stanford.slac.elog_plus.auth.OIDCKeysDescription;
import edu.stanford.slac.elog_plus.auth.OIDCConfiguration;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.SigningKeyResolver;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.net.MalformedURLException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Builder
@AllArgsConstructor
public class SLACTidSignKeyResolver implements SigningKeyResolver {
    private String discoverUrl;
    private RestTemplate restTemplate;

    @Value("${security.oauth2.resourceserver.jwt.jwk-set-uri}")
    @Override
    public Key resolveSigningKey(JwsHeader header, Claims claims) {
        OIDCKeysDescription.Key key = getKeyByID(header.getKeyId());
        try {
            return key.getKey();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Key resolveSigningKey(JwsHeader header, String plaintext) {
        return null;
    }

    private OIDCConfiguration getOIDCConfiguration(String discoverUrl) {
        // Send a GET request to the OIDC configuration URL and map the response to OIDCConfiguration class
        ResponseEntity<OIDCConfiguration> responseEntity = restTemplate.getForEntity(discoverUrl, OIDCConfiguration.class);
        // Check if the request was successful (HTTP status code 200)
        if (responseEntity.getStatusCode().is2xxSuccessful()) {
            return responseEntity.getBody();
        } else {
            // Handle the error, e.g., log an error message or throw an exception
            throw new RuntimeException("Failed to fetch OIDC configuration");
        }
    }

    private Map<String, OIDCKeysDescription.Key> getOIDCKeys(String keysUrl) {
        Map<String, OIDCKeysDescription.Key> keysMap = new HashMap<>();
        ResponseEntity<OIDCKeysDescription> responseEntity = restTemplate.getForEntity(keysUrl, OIDCKeysDescription.class);;
        if (responseEntity.getStatusCode().is2xxSuccessful()) {
            Objects.requireNonNull(responseEntity.getBody()).getKeys().forEach(
                    k-> {
                        keysMap.put(k.getKeyId(), k);
                    }
            );
            return keysMap;
        } else {
            // Handle the error, e.g., log an error message or throw an exception
            throw new RuntimeException("Failed to fetch OIDC configuration");
        }
    }

    private OIDCKeysDescription.Key getKeyByID(String keyId) {
        OIDCKeysDescription.Key key = null;
        OIDCConfiguration conf = getOIDCConfiguration(discoverUrl);
        if(conf.getJwksUri()!=null) {
            Map<String, OIDCKeysDescription.Key> keys = getOIDCKeys(conf.getJwksUri());
            key = keys.getOrDefault(keyId, null);
        }
        return key;
    }
}
