package edu.stanford.slac.elog_plus.service;

import edu.stanford.slac.elog_plus.api.v1.dto.AttachmentDTO;
import edu.stanford.slac.elog_plus.api.v1.mapper.AttachmentMapper;
import edu.stanford.slac.elog_plus.model.Attachment;
import edu.stanford.slac.elog_plus.model.FileObjectDescription;
import edu.stanford.slac.elog_plus.repository.AttachmentRepository;
import edu.stanford.slac.elog_plus.repository.StorageRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static edu.stanford.slac.elog_plus.exception.Utility.assertion;
import static edu.stanford.slac.elog_plus.exception.Utility.wrapCatch;

@Service
@AllArgsConstructor
public class AttachmentService {
    final private StorageRepository storageRepository;
    final private AttachmentRepository attachmentRepository;

    /**
     * @param attachment
     * @return
     */
    @Transactional
    public String createAttachment(FileObjectDescription attachment) {
        Attachment newAttachmentID =
                wrapCatch(
                        () -> attachmentRepository.insert(
                                Attachment
                                        .builder()
                                        .fileName(attachment.getFileName())
                                        .contentType(attachment.getContentType())
                                        .build()
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
        return newAttachmentID.getId();
    }

    /**
     * @param id
     * @param attachment
     */
    public void getAttachmentContent(String id, FileObjectDescription attachment) {
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
    }

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
}
