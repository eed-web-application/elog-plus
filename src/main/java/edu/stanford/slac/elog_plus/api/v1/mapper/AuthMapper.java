package edu.stanford.slac.elog_plus.api.v1.mapper;

import edu.stanford.slac.elog_plus.api.v1.dto.*;
import edu.stanford.slac.elog_plus.model.AuthenticationToken;
import edu.stanford.slac.elog_plus.model.Authorization;
import edu.stanford.slac.elog_plus.model.Group;
import edu.stanford.slac.elog_plus.model.Person;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = "spring"
)
public abstract class AuthMapper {
    public abstract PersonDTO fromModel(Person p);
    public abstract GroupDTO fromModel(Group g);
    @Mapping(target = "authorizationType", expression = "java(Authorization.Type.fromValue(a.getAuthorizationType()).name())")
    public abstract AuthorizationDTO fromModel(Authorization a);

    @Mapping(target = "authorizationType", expression = "java(Authorization.Type.valueOf(a.authorizationType()).getValue())")
    public abstract Authorization toModel(AuthorizationDTO a);

    public abstract List<Authorization> toModel(List<AuthorizationDTO> a);
}