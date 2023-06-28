package edu.stanford.slac.elog_plus.api.v1.controller;

import edu.stanford.slac.elog_plus.api.v1.dto.ApiResultResponse;
import edu.stanford.slac.elog_plus.model.FileObjectDescription;
import edu.stanford.slac.elog_plus.service.AttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController()
@RequestMapping("/v1/attachment")
@AllArgsConstructor
@Schema(description = "Set of api for attachment manipulation")
public class AttachmentsController {
    AttachmentService attachmentService;
    @PostMapping(
            consumes = {"multipart/form-data"},
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    @Operation(summary = "Create a new attachment")
    public ApiResultResponse<String> newAttachment(
            @RequestParam("uploadFile") MultipartFile uploadFile
    ) throws Exception {
        FileObjectDescription desc = FileObjectDescription
                .builder()
                .fileName(
                        uploadFile.getName()
                )
                .contentType(
                        uploadFile.getContentType()
                )
                .is(
                        uploadFile.getInputStream()
                )
                .build();

        return ApiResultResponse.of(
                attachmentService.createAttachment(desc,true)
        );
    }

    @GetMapping(
            path = "/{id}/download"
            //produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE}
    )
    @Operation(summary = "Load an attachment using an unique attachment id")
    public ResponseEntity<Resource> download(
            @PathVariable String id
    ) throws Exception {
        FileObjectDescription desc =  attachmentService.getAttachmentContent(id);
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
            @PathVariable String id
    ) throws Exception {
        FileObjectDescription desc =  attachmentService.getPreviewContent(id);
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
}
