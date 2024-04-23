package edu.stanford.slac.elog_plus.config;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Log4j2
@Getter
@Setter
@ConfigurationProperties(prefix = "edu.stanford.slac.elog-plus")
public class ELOGAppProperties {
    private String imagePreviewTopic;
    private String importEntryTopic;
    private StorageProperties storage;
}
