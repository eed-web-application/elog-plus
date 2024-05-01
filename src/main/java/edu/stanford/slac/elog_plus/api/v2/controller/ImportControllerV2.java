package edu.stanford.slac.elog_plus.api.v2.controller;

import edu.stanford.slac.ad.eed.baselib.api.v1.dto.ApiResultResponse;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationTypeDTO;
import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.ad.eed.baselib.exception.NotAuthorized;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import edu.stanford.slac.elog_plus.api.v1.dto.EntryImportDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.NewLogbookDTO;
import edu.stanford.slac.elog_plus.api.v2.dto.ImportEntryDTO;
import edu.stanford.slac.elog_plus.model.FileObjectDescription;
import edu.stanford.slac.elog_plus.service.ImportService;
import edu.stanford.slac.elog_plus.service.LogbookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.*;

@RestController()
@RequestMapping("/v2/import")

@Log4j2
@AllArgsConstructor
@Schema(description = "Main set of api for inject data into ELog system")
public class ImportControllerV2 {
    AuthService authService;
    ImportService importService;
    LogbookService logbookService;

    @Operation(description = "Import an entry with attachment, ensure the logbooks, attachment and tags are managed correctly")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(
            consumes = {MediaType.MULTIPART_FORM_DATA_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    public ApiResultResponse<String> uploadEntryAndAttachment
            (
                    Authentication authentication,
                    @Parameter(schema = @Schema(type = "string", implementation = ImportEntryDTO.class))
                    @RequestPart("entry") @Valid ImportEntryDTO importEntryDTO,
                    @RequestPart(value = "files", required = false)
                    MultipartFile[] files
            ) {
        log.info("[import {}] manage attachment", importEntryDTO.entry().title());
        assertion
                (
                        NotAuthorized.notAuthorizedBuilder()
                                .errorCode(-1)
                                .errorDomain("ImportController:uploadEntryAndAttachment")
                                .build(),
                        () -> authService.checkAuthentication(authentication)
                );

        List<FileObjectDescription> attachmentList = new ArrayList<>();
        if (files != null) {
            attachmentList = Arrays.stream(files).map(
                    file -> {
                        try {
                            return FileObjectDescription
                                    .builder()
                                    .fileName(
                                            file.getOriginalFilename()
                                    )
                                    .contentType(
                                            file.getContentType()
                                    )
                                    .is(
                                            file.getInputStream()
                                    )
                                    .build();
                        } catch (IOException e) {
                            throw ControllerLogicException
                                    .builder()
                                    .errorCode(-1)
                                    .errorMessage(e.getMessage())
                                    .errorDomain("ImportController:uploadEntryAndAttachment")
                                    .build();
                        }
                    }
            ).collect(Collectors.toList());
        }

        // ensure the logbooks
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
            logbookService.ensureAuthorizationOnLogbook(
                    importEntryDTO.entry().logbooks(),
                    importEntryDTO.readerUserIds(),
                    AuthorizationTypeDTO.Read
            );
        }

        // convert the tags
        log.info("[import {}] tags and logbook conversion", importEntryDTO.entry().title());
        EntryImportDTO entryToImport = importEntryDTO.entry().toBuilder()
                .logbooks(
                        importService.getLogbooksIdsByNames(importEntryDTO.entry().logbooks())
                )
                .tags(
                        importService.ensureTagsNamesOnAllLogbooks(importEntryDTO.entry().tags(), importEntryDTO.entry().logbooks())
                )
                .build();
        return ApiResultResponse.of
                (
                        importService.importSingleEntry
                                (
                                        entryToImport,
                                        attachmentList
                                )
                );
    }
}
