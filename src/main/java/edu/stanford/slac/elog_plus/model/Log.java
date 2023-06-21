package edu.stanford.slac.elog_plus.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Document()
public class Log {
    @Id
    private String id;
    @Field("ENTRY_TYPE")
    private String entryType;
    @Field("LOGBOOK")
    private String logbook;
    @Field("PRIORITY")
    private String priority;
    @Field("SEGMENT")
    private String segment;
    @Field("TAGS")
    private String tags;
    @Field("TITLE")
    private String title;
    @Field("TEXT")
    private String text;
    @Field("FILE_PS")
    private String filePs;
    @Field("FILE_PREVIEW")
    private String filePreview;
    @Field("LOGDATE")
    private LocalDateTime logDate;
    @Field("COMMITDATE")
    private LocalDateTime commitDate;
    @Field("PROGDATE")
    private LocalDateTime progDate;
}
