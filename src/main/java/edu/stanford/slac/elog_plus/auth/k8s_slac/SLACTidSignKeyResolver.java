package edu.stanford.slac.elog_plus.auth.k8s_slac;

import edu.stanford.slac.elog_plus.auth.OIDCConfiguration;
import edu.stanford.slac.elog_plus.auth.OIDCKeysDescription;
import edu.stanford.slac.elog_plus.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.SigningKeyResolverAdapter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static edu.stanford.slac.elog_plus.exception.Utility.assertion;

@Log4j2
@Component
@Profile("!test")
public class SLACTidSignKeyResolver extends SigningKeyResolverAdapter {
    private final AppProperties appProperties;
    private final RestTemplate restTemplate = new RestTemplate();
    private Map<String, OIDCKeysDescription.Key> stringKeyMap = null;
    private OIDCConfiguration oidcConfiguration;

    public SLACTidSignKeyResolver(AppProperties appProperties){
        this.appProperties = appProperties;
    }

    @Override
    public Key resolveSigningKey(JwsHeader header, Claims claims) {
        try {
            if(claims.containsKey("email")) {
                log.debug("Validate jwt token for: %s".formatted(claims.get("email")));
            }

            OIDCKeysDescription.Key key = getKeyByID(header.getKeyId());
            if (key == null) {
                clearCache();
                key = getKeyByID(header.getKeyId());
            }
            if (key == null) {
                throw new BadCredentialsException("No key found jwt verification using the id: %s".formatted(header.getKeyId()));
            }
            return key.getKey();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return the OIDC configuration of the remote server
     */
    synchronized private OIDCConfiguration getOIDCConfiguration() {
        if (oidcConfiguration == null) {
            // Send a GET request to the OIDC configuration URL and map the response to OIDCConfiguration class
            ResponseEntity<OIDCConfiguration> responseEntity = restTemplate.getForEntity(appProperties.getOauthServerDiscover(), OIDCConfiguration.class);
            // Check if the request was successful (HTTP status code 200)
            if (responseEntity.getStatusCode().is2xxSuccessful()) {
                oidcConfiguration = responseEntity.getBody();
            } else {
                // Handle the error, e.g., log an error message or throw an exception
                throw new RuntimeException("Failed to fetch OIDC configuration");
            }
        }
        return oidcConfiguration;
    }

    /**
     *
     */
    synchronized private Map<String, OIDCKeysDescription.Key> getOIDCKeys(String keysUrl) {
        if (stringKeyMap == null) {
            stringKeyMap = new HashMap<>();
            ResponseEntity<OIDCKeysDescription> responseEntity = restTemplate.getForEntity(keysUrl, OIDCKeysDescription.class);
            ;
            if (responseEntity.getStatusCode().is2xxSuccessful()) {
                Objects.requireNonNull(responseEntity.getBody()).getKeys().forEach(
                        k -> {
                            stringKeyMap.put(k.getKeyId(), k);
                        }
                );
                return stringKeyMap;
            } else {
                // Handle the error, e.g., log an error message or throw an exception
                throw new RuntimeException("Failed to fetch OIDC configuration");
            }
        }
        return stringKeyMap;
    }

    synchronized private OIDCKeysDescription.Key getKeyByID(String keyId) {
        OIDCKeysDescription.Key key = null;
        OIDCConfiguration conf = getOIDCConfiguration();
        if (conf.getJwksUri() != null) {
            Map<String, OIDCKeysDescription.Key> keys = getOIDCKeys(conf.getJwksUri());
            if (keys.containsKey(keyId)) {
                key = keys.getOrDefault(keyId, null);
            }
        }
        return key;
    }

    synchronized private void clearCache() {
        oidcConfiguration = null;
        stringKeyMap = null;
    }
}
