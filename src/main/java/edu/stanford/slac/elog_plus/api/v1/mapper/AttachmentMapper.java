package edu.stanford.slac.elog_plus.api.v1.mapper;

import edu.stanford.slac.elog_plus.api.v1.dto.AttachmentDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.ObjectListResultDTO;
import edu.stanford.slac.elog_plus.model.Attachment;
import edu.stanford.slac.elog_plus.model.ObjectListResult;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = "spring"
)
public abstract class AttachmentMapper {
    public abstract AttachmentDTO fromModel(Attachment model);
    public abstract ObjectListResultDTO fromModel(ObjectListResult model);
}
