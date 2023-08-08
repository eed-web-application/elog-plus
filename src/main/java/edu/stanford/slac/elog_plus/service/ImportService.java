package edu.stanford.slac.elog_plus.service;

import edu.stanford.slac.elog_plus.api.v1.dto.EntryImportDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.LogbookDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.NewTagDTO;
import edu.stanford.slac.elog_plus.exception.ControllerLogicException;
import edu.stanford.slac.elog_plus.exception.LogbookNotFound;
import edu.stanford.slac.elog_plus.model.FileObjectDescription;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static edu.stanford.slac.elog_plus.exception.Utility.assertion;

@Log4j2
@Service
@AllArgsConstructor
public class ImportService {
    private EntryService entryService;
    private LogbookService logbookService;
    private AttachmentService attachmentService;


    /**
     * Upload the new entry and return the ID
     *
     * @param entryToUpload   the entry to upload
     * @param attachment the attachment list to create and associate
     * @return the id of the new entry
     */
    @Transactional
    public String importSingleEntry(EntryImportDTO entryToUpload, List<FileObjectDescription> attachment) {
        // in case we have the origin id check for record existence
        if(entryToUpload.originId()!=null) {
           assertion(
                   ()->!entryService.existsByOriginId(entryToUpload.originId()),
                   ControllerLogicException
                           .builder()
                           .errorCode(-1)
                           .errorMessage(String.format("Another entry already exists with the origin id %s", entryToUpload.originId()))
                           .errorDomain("ImportService:importSingleEntry")
                           .build()
           );
        }
        // save the attachment
        List<String> attachmentIDList = attachment.stream().map(
                att -> attachmentService.createAttachment(
                        att,
                        true
                )
        ).toList();

        // ensure logbooks exists
        assertion(
                ()->logbookService.exist(entryToUpload.logbook()),
                LogbookNotFound.logbookNotFoundBuilder()
                        .errorCode(-2)
                        .errorDomain("ImportService:importSingleEntry")
                        .build()
        );

        //fill entry with the attachment
        return entryService.createNew(
                entryToUpload,
                attachmentIDList
        );
    }
}
