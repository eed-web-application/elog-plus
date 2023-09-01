package edu.stanford.slac.elog_plus.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class OIDCConfiguration {

    @JsonProperty("issuer")
    private String issuer;

    @JsonProperty("jwks_uri")
    private String jwksUri;
}
