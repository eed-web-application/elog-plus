package edu.stanford.slac.elog_plus.exception;

import lombok.Builder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;


@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "ItemNotFound")
public class PersonNotFound extends ControllerLogicException {
    @Builder(builderMethodName = "personNotFoundBuilder")
    public PersonNotFound(Integer errorCode, String email, String errorDomain) {
        super(errorCode, String.format("The person with email '%s' has not been found", email), errorDomain);
    }
}
