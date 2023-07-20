package edu.stanford.slac.elog_plus.api.v1.mapper;

import edu.stanford.slac.elog_plus.api.v1.dto.QueryWithAnchorDTO;
import edu.stanford.slac.elog_plus.model.QueryParameterWithAnchor;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface QueryParameterMapper {
    QueryParameterMapper INSTANCE = Mappers.getMapper(QueryParameterMapper.class);

    QueryParameterWithAnchor fromDTO(QueryWithAnchorDTO parameter);
}
