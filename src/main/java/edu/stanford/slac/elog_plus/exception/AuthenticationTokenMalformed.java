package edu.stanford.slac.elog_plus.exception;

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import lombok.Builder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;


@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Authentication token fields malformed")
public class AuthenticationTokenMalformed extends ControllerLogicException {
    @Builder(builderMethodName = "malformedAuthToken")
    public AuthenticationTokenMalformed(Integer errorCode, String errorDomain) {
        super(errorCode, "The name is malformed", errorDomain);
    }
}
