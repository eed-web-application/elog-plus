package edu.stanford.slac.elog_plus.api.v1.mapper;

import edu.stanford.slac.elog_plus.api.v1.dto.NewShiftDTO;
import edu.stanford.slac.elog_plus.model.Shift;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.time.LocalTime;

@Mapper
public interface ShiftMapper {
    ShiftMapper INSTANCE = Mappers.getMapper( ShiftMapper.class );
    @Mapping(target = "id", ignore = true)
    Shift fromDTO(NewShiftDTO model, LocalTime fromTime, LocalTime toTime);
}
