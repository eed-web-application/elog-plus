package edu.stanford.slac.elog_plus.config;

import edu.stanford.slac.elog_plus.api.v1.dto.QueryParameterConfigurationDTO;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static java.util.Arrays.asList;

@Configuration
public class QueryConfiguration {
    @Bean
    public QueryParameterConfigurationDTO queryParameterConfigurationDTO() {
        return QueryParameterConfigurationDTO
                .builder()
                .logbook(
                        asList(
                                "ACCEL",
                                "PEP",
                                "MCC",
                                "NLCTA",
                                "NLCTA-LASER",
                                "XTA",
                                "LBAND-MARX",
                                "LBAND-SNS",
                                "RF",
                                "XFD",
                                "BIC",
                                "ASTA",
                                "SPPS",
                                "ITF",
                                "SPEAR3-RF",
                                "SPEAR3",
                                "SPEAR-SE",
                                "SSRL-BLDO",
                                "AMRF",
                                "PCD-PEE",
                                "PEM-FT",
                                "PEM",
                                "SW_LOG",
                                "LCLS",
                                "LCLS-II",
                                "LCLS-LASER",
                                "FACET",
                                "FACILITIES",
                                "AMG",
                                "SSRL-BLE",
                                "S0-10",
                                "BSYRECONFIG",
                                "SRFGUN",
                                "CRYO",
                                "HRS",
                                "LINAC-EAST",
                                "LTU-UND",
                                "EBD-FEE",
                                "SRF",
                                "TLOG",
                                "PHYSICS-LOG"
                        )
                )
                .build();
    }
}
