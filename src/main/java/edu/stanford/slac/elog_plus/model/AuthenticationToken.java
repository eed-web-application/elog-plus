package edu.stanford.slac.elog_plus.model;

import java.time.LocalDateTime;

/**
 * Is the logbook custom token to authorize third parties application to access logbook data
 */
public class AuthenticationToken {
    private String name;
    private LocalDateTime expires;
    private String token;
}
