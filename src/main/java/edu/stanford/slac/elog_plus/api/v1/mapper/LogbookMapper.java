package edu.stanford.slac.elog_plus.api.v1.mapper;

import edu.stanford.slac.elog_plus.api.v1.dto.*;
import edu.stanford.slac.elog_plus.model.Log;
import edu.stanford.slac.elog_plus.model.Logbook;
import edu.stanford.slac.elog_plus.service.AttachmentService;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import java.util.ArrayList;
import java.util.List;

@Mapper(
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface LogbookMapper {
    LogbookMapper INSTANCE = Mappers.getMapper(LogbookMapper.class);

    LogbookDTO fromModel(Logbook log);
    Logbook fromDTO(NewLogbookDTO logbookDTO);
}
