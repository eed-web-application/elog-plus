package edu.stanford.slac.elog_plus.api.v1.dto;

public enum AuthorizationType {
    Read,
    // can only read and write on the resource data
    Write,
    // can read, write and administer the resource data
    Admin;
}
