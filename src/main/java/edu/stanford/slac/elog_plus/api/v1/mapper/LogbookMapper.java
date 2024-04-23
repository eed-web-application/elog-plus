package edu.stanford.slac.elog_plus.api.v1.mapper;

import edu.stanford.slac.ad.eed.base_mongodb_lib.repository.AuthenticationTokenRepository;
import edu.stanford.slac.ad.eed.base_mongodb_lib.repository.AuthorizationRepository;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthenticationTokenDTO;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationDTO;
import edu.stanford.slac.ad.eed.baselib.api.v1.mapper.AuthMapper;
import edu.stanford.slac.ad.eed.baselib.config.AppProperties;
import edu.stanford.slac.elog_plus.api.v1.dto.*;
import edu.stanford.slac.elog_plus.config.ELOGAppProperties;
import edu.stanford.slac.elog_plus.model.Entry;
import edu.stanford.slac.elog_plus.model.Logbook;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.wrapCatch;

@Mapper(
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = TagMapper.class,
        componentModel = "spring"
)
public abstract class LogbookMapper {
    @Autowired
    protected AuthorizationRepository authorizationRepository;
    @Autowired
    protected AuthenticationTokenRepository authenticationTokenRepository;
    @Autowired
    protected AuthMapper authMapper;
    @Autowired
    protected AppProperties appProperties;
    @Autowired
    protected ELOGAppProperties elogAppProperties;

    public abstract LogbookSummaryDTO fromModelToSummaryDTO(Logbook log);

    public abstract LogbookSummaryDTO fromModelToSummaryDTO(LogbookDTO log);

    public abstract LogbookDTO fromModel(Logbook logbook);

    @Mapping(target = "authorizations", expression = "java(getAuthorizations(logbook.getId(), includeAuthorizations))")
    public abstract LogbookDTO fromModel(Logbook logbook, boolean includeAuthorizations);

    public abstract Logbook fromDTO(NewLogbookDTO logbookDTO);

    public abstract Logbook fromDTO(LogbookDTO logbookDTO);

    public abstract Logbook fromDTO(UpdateLogbookDTO logbookDTO);

    public abstract Entry fromDTO(EntryNewDTO entryNewDTO, String firstName, String lastName, String userName);

    public abstract UpdateLogbookDTO toUpdateDTO(LogbookDTO logbook);

    /**
     * Return all the authorizations for a logbook
     *
     * @param id                    the id of the logbook
     * @param includeAuthorizations if false a null is returned as list of authorizations
     * @return the list of authorizations of the logbook
     */
    public List<AuthorizationDTO> getAuthorizations(String id, boolean includeAuthorizations) {
        if (id == null || id.isEmpty()) return null;
        if (!includeAuthorizations) return null;
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
