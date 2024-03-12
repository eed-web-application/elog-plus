package edu.stanford.slac.elog_plus.exception;

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import lombok.Builder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;


@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Logbook has not been found")
public class LogbookNotFound extends ControllerLogicException {
    @Builder(builderMethodName = "logbookNotFoundBuilder")
    public LogbookNotFound(Integer errorCode, String errorDomain) {
        super(errorCode, "The logbooks has not been found", errorDomain);
    }
    @Builder(builderMethodName = "logbookNotFoundBuilderWitLId")
    public LogbookNotFound(Integer errorCode, String logbookId, String errorDomain) {
        super(errorCode, "The logbooks %s has not been found".formatted(logbookId), errorDomain);
    }
}
