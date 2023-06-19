package edu.stanford.slac.elog_plus.api.v1.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record QueryParameterConfigurationDTO(
        List<String> logbook
){
}
