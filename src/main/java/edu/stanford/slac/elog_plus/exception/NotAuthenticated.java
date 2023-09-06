package edu.stanford.slac.elog_plus.exception;

import lombok.Builder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.UNAUTHORIZED, reason = "The use need to be authenticated")
public class NotAuthenticated extends ControllerLogicException {
    @Builder(builderMethodName = "notAuthenticatedBuilder")
    public NotAuthenticated(Integer errorCode, String errorDomain) {
        super(errorCode, "The use need to be authenticated", errorDomain);
    }
}