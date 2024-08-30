package edu.stanford.slac.elog_plus.service;

import edu.stanford.slac.elog_plus.api.v1.dto.EntryImportDTO;
import edu.stanford.slac.elog_plus.api.v1.mapper.EntryMapper;
import edu.stanford.slac.elog_plus.exception.EntryNotFound;
import edu.stanford.slac.elog_plus.model.Entry;
import edu.stanford.slac.elog_plus.model.FileObjectDescription;
import edu.stanford.slac.elog_plus.repository.EntryRepository;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.assertion;
import static edu.stanford.slac.ad.eed.baselib.exception.Utility.wrapCatch;

@Log4j2
@Service
@AllArgsConstructor
public class ImportService {
    private final EntryMapper entryMapper;
    private final EntryService entryService;
    private final LogbookService logbookService;
    private final AttachmentService attachmentService;
    private final EntryRepository entryRepository;


    /**
     * Upload the new entry and return the ID
     *
     * @param entryToUpload the entry to upload
     * @param attachment    the attachment list to create and associate
     * @return the id of the new entry
     */
    public String importSingleEntry(EntryImportDTO entryToUpload, List<FileObjectDescription> attachment) {
        // in case we have the origin id check for record existence
        Entry supersededEntry = null;
        String newEntryId = null;
        boolean entryExists = false;

        log.info("[import {}] check if already exists", entryToUpload.title());
        if (entryToUpload.originId() != null) {
            log.info("[import {}] check for already present origin id", entryToUpload.title());
            assertion(
                    () -> !entryService.existsByOriginId(entryToUpload.originId()),
                    -2,
                    "Entry with origin id '%s' already exists".formatted(entryToUpload.originId()),
                    "ImportService::importSingleEntry"
            );
        }

        log.info("[import {}] load superseded entry", entryToUpload.title());
        if (entryToUpload.supersedeOfByOriginId() != null) {
            // fetch the entry for update
            supersededEntry = entryRepository
                    .findByOriginId(entryToUpload.supersedeOfByOriginId())
                    .orElseThrow(
                            () -> EntryNotFound.entryNotFoundBuilderWithName()
                                    .errorCode(-1)
                                    .entryName(entryToUpload.originId())
                                    .errorDomain("ImportService::importSingleEntry")
                                    .build()
                    );
        }

        log.info("[import {}] create attachment", entryToUpload.title());
        // save the attachment
        List<String> attachmentIDList = attachment.stream().map(
                att -> attachmentService.createAttachment(
                        att,
                        true
                )
        ).toList();
        log.info("[import {}] create entry", entryToUpload.title());
        Entry newEntryModel = entryMapper.fromDTO(
                entryToUpload,
                attachmentIDList
        );

        if (entryToUpload.referencesByOriginId() != null) {
            List<String> localIdReferenced = new ArrayList<>();
            for (String originalIdReference :
                    entryToUpload.referencesByOriginId()) {
                String localId = wrapCatch(
                        () -> entryService.getIdFromOriginId(originalIdReference),
                        -3,
                        "ImportService::importSingleEntry"
                );
                assertion(
                        () -> localId != null,
                        -3,
                        "No local ide found ofr the original id:%s".formatted(originalIdReference),
                        "ImportService::importSingleEntry"
                );
                localIdReferenced.add(localId);
            }
            newEntryModel.setReferences(localIdReferenced);
        }

        //fill entry with the attachment
        newEntryId = wrapCatch(
                () -> entryService.createNew(
                        newEntryModel
                ),
                -2,
                "ImportService::importSingleEntry"
        );

        // check if we have completed
        if (supersededEntry != null) {
            //the entry can be upgraded so go on with superseded check
            log.info("[import {}] update to be supersede by {}", entryToUpload.title(), entryToUpload.supersedeOfByOriginId());
            // check for supersede

            if (supersededEntry.getSupersededBy() != null) {
                Entry finalSupersededEntry = supersededEntry;
                assertion(
                        () -> finalSupersededEntry.getSupersededBy().compareTo(entryToUpload.supersedeOfByOriginId()) == 0,
                        -3,
                        "The entry is already superseded by %s".formatted(finalSupersededEntry.getSupersededBy()),
                        "ImportService::importSingleEntry"
                );
            } else {
                Entry finalSupersededEntry = supersededEntry;
                String finalNewEntryId = newEntryId;
                wrapCatch(
                        () -> {
                            entryRepository.setSupersededBy(finalSupersededEntry.getId(), finalNewEntryId);
                            return null;
                        },
                        -4,
                        ""
                );
            }
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
     * Get the logbooks ids by names
     * @param logbooks the logbooks names
     * @return the list of logbooks ids
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
