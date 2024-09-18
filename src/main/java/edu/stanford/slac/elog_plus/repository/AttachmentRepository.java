package edu.stanford.slac.elog_plus.repository;

import edu.stanford.slac.elog_plus.model.Attachment;
import org.apache.james.mime4j.dom.datetime.DateTime;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for the attachment managements
 */
public interface AttachmentRepository  extends MongoRepository<Attachment, String>, AttachmentRepositoryCustom {
    boolean existsById(String id);
    List<Attachment> findAllByReferenceInfo(String referenceId);
    // delete all attachment that are expired since some minutes
    void deleteByCreatedDateLessThanAndInUseIsFalse(LocalDateTime expirationTime);
}
