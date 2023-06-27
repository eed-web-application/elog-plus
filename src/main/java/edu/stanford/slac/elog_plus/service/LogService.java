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

import java.util.Optional;

import static edu.stanford.slac.elog_plus.exception.Utility.assertion;
import static edu.stanford.slac.elog_plus.exception.Utility.wrapCatch;

@Service
@AllArgsConstructor
public class LogService {
    final private LogRepository logRepository;
    final private QueryParameterConfigurationDTO queryParameterConfigurationDTO;

    public QueryPagedResultDTO<SearchResultLogDTO> searchAll(QueryParameterDTO queryParameter) {
        Page<Log> found = logRepository.searchAll(
                QueryParameterMapper.INSTANCE.fromDTO(
                        queryParameter
                )
        );
        return QueryResultMapper.from(
                found.map(
                        LogMapper.INSTANCE::toSearchResultFromDTO
                )
        );
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

    /**
     * return the full log
     *
     * @param id the unique identifier of the log
     * @return the full log description
     */
    public LogDTO getFullLog(String id) {
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
        return LogMapper.INSTANCE.fromModel(foundLog.get());
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
}
