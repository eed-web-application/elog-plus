package edu.stanford.slac.elog_plus.api.v1.mapper;

import edu.stanford.slac.elog_plus.api.v1.dto.*;
import edu.stanford.slac.elog_plus.model.Logbook;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

@Mapper(
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = "spring"
)
public abstract class LogbookMapper {
    public abstract LogbookSummaryDTO fromModelToSummaryDTO(Logbook log);
    public abstract LogbookDTO fromModel(Logbook log);
    public abstract Logbook fromDTO(NewLogbookDTO logbookDTO);
    public abstract Logbook fromDTO(LogbookDTO logbookDTO);
    public abstract Logbook fromDTO(UpdateLogbookDTO logbookDTO);
}
