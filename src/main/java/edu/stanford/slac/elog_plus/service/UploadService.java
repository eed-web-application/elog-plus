package edu.stanford.slac.elog_plus.service;

import edu.stanford.slac.elog_plus.api.v1.dto.EntryImportDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.EntryNewDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.LogbookDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.NewTagDTO;
import edu.stanford.slac.elog_plus.model.FileObjectDescription;
import edu.stanford.slac.elog_plus.model.Logbook;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.List;

@Log4j2
@Service
@AllArgsConstructor
public class UploadService {
    private EntryService entryService;
    private LogbookService logbookService;
    private AttachmentService attachmentService;


    /**
     * Upload the new entry and return the ID
     *
     * @param newEntry   the entry to upload
     * @param attachment the attachment list to create and associate
     * @return the id of the new entry
     */
    public String uploadSingleEntry(EntryImportDTO newEntry, List<FileObjectDescription> attachment) {
        // save the attachment
        List<String> attachmentIDList = attachment.stream().map(
                att -> attachmentService.createAttachment(
                        att,
                        true
                )
        ).toList();

        // ensure logbooks exists
        LogbookDTO lb = logbookService.getLogbookByName(newEntry.logbook());

        // ensure tags
        if (newEntry.tags() != null) {
            newEntry.tags().forEach(
                    tagName -> {
                        if (!logbookService.tagExistForLogbook(lb.id(), tagName)) {
                            logbookService.createNewTag(
                                    lb.id(),
                                    NewTagDTO
                                            .builder()
                                            .name(tagName)
                                            .build()
                            );
                        }
                    }
            );
        }

        //fill entry with the attachment
        newEntry.attachments().addAll(attachmentIDList);

        return entryService.createNew(
                newEntry
        );
    }
}
