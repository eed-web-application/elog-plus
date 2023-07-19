package edu.stanford.slac.elog_plus.api.v1.mapper;

import edu.stanford.slac.elog_plus.api.v1.dto.AttachmentDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.LogDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.NewEntryDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.EntrySummaryDTO;
import edu.stanford.slac.elog_plus.model.Log;
import edu.stanford.slac.elog_plus.service.AttachmentService;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import java.util.ArrayList;
import java.util.List;

@Mapper(
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = "spring")
public interface LogMapper {
    LogMapper INSTANCE = Mappers.getMapper(LogMapper.class);

    @Mapping(target = "loggedBy", expression = "java(log.getFirstName() + \" \" + log.getLastName())")
    @Mapping(target = "attachments", source = "attachments", qualifiedByName = "mapAttachments")
    @Mapping(target = "followUp", ignore = true)
    LogDTO fromModel(Log log, @Context AttachmentService attachmentService);

    @Mapping(target = "loggedBy", expression = "java(log.getFirstName() + \" \" + log.getLastName())")
    @Mapping(target = "attachments", ignore = true)
    @Mapping(target = "followUp", ignore = true)
    LogDTO fromModelNoAttachment(Log log);

    @Mapping(target = "loggedBy", expression = "java(log.getFirstName() + \" \" + log.getLastName())")
    @Mapping(target = "attachments", source = "attachments", qualifiedByName = "mapAttachments")
    EntrySummaryDTO toSearchResultFromDTO(Log log, @Context AttachmentService attachmentService);

    @Named("mapAttachments")
    default List<AttachmentDTO> mapAttachments(List<String> attachments, @Context AttachmentService attachmentService) {
        if (attachments == null) {
            return null;
        }

        List<AttachmentDTO> list = new ArrayList<>(attachments.size());
        for (String attachmentID : attachments) {
            list.add(attachmentService.getAttachment(attachmentID));

        }
        return list;
    }

    Log fromDTO(NewEntryDTO newEntryDTO, String firstName, String lastName, String userName);



}
