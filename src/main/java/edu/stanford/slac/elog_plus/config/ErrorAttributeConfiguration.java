package edu.stanford.slac.elog_plus.config;

import edu.stanford.slac.elog_plus.exception.ControllerLogicException;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;

@Configuration
public class ErrorAttributeConfiguration {
    @Bean
    public ErrorAttributes errorAttributes() {
        return new DefaultErrorAttributes() {
            @Override
            public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
                Map<String, Object> errorAttributes = super.getErrorAttributes(webRequest, options);
                Throwable error = getError(webRequest);
                if (error != null) {
                    if (error instanceof ControllerLogicException) {
                        ControllerLogicException myException = (ControllerLogicException) error;
                        errorAttributes.put("errorCode", myException.getErrorCode());
                        errorAttributes.put("errorMessage", myException.getErrorMessage());
                        errorAttributes.put("errorDomain", myException.getErrorDomain());
                    } else if (error instanceof BindException) {
                        int errNum = 1;
                        BindException bindExc = (BindException) error;
                        for (ObjectError err : bindExc.getAllErrors()) {
                            errorAttributes.put(String.format("Err.%d", errNum++), err.toString());
                        }
                    } else {
                        errorAttributes.put(error.getClass().getName(), error.toString());
                    }
                }
                return errorAttributes;
            }
        };
    }
}
