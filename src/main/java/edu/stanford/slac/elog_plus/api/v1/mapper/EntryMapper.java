package edu.stanford.slac.elog_plus.api.v1.mapper;

import edu.stanford.slac.elog_plus.api.v1.dto.*;
import edu.stanford.slac.elog_plus.exception.TagNotFound;
import edu.stanford.slac.elog_plus.model.Entry;
import edu.stanford.slac.elog_plus.repository.EntryRepository;
import edu.stanford.slac.elog_plus.service.AttachmentService;
import edu.stanford.slac.elog_plus.service.LogbookService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.wrapCatch;

@Mapper(
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = "spring"
)
public abstract class EntryMapper {
    static Pattern pattern = Pattern.compile("<a\\s+(?:[^>]*?\\\\s+)?href=([\"'])(https?://[^/]+/([^/\"']+))");
    @Autowired
    private EntryRepository entryRepository;
    @Autowired
    private AttachmentService attachmentService;
    @Autowired
    private LogbookService logbookService;

    @Mapping(target = "loggedBy", expression = "java(entry.getFirstName() + \" \" + entry.getLastName())")
    @Mapping(target = "followUps", ignore = true)
    @Mapping(source = "logbooks", target = "logbooks", qualifiedByName = "mapToLogbookSummary")
    @Mapping(target = "referencedBy", ignore = true)
    @Mapping(target = "references", ignore = true)
    @Mapping(target = "referencesInBody", expression = "java(entry.getOriginId()==null)")
    public abstract EntryDTO fromModel(Entry entry);

    @Mapping(target = "loggedBy", expression = "java(entry.getFirstName() + \" \" + entry.getLastName())")
    @Mapping(target = "attachments", ignore = true)
    @Mapping(target = "followUps", ignore = true)
    @Mapping(source = "logbooks", target = "logbooks", qualifiedByName = "mapToLogbookSummary")
    @Mapping(target = "referencedBy", ignore = true)
    @Mapping(target = "references", ignore = true)
    @Mapping(target = "referencesInBody", expression = "java(entry.getOriginId()==null)")
    public abstract EntryDTO fromModelNoAttachment(Entry entry);

    @Mapping(target = "loggedBy", expression = "java(entry.getFirstName() + \" \" + entry.getLastName())")
    @Mapping(source = "logbooks", target = "logbooks", qualifiedByName = "mapToLogbookSummary")
    @Mapping(target = "followingUp", expression = "java(getFollowingUp(entry.getId()))")
    @Mapping(target = "referencedBy", expression = "java(getReferenceBy(entry.getId()))")
    public abstract EntrySummaryDTO toSearchResult(Entry entry);

    @Mapping(target = "references", expression = "java(createReferences(entryNewDTO.text()))")
    public abstract Entry fromDTO(EntryNewDTO entryNewDTO, String firstName, String lastName, String userName);

    @Mapping(target = "references", expression = "java(createReferences(entryNewDTO.text()))")
    public abstract Entry fromDTO(EntryImportDTO entryNewDTO, List<String> attachments);

    @Named("getFollowingUp")
    public String getFollowingUp(String id) {
        if (id == null || id.isEmpty()) return null;
        return wrapCatch(
                () -> entryRepository.findByFollowUpsContains(id)
                        .map(
                                Entry::getId
                        ).orElse(null),
                -1,
                "EntryMapper::getFollowingUp"
        );
    }

    /**
     * Fill the referenced by of an entry
     *
     * @param id the unique id of an entry
     * @return all the id of the entries that refere the entry
     */
    public List<String> getReferenceBy(String id) {
        if (id == null || id.isEmpty()) return null;
        return wrapCatch(
                () -> entryRepository.findAllByReferencesContainsAndSupersedeByExists(id, false)
                        .stream()
                        .map(
                                Entry::getId
                        ).toList(),
                -1,
                "EntryMapper::getFollowgetReferenceByingUp"
        );
    }

    @Named("createReferences")
    public List<String> createReferences(String text) {
        List<String> result = new ArrayList<>();
        if (text == null || text.isEmpty()) return result;
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            if (matcher.groupCount() >= 3) {
                result.add(matcher.group(3));
            }
        }
        return result;
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
                                    () -> TagNotFound.tagNotFoundBuilder()
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
