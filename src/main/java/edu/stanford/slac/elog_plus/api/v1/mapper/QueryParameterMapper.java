package edu.stanford.slac.elog_plus.api.v1.mapper;

import edu.stanford.slac.elog_plus.api.v1.dto.QueryParameterDTO;
import edu.stanford.slac.elog_plus.model.QueryParameter;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface QueryParameterMapper {
    QueryParameterMapper INSTANCE = Mappers.getMapper(QueryParameterMapper.class);
    @Mapping(target = "page", defaultValue = "0")
    @Mapping(target = "rowPerPage", defaultValue = "30")
    QueryParameter fromDTO(QueryParameterDTO parameter);
}
