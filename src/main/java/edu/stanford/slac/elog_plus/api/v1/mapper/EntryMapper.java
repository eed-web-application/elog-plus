package edu.stanford.slac.elog_plus.api.v1.mapper;

import edu.stanford.slac.elog_plus.api.v1.dto.*;
import edu.stanford.slac.elog_plus.model.Entry;
import edu.stanford.slac.elog_plus.service.AttachmentService;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import java.util.ArrayList;
import java.util.List;

@Mapper(
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = "spring")
public interface EntryMapper {
    EntryMapper INSTANCE = Mappers.getMapper(EntryMapper.class);

    @Mapping(target = "loggedBy", expression = "java(entry.getFirstName() + \" \" + entry.getLastName())")
    @Mapping(target = "attachments", source = "attachments", qualifiedByName = "mapAttachments")
    @Mapping(target = "followUps", ignore = true)
    EntryDTO fromModel(Entry entry, @Context AttachmentService attachmentService);

    @Mapping(target = "loggedBy", expression = "java(entry.getFirstName() + \" \" + entry.getLastName())")
    @Mapping(target = "attachments", ignore = true)
    @Mapping(target = "followUps", ignore = true)
    EntryDTO fromModelNoAttachment(Entry entry);

    @Mapping(target = "loggedBy", expression = "java(entry.getFirstName() + \" \" + entry.getLastName())")
    @Mapping(target = "attachments", source = "attachments", qualifiedByName = "mapAttachments")
    EntrySummaryDTO toSearchResultFromDTO(Entry entry, @Context AttachmentService attachmentService);

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

    Entry fromDTO(EntryNewDTO entryNewDTO, String firstName, String lastName, String userName);



}
