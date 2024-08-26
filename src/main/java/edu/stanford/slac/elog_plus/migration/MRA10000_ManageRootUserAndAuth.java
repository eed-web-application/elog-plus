package edu.stanford.slac.elog_plus.migration;

import edu.stanford.slac.ad.eed.base_mongodb_lib.utility.RootAuthorizationManagement;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import lombok.AllArgsConstructor;

/**
 * This will always run at application startup
 * updating the root user and authorization
 *
 */
@AllArgsConstructor
@ChangeUnit(id = "manage-root-user-token-auth", order = "10000", author = "bisegni", runAlways = true)
public class MRA10000_ManageRootUserAndAuth {
    private final AuthService authService;
    @Execution
    public void changeSet() {
        RootAuthorizationManagement
                .builder()
                .authService(authService)
                .build()
                .updateRootAuthorization();
    }

    @RollbackExecution
    public void rollback() {

    }
}
