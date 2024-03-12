package edu.stanford.slac.elog_plus.exception;

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import lombok.Builder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;


@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Authentication token has not been found")
public class AuthenticationTokenNotFound extends ControllerLogicException {
    @Builder(builderMethodName = "authTokenNotFoundBuilder")
    public AuthenticationTokenNotFound(Integer errorCode, String tokenName, String errorDomain) {
        super(errorCode, String.format("The token '%s' has not been found", tokenName), errorDomain);
    }
}
