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
@Profile("!test")
@ChangeUnit(id = "root-authorization-management", order = "0", author = "bisegni", runAlways = true)
public class RootAuthorizationManagement extends MongoDDLOps {
    private final AuthService authService;
    private final MongoMappingContext mongoMappingContext;

    @Execution
    public void changeSet() {
        authService.updateRootUser();
    }

    @RollbackExecution
    public void rollback() {

    }
}
