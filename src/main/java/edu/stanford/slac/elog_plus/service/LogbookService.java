package edu.stanford.slac.elog_plus.service;

import edu.stanford.slac.elog_plus.api.v1.dto.LogbookDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.NewLogbookDTO;
import edu.stanford.slac.elog_plus.api.v1.mapper.LogbookMapper;
import edu.stanford.slac.elog_plus.model.Logbook;
import edu.stanford.slac.elog_plus.repository.LogbookRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import static edu.stanford.slac.elog_plus.exception.Utility.wrapCatch;

@Service
@AllArgsConstructor
public class LogbookService {
    LogbookRepository logbookRepository;

    /**
     * Return all the logbook
     * @return the lis tof all logbook
     */
    public List<LogbookDTO> getAllLogbook() {
        return wrapCatch(
                () -> logbookRepository.findAll()
                        .stream()
                        .map(
                                LogbookMapper.INSTANCE::fromModel
                        ).collect(Collectors.toList()),
                -1,
                "LogbookService::getAllLogbook"
        );
    }

    /**
     * Create a new logbook
     * @param newLogbookDTO the new logbook
     * @return the id of the newly created logbook
     */
    public String createNew(NewLogbookDTO newLogbookDTO) {
        Logbook newLogbook = wrapCatch(
                () -> logbookRepository.save(
                        LogbookMapper.INSTANCE.fromDTO(newLogbookDTO)
                ),
                -1,
                "LogbookService::createNew");
        return newLogbook.getId();
    }

    /**
     * Check if a logbook with a specific name exists
     * @param logbook the name of the logbook to check
     * @return true if the logbook exists
     */
    public Boolean exist(String logbook) {
        return wrapCatch(
                () -> logbookRepository.existsByName(
                        logbook
                ),
                -1,
                "LogbookService::exist");
    }
}
