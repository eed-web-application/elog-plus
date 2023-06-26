package edu.stanford.slac.elog_plus.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.InputStream;

@Getter
@Setter
@Builder
public class FileObjectDescription {
    private InputStream is;
    private String fileName;
    private String contentType;
}
