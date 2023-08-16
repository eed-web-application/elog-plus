package edu.stanford.slac.elog_plus.api.v1.mapper;

import edu.stanford.slac.elog_plus.api.v1.dto.LogbookSummaryDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.NewTagDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.TagDTO;
import edu.stanford.slac.elog_plus.exception.LogbookNotFound;
import edu.stanford.slac.elog_plus.model.Logbook;
import edu.stanford.slac.elog_plus.model.Tag;
import edu.stanford.slac.elog_plus.repository.LogbookRepository;
import edu.stanford.slac.elog_plus.service.LogbookService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static edu.stanford.slac.elog_plus.exception.Utility.wrapCatch;

@Mapper(
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = "spring"
)
public abstract class TagMapper {
    @Autowired
    LogbookRepository logbookRepository;

    @Mapping(target = "logbook", expression = "java(fillLogbookSummary(tag.getId()))")
    public abstract TagDTO fromModel(Tag tag);
    @Mapping(target = "id", ignore = true)
    public abstract Tag fromDTO(NewTagDTO dto);

    public LogbookSummaryDTO fillLogbookSummary(String tagId) {
        Optional<Logbook> logbook = wrapCatch(
                () -> logbookRepository.findByTagsIdIs(tagId),
                -1,
                "LogbookService:getLogbookSummaryForTagId"
        );
        return logbook.map(
                l->LogbookSummaryDTO.builder()
                        .id(l.getId())
                        .name(l.getName())
                        .build()
        ).orElseThrow(
                ()-> LogbookNotFound.logbookNotFoundBuilder()
                        .errorCode(-1)
                        .errorDomain("TagMapper::fillLogbookSummary")
                        .build()
        );
    }
}
