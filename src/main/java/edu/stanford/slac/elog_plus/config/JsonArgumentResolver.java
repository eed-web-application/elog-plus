package edu.stanford.slac.elog_plus.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.stanford.slac.elog_plus.annotations.RequestJsonParam;
import edu.stanford.slac.elog_plus.exception.ControllerLogicException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;

import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

public class JsonArgumentResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(RequestJsonParam.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) throws Exception {
        RequestJsonParam annot = parameter.getParameterAnnotation(RequestJsonParam.class);
        String jsonPartName = annot.value();
        MultipartHttpServletRequest multipartRequest = webRequest.getNativeRequest(MultipartHttpServletRequest.class);
        if(multipartRequest == null) return null;
        String json = multipartRequest.getParameter(jsonPartName);
        if(json != null) {
            return new ObjectMapper().readValue(json, parameter.getParameterType());
        } else if (annot.required()){
            throw new MissingServletRequestPartException(jsonPartName);
        } else {
            return null;
        }

    }
}
