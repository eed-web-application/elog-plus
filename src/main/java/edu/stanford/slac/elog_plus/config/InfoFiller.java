package edu.stanford.slac.elog_plus.config;

import edu.stanford.slac.elog_plus.api.v1.dto.InfoDTO;
import lombok.AllArgsConstructor;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;


@Component
@AllArgsConstructor
public class InfoFiller implements InfoContributor {
    private BuildProperties buildProperties;
    @Override
    public void contribute(Info.Builder builder) {
//        InfoDTO info = InfoDTO
//                .builder()
//                .name(buildProperties.getName())
//                .version(buildProperties.getVersion())
//                .build();
//
//        builder.withDetail("app", info);
    }
}