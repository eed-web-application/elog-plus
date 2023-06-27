package edu.stanford.slac.elog_plus.api.v1.mapper;

import edu.stanford.slac.elog_plus.api.v1.dto.AttachmentDTO;
import edu.stanford.slac.elog_plus.model.Attachment;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface AttachmentMapper {
    AttachmentMapper INSTANCE = Mappers.getMapper( AttachmentMapper.class );
    AttachmentDTO fromModel(Attachment model);
}
