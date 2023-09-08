package edu.stanford.slac.elog_plus.api.v1.mapper;

import edu.stanford.slac.elog_plus.api.v1.dto.*;
import edu.stanford.slac.elog_plus.model.Entry;
import edu.stanford.slac.elog_plus.model.Logbook;
import edu.stanford.slac.elog_plus.repository.AuthorizationRepository;
import edu.stanford.slac.elog_plus.service.AuthService;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static edu.stanford.slac.elog_plus.exception.Utility.wrapCatch;

@Mapper(
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = TagMapper.class,
        componentModel = "spring"
)
public abstract class LogbookMapper {
    @Autowired
    private AuthorizationRepository authorizationRepository;
    @Autowired
    private AuthMapper authMapper;
    public abstract LogbookSummaryDTO fromModelToSummaryDTO(Logbook log);
    public abstract LogbookSummaryDTO fromModelToSummaryDTO(LogbookDTO log);
    public abstract LogbookDTO fromModel(Logbook log);
    @Mapping(target = "authorizations", expression = "java(getAuthorizations(log.getId(), includeAuthorizations))")
    public abstract LogbookDTO fromModel(Logbook log, boolean includeAuthorizations);
    public abstract Logbook fromDTO(NewLogbookDTO logbookDTO);
    public abstract Logbook fromDTO(LogbookDTO logbookDTO);
    public abstract Logbook fromDTO(UpdateLogbookDTO logbookDTO);
    public abstract Entry fromDTO(EntryNewDTO entryNewDTO, String firstName, String lastName, String userName);

    /**
     * Return all the authorizations for a logbook
     * @param id the id of the logbook
     * @param includeAuthorizations if false a null is returned as list of authorizations
     * @return the list of authorizations of the logbook
     */
    public List<AuthorizationDTO> getAuthorizations(String id, boolean includeAuthorizations) {
        if (id == null || id.isEmpty()) return null;
        if(!includeAuthorizations) return null;
        return wrapCatch(
                () -> authorizationRepository.findByResourceIs(String.format("/logbook/%s", id))
                        .stream()
                        .map(
                                authMapper::fromModel
                        ).toList(),
                -1,
                "LogbookMapper::getAuthorizations"
        );
    }
}
