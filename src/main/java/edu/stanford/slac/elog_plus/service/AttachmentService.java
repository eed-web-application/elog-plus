package edu.stanford.slac.elog_plus.service;

import edu.stanford.slac.elog_plus.api.v1.dto.AttachmentDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.ObjectListResultDTO;
import edu.stanford.slac.elog_plus.api.v1.mapper.AttachmentMapper;
import edu.stanford.slac.elog_plus.config.ELOGAppProperties;
import edu.stanford.slac.elog_plus.exception.AttachmentNotFound;
import edu.stanford.slac.elog_plus.model.Attachment;
import edu.stanford.slac.elog_plus.model.FileObjectDescription;
import edu.stanford.slac.elog_plus.repository.AttachmentRepository;
import edu.stanford.slac.elog_plus.repository.StorageRepository;
import io.micrometer.core.instrument.Counter;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.mongodb.MongoTransactionException;
import org.springframework.data.mongodb.UncategorizedMongoDbException;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.wrapCatch;


@Log4j2
@Service
@AllArgsConstructor
public class AttachmentService {
    public static final String ATTACHMENT_QUEUED_REFERENCE = "queued";
    final private AttachmentMapper attachmentMapper;
    final private ELOGAppProperties appProperties;
    final private StorageRepository storageRepository;
    final private AttachmentRepository attachmentRepository;
    final private KafkaTemplate<String, Attachment> attachmentKafkaTemplate;
    final private Counter previewSubmittedCounter;

    /**
     * Create a new attachment
     * @param attachment the new attachment content
     * @return the id of the new created attachment
     */
    @Transactional
    public String createAttachment(FileObjectDescription attachment, boolean createPreview) {
        return createAttachment(attachment, createPreview, Optional.empty());
    }

    /**
     * Create a new attachment
     * @param attachment the new attachment content
     * @param createPreview if true, create a preview of the attachment
     * @param referenceInfo the reference information
     * @return the id of the new created attachment
     */
    @Transactional
    public String createAttachment(FileObjectDescription attachment, boolean createPreview, Optional<String> referenceInfo) {
        Attachment att = Attachment
                .builder()
                .fileName(attachment.getFileName())
                .contentType(attachment.getContentType())
                .referenceInfo(referenceInfo.orElse(null))
                .build();

        Attachment newAttachmentID =
                wrapCatch(
                        () -> attachmentRepository.insert(
                                att
                        ),
                        0,
                        "AttachmentService::createAttachment");
        try {
            wrapCatch(
                    () -> {
                        storageRepository.uploadFile(newAttachmentID.getId(), attachment);
                        return null;
                    },
                    -1,
                    "AttachmentService::createAttachment"
            );
        } finally {
            wrapCatch(
                    () -> {
                        attachment.getIs().close();
                        return null;
                    },
                    -2,
                    "AttachmentService::createAttachment"
            );
        }

        if (createPreview) {
            attachmentKafkaTemplate.send(appProperties.getImagePreviewTopic(), att);
            previewSubmittedCounter.increment();
        }
        log.info("New attachment created with id {}", newAttachmentID.getId());
        return newAttachmentID.getId();
    }

    /**
     * Return the attachment raw content file
     * @param id the unique id of the attachment
     */
    public boolean exists(String id) {
        return wrapCatch(
                // the attachment exist only if it exists and cannot be deleted
                () -> attachmentRepository.existsByIdAndCanBeDeletedIsFalse(
                        id
                ),
                0,
                "AttachmentService::exists");
    }

    /**
     * Return the attachment raw content file
     * @param id the unique id of the attachment
     */
    public FileObjectDescription getAttachmentContent(String id) {
        FileObjectDescription attachment = FileObjectDescription.builder().build();
        // fetch
        Attachment foundAttachment = wrapCatch(
                () -> attachmentRepository.findById(id),
                -1,
                "AttachmentService::getAttachmentContent"
        ).orElseThrow(
                () -> AttachmentNotFound.attachmentNotFoundBuilder()
                        .errorCode(-2)
                        .attachmentID(id)
                        .errorDomain("AttachmentService::getAttachmentContent")
                        .build()
        );


        // retrieve stored file
        attachment.setFileName(foundAttachment.getFileName());
        wrapCatch(
                () -> {
                    storageRepository.getFile(id, attachment);
                    return null;
                },
                -1,
                "AttachmentService::getAttachment"
        );
        return attachment;
    }

    /**
     * return the preview content
     * @param id the id of the attachment
     * @return the preview content
     */
    public FileObjectDescription getPreviewContent(String id) {
        FileObjectDescription attachment = FileObjectDescription.builder().build();
        // fetch
        Attachment foundAttachment = wrapCatch(
                () -> attachmentRepository.findById(id),
                -1,
                "AttachmentService::getAttachment"
        ).orElseThrow(
                () -> AttachmentNotFound.attachmentNotFoundBuilder()
                        .errorCode(-2)
                        .attachmentID(id)
                        .errorDomain("AttachmentService::getAttachment")
                        .build()
        );

        // retrieve stored file
        attachment.setFileName(foundAttachment.getFileName());
        wrapCatch(
                () -> {
                    storageRepository.getFile(foundAttachment.getPreviewID(), attachment);
                    return null;
                },
                -1,
                "AttachmentService::getAttachment"
        );
        return attachment;
    }

    /**
     * Return the mini preview object description
     *
     * @param id the unique identifier of the attachment
     * @return the object stream of the mini preview
     */
    public FileObjectDescription getMiniPreviewContent(String id) {
        FileObjectDescription attachment = FileObjectDescription.builder().build();
        // fetch
        Attachment foundAttachment = wrapCatch(
                () -> attachmentRepository.findById(id),
                -1,
                "AttachmentService::getMiniPreviewContent"
        ).orElseThrow(
                () -> AttachmentNotFound.attachmentNotFoundBuilder()
                        .errorCode(-2)
                        .attachmentID(id)
                        .errorDomain("AttachmentService::getMiniPreviewContent")
                        .build()
        );

        // retrieve stored mini preview from model
        attachment.setFileName(foundAttachment.getFileName());
        attachment.setIs(new ByteArrayInputStream(foundAttachment.getMiniPreview()));
        attachment.setContentType(MediaType.IMAGE_JPEG_VALUE);
        return attachment;
    }

    /**
     * Return the attachment dto
     *
     * @param id the attachment id
     * @return the attachment dto
     */
    public AttachmentDTO getAttachment(String id) {
        // fetch
        Attachment foundAttachment = wrapCatch(
                () -> attachmentRepository.findById(id),
                -1,
                "AttachmentService::getAttachment"
        ).orElseThrow(
                () -> AttachmentNotFound.attachmentNotFoundBuilder()
                        .errorCode(-2)
                        .attachmentID(id)
                        .errorDomain("AttachmentService::getAttachment")
                        .build()
        );
        return attachmentMapper.fromModel(
                foundAttachment
        );
    }

    /**
     * Set the preview id
     *
     * @param id        the id of the attachment
     * @param previewID the preview identifier for fetch it from object store
     */
    public void setPreviewID(String id, String previewID) {
        // fetch
        Attachment foundAttachment = wrapCatch(
                () -> attachmentRepository.findById(id),
                -1,
                "AttachmentService::setPreviewID"
        ).orElseThrow(
                () -> AttachmentNotFound.attachmentNotFoundBuilder()
                        .errorCode(-2)
                        .attachmentID(id)
                        .errorDomain("AttachmentService::setPreviewID")
                        .build()
        );
        foundAttachment.setPreviewID(previewID);
        Attachment finalFoundAttachment = foundAttachment;
        foundAttachment = wrapCatch(
                () -> attachmentRepository.save(finalFoundAttachment),
                -3,
                "AttachmentService::setPreviewProcessingState"
        );
        log.info("Set the preview id to {} for the attachment {}", previewID, foundAttachment.getId());
    }

    /**
     * Update the processing state of the attachment
     *
     * @param id              the unique identifier of the attachment
     * @param processingState the new state of the attachment
     */
    public void setPreviewProcessingState(String id, Attachment.PreviewProcessingState processingState) {
        // fetch
        Attachment foundAttachment = wrapCatch(
                () -> attachmentRepository.findById(id),
                -1,
                "AttachmentService::setPreviewProcessingState"
        ).orElseThrow(
                () -> AttachmentNotFound.attachmentNotFoundBuilder()
                        .errorCode(-2)
                        .attachmentID(id)
                        .errorDomain("AttachmentService::setPreviewProcessingState")
                        .build()
        );
        foundAttachment.setPreviewState(processingState);
        Attachment finalFoundAttachment = foundAttachment;
        foundAttachment = wrapCatch(
                () -> attachmentRepository.save(finalFoundAttachment),
                -3,
                "AttachmentService::setPreviewProcessingState"
        );
        log.info("Update the preview processing state to {} for the attachment {}", processingState, foundAttachment.getId());
    }

    /**
     * Return the processing state of the attachment
     *
     * @param id the unique id of the attachment
     * @return The string that represent the processing state
     */
    public String getPreviewProcessingState(String id) {
        // fetch
        Attachment foundAttachment = wrapCatch(
                () -> attachmentRepository.findById(id),
                -1,
                "AttachmentService::getPreviewProcessingState"
        ).orElseThrow(
                () -> AttachmentNotFound.attachmentNotFoundBuilder()
                        .errorCode(-2)
                        .attachmentID(id)
                        .errorDomain("AttachmentService::getPreviewProcessingState")
                        .build()
        );
        return foundAttachment.getPreviewState().name();
    }

    /**
     * Set the mini preview of an attachment
     *
     * @param id        the unique identifier of an attachment
     * @param byteArray the byte array represent the mini preview
     */
    public void setMiniPreview(String id, byte[] byteArray) {
        // fetch
        Attachment foundAttachment = wrapCatch(
                () -> attachmentRepository.findById(id),
                -1,
                "AttachmentService::setMiniPreview"
        ).orElseThrow(
                () -> AttachmentNotFound.attachmentNotFoundBuilder()
                        .errorCode(-2)
                        .attachmentID(id)
                        .errorDomain("AttachmentService::setMiniPreview")
                        .build()
        );

        foundAttachment.setMiniPreview(byteArray);
        Attachment finalFoundAttachment = foundAttachment;
        foundAttachment = wrapCatch(
                () -> attachmentRepository.save(finalFoundAttachment),
                -3,
                "AttachmentService::setMiniPreview"
        );
        log.info("Set the mini preview for the attachment {}", foundAttachment.getId());
    }

    /**
     * return the list of objet in a paged way
     * @param maxKeysPerPage
     * @param continuationToken
     * @return
     */
    public ObjectListResultDTO listFromStorage(int maxKeysPerPage, String continuationToken) {
        return wrapCatch(
                () -> attachmentMapper.fromModel(
                        storageRepository.listFilesInBucket(
                                maxKeysPerPage,
                                continuationToken
                        )
                ),
                -1,
                "AttachmentService::listFromStorage"
        );
    }

    /**
     * Set the in use flag of an attachment
     * @param attachmentID the attachment id
     * @param inUse the 'in use' flag
     */
    public void setInUse(String attachmentID, boolean inUse) {
        wrapCatch(
                () -> {
                    attachmentRepository.setInUseState(
                            attachmentID,
                            inUse
                    );
                    return null;
                },
                -1,
                "AttachmentService::setInUse"
        );
    }

    /**
     * Return the list of attachment by reference info
     * @param referenceInfo the reference info value to search for
     * @return the list of attachment
     */
    public List<AttachmentDTO> findAllByReferenceInfo(String referenceInfo) {
        return wrapCatch(
                ()->attachmentRepository.findAllByReferenceInfo(referenceInfo),
                -1,
                "AttachmentService::setInUse"
        ).stream().map(
                attachmentMapper::fromModel
        ).toList();
    }

    /**
     * remove from queue all expired attachment
     * @param expirationMinutes the expiration time in minutes
     */
    public void enqueueAllExpired(Integer expirationMinutes) {
        // calculate expiration date
        LocalDateTime expirationTime = LocalDateTime.now().minusMinutes(expirationMinutes);

        // remove all reference for queued attachment that are expired
        log.info("Remove reference info form expired and in use attachment since {}", expirationTime);
        wrapCatch(
                ()->{
                    attachmentRepository.removeReferenceInfoOnAllInUseAndExpired(ATTACHMENT_QUEUED_REFERENCE, expirationTime);
                    return null;
                },
                -2,
                "AttachmentService::deleteAllExpired"
        );
    }
}
