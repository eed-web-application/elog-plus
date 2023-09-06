package edu.stanford.slac.elog_plus.exception;

import lombok.Builder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "The user has not been found")
public class UserNotFound extends ControllerLogicException {
    @Builder(builderMethodName = "userNotFound")
    public UserNotFound(Integer errorCode, String userName, String errorDomain) {
        super(errorCode, "The user '%s' has not been found".formatted(userName), errorDomain);
    }
}