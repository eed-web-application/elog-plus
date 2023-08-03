package edu.stanford.slac.elog_plus.model;

import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder(toBuilder = true)
public class Summarizes {
    private String shiftId;
    private LocalDate date;
}
