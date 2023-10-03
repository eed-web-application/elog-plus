package edu.stanford.slac.elog_plus.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

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
    private List<String> rootUserList;
    // all email that belong to this domain belongs to application toke authorization
    private String applicationTokenDomain = "elog.slac.app$";
    private String logbookEmailRegex =  ".*@.*\\.elog\\.slac\\.app\\$";
}
