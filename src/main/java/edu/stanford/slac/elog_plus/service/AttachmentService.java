package edu.stanford.slac.elog_plus.service;

import edu.stanford.slac.elog_plus.api.v1.dto.AttachmentDTO;
import edu.stanford.slac.elog_plus.api.v1.mapper.AttachmentMapper;
import edu.stanford.slac.elog_plus.config.AppProperties;
import edu.stanford.slac.elog_plus.model.Attachment;
import edu.stanford.slac.elog_plus.model.FileObjectDescription;
import edu.stanford.slac.elog_plus.repository.AttachmentRepository;
import edu.stanford.slac.elog_plus.repository.StorageRepository;
import lombok.AllArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static edu.stanford.slac.elog_plus.exception.Utility.assertion;
import static edu.stanford.slac.elog_plus.exception.Utility.wrapCatch;

@Service
@AllArgsConstructor
public class AttachmentService {
    final private AppProperties appProperties;
    final private StorageRepository storageRepository;
    final private AttachmentRepository attachmentRepository;
    final private KafkaTemplate<String, Attachment> attachmentProducer;
    /**
     * @param attachment
     * @return
     */
    @Transactional
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
        wrapCatch(
                () -> {
                    storageRepository.uploadFile(newAttachmentID.getId(), attachment);
                    return null;
                },
                -1,
                "AttachmentService::createAttachment"
        );
        if(createPreview) {
            attachmentProducer.send(appProperties.getImagePreviewTopic(), att);
        }
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
     * @param id
     */
    public FileObjectDescription getAttachmentContent(String id) {
        FileObjectDescription attachment = FileObjectDescription.builder().build();
        // fetch
        Optional<Attachment> foundAttachment = wrapCatch(
                () -> attachmentRepository.findById(id),
                -1,
                "AttachmentService::getAttachment"
        );

        //check
        assertion(
                foundAttachment::isPresent,
                -2,
                "Attachment not found",
                "AttachmentService::getAttachment"
        );

        // retrieve stored file
        attachment.setFileName(foundAttachment.get().getFileName());
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
     *
     * @param id
     * @return
     */
    public FileObjectDescription getPreviewContent(String id) {
        FileObjectDescription attachment = FileObjectDescription.builder().build();
        // fetch
        Optional<Attachment> foundAttachment = wrapCatch(
                () -> attachmentRepository.findById(id),
                -1,
                "AttachmentService::getAttachment"
        );

        //check
        assertion(
                foundAttachment::isPresent,
                -2,
                "Attachment not found",
                "AttachmentService::getAttachment"
        );

        // retrieve stored file
        attachment.setFileName(foundAttachment.get().getFileName());
        wrapCatch(
                () -> {
                    storageRepository.getFile(foundAttachment.get().getPreviewID(), attachment);
                    return null;
                },
                -1,
                "AttachmentService::getAttachment"
        );
        return attachment;
    }

    /**
     *
     * @param id
     * @return
     */
    public AttachmentDTO getAttachment(String id) {
        // fetch
        Optional<Attachment> foundAttachment = wrapCatch(
                () -> attachmentRepository.findById(id),
                -1,
                "AttachmentService::getAttachment"
        );

        //check
        assertion(
                foundAttachment::isPresent,
                -2,
                "Attachment not found",
                "AttachmentService::getAttachment"
        );

       return AttachmentMapper.INSTANCE.fromModel(foundAttachment.get());
    }

    /**
     *
     * @param ids
     * @return
     */
    public List<AttachmentDTO> getAttachment(List<String> ids) {
        List<AttachmentDTO> result = new ArrayList<>();
        for (String id:
             ids) {
            // fetch
            Optional<Attachment> foundAttachment = wrapCatch(
                    () -> attachmentRepository.findById(id),
                    -1,
                    "AttachmentService::getAttachment"
            );

            //check
            assertion(
                    foundAttachment::isPresent,
                    -2,
                    "Attachment not found",
                    "AttachmentService::getAttachment"
            );
            result.add(
                    AttachmentMapper.INSTANCE.fromModel(foundAttachment.get())
            );
        }

        return result;
    }

    /**
     *
     * @param id
     * @param previewID
     * @return
     */
    public boolean setPreviewID(String id, String previewID) {
        // fetch
        Optional<Attachment> foundAttachment = wrapCatch(
                () -> attachmentRepository.findById(id),
                -1,
                "AttachmentService::setPreviewID"
        );

        //check
        assertion(
                foundAttachment::isPresent,
                -2,
                "Attachment not found",
                "AttachmentService::setPreviewID"
        );

        return attachmentRepository.setPreviewID(id, previewID);
    }

    /**
     *
     * @param id
     * @param processingState
     * @return
     */
    public boolean setPreviewProcessingState(String id, Attachment.PreviewProcessingState processingState) {
        // fetch
        Optional<Attachment> foundAttachment = wrapCatch(
                () -> attachmentRepository.findById(id),
                -1,
                "AttachmentService::setPreviewProcessingState"
        );

        //check
        assertion(
                foundAttachment::isPresent,
                -2,
                "Attachment not found",
                "AttachmentService::setPreviewProcessingState"
        );

        return attachmentRepository.setPreviewState(id, processingState);
    }

    /**
     *
     * @param id
     * @return
     */
    public String getPreviewProcessingState(String id) {
        // fetch
        Optional<Attachment> foundAttachment = wrapCatch(
                () -> attachmentRepository.findById(id),
                -1,
                "AttachmentService::setPreviewProcessingState"
        );

        //check
        assertion(
                foundAttachment::isPresent,
                -2,
                "Attachment not found",
                "AttachmentService::setPreviewProcessingState"
        );

        return attachmentRepository.getPreviewState(id).name();
    }
}
