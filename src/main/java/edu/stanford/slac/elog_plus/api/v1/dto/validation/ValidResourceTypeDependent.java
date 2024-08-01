package edu.stanford.slac.elog_plus.api.v1.dto.validation;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = ResourceTypeDependentValidator.class)
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidResourceTypeDependent {
    String message() default "Invalid resourceId for the given resourceType";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}