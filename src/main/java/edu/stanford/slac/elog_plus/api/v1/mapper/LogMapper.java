package edu.stanford.slac.elog_plus.api.v1.mapper;

import edu.stanford.slac.elog_plus.api.v1.dto.LogDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.NewLogDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.SearchResultLogDTO;
import edu.stanford.slac.elog_plus.model.Log;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface LogMapper {
    LogMapper INSTANCE = Mappers.getMapper(LogMapper.class);

    @Mapping( target = "author", expression = "java(log.getFirstName() + \" \" + log.getLastName())")
    LogDTO fromModel(Log log);

    @Mapping( target = "author", expression = "java(log.getFirstName() + \" \" + log.getLastName())")
    SearchResultLogDTO toSearchResultFromDTO(Log log);

    Log fromDTO(NewLogDTO newLogDTO, String firstName, String lastName, String userName);
}
