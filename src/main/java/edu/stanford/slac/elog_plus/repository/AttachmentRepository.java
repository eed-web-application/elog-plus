package edu.stanford.slac.elog_plus.repository;

import edu.stanford.slac.elog_plus.model.Attachment;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Repository for the attachment managements
 */
public interface AttachmentRepository  extends MongoRepository<Attachment, String>, AttachmentRepositoryCustom {
    boolean existsById(String id);
}
