package edu.stanford.slac.elog_plus.migration;


import edu.stanford.slac.elog_plus.model.Logbook;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@AllArgsConstructor
@ChangeUnit(id = "fix-read-write-all", order = "7", author = "bisegni")
public class M007_FixNullOnLogbookReadAWriteAll {
    private final MongoTemplate mongoTemplate;

    @Execution
    public void changeSet() {
        //entry index
        updateReadAll();
        updateWriteAll();
    }

    @RollbackExecution
    public void rollback() {}

    /**
     * Update all the entries that have null value for the readAll field
     */
    private void updateWriteAll() {
        Query query = new Query(
                Criteria.where("writeAll").exists(false)
        );
        Update update = new Update();
        update.set("writeAll", true);
        mongoTemplate.updateMulti(query, update, Logbook.class);
    }

    /**
     * Update all the entries that have null value for the readAll field
     */
    private void updateReadAll() {
        Query query = new Query(
                Criteria.where("readAll").exists(false)
        );
        Update update = new Update();
        update.set("readAll", true);
        mongoTemplate.updateMulti(query, update, Logbook.class);
    }
}
