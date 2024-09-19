package edu.stanford.slac.elog_plus.repository;

import com.mongodb.client.result.UpdateResult;
import edu.stanford.slac.elog_plus.model.Attachment;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.BooleanOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;


@Log4j2
@Repository
@AllArgsConstructor
public class AttachmentRepositoryImpl implements AttachmentRepositoryCustom {
    private static final long PROCESSING_TIMEOUT = 60000; // 60 seconds
    final private MongoTemplate mongoTemplate;

    @Override
    public void setPreviewID(String id, String previewID) {
        Query q = new Query();
        q.addCriteria(
                Criteria.where("id").is(id)
        );
        Update u = new Update();
        u.set("previewID", previewID);
        UpdateResult ur = mongoTemplate.updateFirst(q, u, Attachment.class);
        log.debug("Set preview id update operation {}", ur.getModifiedCount() == 1);
    }

    @Override
    public void setMiniPreview(String id, byte[] byteArray) {
        Query q = new Query();
        q.addCriteria(
                Criteria.where("id").is(id)
        );
        Update u = new Update();
        u.set("miniPreview", byteArray);
        UpdateResult ur = mongoTemplate.updateFirst(q, u, Attachment.class);
        log.debug("Set mini preview update operation {}", ur.getModifiedCount() == 1);
    }

    @Override
    public void setPreviewState(String id, Attachment.PreviewProcessingState state) {
        Query q = new Query();
        q.addCriteria(
                Criteria.where("id").is(id)
        );
        Update u = new Update();
        u.set("previewState", state);

        UpdateResult ur = mongoTemplate.updateFirst(q, u, Attachment.class);
        log.debug("Set preview state update operation {}", ur.getModifiedCount() == 1);
    }

    @Override
    public Attachment.PreviewProcessingState getPreviewState(String id) {
        Query q = new Query();
        q.addCriteria(
                Criteria.where("id").is(id)
        ).fields().include("previewState");
        var a = Optional.ofNullable(
                mongoTemplate.findOne(q, Attachment.class)
        );
        return a.orElseThrow().getPreviewState();
    }

    @Override
    public void setInUseState(String id, Boolean inUse) {
        Query q = new Query();
        q.addCriteria(
                Criteria.where("id").is(id)
        );
        Update u = new Update();
        u.set("inUse", inUse);

        UpdateResult ur = mongoTemplate.updateFirst(q, u, Attachment.class);
        log.debug("Set 'in use' state update operation {}", ur.getModifiedCount() == 1);
    }

    @Override
    public void removeReferenceInfoOnAllInUseAndExpired(String referenceInfo, LocalDateTime expirationTime) {
        Query q = new Query();
        q.addCriteria(
                Criteria.where("referenceInfo").is(referenceInfo)
        );
        q.addCriteria(
                Criteria.where("inUse").is(true)
        );
        q.addCriteria(
                Criteria.where("createdDate").lt(expirationTime)
        );
        Update u = new Update();
        u.unset("referenceInfo");
        UpdateResult ur = mongoTemplate.updateMulti(q, u, Attachment.class);
        log.debug("Remove reference info update operation {}", ur.getModifiedCount());
    }

    @Override
    public Attachment findAndUpdateNextAvailableModel(Integer expirationMinutes, Integer processingTimeoutMinutes) {
        // Calculate the expiration date for createdDate
        Instant expirationInstant = Instant.now().minus(expirationMinutes, ChronoUnit.MINUTES);
        Date expirationDate = Date.from(expirationInstant);

        // Calculate the timeout threshold for processingTimestamp
        Instant timeoutInstant = Instant.now().minus(processingTimeoutMinutes, ChronoUnit.MINUTES);
        Date timeoutDate = Date.from(timeoutInstant);

        // Build the criteria
        Criteria criteria = new Criteria().andOperator(
                Criteria.where("createdDate").lte(expirationDate),
                Criteria.where("inUse").is(false),
                new Criteria().orOperator(
                        Criteria.where("processingId").exists(false),
                        Criteria.where("processingId").is(null),
                        Criteria.where("processingTimestamp").lte(timeoutDate)
                ),
                new Criteria().orOperator(
                        Criteria.where("canBeDeleted").exists(false),
                        Criteria.where("canBeDeleted").is(null),
                        Criteria.where("canBeDeleted").is(false)
                )
        );

        Query query = new Query(criteria);
        query.limit(1); // Limit to one document

        // Update to set the processingId and processingTimestamp
        Update update = new Update()
                .set("processingId", UUID.randomUUID().toString())
                .set("processingTimestamp", new Date());

        // Options to return the new document after update
        FindAndModifyOptions options = new FindAndModifyOptions()
                .returnNew(true)
                .upsert(false);

        return mongoTemplate.findAndModify(query, update, options, Attachment.class);
    }
}
