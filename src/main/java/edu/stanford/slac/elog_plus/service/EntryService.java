package edu.stanford.slac.elog_plus.service;

import com.github.javafaker.Faker;
import edu.stanford.slac.elog_plus.api.v1.dto.*;
import edu.stanford.slac.elog_plus.api.v1.mapper.EntryMapper;
import edu.stanford.slac.elog_plus.api.v1.mapper.QueryParameterMapper;
import edu.stanford.slac.elog_plus.exception.*;
import edu.stanford.slac.elog_plus.model.Entry;
import edu.stanford.slac.elog_plus.repository.EntryRepository;
import lombok.AllArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static edu.stanford.slac.elog_plus.exception.Utility.assertion;
import static edu.stanford.slac.elog_plus.exception.Utility.wrapCatch;

@Service
@AllArgsConstructor
public class EntryService {
    final private TagService tagService;
    final private EntryRepository entryRepository;
    final private LogbookService logbookService;
    final private AttachmentService attachmentService;

    public List<EntrySummaryDTO> searchAll(QueryWithAnchorDTO queryWithAnchorDTO) {
        List<Entry> found = wrapCatch(
                () -> entryRepository.searchAll(
                        QueryParameterMapper.INSTANCE.fromDTO(
                                queryWithAnchorDTO
                        )
                ),
                -1,
                "LogService::searchAll"
        );
        return found.stream().map(
                log -> {
                    return EntryMapper.INSTANCE.toSearchResultFromDTO(log, attachmentService);
                }

        ).collect(Collectors.toList());
    }

    /**
     * Create a new log entry
     *
     * @param entryNewDTO is a new log information
     * @return the id of the newly created log
     */
    @Transactional(propagation = Propagation.NESTED)
    public String createNew(EntryNewDTO entryNewDTO) {
        Faker faker = new Faker();
        Entry newEntry = EntryMapper.INSTANCE.fromDTO(entryNewDTO, faker.name().firstName(), faker.name().lastName(), faker.name().username());

        if (newEntry.getTags() != null) {
            List<String> tagsNormalizedNames = newEntry
                    .getTags()
                    .stream()
                    .map(
                            tagService::tagNameNormalization
                    )
                    .toList();
            newEntry.setTags(
                    tagsNormalizedNames
            );
        }

        //check logbook
        assertion(
                () -> logbookService.exist(entryNewDTO.logbook()),
                NotebookNotFound.notebookNotFoundBuilder()
                        .errorCode(-1)
                        .errorDomain("LogService::createNew")
                        .build()
        );

        newEntry
                .getTags()
                .forEach(
                        tagName -> {
                            assertion(
                                    () -> tagService.existsByName(tagName),
                                    TagNotFound.tagNotFoundBuilder()
                                            .errorCode(-2)
                                            .tagName(tagName)
                                            .errorDomain("LogService::createNew")
                                            .build()
                            );
                        }
                );

        newEntry
                .getAttachments()
                .forEach(
                        attachementID -> {
                            if (!attachmentService.exists(attachementID)) {
                                String error = String.format("The attachment id '%s' has not been found", attachementID);
                                throw ControllerLogicException.of(
                                        -3,
                                        error,
                                        "LogService::createNew"
                                );
                            }
                        }
                );

        //sanitize title and text
        newEntry.setTitle(
                Jsoup.clean(newEntry.getTitle(), Safelist.basic())
        );
        newEntry.setText(
                Jsoup.clean(newEntry.getText(), Safelist.basicWithImages())
        );

        // other check
        Entry finalNewEntry = newEntry;
        newEntry =
                wrapCatch(
                        () -> entryRepository.insert(
                                finalNewEntry
                        ),
                        -2,
                        "LogService::createNew"
                );
        return newEntry.getId();

    }

    public EntryDTO getFullLog(String id) {
        return getFullLog(id, Optional.of(false), Optional.of(false), Optional.of(false));
    }

    /**
     * return the full log
     *
     * @param id                  the unique identifier of the log
     * @param includeFollowUps    if true the result will include the follow-up logs
     * @param includeFollowingUps if true the result will include all the following up of this
     * @param followHistory       if true the result will include the log history
     * @return the full log description
     */
    public EntryDTO getFullLog(String id, Optional<Boolean> includeFollowUps, Optional<Boolean> includeFollowingUps, Optional<Boolean> followHistory) {
        EntryDTO result = null;
        Optional<Entry> foundEntry =
                wrapCatch(
                        () -> entryRepository.findById(id),
                        -1,
                        "LogService::getFullLog"
                );
        assertion(
                foundEntry::isPresent,
                EntryNotFound.entryNotFoundBuilder()
                        .errorCode(-2)
                        .errorDomain("LogService::getFullLog")
                        .build()
        );

        result = EntryMapper.INSTANCE.fromModel(
                foundEntry.get(),
                attachmentService
        );

        if (includeFollowUps.isPresent() && includeFollowUps.get()) {
            List<EntryDTO> list = new ArrayList<>(foundEntry.get().getFollowUp().size());
            for (String fID : foundEntry.get().getFollowUp()) {
                list.add(getFullLog(fID));

            }
            result = result.toBuilder()
                    .followUp(list)
                    .build();
        }

        if (includeFollowingUps.isPresent() && includeFollowingUps.get()) {
            Optional<Entry> followingLog = wrapCatch(
                    () -> entryRepository.findByFollowUpContains(id),
                    -3,
                    "LogService::getFullLog"
            );
            if (followingLog.isPresent()) {
                result = result.toBuilder()
                        .followingUp(
                                followingLog.map(
                                        l -> EntryMapper.INSTANCE.fromModel(l, attachmentService)
                                ).orElse(null)
                        )
                        .build();
            }

        }

        if (followHistory.isPresent() && followHistory.get()) {
            // load all the history
            List<EntryDTO> logHistory = new ArrayList<>();
            getLogHistory(id, logHistory);
            if (logHistory.size() > 0) {
                result = result.toBuilder()
                        .history(logHistory)
                        .build();
            }
        }
        return result;
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
        return foundLog.map(EntryMapper.INSTANCE::fromModelNoAttachment).orElse(null);
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
        Optional<Entry> supersededLog =
                wrapCatch(
                        () -> entryRepository.findById(id),
                        -1,
                        "LogService::createNewSupersede"
                );
        assertion(
                supersededLog::isPresent,
                EntryNotFound.entryNotFoundBuilder()
                        .errorCode(-2)
                        .errorDomain("LogService::createNewSupersede")
                        .build()
        );
        Entry l = supersededLog.get();
        assertion(
                () -> (l.getSupersedeBy() == null ||
                        l.getSupersedeBy().isEmpty())
                ,
                SupersedeAlreadyCreated.supersedeAlreadyCreatedBuilder()
                        .errorCode(-3)
                        .errorDomain("LogService::createNewSupersede")
                        .build()
        );
        //create supersede
        String newLogID = createNew(newLog);
        // update supersede
        l.setSupersedeBy(newLogID);
        wrapCatch(
                () -> entryRepository.save(l),
                -4,
                "LogService::createNewSupersede"
        );
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
    public String createNewFollowUp(String id, EntryNewDTO newLog) {
        Optional<Entry> rootLog =
                wrapCatch(
                        () -> entryRepository.findById(id),
                        -1,
                        "LogService::createNewFollowUp"
                );
        assertion(
                rootLog::isPresent,
                -2,
                "The log to follow-up has not been found",
                "LogService::createNewFollowUp"
        );
        Entry l = rootLog.get();
        String newFollowupLogID = createNew(newLog);
        // update supersede
        l.getFollowUp().add(newFollowupLogID);
        wrapCatch(
                () -> entryRepository.save(l),
                -4,
                "LogService::createNewSupersede"
        );
        return newFollowupLogID;
    }

    /**
     * Return all the follow-up log for a specific one
     *
     * @param id the id of the log parent of the follow-up
     * @return the list of all the followup of the specific log identified by the id
     */
    public List<EntrySummaryDTO> getAllFollowUpForALog(String id) {
        Optional<Entry> rootLog =
                wrapCatch(
                        () -> entryRepository.findById(id),
                        -1,
                        "LogService::getAllFollowUpForALog"
                );
        assertion(
                rootLog::isPresent,
                EntryNotFound.entryNotFoundBuilder()
                        .errorCode(-2)
                        .errorDomain("LogService::getAllFollowUpForALog")
                        .build()
        );
        assertion(
                () -> (rootLog.get().getFollowUp().size() > 0),
                -3,
                "The log has not been found",
                "LogService::getAllFollowUpForALog"
        );
        List<Entry> followUp =
                wrapCatch(
                        () -> entryRepository.findAllByIdIn(rootLog.get().getFollowUp()),
                        -1,
                        "LogService::getAllFollowUpForALog"
                );
        return followUp
                .stream()
                .map(
                        l -> EntryMapper.INSTANCE.toSearchResultFromDTO(l, attachmentService)
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
}
