package edu.stanford.slac.elog_plus.api.v1.mapper;

import edu.stanford.slac.elog_plus.api.v1.dto.NewShiftDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.ShiftDTO;
import edu.stanford.slac.elog_plus.model.Shift;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import java.time.LocalTime;
import java.util.List;

@Mapper(
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = "spring"
)
public abstract class ShiftMapper {
    @Mapping(target = "id", ignore = true)
    public abstract Shift fromDTO(NewShiftDTO model, LocalTime fromTime, LocalTime toTime);
    public abstract NewShiftDTO toNewDTO(ShiftDTO dto);

    public abstract Shift fromDTO(ShiftDTO shiftDTO);
    public abstract Shift fromDTO(NewShiftDTO shiftDTO);
    public abstract ShiftDTO fromModel(Shift shift);
    public abstract List<Shift> fromDTO(List<ShiftDTO> allNewShift);
}
