package edu.stanford.slac.elog_plus.api.v1.controller;

import edu.stanford.slac.elog_plus.annotations.RequestJsonParam;
import edu.stanford.slac.elog_plus.api.v1.dto.ApiResultResponse;
import edu.stanford.slac.elog_plus.api.v1.dto.EntryNewDTO;
import edu.stanford.slac.elog_plus.exception.ControllerLogicException;
import edu.stanford.slac.elog_plus.model.Attachment;
import edu.stanford.slac.elog_plus.model.Entry;
import edu.stanford.slac.elog_plus.model.FileObjectDescription;
import edu.stanford.slac.elog_plus.model.Logbook;
import edu.stanford.slac.elog_plus.service.UploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController()
@RequestMapping("/v1/upload")
@AllArgsConstructor
@Schema(description = "Main set of api for inject data into ELog system")
public class ImportController {
    UploadService uploadService;

    @Operation(description = "Upload data for import an entry")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(
            produces = {MediaType.APPLICATION_JSON_VALUE}
    )
    public ApiResultResponse<String> uploadEntryAndAttachment(
            @RequestJsonParam("entry") EntryNewDTO entry,
            @RequestParam(value = "attachments", required = false) MultipartFile[] attachment) throws IOException {
        List<FileObjectDescription> attachmentList = new ArrayList<>();
        if(attachment != null) {
            attachmentList = Arrays.stream(attachment).map(
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
                            throw ControllerLogicException.of(
                                    -1,
                                    e.getMessage(),
                                    "ImportController:uploadEntryAndAttachment"
                            );
                        }
                    }
            ).collect(Collectors.toList());
        }
        return ApiResultResponse.of(uploadService.uploadSingleEntry(entry, attachmentList));
    }
}
