package edu.stanford.slac.elog_plus.exception;

import lombok.Builder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;


@ResponseStatus(value = HttpStatus.CONFLICT, reason = "LogbookAlreadyExists")
public class LogbookAlreadyExists extends ControllerLogicException {
    @Builder(builderMethodName = "logbookAlreadyExistsBuilder")
    public LogbookAlreadyExists(Integer errorCode, String errorDomain) {
        super(errorCode, "A logbook with the same name already exists", errorDomain);
    }
}
