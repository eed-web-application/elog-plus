package edu.stanford.slac.elog_plus.exception;

import lombok.Builder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;


@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "EntryNotFound")
public class EntryNotFound extends ControllerLogicException {
    @Builder(builderMethodName = "entryNotFoundBuilder")
    public EntryNotFound(Integer errorCode, String errorDomain) {
        super(errorCode, "The Entry has not been found", errorDomain);
    }
}
