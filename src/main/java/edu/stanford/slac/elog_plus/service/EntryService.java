package edu.stanford.slac.elog_plus.service;

import com.github.javafaker.Faker;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.PersonDTO;
import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.elog_plus.api.v1.dto.*;
import edu.stanford.slac.elog_plus.api.v1.mapper.EntryMapper;
import edu.stanford.slac.elog_plus.api.v1.mapper.QueryParameterMapper;
import edu.stanford.slac.elog_plus.exception.*;
import edu.stanford.slac.elog_plus.model.Entry;
import edu.stanford.slac.elog_plus.model.Summarizes;
import edu.stanford.slac.elog_plus.repository.EntryRepository;
import edu.stanford.slac.elog_plus.utility.StringUtilities;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.assertion;
import static edu.stanford.slac.ad.eed.baselib.exception.Utility.wrapCatch;
import static java.util.Collections.emptyList;

@Service
@Log4j2
@AllArgsConstructor
public class EntryService {
    final private QueryParameterMapper queryParameterMapper;
    final private EntryRepository entryRepository;
    final private LogbookService logbookService;
    final private AttachmentService attachmentService;
    final private EntryMapper entryMapper;

    /**
     * Return the logbook id for the entry
     *
     * @param id the id of the entry for which we need to return the logbook id
     * @return the log book id which the entry belongs
     */
    public List<String> getLogbooksForAnEntryId(String id) {
        return getFullEntry(id)
                .logbooks()
                .stream()
                .map(
                        LogbookSummaryDTO::id
                )
                .toList();
    }

    /**
     * Perform the search operation on all the entries
     *
     * @param queryWithAnchorDTO the parameter for the search operation
     * @return the list of found entries that matches the input parameter
     */
    public List<EntrySummaryDTO> searchAll(QueryWithAnchorDTO queryWithAnchorDTO) {
        List<Entry> found = wrapCatch(
                () -> entryRepository.searchAll(
                        queryParameterMapper.fromDTO(
                                queryWithAnchorDTO
                        )
                ),
                -1,
                "LogService::searchAll"
        );
        return found.stream().map(
                entry -> {
                    EntrySummaryDTO es = entryMapper.toSearchResult(
                            entry
                    );
                    return es.toBuilder()
                            .shifts(
                                    getShiftsForEntry(
                                            es.logbooks().stream().map(LogbookSummaryDTO::id).toList(),
                                            es.eventAt()
                                    )
                            )
                            .build();
                }

        ).collect(Collectors.toList());
    }

    /**
     * Return the shift that are in common with all the logbooks in input
     * in the same time
     *
     * @param logbookIds the list of the logbook ids
     * @param eventAt    the time which we need the shift
     * @return the shift list
     */
    private List<LogbookShiftDTO> getShiftsForEntry(List<String> logbookIds, LocalDateTime eventAt) {
        List<LogbookShiftDTO> shifts = new ArrayList<>();
        if (logbookIds == null || logbookIds.isEmpty()) return shifts;
        if (eventAt == null) return shifts;
        for (String logbookId :
                logbookIds) {
            var shiftDTO = wrapCatch(
                    () -> logbookService.findShiftByLocalTime(
                            logbookId,
                            eventAt.toLocalTime()
                    ),
                    -1,
                    "LogService::getShiftsForEntry"
            );
            //add in case the shift is present
            shiftDTO.ifPresent(shifts::add);
        }
        return shifts;
    }

    /**
     * Find the entry id by shift name ad a date
     *
     * @param shiftId the shift name
     * @param date    the date of the summary to find
     * @return the id of te summary
     */
    public String findSummaryIdForShiftIdAndDate(String shiftId, LocalDate date) {
        Optional<Entry> summary = wrapCatch(
                () -> entryRepository.findBySummarizes_ShiftIdAndSummarizes_Date(
                        shiftId,
                        date
                ),
                -1,
                "EntryService:getSummaryIdForShiftIdAndDate"
        );
        return summary.orElseThrow(
                () -> EntryNotFound.entryNotFoundBuilder()
                        .errorCode(-2)
                        .errorDomain("EntryService:getSummaryIdForShiftIdAndDat")
                        .build()
        ).getId();
    }

    /**
     * Create a new log entry
     *
     * @param entryNewDTO is a new log information
     * @return the id of the newly created log
     */
    @Transactional
    public String createNew(EntryNewDTO entryNewDTO, PersonDTO personDTO) {
        String firstname = "";
        String lastName = "";
        String[] slittedGecos = personDTO.gecos().split(" ");
        if (slittedGecos.length >= 2) {
            firstname = slittedGecos[0];
            lastName = slittedGecos[1];
        } else if (slittedGecos.length == 1) {
            firstname = slittedGecos[0];
        }

        return createNew(
                entryMapper.fromDTO(
                        entryNewDTO,
                        firstname,
                        lastName,
                        personDTO.mail()
                )
        );
    }

    /**
     * Create a new log entry
     *
     * @param entryNewDTO is a new log information
     * @return the id of the newly created log
     */
    public Entry toModelWithAuthorization(EntryNewDTO entryNewDTO) {
        Faker faker = new Faker();
        return entryMapper.fromDTO(
                entryNewDTO,
                faker.name().firstName(),
                faker.name().lastName(),
                faker.name().username()

        );
    }

    /**
     * Create a new log entry
     *
     * @param newEntry is a new log information
     * @return the id of the newly created log
     */
    @Transactional()
    public String createNew(Entry newEntry) {
        //get and check for logbooks
        Entry finalNewEntry = newEntry;
        assertion(
                () -> (finalNewEntry.getLogbooks() != null && !finalNewEntry.getLogbooks().isEmpty()),
                ControllerLogicException
                        .builder()
                        .errorCode(-1)
                        .errorMessage("The logbooks are mandatory, entry need to belong to almost one logbook")
                        .errorDomain("LogService::createNew")
                        .build()
        );

        if (finalNewEntry.getSummarizes() != null) {
            assertion(
                    () -> (finalNewEntry.getLogbooks().size() == 1),
                    ControllerLogicException
                            .builder()
                            .errorCode(-2)
                            .errorMessage("An entry that is a summary's shift needs to belong only to one logbook")
                            .errorDomain("LogService::createNew")
                            .build()
            );

            //check for shift
            LogbookDTO lb =
                    wrapCatch(
                            () -> logbookService.getLogbook(finalNewEntry.getLogbooks().get(0)),
                            -3,
                            "EntryService:createNew"
                    );
            // check for summarization
            checkForSummarization(lb, newEntry.getSummarizes());
        } else {
            // check  all logbooks
            newEntry.getLogbooks().forEach(
                    logbookId -> {
                        assertion(
                                () -> logbookService.existById(logbookId),
                                LogbookNotFound
                                        .logbookNotFoundBuilder()
                                        .errorCode(-4)
                                        .errorDomain("LogService::createNew")
                                        .build()
                        );
                    }
            );
        }
        // check for attachment
        newEntry
                .getAttachments()
                .forEach(
                        attachmentID -> {
                            // check for presence of the attachment
                            assertion(
                                    ControllerLogicException.builder()
                                            .errorCode(-3)
                                            .errorMessage("The attachment id '%s' has not been found".formatted(attachmentID))
                                            .errorDomain("LogService::createNew")
                                            .build(),
                                    () -> attachmentService.exists(attachmentID)
                            );

                            //tag attachment as used
                            attachmentService.setInUse(attachmentID, true);
                        }
                );
        // check for tags
        newEntry
                .getTags()
                .forEach(
                        tagId -> {
                            assertion(
                                    () -> logbookService.tagIdExistInAnyLogbookIds
                                            (
                                                    tagId,
                                                    finalNewEntry.getLogbooks()
                                            ),
                                    TagNotFound.tagNotFoundBuilder()
                                            .errorCode(-4)
                                            .tagName(tagId)
                                            .errorDomain("LogService::createNew")
                                            .build()
                            );
                        }
                );

        //sanitize title and text
        Entry finalNewEntry1 = newEntry;

        assertion(
                () -> (finalNewEntry1.getTitle() != null && !finalNewEntry1.getTitle().

                        isEmpty()),
                ControllerLogicException
                        .builder()
                        .errorCode(-4)
                        .errorMessage("The title is mandatory")
                        .errorDomain("LogService::createNew")
                        .build()
        );
        newEntry.setTitle(
                StringUtilities.sanitizeEntryTitle(newEntry.getTitle())
        );

        assertion(
                () -> (finalNewEntry1.getText() != null),
                ControllerLogicException
                        .builder()
                        .errorCode(-4)
                        .errorMessage("The body is mandatory also if empty")
                        .errorDomain("LogService::createNew")
                        .build()
        );
        newEntry.setText(
                StringUtilities.sanitizeEntryText(newEntry.getText())
        );

        // remove the invalid references
        filterOutInvalidReference(newEntry);

        // other check
        Entry finalNewEntryToSave = newEntry;

        newEntry =

                wrapCatch(
                        () -> entryRepository.insert(
                                finalNewEntryToSave
                        ),
                        -5,
                        "LogService::createNew"
                );
        log.info("New entry '{}' created", newEntry.getTitle());
        return newEntry.getId();
    }

    /**
     * Return the ids of the logbooks which the parent entry is associated
     *
     * @param id the attachment id
     * @return
     */
    public List<EntrySummaryDTO> getEntriesThatOwnTheAttachment(String id) {
        return wrapCatch(
                () -> entryRepository.findAllByAttachmentsContains(id),
                -1,
                "LogService::createNew"
        ).stream()
                .map(
                        entryMapper::toSearchResult
                ).toList();
    }

    /**
     * Create and manage references for the entry to create
     * <p>
     * the reference will be checked for the existence
     *
     * @param newEntry the new entry that need to be created
     */
    public void filterOutInvalidReference(Entry newEntry) {
        if (newEntry.getReferences() == null || newEntry.getReferences().isEmpty()) return;
        List<String> validReference = new ArrayList<>();
        for (String referencedEntryId :
                newEntry.getReferences()) {
            // check for the reference entry if exists
            if (
                    wrapCatch(
                            () -> entryRepository.existsById(referencedEntryId),
                            -1,
                            "EntryService::manageNewEntryReferences"
                    )
            ) {
                // referenced entry exists
                validReference.add(referencedEntryId);
            }
        }
        newEntry.setReferences(validReference);
    }

    /**
     * Get the full entry
     *
     * @param id unique id of the entry
     * @return the full entry
     */
    public EntryDTO getFullEntry(String id) {
        return getFullEntry(
                id,
                Optional.of(false),
                Optional.of(false),
                Optional.of(false),
                Optional.of(false),
                Optional.of(false)
        );
    }

    /**
     * Return the full entry
     *
     * @param id                  the unique identifier of the log
     * @param includeFollowUps    if true the result will include the follow-up logs
     * @param includeFollowingUps if true the result will include all the following up of this
     * @param followHistory       if true the result will include the log history
     * @return the full entry
     */
    public EntryDTO getFullEntry(
            String id,
            Optional<Boolean> includeFollowUps,
            Optional<Boolean> includeFollowingUps,
            Optional<Boolean> followHistory,
            Optional<Boolean> includeReferences,
            Optional<Boolean> includeReferencedBy) {
        EntryDTO result = null;
        Entry foundEntry =
                wrapCatch(
                        () -> entryRepository.findById(id),
                        -1,
                        "LogService::getFullEntry"
                ).orElseThrow(
                        () -> EntryNotFound.entryNotFoundBuilder()
                                .errorCode(-2)
                                .errorDomain("LogService::getFullEntry")
                                .build()
                );

        // convert to model
        result = entryMapper.fromModel(
                foundEntry
        );

        if (includeFollowUps.isPresent() && includeFollowUps.get()) {
            List<EntryDTO> list = new ArrayList<>(foundEntry.getFollowUps().size());
            for (String fID : foundEntry.getFollowUps()) {
                list.add(getFullEntry(fID));

            }
            result = result.toBuilder()
                    .followUps(list)
                    .build();
        }

        if (includeFollowingUps.isPresent() && includeFollowingUps.get()) {
            Optional<Entry> followingLog = wrapCatch(
                    () -> entryRepository.findByFollowUpsContains(id),
                    -3,
                    "LogService::getFullEntry"
            );
            if (followingLog.isPresent()) {
                result = result.toBuilder()
                        .followingUp(
                                followingLog.map(
                                        entryMapper::fromModel
                                ).orElse(null)
                        )
                        .build();
            }
        }

        // fill history
        if (followHistory.isPresent() && followHistory.get()) {
            // load all the history
            List<EntryDTO> logHistory = new ArrayList<>();
            getLogHistory(id, logHistory);
            if (!logHistory.isEmpty()) {
                result = result.toBuilder()
                        .history(logHistory)
                        .build();
            }
        }

        if (includeReferences.orElse(false)) {
            // fill the references field
            result = result.toBuilder()
                    .references(
                            foundEntry.getReferences()
                                    .stream()
                                    .map(
                                            refId -> wrapCatch(
                                                    () -> entryRepository.findById(refId)
                                                            .map(
                                                                    entryMapper::toSearchResult
                                                            )
                                                            .orElseThrow(
                                                                    () -> EntryNotFound.entryNotFoundBuilder()
                                                                            .errorCode(-4)
                                                                            .errorDomain("LogService::getFullEntry")
                                                                            .build()
                                                            ),
                                                    -5,
                                                    "LogService::getFullEntry"
                                            )
                                    ).toList()
                    )
                    .build();
        } else {
            result = result.toBuilder()
                    .references(emptyList())
                    .build();
        }

        if (includeReferencedBy.orElse(false)) {
            // fill the referencedBy field
            result = result.toBuilder()
                    .referencedBy(
                            wrapCatch(
                                    () -> entryRepository.findAllByReferencesContainsAndSupersedeByExists(foundEntry.getId(), false)
                                            .stream()
                                            .map(
                                                    entryMapper::toSearchResult
                                            )
                                            .toList(),
                                    -6,
                                    "EntryMapper::getFullEntry"
                            )
                    )
                    .build();
        } else {
            result = result.toBuilder()
                    .referencedBy(emptyList())
                    .build();
        }

        // fill shift
        return result.toBuilder()
                .shifts(
                        getShiftsForEntry(
                                foundEntry.getLogbooks(),
                                foundEntry.getEventAt()
                        )
                )
                .build();
    }

    /**
     * Return the previous log in the history, the superseded log is returned without attachment
     *
     * @param newestLogID is the log of the root release for which we want the history
     * @return the log superseded byt the one identified by newestLogID
     */
    public EntryDTO getSuperseded(String newestLogID) {
        Optional<Entry> foundLog =
                wrapCatch(
                        () -> entryRepository.findBySupersedeBy(newestLogID),
                        -1,
                        "LogService::getLogHistory"
                );
        return foundLog.map(entryMapper::fromModelNoAttachment).orElse(null);
    }

    /**
     * Return all the history of the log from the newest one passed in input until the last
     *
     * @param newestLogID the log of the newest id
     * @param history     the list of the log until the last, from the one identified by newestLogID
     */
    public void getLogHistory(String newestLogID, List<EntryDTO> history) {
        if (history == null) return;
        EntryDTO prevInHistory = getSuperseded(newestLogID);
        if (prevInHistory == null) return;

        history.add(prevInHistory);
        getLogHistory(prevInHistory.id(), history);
    }

    /**
     * Create a new supersede of the log
     *
     * @param id     the identifier of the log to supersede
     * @param newLog the content of the new supersede log
     * @return the id of the new supersede log
     */
    @Transactional
    public String createNewSupersede(String id, EntryNewDTO newLog) {
        Entry supersededLog =
                wrapCatch(
                        () -> entryRepository.findById(id),
                        -1,
                        "LogService::createNewSupersede"
                ).orElseThrow(
                        () -> EntryNotFound.entryNotFoundBuilder()
                                .errorCode(-2)
                                .errorDomain("LogService::createNewSupersede")
                                .build()
                );
        assertion(
                () -> (supersededLog.getSupersedeBy() == null ||
                        supersededLog.getSupersedeBy().isEmpty())
                ,
                SupersedeAlreadyCreated.supersedeAlreadyCreatedBuilder()
                        .errorCode(-3)
                        .errorDomain("LogService::createNewSupersede")
                        .build()
        );

        // create supersede
        Entry newEntryModel = toModelWithAuthorization(newLog);
        // copy followups to the supersede entry
        newEntryModel.setFollowUps(supersededLog.getFollowUps());
        // create entry
        String newLogID = createNew(newEntryModel);
        // update supersede
        supersededLog.setSupersedeBy(newLogID);
        //update superseded entry
        wrapCatch(
                () -> entryRepository.save(supersededLog),
                -4,
                "LogService::createNewSupersede"
        );
        log.info("New supersede for '{}' created with id '{}'", supersededLog.getTitle(), newLogID);
        return newLogID;
    }

    /**
     * Create a new follow-up for a specific log
     *
     * @param id     the id for the log the need to be followed
     * @param newLog the content of the new follow-up log
     * @return the id of the new follow-up log
     */
    @Transactional
    public String createNewFollowUp(String id, EntryNewDTO newLog, PersonDTO personDTO) {
        Entry rootLog =
                wrapCatch(
                        () -> entryRepository.findById(id),
                        -1,
                        "LogService::createNewFollowUp"
                ).orElseThrow(
                        () -> EntryNotFound.entryNotFoundBuilder()
                                .errorCode(-2)
                                .errorDomain("LogService::createNewFollowUp")
                                .build()
                );
        String newFollowupLogID = createNew(newLog, personDTO);
        // update supersede
        rootLog.getFollowUps().add(newFollowupLogID);
        wrapCatch(
                () -> entryRepository.save(rootLog),
                -4,
                "LogService::createNewSupersede"
        );
        log.info("New followup for '{}' created with id '{}'", rootLog.getTitle(), newFollowupLogID);
        return newFollowupLogID;
    }

    /**
     * Return all the follow-up log for a specific one
     *
     * @param id the id of the log parent of the follow-up
     * @return the list of all the followup of the specific log identified by the id
     */
    public List<EntrySummaryDTO> getAllFollowUpForALog(String id) {
        Entry rootLog =
                wrapCatch(
                        () -> entryRepository.findById(id),
                        -1,
                        "LogService::getAllFollowUpForALog"
                ).orElseThrow(
                        () -> EntryNotFound.entryNotFoundBuilder()
                                .errorCode(-2)
                                .errorDomain("LogService::getAllFollowUpForALog")
                                .build()
                );
        assertion(
                () -> !rootLog.getFollowUps().isEmpty(),
                -3,
                "The log has not been found",
                "LogService::getAllFollowUpForALog"
        );
        List<Entry> followUp =
                wrapCatch(
                        () -> entryRepository.findAllByIdIn(rootLog.getFollowUps()),
                        -1,
                        "LogService::getAllFollowUpForALog"
                );
        return followUp
                .stream()
                .map(
                        entryMapper::toSearchResult
                )
                .collect(Collectors.toList());
    }

    public List<String> getAllTags() {
        return wrapCatch(
                entryRepository::getAllTags,
                -1,
                "LogService::getAllTags"
        );
    }

    /**
     * In case of summary information this wil check and in case will file the exception
     *
     * @param lb        the logbooks ofr the current entry
     * @param summarize the summarization information
     */
    private void checkForSummarization(LogbookDTO lb, Summarizes summarize) {
        if (summarize == null) return;
        assertion(
                () -> ((lb.shifts() != null && !lb.shifts().isEmpty())),
                ControllerLogicException
                        .builder()
                        .errorCode(-1)
                        .errorMessage("The logbooks has not any shift")
                        .errorDomain("EntryService:checkForSummarization")
                        .build()
        );

        assertion(
                () -> (summarize.getShiftId() != null && !summarize.getShiftId().isEmpty()),
                ControllerLogicException
                        .builder()
                        .errorCode(-2)
                        .errorMessage("Shift name is mandatory on summarizes object")
                        .errorDomain("EntryService:checkForSummarization")
                        .build()
        );
        assertion(
                () -> (summarize.getDate() != null),
                ControllerLogicException
                        .builder()
                        .errorCode(-3)
                        .errorMessage("Shift date is mandatory on summarizes object")
                        .errorDomain("EntryService:checkForSummarization")
                        .build()
        );
        List<ShiftDTO> allShift = lb.shifts();
        allShift.stream().filter(
                s -> s.id().compareToIgnoreCase(summarize.getShiftId()) == 0
        ).findAny().orElseThrow(
                () -> ShiftNotFound.shiftNotFoundBuilder()
                        .errorCode(-4)
                        .shiftName(summarize.getShiftId())
                        .errorDomain("EntryService:checkForSummarization")
                        .build()
        );
    }

    /**
     * check if an entry exists using the origin id
     *
     * @param originId the origin id
     */
    public boolean existsByOriginId(String originId) {
        return wrapCatch(
                () -> entryRepository.existsByOriginId(originId),
                -1,
                "EntryService::existsByOriginId"
        );
    }

    public String getIdFromOriginId(String originId) {
        Entry foundEntry = wrapCatch(
                () -> entryRepository.findByOriginId(originId),
                -1,
                "EntryService::getIdFromOriginId"
        ).orElseThrow(
                () -> EntryNotFound.entryNotFoundBuilderWithName()
                        .errorCode(-2)
                        .entryName(originId)
                        .errorDomain("EntryService::getIdFromOriginId")
                        .build()
        );
        return foundEntry.getId();
    }

    /**
     * Return all the referenced entries by this one identified by the id
     *
     * @param id the unique id of the source entry
     * @return the list of the referenced entries
     */
    public List<EntrySummaryDTO> getReferencesByEntryID(String id) {
        List<String> foundReferencesIds = wrapCatch(
                () -> entryRepository.findReferencesBySourceId(id),
                -1,
                "EntryService::getReferencesByEntryID"
        );
        if (foundReferencesIds == null) return emptyList();
        return foundReferencesIds.stream().map(
                refId -> wrapCatch(
                        () -> entryRepository.findById(refId),
                        -2,
                        "EntryService::getReferencesByEntryID"
                ).map(
                        entryMapper::toSearchResult
                ).orElseThrow(
                        () -> EntryNotFound.entryNotFoundBuilderWithName()
                                .errorCode(-3)
                                .entryName(refId)
                                .errorDomain("EntryService::getReferencesByEntryID")
                                .build()
                )
        ).toList();
    }
}
