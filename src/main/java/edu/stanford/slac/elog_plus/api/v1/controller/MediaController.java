package edu.stanford.slac.elog_plus.api.v1.controller;

import edu.stanford.slac.elog_plus.api.v1.dto.FileEntryDTO;
import edu.stanford.slac.elog_plus.service.v0.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController()
@RequestMapping("/v1/media")
@AllArgsConstructor
@Schema(description = "Api set for media management")
public class MediaController {
    private final StorageService storageService;

    @PostMapping(
            path = "/download",
            produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE},
            consumes = {MediaType.APPLICATION_JSON_VALUE})
    @Operation(summary = "Download a content from a file entry")
    public ResponseEntity<Resource> download(@RequestBody() FileEntryDTO fileEntry) throws Exception {
        StorageService.GetFileResult fs = new StorageService.GetFileResult();
        storageService.getFileObject(fileEntry.path(), fs);
        InputStreamResource resource = new InputStreamResource(fs.getIs());
        MediaType mediaType = MediaType.valueOf(fs.getContentType());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        ContentDisposition disposition = ContentDisposition
                // 3.2
                .inline() // or .attachment()
                // 3.1
                .filename(fs.getFileName())
                .build();
        headers.setContentDisposition(disposition);
        return new ResponseEntity<>(resource, headers, HttpStatus.OK);
    }

    @GetMapping(
            path = "/download",
            produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE})
    @Operation(summary = "Download a content from a file entry")
    public ResponseEntity<Resource> download(@RequestParam() String url) throws Exception {
        StorageService.GetFileResult fs = new StorageService.GetFileResult();
        storageService.getFileObject(url, fs);
        InputStreamResource resource = new InputStreamResource(fs.getIs());
        MediaType mediaType = MediaType.valueOf(fs.getContentType());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        ContentDisposition disposition = ContentDisposition
                // 3.2
                .inline() // or .attachment()
                // 3.1
                .filename(fs.getFileName())
                .build();
        headers.setContentDisposition(disposition);
        return new ResponseEntity<>(resource, headers, HttpStatus.OK);
    }
}
