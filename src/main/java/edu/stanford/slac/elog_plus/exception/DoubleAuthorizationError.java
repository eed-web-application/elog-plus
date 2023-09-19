package edu.stanford.slac.elog_plus.exception;

import edu.stanford.slac.elog_plus.model.Authorization;
import lombok.Builder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;


@ResponseStatus(value = HttpStatus.CONFLICT, reason = "Double authorization error")
public class DoubleAuthorizationError extends ControllerLogicException {
    @Builder(builderMethodName = "doubleAuthorizationError")
    public DoubleAuthorizationError(Integer errorCode, String owner, Authorization.OType oType, String errorDomain) {
        super(errorCode, String.format("There is another authentication for the owner:%s and type:'%s'", owner, oType), errorDomain);
    }

}
