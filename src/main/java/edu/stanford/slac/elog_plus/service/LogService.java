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
import org.springframework.transaction.annotation.Transactional;

import static edu.stanford.slac.elog_plus.exception.Utility.assertion;
import static edu.stanford.slac.elog_plus.exception.Utility.wrapCatch;

@Service
@AllArgsConstructor
public class LogService {
    final private LogRepository logRepository;
    final private QueryParameterConfigurationDTO queryParameterConfigurationDTO;

    public QueryPagedResultDTO<LogDTO> searchAll(QueryParameterDTO queryParameter) {
        Page<Log> found = logRepository.searchAll(
                QueryParameterMapper.INSTANCE.fromDTO(
                        queryParameter
                )
        );
        return QueryResultMapper.from(
                found.map(
                        LogMapper.INSTANCE::fromModel
                )
        );
    }

    /**
     * Create a new log entry
     *
     * @param newLogDTO is a new log information
     * @return the id of the newly created log
     */
    @Transactional
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
}
