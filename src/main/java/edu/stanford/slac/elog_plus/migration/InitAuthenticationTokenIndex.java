package edu.stanford.slac.elog_plus.migration;

import edu.stanford.slac.elog_plus.model.AuthenticationToken;
import edu.stanford.slac.elog_plus.model.Authorization;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

@AllArgsConstructor
@ChangeUnit(id = "init-authentication-token-index", order = "8", author = "bisegni")
public class InitAuthenticationTokenIndex extends MongoDDLOps {
    private final MongoTemplate mongoTemplate;
    private final MongoMappingContext mongoMappingContext;

    @Execution
    public void changeSet() {
        MongoDDLOps.createIndex(
                AuthenticationToken.class,
                mongoTemplate,
                new Index()
                        .on(
                                "name",
                                Sort.Direction.ASC
                        )
                        .unique()
                        .named("name")
        );
    }

    @RollbackExecution
    public void rollback() {

    }
}
