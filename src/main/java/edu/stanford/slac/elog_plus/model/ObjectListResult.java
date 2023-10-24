package edu.stanford.slac.elog_plus.model;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ObjectListResult {
    String continuationToken;
    List<String> keyFounds;
}
