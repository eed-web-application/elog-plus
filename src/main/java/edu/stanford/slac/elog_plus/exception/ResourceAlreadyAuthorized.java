package edu.stanford.slac.elog_plus.exception;

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import lombok.Builder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;


@ResponseStatus(value = HttpStatus.CONFLICT, reason = "resourceType has been already authorized for same ownerId")
public class ResourceAlreadyAuthorized extends ControllerLogicException {
    @Builder(builderMethodName = "byResource")
    public ResourceAlreadyAuthorized(Integer errorCode, String errorDomain, String resource, String owner) {
        super(errorCode, "The resourceType '%s' has been already authorized to the ownerId '%s'".formatted(resource, owner), errorDomain);
    }

    @Builder(builderMethodName = "ownerNotFound")
    public ResourceAlreadyAuthorized(Integer errorCode, String errorDomain) {
        super(errorCode, "The ownerId is mandatory", errorDomain);
    }
}
