package edu.stanford.slac.elog_plus.exception;

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import lombok.Builder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;


@ResponseStatus(value = HttpStatus.UNAUTHORIZED, reason = "Logbook has not been authorized")
public class LogbookNotAuthorized extends ControllerLogicException {
    @Builder(builderMethodName = "logbookAuthorizedBuilder")
    public LogbookNotAuthorized(Integer errorCode, String logbookName, String errorDomain) {
        super(errorCode, "The logbooks '%s' has not been found".formatted(logbookName), errorDomain);
    }
}
