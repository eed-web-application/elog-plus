package edu.stanford.slac.elog_plus.migration;

import edu.stanford.slac.elog_plus.api.v1.dto.NewLogbookDTO;
import edu.stanford.slac.elog_plus.model.Logbook;
import edu.stanford.slac.elog_plus.service.LogbookService;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

import java.util.List;

import static java.util.Arrays.asList;

@Log4j2
@Profile("!test")
@AllArgsConstructor
@ChangeUnit(id = "logbook-initializer", order = "2", author = "bisegni")
public class InitLogbook {
    private final LogbookService logbookService;
    private final MongoTemplate mongoTemplate;
    private final MongoMappingContext mongoMappingContext;
    public static List<String> logbookNames = asList(
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
    );
    @Execution
    public void changeSet() {
        // create logbook index
        MongoDDLOps.createIndex(Logbook.class, mongoTemplate, mongoMappingContext);

        // fill logbook
        for (String logbookName:
                logbookNames) {
            if(!logbookService.exist("MCC")) {
                String id = logbookService.createNew(
                        NewLogbookDTO
                                .builder()
                                .name(logbookName)
                                .build()
                );
                log.info("Initialized logbook {} with id {}", logbookName, id);
            } else {
                log.info("Logbook {} already exists", logbookName);
            }

        }
    }

    @RollbackExecution
    public void rollback() {

    }
}
