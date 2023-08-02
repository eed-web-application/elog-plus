package edu.stanford.slac.elog_plus.annotations;

import java.lang.annotation.*;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestJsonParam {
    String value() default "";
    boolean required() default true;
}
