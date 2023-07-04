package edu.stanford.slac.elog_plus.service;

import com.github.javafaker.Faker;
import edu.stanford.slac.elog_plus.api.v1.dto.*;
import edu.stanford.slac.elog_plus.api.v1.mapper.LogMapper;
import edu.stanford.slac.elog_plus.api.v1.mapper.QueryParameterMapper;
import edu.stanford.slac.elog_plus.api.v1.mapper.QueryResultMapper;
import edu.stanford.slac.elog_plus.model.Log;
import edu.stanford.slac.elog_plus.repository.LogRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static edu.stanford.slac.elog_plus.exception.Utility.assertion;
import static edu.stanford.slac.elog_plus.exception.Utility.wrapCatch;

@Service
@AllArgsConstructor
public class LogService {
    final private LogRepository logRepository;
    final private AttachmentService attachmentService;
    final private QueryParameterConfigurationDTO queryParameterConfigurationDTO;

    public QueryPagedResultDTO<SearchResultLogDTO> searchAll(QueryParameterDTO queryParameter) {
        Page<Log> found = logRepository.searchAll(
                QueryParameterMapper.INSTANCE.fromDTO(
                        queryParameter
                )
        );
        return QueryResultMapper.from(
                found.map(
                        log -> {
                            return LogMapper.INSTANCE.toSearchResultFromDTO(log, attachmentService);
                        }
                )
        );
    }

    public List<SearchResultLogDTO> searchAll(QueryWithAnchorDTO queryWithAnchorDTO) {
        List<Log> found = logRepository.searchAll(
                QueryParameterMapper.INSTANCE.fromDTO(
                        queryWithAnchorDTO
                )
        );
        return found.stream().map(
                log -> {
                    return LogMapper.INSTANCE.toSearchResultFromDTO(log, attachmentService);
                }

        ).collect(Collectors.toList());
    }

    /**
     * Create a new log entry
     *
     * @param newLogDTO is a new log information
     * @return the id of the newly created log
     */
    @Transactional(propagation = Propagation.NESTED)
    public String createNew(NewLogDTO newLogDTO) {
        Faker faker = new Faker();
        Log newLog = LogMapper.INSTANCE.fromDTO(newLogDTO, faker.name().firstName(), faker.name().lastName(), faker.name().username());
        //check logbook
        assertion(
                () -> queryParameterConfigurationDTO.logbook().contains(newLogDTO.logbook()),
                -1,
                "The logbook is not valid",
                "LogService::createNew"
        );

        // other check
        Log finalNewLog = newLog;
        newLog =
                wrapCatch(
                        () -> logRepository.insert(
                                finalNewLog
                        ),
                        -2,
                        "LogService::createNew"
                );
        return newLog.getId();

    }

    public LogDTO getFullLog(String id) {
        return getFullLog(id, Optional.of(false));
    }

    /**
     * return the full log
     *
     * @param id               the unique identifier of the log
     * @param includeFollowUps
     * @return the full log description
     */
    public LogDTO getFullLog(String id, Optional<Boolean> includeFollowUps) {
        LogDTO result = null;
        Optional<Log> foundLog =
                wrapCatch(
                        () -> logRepository.findById(id),
                        -1,
                        "LogService::getFullLog"
                );
        assertion(
                foundLog::isPresent,
                -2,
                "The log has not been found",
                "LogService::getFullLog"
        );

        if (includeFollowUps.isPresent() && includeFollowUps.get()) {
            result = LogMapper.INSTANCE.fromModelWithFollowUpsAndAttachement(
                    foundLog.get(),
                    this,
                    attachmentService
            );
        } else {
            result = LogMapper.INSTANCE.fromModel(
                    foundLog.get(),
                    attachmentService
            );
        }
        return result;
    }

    /**
     * Create a new supersede of the log
     *
     * @param id     the identifier of the log to supersede
     * @param newLog the content of the new supersede log
     * @return the id of the new supersede log
     */
    @Transactional
    public String createNewSupersede(String id, NewLogDTO newLog) {
        Optional<Log> supersededLog =
                wrapCatch(
                        () -> logRepository.findById(id),
                        -1,
                        "LogService::createNewSupersede"
                );
        assertion(
                supersededLog::isPresent,
                -2,
                "The log to supersede has not been found",
                "LogService::getFullLog"
        );
        Log l = supersededLog.get();
        assertion(
                () -> (l.getSupersedeBy() == null ||
                        l.getSupersedeBy().isEmpty())
                ,
                -3,
                "The log has already a supersede",
                "LogService::getFullLog"
        );
        //create supersede
        String newLogID = createNew(newLog);
        // update supersede
        l.setSupersedeBy(newLogID);
        wrapCatch(
                () -> logRepository.save(l),
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
    public String createNewFollowUp(String id, NewLogDTO newLog) {
        Optional<Log> rootLog =
                wrapCatch(
                        () -> logRepository.findById(id),
                        -1,
                        "LogService::createNewFollowUp"
                );
        assertion(
                rootLog::isPresent,
                -2,
                "The log to follow-up has not been found",
                "LogService::createNewFollowUp"
        );
        Log l = rootLog.get();
        String newFollowupLogID = createNew(newLog);
        // update supersede
        l.getFollowUp().add(newFollowupLogID);
        wrapCatch(
                () -> logRepository.save(l),
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
    public List<SearchResultLogDTO> getAllFollowUpForALog(String id) {
        Optional<Log> rootLog =
                wrapCatch(
                        () -> logRepository.findById(id),
                        -1,
                        "LogService::getAllFollowUpForALog"
                );
        assertion(
                rootLog::isPresent,
                -2,
                "The log has not been found",
                "LogService::getAllFollowUpForALog"
        );

        assertion(
                () -> (rootLog.get().getFollowUp().size() > 0),
                -3,
                "The log has not been found",
                "LogService::getAllFollowUpForALog"
        );
        List<Log> followUp =
                wrapCatch(
                        () -> logRepository.findAllByIdIn(rootLog.get().getFollowUp()),
                        -1,
                        "LogService::getAllFollowUpForALog"
                );
        return followUp
                .stream()
                .map(
                        l -> LogMapper.INSTANCE.toSearchResultFromDTO(l, attachmentService)
                )
                .collect(Collectors.toList());
    }
}
