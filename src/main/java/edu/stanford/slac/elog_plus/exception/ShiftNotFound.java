package edu.stanford.slac.elog_plus.exception;

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import lombok.Builder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;


@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "ShiftNotFound")
public class ShiftNotFound extends ControllerLogicException {
    @Builder(builderMethodName = "shiftNotFoundBuilder")
    public ShiftNotFound(Integer errorCode, String shiftName, String errorDomain) {
        super(errorCode, String.format("The Tag '%s' has not been found", shiftName), errorDomain);
    }
}
