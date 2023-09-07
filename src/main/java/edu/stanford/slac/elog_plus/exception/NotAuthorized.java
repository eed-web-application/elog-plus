package edu.stanford.slac.elog_plus.exception;

import lombok.Builder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.UNAUTHORIZED, reason = "The operation is not authorized")
public class NotAuthorized extends ControllerLogicException {
    @Builder(builderMethodName = "notAuthorizedOnOperationBuilder")
    public NotAuthorized(Integer errorCode, String operation, String errorDomain) {
        super(errorCode, "Not authorized for: '%s'".formatted(operation), errorDomain);
    }

    @Builder(builderMethodName = "notAuthorizedBuilder")
    public NotAuthorized(Integer errorCode, String errorDomain) {
        super(errorCode, "The operation is not authorized", errorDomain);
    }
}