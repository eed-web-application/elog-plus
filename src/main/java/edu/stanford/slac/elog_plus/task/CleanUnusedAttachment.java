package edu.stanford.slac.elog_plus.task;

import edu.stanford.slac.elog_plus.config.ELOGAppProperties;
import edu.stanford.slac.elog_plus.model.Attachment;
import edu.stanford.slac.elog_plus.repository.AttachmentRepository;
import edu.stanford.slac.elog_plus.repository.EntryRepository;
import edu.stanford.slac.elog_plus.service.AttachmentService;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.wrapCatch;
import static edu.stanford.slac.elog_plus.service.AttachmentService.ATTACHMENT_QUEUED_REFERENCE;

@Log4j2
@Component
@AllArgsConstructor
public class CleanUnusedAttachment {
    private final Clock clock;
    private final EntryRepository entryRepository;
    private final AttachmentRepository attachmentRepository;
    private final ELOGAppProperties elogAppProperties;

    @Scheduled(cron = "${edu.stanford.slac.elog-plus.attachment-clean-expired-cron}")
    public void cleanExpiredNonUsedAttachments(){
        Attachment attachment = null;
        // the expiration data is calculated in minuted form now depending on the configuration
        var expirationAttachmentDate = LocalDateTime.now(clock).minusMinutes(elogAppProperties.getAttachmentExpirationMinutes());
        while ((attachment = attachmentRepository.findAndUpdateNextAvailableModel(expirationAttachmentDate, expirationAttachmentDate.minusSeconds(30))) != null) {
            try {
                log.info("Processing attachment {}", attachment.getId());
                var attachmentIsUsed = entryRepository.existsByAttachmentsContains(attachment.getId());
                if(attachmentIsUsed) {
                    log.info("Attachment {} is used so it will no be checked anymore", attachment.getId());
                    attachment.setInUse(true);
                } else {
                    // set that can be deleted
                    attachment.setCanBeDeleted(true);
                    log.info("Attachment {} is not used and is tagged as to be deleted", attachment.getId());
                }
                // remove reference to so in case it was enqueued, it is also removed from the attachment queue
                attachment.setReferenceInfo(null);
            } catch (Exception e) {
                log.error("Error processing attachment {}", attachment.getId(), e);
            } finally {
                attachment.setProcessingId(null);
                attachment.setProcessingTimestamp(null);
                attachmentRepository.save(attachment);
            }
        }
    }
}
