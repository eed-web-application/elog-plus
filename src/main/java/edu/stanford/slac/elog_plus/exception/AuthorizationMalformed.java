package edu.stanford.slac.elog_plus.exception;

import lombok.Builder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;


@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Authorization fields malformed")
public class AuthorizationMalformed extends ControllerLogicException {
    @Builder(builderMethodName = "ownerTypeNotFound")
    public AuthorizationMalformed(Integer errorCode, String owner, String errorDomain) {
        super(errorCode, String.format("The owner type is mandatory for the authorization owner: '%s'", owner), errorDomain);
    }

    @Builder(builderMethodName = "ownerNotFound")
    public AuthorizationMalformed(Integer errorCode, String errorDomain) {
        super(errorCode, "The owner is mandatory", errorDomain);
    }
}
