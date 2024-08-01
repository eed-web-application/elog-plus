package edu.stanford.slac.elog_plus.api.v1.dto.validation;

import edu.stanford.slac.elog_plus.api.v1.dto.NewAuthorizationDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.ResourceTypeDTO;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ResourceTypeDependentValidator implements ConstraintValidator<ValidResourceTypeDependent, NewAuthorizationDTO> {

    @Override
    public void initialize(ValidResourceTypeDependent constraintAnnotation) {
        // Initialization code if needed
    }

    @Override
    public boolean isValid(NewAuthorizationDTO dto, ConstraintValidatorContext context) {
        if (dto == null) {
            return false; // Skip validation if dto is null
        }
        boolean isValid = true;
        // Add your custom validation logic here
        if (dto.resourceType() == ResourceTypeDTO.All) {
            return true;
        } else if (dto.resourceType() == ResourceTypeDTO.Group) {
            return true;
        } else if (dto.resourceId()==null) {
            isValid = false;
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("resourceId must cannot be null")
                    .addPropertyNode("resourceId")
                    .addConstraintViolation();
        }

        return isValid;
    }
}