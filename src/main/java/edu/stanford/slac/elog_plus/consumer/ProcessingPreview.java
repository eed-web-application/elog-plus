package edu.stanford.slac.elog_plus.consumer;

import edu.stanford.slac.elog_plus.config.AppProperties;
import edu.stanford.slac.elog_plus.model.Attachment;
import edu.stanford.slac.elog_plus.model.FileObjectDescription;
import edu.stanford.slac.elog_plus.repository.AttachmentRepository;
import edu.stanford.slac.elog_plus.repository.StorageRepository;
import edu.stanford.slac.elog_plus.service.AttachmentService;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.tasks.UnsupportedFormatException;
import org.springframework.http.MediaType;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

@Log4j2
@Component
@AllArgsConstructor
public class ProcessingPreview {
    final private AppProperties appProperties;
    final private AttachmentService attachmentService;
    final private AttachmentRepository attachmentRepository;
    final private StorageRepository storageRepository;

    @RetryableTopic(attempts = "3", backoff = @Backoff(delay = 2_000, maxDelay = 10_000, multiplier = 2))
    @KafkaListener(topics = "${edu.stanford.slac.elogs-plus.image-preview-topic}")
    public void processPreview(
            Attachment attachment,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) long offset
    ) throws RuntimeException {
        log.info("Process preview for attachment: {} from {} @ {}", attachment, topic, offset);
        try {
            attachmentService.setPreviewProcessingState(attachment.getId(), Attachment.PreviewProcessingState.Processing);
            FileObjectDescription fod = attachmentService.getAttachmentContent(attachment.getId());
            String previewID = String.format("%s-preview", attachment.getId());
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Thumbnails.of(fod.getIs())
                    .size(1024, 1024)
                    .outputFormat("jpg")
                    .toOutputStream(baos);
            storageRepository.uploadFile(
                    previewID,
                    FileObjectDescription
                            .builder()
                            .fileName(previewID)
                            .contentType(MediaType.IMAGE_JPEG_VALUE)
                            .is(new ByteArrayInputStream(baos.toByteArray()))
                            .build()
            );
            attachmentService.setPreviewID(attachment.getId(), previewID);
        } catch (UnsupportedFormatException e) {
            attachmentService.setPreviewProcessingState(attachment.getId(), Attachment.PreviewProcessingState.PreviewNotAvailable);
            // in this case we manage this error with the state of image not available
            log.info("Unsupported image for preview for the attachment {}", attachment);
        } catch (Throwable e) {
            attachmentService.setPreviewProcessingState(attachment.getId(), Attachment.PreviewProcessingState.Error);
            log.error("Error during preview generation for the attachment {} with error {}", attachment, e.getCause());
            throw new RuntimeException(e);
        }

    }
}
