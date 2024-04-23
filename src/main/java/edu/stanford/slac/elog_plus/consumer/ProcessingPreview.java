package edu.stanford.slac.elog_plus.consumer;

import edu.stanford.slac.elog_plus.model.Attachment;
import edu.stanford.slac.elog_plus.model.FileObjectDescription;
import edu.stanford.slac.elog_plus.repository.StorageRepository;
import edu.stanford.slac.elog_plus.service.AttachmentService;
import io.micrometer.core.instrument.Counter;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.tasks.UnsupportedFormatException;
import org.springframework.http.MediaType;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Log4j2
@Component
@AllArgsConstructor
public class ProcessingPreview {
    final private AttachmentService attachmentService;
    final private StorageRepository storageRepository;
    final private Counter previewProcessedCounter;
    final private Counter previewErrorsCounter;

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 2_000, maxDelay = 10_000, multiplier = 2),
            autoCreateTopics = "false",
            kafkaTemplate = "attachmentKafkaTemplate"
    )
    @KafkaListener(
            topics = "${edu.stanford.slac.elog-plus.image-preview-topic}",
            containerFactory = "attachmentKafkaListenerContainerFactory"
    )
    public void processPreview(
            Attachment attachment,
            Acknowledgment acknowledgment,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) long offset
    ) throws RuntimeException, IOException {
        log.info("Process preview for attachment: {} from {} @ {}", attachment, topic, offset);
        FileObjectDescription fod = null;
        try {
            attachmentService.setPreviewProcessingState(attachment.getId(), Attachment.PreviewProcessingState.Processing);
            fod = attachmentService.getAttachmentContent(attachment.getId());
            byte[] imageBytes = fod.getIs().readAllBytes();
            String previewID = String.format("%s-preview", attachment.getId());
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Thumbnails.of(new ByteArrayInputStream(imageBytes))
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

            baos = new ByteArrayOutputStream();
            Thumbnails.of(new ByteArrayInputStream(imageBytes))
                    .size(32, 32)
                    .outputFormat("jpg")
                    .toOutputStream(baos);
            attachmentService.setMiniPreview(attachment.getId(), baos.toByteArray());

            attachmentService.setPreviewProcessingState(attachment.getId(), Attachment.PreviewProcessingState.Completed);
            previewProcessedCounter.increment();
            acknowledgment.acknowledge();
        } catch (UnsupportedFormatException e) {
            attachmentService.setPreviewProcessingState(attachment.getId(), Attachment.PreviewProcessingState.PreviewNotAvailable);
            // in this case we manage this error with the state of image not available
            log.info("Unsupported image for preview for the attachment {}", attachment);
            previewErrorsCounter.increment();
        } catch (Throwable e) {
            attachmentService.setPreviewProcessingState(attachment.getId(), Attachment.PreviewProcessingState.Error);
            log.error("Error during preview generation for the attachment {} with error with message '{}' - [{}]", attachment, e.getMessage(), e);
            previewErrorsCounter.increment();
            throw new RuntimeException(e);
        } finally {
            if(fod != null && fod.getIs() != null) {
                fod.getIs().close();
            }
        }

    }
}
