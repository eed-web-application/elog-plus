package edu.stanford.slac.elog_plus.repository;

import edu.stanford.slac.elog_plus.model.Attachment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * Repository for the attachment managements
 */
public interface AttachmentRepository  extends MongoRepository<Attachment, String> {
    /**
     * Return the number of attachment which the id is contained into the input list
     * @param ids the list of the id to count
     * @return the count of the found element
     */
    Long countAllByIdIn(List<String> ids);
}
