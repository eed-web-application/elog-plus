package edu.stanford.slac.elog_plus.exception;

import edu.stanford.slac.elog_plus.model.Authorization;
import lombok.Builder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;


@ResponseStatus(value = HttpStatus.CONFLICT, reason = "Double authorization error")
public class DoubleAuthenticationTokenError extends ControllerLogicException {
    @Builder(builderMethodName = "doubleAuthTokenError")
    public DoubleAuthenticationTokenError(Integer errorCode, String tokenName, String errorDomain) {
        super(errorCode, String.format("There is another token with the name '%s'", tokenName), errorDomain);
    }

}
