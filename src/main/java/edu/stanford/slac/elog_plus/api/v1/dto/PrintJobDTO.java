package edu.stanford.slac.elog_plus.api.v1.dto;

import lombok.Builder;

@Builder
public record PrintJobDTO (
    Long id,
    String name,
    String status,
    String printerName,
    String fileName,
    String userName,
    String userDepartment,
    String userLocation
){}
