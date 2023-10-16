package edu.stanford.slac.elog_plus.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.stanford.slac.elog_plus.api.v1.dto.NewAuthenticationTokenDTO;
import edu.stanford.slac.elog_plus.model.AuthenticationToken;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Log4j2
@Getter
@Setter
@ConfigurationProperties(prefix = "edu.stanford.slac.elog-plus")
public class AppProperties {
    private String appTokenJwtKey;
    private String dbAdminUri;
    private String userHeaderName;
    private String imagePreviewTopic;
    private StorageProperties storage;
    private String oauthServerDiscover;
    private List<String> rootUserList = new ArrayList<>();
    private List<NewAuthenticationTokenDTO> rootAuthenticationTokenList = new ArrayList<>();
    private String rootAuthenticationTokenListJson;
    // all email that belong to this domain belongs to application toke authorization
    private String applicationTokenDomain = "elog.slac.app$";
    private String logbookEmailRegex =  ".*@.*\\.elog\\.slac\\.app\\$";

    @PostConstruct
    public void init() {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            rootAuthenticationTokenList = objectMapper.readValue(rootAuthenticationTokenListJson, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
        }
    }
}
