package edu.stanford.slac.elog_plus.api.v1.dto;

import lombok.Builder;

@Builder
public record FileEntryDTO(
    String path
){}
