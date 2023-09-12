package edu.stanford.slac.elog_plus.api.v1.controller;

import edu.stanford.slac.elog_plus.api.v1.dto.ApiResultResponse;
import edu.stanford.slac.elog_plus.api.v1.dto.EntrySummaryDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.LogbookSummaryDTO;
import edu.stanford.slac.elog_plus.exception.NotAuthorized;
import edu.stanford.slac.elog_plus.model.Authorization;
import edu.stanford.slac.elog_plus.model.FileObjectDescription;
import edu.stanford.slac.elog_plus.service.AttachmentService;
import edu.stanford.slac.elog_plus.service.AuthService;
import edu.stanford.slac.elog_plus.service.EntryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static edu.stanford.slac.elog_plus.exception.Utility.assertion;
import static edu.stanford.slac.elog_plus.model.Authorization.Type.Read;
import static edu.stanford.slac.elog_plus.model.Authorization.Type.Write;

@RestController()
@RequestMapping("/v1/attachment")
@AllArgsConstructor
@Schema(description = "Set of api for attachment manipulation")
public class AttachmentsController {
    AuthService authService;
    EntryService entryService;
    AttachmentService attachmentService;

    @PostMapping(
            consumes = {"multipart/form-data"},
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @Operation(summary = "Create a new attachment")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResultResponse<String> newAttachment(
            @RequestParam("uploadFile") MultipartFile uploadFile,
            Authentication authentication
    ) throws Exception {
        // check if the user is authenticated

        FileObjectDescription desc = FileObjectDescription
                .builder()
                .fileName(
                        uploadFile.getOriginalFilename()
                )
                .contentType(
                        uploadFile.getContentType()
                )
                .is(
                        uploadFile.getInputStream()
                )
                .build();

        // check the authorization
        assertion(
                NotAuthorized.notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("AttachmentsController::newAttachment")
                        .build(),
                // should be authenticated
                () -> authService.checkAuthentication(authentication),
                // should be able to write on some logbook
                () -> authService.checkAuthorizationForOwnerAuthTypeAndResourcePrefix(
                        authentication,
                        Write,
                        "/"
                )
        );
        return ApiResultResponse.of(
                attachmentService.createAttachment(desc, true)
        );
    }

    @GetMapping(
            path = "/{id}/download"
            //produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE}
    )
    @Operation(summary = "Load an attachment using an unique attachment id")
    public ResponseEntity<Resource> download(
            @PathVariable String id,
            Authentication authentication
    ) throws Exception {
        checkAuthorizedOnAttachment(id, authentication);
        FileObjectDescription desc = attachmentService.getAttachmentContent(id);
        InputStreamResource resource = new InputStreamResource(desc.getIs());
        MediaType mediaType = MediaType.valueOf(desc.getContentType());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        ContentDisposition disposition = ContentDisposition
                // 3.2
                .inline() // or .attachment()
                // 3.1
                .filename(desc.getFileName())
                .build();
        headers.setContentDisposition(disposition);
        return new ResponseEntity<>(resource, headers, HttpStatus.OK);
    }

    @GetMapping(
            path = "/{id}/preview.jpg"
            //produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE}
    )
    @Operation(summary = "Load an attachment using an unique attachment id")
    public ResponseEntity<Resource> downloadPreview(
            @PathVariable String id,
            Authentication authentication
    ) throws Exception {
        checkAuthorizedOnAttachment(id, authentication);
        FileObjectDescription desc = attachmentService.getPreviewContent(id);
        InputStreamResource resource = new InputStreamResource(desc.getIs());
        MediaType mediaType = MediaType.valueOf(desc.getContentType());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        ContentDisposition disposition = ContentDisposition
                // 3.2
                .inline() // or .attachment()
                // 3.1
                .filename(desc.getFileName())
                .build();
        headers.setContentDisposition(disposition);
        return new ResponseEntity<>(resource, headers, HttpStatus.OK);
    }

    private void checkAuthorizedOnAttachment(String attachmentId, Authentication authentication) {
        // fetch all the entries that are parent of the attachment and filter all those are readable by the user
        List<EntrySummaryDTO> entryThatOwnTheAttachment = entryService.getEntriesThatOwnTheAttachment(attachmentId)
                .stream()
                .map(
                        summary -> {
                            List<LogbookSummaryDTO> filteredLogbook = summary.logbooks().stream()
                                    .filter(
                                            lbSummary -> authService.checkAuthorizationForOwnerAuthTypeAndResourcePrefix(
                                                    authentication,
                                                    Read,
                                                    "/logbook/%s".formatted(lbSummary.id())
                                            )
                                    ).toList();
                            return summary.toBuilder()
                                    .logbooks(
                                            filteredLogbook
                                    ).build();
                        }
                )
                .filter(
                        summary -> !summary.logbooks().isEmpty()
                )
                .toList();
        assertion(
                NotAuthorized.notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("")
                        .build(),
                ()->authService.checkAuthentication(authentication),
                () -> !entryThatOwnTheAttachment.isEmpty()
        );
    }
}
