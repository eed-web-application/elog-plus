package edu.stanford.slac.elog_plus.service;

import edu.stanford.slac.elogplus.api.v1.dto.LogDTO;
import edu.stanford.slac.elogplus.api.v1.dto.QueryPagedResultDTO;
import edu.stanford.slac.elogplus.api.v1.dto.QueryParameterDTO;
import edu.stanford.slac.elogplus.api.v1.mapper.LogMapper;
import edu.stanford.slac.elogplus.api.v1.mapper.QueryParameterMapper;
import edu.stanford.slac.elogplus.api.v1.mapper.QueryResultMapper;
import edu.stanford.slac.elogplus.model.Log;
import edu.stanford.slac.elogplus.repository.LogRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class LogService {
    final private LogRepository logRepository;

    public QueryPagedResultDTO<LogDTO> searchAll(QueryParameterDTO queryParameter) {
        Page<Log> found = logRepository.searchAll(
                QueryParameterMapper.INSTANCE.fromDTO(
                        queryParameter
                )
        );
        return QueryResultMapper.from(
                found.map(
                        (model) -> LogMapper.INSTANCE.fromModel(model)
                )
        );
    }
}
