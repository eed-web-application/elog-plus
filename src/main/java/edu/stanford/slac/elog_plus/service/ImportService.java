package edu.stanford.slac.elog_plus.service;

import edu.stanford.slac.elog_plus.api.v1.dto.EntryDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.EntryImportDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.LogbookDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.NewTagDTO;
import edu.stanford.slac.elog_plus.exception.ControllerLogicException;
import edu.stanford.slac.elog_plus.exception.EntryNotFound;
import edu.stanford.slac.elog_plus.exception.LogbookNotFound;
import edu.stanford.slac.elog_plus.model.Entry;
import edu.stanford.slac.elog_plus.model.FileObjectDescription;
import edu.stanford.slac.elog_plus.repository.EntryRepository;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static edu.stanford.slac.elog_plus.exception.Utility.assertion;
import static edu.stanford.slac.elog_plus.exception.Utility.wrapCatch;

@Log4j2
@Service
@AllArgsConstructor
public class ImportService {
    private EntryService entryService;
    private LogbookService logbookService;
    private AttachmentService attachmentService;
    private EntryRepository entryRepository;


    /**
     * Upload the new entry and return the ID
     *
     * @param entryToUpload the entry to upload
     * @param attachment    the attachment list to create and associate
     * @return the id of the new entry
     */
    public String importSingleEntry(EntryImportDTO entryToUpload, List<FileObjectDescription> attachment) {
        // in case we have the origin id check for record existence
        String newEntryId = null;
        boolean entryExists = false;

        if (entryToUpload.originId() != null) {
            log.info("[import {}] check for already present origin id", entryToUpload.title());
            entryExists = wrapCatch(
                    () -> entryService.existsByOriginId(entryToUpload.originId()),
                    -1,
                    "ImportService::importSingleEntry"
            );
        }

        if (!entryExists) {
            log.info("[import {}] create attachment", entryToUpload.title());
            // save the attachment
            List<String> attachmentIDList = attachment.stream().map(
                    att -> attachmentService.createAttachment(
                            att,
                            true
                    )
            ).toList();
            log.info("[import {}] create entry", entryToUpload.title());
            //fill entry with the attachment
            newEntryId = entryService.createNew(
                    entryToUpload,
                    attachmentIDList
            );
        }

        // check if have completed
        if (entryToUpload.originId() == null) return newEntryId;
        if(entryToUpload.supersedeOf() == null && entryToUpload.followUpOf()==null) return newEntryId;

        // fetch the entry for update
        Entry entryDTO = entryRepository
                .findByOriginId(entryToUpload.supersedeOf())
                .orElseThrow(
                        () -> EntryNotFound.entryNotFoundBuilderWithName()
                                .errorCode(-2)
                                .entryName(entryToUpload.originId())
                                .errorDomain("ImportService::importSingleEntry")
                                .build()
                );

        //the entry can be upgraded so go on with superseded check
        if(entryToUpload.supersedeOf() != null) {
            log.info("[import {}] update to be supersede by {}", entryToUpload.title(), entryToUpload.supersedeOf());
            // check for supersede

            if(entryDTO.getSupersedeBy() != null) {
                assertion(
                        ()->entryDTO.getSupersedeBy().compareTo(entryToUpload.supersedeOf()) == 0,
                        -2,
                        "The entry is already superseded by %s".formatted(entryDTO.getSupersedeBy()),
                        "ImportService::importSingleEntry"
                );
            } else {
                entryDTO.setSupersedeBy(entryToUpload.supersedeOf());
            }
        }

        //the entry can be upgraded so go on with superseded check
        if(entryToUpload.referencesTo() != null) {
//            log.info("[import {}] update it to follow up {}", entryToUpload.title(), entryToUpload.followUpOf());
//            // check for supersede
//
//            if(entryDTO.getSupersedeBy() != null) {
//                assertion(
//                        ()->entryDTO.getSupersedeBy().compareTo(entryToUpload.supersedeOf()) == 0,
//                        -2,
//                        "The entry is already superseded by %s".formatted(entryDTO.getSupersedeBy()),
//                        "ImportService::importSingleEntry"
//                );
//            } else {
//                entryDTO.setSupersedeBy(entryToUpload.supersedeOf());
//            }
        }
        return newEntryId;
    }

    /**
     * Create tags name on all logbooks
     *
     * @param tags     all tag names
     * @param logbooks all logbook names
     * @return the list of all tags ids
     */
    public List<String> ensureTagsNamesOnAllLogbooks(List<String> tags, List<String> logbooks) {
        List<String> tagIds = new ArrayList<>();
        for (String logbookName :
                logbooks) {
            var lb = wrapCatch(
                    () -> logbookService.getLogbookByName(logbookName),
                    -1,
                    "ImportService::ensureTagByNameAndLogbooks"
            );

            for (String tagName :
                    tags) {
                tagIds.add(wrapCatch(
                                () -> logbookService.ensureTag(lb.id(), tagName),
                                -2,
                                "ImportService::ensureTagByNameAndLogbooks"
                        )
                );
            }

        }
        return tagIds;
    }

    /**
     * @param logbooks
     * @return
     */
    public List<String> getLogbooksIdsByNames(List<String> logbooks) {
        List<String> logBooksId = new ArrayList<>();
        for (String logbookName :
                logbooks) {
            var lb = wrapCatch(
                    () -> logbookService.getLogbookByName(logbookName),
                    -1,
                    "ImportService::ensureTagByNameAndLogbooks"
            );
            logBooksId.add(lb.id());
        }
        return logBooksId;
    }
}
