package edu.stanford.slac.elog_plus.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "edu.stanford.slac.elogs-plus")
public class AppProperties {
    private String dbAdminUri;
    private String userHeaderName;
    private String imagePreviewTopic;
    private StorageProperties storage;
    private String oauthServerDiscover;
    private List<String> rootUserList;
}
