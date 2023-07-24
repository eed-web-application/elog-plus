package edu.stanford.slac.elog_plus.repository;

import com.mongodb.client.result.UpdateResult;
import edu.stanford.slac.elog_plus.exception.LogbookNotFound;
import edu.stanford.slac.elog_plus.model.Logbook;
import edu.stanford.slac.elog_plus.model.Tag;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static edu.stanford.slac.elog_plus.exception.Utility.assertion;

@Repository
@AllArgsConstructor
public class LogbookRepositoryImpl implements LogbookRepositoryCustom {
    final private MongoTemplate mongoTemplate;

    @Override
    public String createNewTag(String logbookId, Tag newTag) {
        newTag.setId(
                UUID.randomUUID().toString()
        );
        Query q = new Query();
        q.addCriteria(
                Criteria.where("id").is(logbookId)
        );

        Update u = new Update();
        u.addToSet(
                "tags",
                newTag
        );
        UpdateResult ur = mongoTemplate.updateFirst(
                q,
                u,
                Logbook.class
        );
        return ur.getModifiedCount() == 1 ? newTag.getId() : null;
    }

    @Override
    public List<Tag> getAllTagFor(String logbookId) {
        Query q = new Query();
        q.addCriteria(
                Criteria.where("id").is(logbookId)
        );
        q.fields().include("tags");
        Logbook lb = mongoTemplate.findOne(q, Logbook.class);
        assertion(
                () -> lb != null,
                LogbookNotFound.logbookNotFoundBuilder()
                        .errorCode(-1)
                        .errorDomain("LogbookRepositoryImpl:getAllTagFor")
                        .build()
        );
        return Objects.requireNonNull(lb).getTags();
    }

    @Override
    public boolean tagExistByName(String logbookId, String tagName) {
        Query query = new Query();
        query.addCriteria(
                Criteria
                        .where("id").is(logbookId)
                        .and("tags").elemMatch(
                                Criteria.where("name").is(tagName)
                        )
        );

        Logbook l = mongoTemplate.findOne(query, Logbook.class);
        if (l != null) {
            return l.getTags().size() == 1;
        } else {
            return false;
        }
    }

    @Override
    public List<String> getAllLogbook() {
        List<String> result = new ArrayList<>();
        Query query = new Query();
        query.fields().include("name");
        List<Logbook> l = mongoTemplate.find(query, Logbook.class);
        for (Logbook lb:
             l) {
            result.add(lb.getName());
        }
        return result;
    }
}
