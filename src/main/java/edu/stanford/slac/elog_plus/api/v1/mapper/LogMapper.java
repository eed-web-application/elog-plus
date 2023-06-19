package edu.stanford.slac.elog_plus.api.v1.mapper;

import edu.stanford.slac.elogplus.api.v1.dto.LogDTO;
import edu.stanford.slac.elogplus.model.Log;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface LogMapper {
    LogMapper INSTANCE = Mappers.getMapper(LogMapper.class);

    LogDTO fromModel(Log parameter);
}
