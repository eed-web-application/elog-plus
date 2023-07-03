package edu.stanford.slac.elog_plus.api.v1.mapper;

import edu.stanford.slac.elog_plus.api.v1.dto.AttachmentDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.LogDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.NewLogDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.SearchResultLogDTO;
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

    @Mapping(target = "author", expression = "java(log.getFirstName() + \" \" + log.getLastName())")
    @Mapping(target = "attachments", source = "attachments", qualifiedByName = "mapAttachments")
    LogDTO fromModel(Log log, @Context AttachmentService attachmentService);


    @Mapping(target = "author", expression = "java(log.getFirstName() + \" \" + log.getLastName())")
    @Mapping(target = "attachments", ignore = true)
    LogDTO fromModelWithFollowUpsAndAttachement(Log log, List<SearchResultLogDTO> followUps, List<AttachmentDTO> attachments);

    @Mapping(target = "author", expression = "java(log.getFirstName() + \" \" + log.getLastName())")
    @Mapping(target = "attachments", source = "attachments", qualifiedByName = "mapAttachments")
    SearchResultLogDTO toSearchResultFromDTO(Log log, @Context AttachmentService attachmentService);

    @Named("mapAttachments")
    // Use this method to map the lists
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

    Log fromDTO(NewLogDTO newLogDTO, String firstName, String lastName, String userName);



}
