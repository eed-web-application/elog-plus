package edu.stanford.slac.elog_plus.api.v1.mapper;

import edu.stanford.slac.elog_plus.api.v1.dto.*;
import edu.stanford.slac.elog_plus.exception.TagNotFound;
import edu.stanford.slac.elog_plus.model.Entry;
import edu.stanford.slac.elog_plus.model.Tag;
import edu.stanford.slac.elog_plus.repository.EntryRepository;
import edu.stanford.slac.elog_plus.repository.LogbookRepository;
import edu.stanford.slac.elog_plus.service.AttachmentService;
import edu.stanford.slac.elog_plus.service.LogbookService;
import edu.stanford.slac.elog_plus.utility.StringUtilities;
import lombok.AllArgsConstructor;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static edu.stanford.slac.elog_plus.exception.Utility.wrapCatch;

@Mapper(
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = "spring"
)
public abstract class EntryMapper {
    static Pattern pattern = Pattern.compile("data-references-entry=\"([^\"]+)\"");
    @Autowired
    private EntryRepository entryRepository;
    @Autowired
    private AttachmentService attachmentService;
    @Autowired
    private LogbookService logbookService;

    @Mapping(target = "loggedBy", expression = "java(entry.getFirstName() + \" \" + entry.getLastName())")
    @Mapping(target = "followUps", ignore = true)
    @Mapping(source = "logbooks", target = "logbooks", qualifiedByName = "mapToLogbookSummary")
    @Mapping(target = "referencedFrom", expression = "java(findReferencedFromEntries(entry.getId()))")
    public abstract EntryDTO fromModel(Entry entry);

    @Mapping(target = "loggedBy", expression = "java(entry.getFirstName() + \" \" + entry.getLastName())")
    @Mapping(target = "attachments", ignore = true)
    @Mapping(target = "followUps", ignore = true)
    @Mapping(source = "logbooks", target = "logbooks", qualifiedByName = "mapToLogbookSummary")
    @Mapping(target = "referencedFrom", expression = "java(findReferencedFromEntries(entry.getId()))")
    public abstract EntryDTO fromModelNoAttachment(Entry entry);

    @Mapping(target = "loggedBy", expression = "java(entry.getFirstName() + \" \" + entry.getLastName())")
    @Mapping(source = "logbooks", target = "logbooks", qualifiedByName = "mapToLogbookSummary")
    @Mapping(target = "referencedFrom", expression = "java(findReferencedFromEntries(entry.getId()))")
    public abstract EntrySummaryDTO toSearchResultFromDTO(Entry entry);

    @Mapping(target = "referencesTo", expression = "java(createReferences(entryNewDTO.text()))")
    public abstract Entry fromDTO(EntryNewDTO entryNewDTO, String firstName, String lastName, String userName);

    @Mapping(target = "referencesTo", expression = "java(createReferences(entryNewDTO.text()))")
    public abstract Entry fromDTO(EntryImportDTO entryNewDTO, List<String> attachments);
    @Named("createReferences")
    public List<String> createReferences(String text) {
        List<String> result = new ArrayList<>();
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            result.add(matcher.group(1));
        }
        return result;
    }

    /**
     * Find all the entry that reference the one identified by the entryId
     *
     * @param entryId the id of the referenced entry
     * @return the entries that reference the one with the @entryId
     */
    @Named("findReferencedFromEntries")
    public List<String> findReferencedFromEntries(String entryId) {
        return wrapCatch(
                () ->entryRepository.findAllByReferencesToContainsAndSupersedeByExists(entryId, false)
                        .stream()
                        .map(
                                Entry::getId
                        )
                        .toList(),
                -2,
                "EntryMapper::findReferencedFromEntries"
        );
    }

    public List<AttachmentDTO> map(List<String> attachments) {
        if (attachments == null) {
            return null;
        }

        List<AttachmentDTO> list = new ArrayList<>(attachments.size());
        for (String attachmentID : attachments) {
            list.add(attachmentService.getAttachment(attachmentID));

        }
        return list;
    }

    @Named("mapToLogbookSummary")
    public List<LogbookSummaryDTO> mapToLogbookSummary(List<String> logbookIds) {
        if (logbookIds == null) {
            return null;
        }

        List<LogbookSummaryDTO> list = new ArrayList<>(logbookIds.size());
        for (String logbookId : logbookIds) {
            list.add(
                    logbookService.getSummaryById(logbookId)
            );

        }
        return list;
    }

    public List<TagDTO> fromTagId(List<String> tags) {
        if (tags == null) {
            return null;
        }

        List<TagDTO> list = new ArrayList<>(tags.size());
        for (String tagsId : tags) {
            list.add(
                    logbookService.getTagById(tagsId)
                            .orElseThrow(
                                    ()-> TagNotFound.tagNotFoundBuilder()
                                            .errorCode(-1)
                                            .tagName("tagsId")
                                            .errorDomain("EntryMapper::fromTagId")
                                            .build()
                            )
            );
        }
        return list;
    }


}
