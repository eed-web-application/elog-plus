package edu.stanford.slac.elog_plus.service;

import com.hp.jipp.encoding.IppPacket;
import com.hp.jipp.model.JobState;
import com.hp.jipp.model.JobStateReason;
import com.hp.jipp.model.Status;
import com.hp.jipp.model.Types;
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
import org.apache.tika.config.TikaConfig;
import org.apache.tika.detect.Detector;
import org.apache.tika.metadata.Metadata;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.Collections;

import static com.hp.jipp.encoding.Tag.operationAttributes;
import static edu.stanford.slac.ad.eed.baselib.exception.Utility.wrapCatch;


@Log4j2
@Service
@AllArgsConstructor
public class AttachmentService {
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
    public String createAttachment(FileObjectDescription attachment, boolean createPreview) {
        Attachment att = Attachment
                .builder()
                .fileName(attachment.getFileName())
                .contentType(attachment.getContentType())
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

    boolean exists(String id) {
        return wrapCatch(
                () -> attachmentRepository.existsById(
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
     * @return true
     */
    public Boolean setInUse(String attachmentID, boolean inUse) {
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
        return true;
    }
}
