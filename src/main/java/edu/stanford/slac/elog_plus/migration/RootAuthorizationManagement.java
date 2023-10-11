package edu.stanford.slac.elog_plus.migration;

import edu.stanford.slac.elog_plus.service.AuthService;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;


@Log4j2
@AllArgsConstructor
@Profile("manage-root-user")
@ChangeUnit(id = "root-authorizations-management", order = "0", author = "bisegni", runAlways = true)
public class RootAuthorizationManagement extends MongoDDLOps {
    private final AuthService authService;
    private final MongoMappingContext mongoMappingContext;

    @Execution
    public void changeSet() {
        try {
            log.info("Start managing root user");
            authService.updateRootUser();
        } catch (RuntimeException ex) {
            log.error("Error during root user management: {}", ex.toString());
        }
        try {
            log.info("Start managing root authentication token");
            authService.updateAutoManagedRootToken();
        } catch (RuntimeException ex) {
            log.error("Error during root token management: {}", ex.toString());
        }
    }

    @RollbackExecution
    public void rollback() {

    }
}
