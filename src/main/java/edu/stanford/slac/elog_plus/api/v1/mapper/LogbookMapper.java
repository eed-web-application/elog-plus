package edu.stanford.slac.elog_plus.api.v1.mapper;

import edu.stanford.slac.ad.eed.base_mongodb_lib.repository.AuthenticationTokenRepository;
import edu.stanford.slac.ad.eed.base_mongodb_lib.repository.AuthorizationRepository;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationDTO;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationOwnerTypeDTO;
import edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationTypeDTO;
import edu.stanford.slac.ad.eed.baselib.api.v1.mapper.AuthMapper;
import edu.stanford.slac.ad.eed.baselib.config.AppProperties;
import edu.stanford.slac.ad.eed.baselib.model.Authorization;
import edu.stanford.slac.ad.eed.baselib.model.AuthorizationOwnerType;
import edu.stanford.slac.elog_plus.api.v1.dto.*;
import edu.stanford.slac.elog_plus.config.ELOGAppProperties;
import edu.stanford.slac.elog_plus.model.Entry;
import edu.stanford.slac.elog_plus.model.Logbook;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
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
    public List<LogbookOwnerAuthorizationDTO> getAuthorizations(String id, boolean includeAuthorizations) {
        if (id == null || id.isEmpty()) return Collections.emptyList();
        if (!includeAuthorizations) return Collections.emptyList();
        return wrapCatch(
                () -> authorizationRepository.findByResourceIs(String.format("/logbook/%s", id))
                        .stream()
                        .map(
                                auth -> fromAuthorization(auth)
                        ).toList(),
                -1,
                "LogbookMapper::getAuthorizations"
        );
    }

    /**
     * Return all the authorizations for a logbook
     *
     * @param authorization standard authorization
     * @return the list of owner authorizations
     */
    public LogbookOwnerAuthorizationDTO fromAuthorization(Authorization authorization) {
        AuthorizationDTO dto = authMapper.fromModel(authorization);
        return fromAuthorizationDTO(dto);
    }

    /**
     * Return all the authorizations for a logbook
     *
     * @param authorization standard authorization
     * @return the list of owner authorizations
     */
    public LogbookOwnerAuthorizationDTO fromAuthorizationDTO(AuthorizationDTO authorization) {
        return LogbookOwnerAuthorizationDTO.builder()
                .id(authorization.id())
                .owner(authorization.owner())
                .ownerType(authorization.ownerType())
                .permission(authorization.authorizationType())
                .build();
    }

    public List<Authorization> toModel(List<LogbookOwnerAuthorizationDTO> authorizations) {
        if (authorizations == null || authorizations.isEmpty()) return Collections.emptyList();
        return authorizations.stream()
                .map(
                        auth -> Authorization.builder()
                                .id(auth.id())
                                .owner(auth.owner())
                                .ownerType(getAuthorizationOwnerType(auth.ownerType()))
                                .authorizationType(getAuthorizationType(auth.permission()))
                                .build()
                ).toList();
    }

    /**
     * Return the AuthorizationOwnerTypeDTO from the AuthorizationOwnerType
     *
     * @param oType the AuthorizationOwnerType
     * @return the AuthorizationOwnerTypeDTO
     */
    public AuthorizationOwnerType getAuthorizationOwnerType(AuthorizationOwnerTypeDTO oType) {
        if(oType == null) return null;
        switch (oType){
            case AuthorizationOwnerTypeDTO.User:
                return AuthorizationOwnerType.User;
            case AuthorizationOwnerTypeDTO.Group:
                return AuthorizationOwnerType.Group;
            case AuthorizationOwnerTypeDTO.Token:
                return AuthorizationOwnerType.Token;
        }
        return null;
    }

    /**
     * Return the AuthorizationOwnerTypeDTO from the AuthorizationOwnerType
     *
     * @param aType the AuthorizationType
     * @return the Authorization.Type
     */
    public Integer getAuthorizationType(AuthorizationTypeDTO aType) {
        if (aType == null) return null;
        switch (aType){
            case AuthorizationTypeDTO.Read:
                return Authorization.Type.Read.getValue();
            case AuthorizationTypeDTO.Write:
                return Authorization.Type.Write.getValue();
            case AuthorizationTypeDTO.Admin:
                return Authorization.Type.Admin.getValue();
        }
        return null;
    }
}
