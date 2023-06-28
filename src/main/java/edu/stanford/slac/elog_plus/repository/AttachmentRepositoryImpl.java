package edu.stanford.slac.elog_plus.repository;

import com.mongodb.client.result.UpdateResult;
import edu.stanford.slac.elog_plus.model.Attachment;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@AllArgsConstructor
public class AttachmentRepositoryImpl implements AttachmentRepositoryCustom{
    final private MongoTemplate mongoTemplate;
    @Override
    public boolean setPreviewID(String id, String previewID) {
        Query q = new Query();
        q.addCriteria(
                Criteria.where("id").is(id)
        );
        Update u = new Update();
        u.set("previewID", previewID);
        u.set("previewState", Attachment.PreviewProcessingState.Completed);
        UpdateResult ur = mongoTemplate.updateFirst(q, u, Attachment.class);
        return ur.getModifiedCount() == 1;
    }

    @Override
    public boolean setPreviewState(String id, Attachment.PreviewProcessingState state) {
        Query q = new Query();
        q.addCriteria(
                Criteria.where("id").is(id)
        );
        Update u = new Update();
        u.set("previewState", state);

        UpdateResult ur = mongoTemplate.updateFirst(q, u, Attachment.class);
        return ur.getModifiedCount() == 1;
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
}
