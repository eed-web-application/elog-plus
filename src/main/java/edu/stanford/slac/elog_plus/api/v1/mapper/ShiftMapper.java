package edu.stanford.slac.elog_plus.api.v1.mapper;

import edu.stanford.slac.elog_plus.api.v1.dto.NewShiftDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.ShiftDTO;
import edu.stanford.slac.elog_plus.model.Shift;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import java.time.LocalTime;

@Mapper(
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ShiftMapper {
    ShiftMapper INSTANCE = Mappers.getMapper( ShiftMapper.class );
    @Mapping(target = "id", ignore = true)
    Shift fromDTO(NewShiftDTO model, LocalTime fromTime, LocalTime toTime);
    NewShiftDTO toNewDTO(ShiftDTO dto);

    Shift fromDTO(ShiftDTO shiftDTO);
    Shift fromDTO(NewShiftDTO shiftDTO);
    ShiftDTO fromModel(Shift shift);
}
