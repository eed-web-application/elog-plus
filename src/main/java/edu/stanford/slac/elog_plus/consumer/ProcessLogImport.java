package edu.stanford.slac.elog_plus.consumer;

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationTypeDTO;
import edu.stanford.slac.ad.eed.baselib.auth.jwt.SLACAuthenticationToken;
import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import edu.stanford.slac.elog_plus.api.v1.dto.EntryImportDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.NewLogbookDTO;
import edu.stanford.slac.elog_plus.api.v2.dto.ImportEntryDTO;
import edu.stanford.slac.elog_plus.model.FileObjectDescription;
import edu.stanford.slac.elog_plus.service.ImportService;
import edu.stanford.slac.elog_plus.service.LogbookService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.retry.annotation.Backoff;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.*;

@Log4j2
@Component
@AllArgsConstructor
public class ProcessLogImport {
    private final AuthService authService;
    private final ImportService importService;
    private final LogbookService logbookService;
    private final AuthenticationManager authenticationManager;

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 2_000, maxDelay = 10_000, multiplier = 2),
            autoCreateTopics = "false",
            kafkaTemplate = "importEntryDTOKafkaTemplate"
    )
    @KafkaListener(
            topics = "${edu.stanford.slac.elog-plus.import-entry-topic}",
            containerFactory = "importEntryKafkaListenerContainerFactory"
    )
    public void processImport
            (
                    @Valid ImportEntryDTO importEntryDTO,
                    Acknowledgment acknowledgment,
                    @Headers MessageHeaders headers
            ) {
        try {
            if(!headers.containsKey("Authorization")) {
                log.info("Authorization header present message will not be processed {}", headers);
                acknowledgment.acknowledge();
            }
            List<FileObjectDescription> attachmentList = new ArrayList<>();
            // create authentication token validating the user token
            Authentication authentication = authenticationManager.authenticate(
                    SLACAuthenticationToken
                    .builder()
                    .userToken(
                            new String(Objects.requireNonNull(headers.get("Authorization", byte[].class)), StandardCharsets.UTF_8)
                    )
                    .build()
            );
            // authorize current context
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.info("[import {}] manage attachment", importEntryDTO.entry().title());

            // ensure the logbooks
            log.info("[import {}] ensure logbooks", importEntryDTO.entry().title());
            boolean isRoot = authService.checkForRoot(authentication);
            List<String> notFoundLogbook = importEntryDTO.entry().logbooks()
                    .stream()
                    .filter(logbookName -> !logbookService.existByName(logbookName))
                    .toList();
            assertion(
                    ControllerLogicException.builder().build(),
                    () -> any(
                            // or the logbook is empty
                            notFoundLogbook::isEmpty,
                            // or the user is root
                            () -> all(
                                    () -> !notFoundLogbook.isEmpty(),
                                    () -> isRoot
                            )
                    )
            );
            log.info("[import {}] logbooks to create: {}", importEntryDTO.entry().title(), notFoundLogbook);
            // if we are here and there are logbooks not found, the user is root
            notFoundLogbook.forEach(
                    logbookName -> {
                        log.info("[import {}] logbooks {}, creating", importEntryDTO.entry().title(), logbookName);
                        logbookService.createNew(
                                NewLogbookDTO
                                        .builder()
                                        .name(logbookName)
                                        .build()
                        );
                    }
            );

            // authorize reader on logbook
            if (importEntryDTO.readerUserIds() != null && !importEntryDTO.readerUserIds().isEmpty()) {
                log.info("[import {}] Authorize readers {}", importEntryDTO.entry().title(), importEntryDTO.readerUserIds());
                logbookService.ensureAuthorizationOnLogbook(
                        importEntryDTO.entry().logbooks(),
                        importEntryDTO.readerUserIds(),
                        AuthorizationTypeDTO.Read
                );
            }

            // import the entry
            log.info("[import {}] get logbooks id", importEntryDTO.entry().title());
            var logbooksId =  importService.getLogbooksIdsByNames(importEntryDTO.entry().logbooks());
            log.info("[import {}] convert and get tags id", importEntryDTO.entry().title());
            var tagsId = importService.ensureTagsNamesOnAllLogbooks(importEntryDTO.entry().tags(), importEntryDTO.entry().logbooks());
            EntryImportDTO entryToImport = importEntryDTO.entry().toBuilder()
                    .logbooks(logbooksId)
                    .tags(tagsId)
                    .build();
            log.info("[import {}] create new entry", importEntryDTO.entry().title());
            String newEntryId = importService.importSingleEntry(entryToImport, attachmentList);
            log.info("[import {}] new entry created with id {}", importEntryDTO.entry().title(), newEntryId);
            // Manually acknowledge the message only if processing is successful
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing message", e);
        }
    }
}
