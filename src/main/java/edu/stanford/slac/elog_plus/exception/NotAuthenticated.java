package edu.stanford.slac.elog_plus.exception;

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import lombok.Builder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;


@ResponseStatus(value = HttpStatus.UNAUTHORIZED, reason = "User is not authenticated")
public class NotAuthenticated extends ControllerLogicException {
    @Builder(builderMethodName = "notAuthenticatedBuilder")
    public NotAuthenticated(Integer errorCode) {
        super(errorCode, "The user is not authenticated", null);
    }
}
