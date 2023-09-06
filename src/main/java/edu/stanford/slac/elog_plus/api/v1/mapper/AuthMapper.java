package edu.stanford.slac.elog_plus.api.v1.mapper;

import edu.stanford.slac.elog_plus.api.v1.dto.GroupDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.PersonDTO;
import edu.stanford.slac.elog_plus.model.Group;
import edu.stanford.slac.elog_plus.model.Person;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = "spring"
)
public abstract class AuthMapper {
    public abstract PersonDTO fromModel(Person p);
    public abstract GroupDTO fromModel(Group g);
}