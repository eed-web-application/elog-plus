package edu.stanford.slac.elog_plus.task;

import edu.stanford.slac.elog_plus.config.ELOGAppProperties;
import edu.stanford.slac.elog_plus.service.AttachmentService;
import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class CleanUnusedAttachment {
    AttachmentService attachmentService;
    ELOGAppProperties elogAppProperties;

    @Scheduled(cron = "${edu.stanford.slac.elog-plus.attachment-clean-expired-cron}")
    public void cleanExpiredNonUsedAttachments(){
        // This method will run every hour
        //attachmentService.deleteAllExpired(elogAppProperties.getAttachmentExpirationMinutes());
    }
}
