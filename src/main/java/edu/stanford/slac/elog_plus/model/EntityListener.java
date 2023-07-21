package edu.stanford.slac.elog_plus.model;

import com.mongodb.DBObject;
import org.bson.Document;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeSaveEvent;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;

@Component
public class EntityListener extends AbstractMongoEventListener<Entry> {

    @Override
    public void onBeforeSave(BeforeSaveEvent<Entry> event) {
        final Document dbObject = event.getDocument();
        Entry source = event.getSource();
        if (source.getEventAt() == null) {
            if (source.getLoggedAt() != null) {
                source.setEventAt(source.getLoggedAt());
            } else {
                source.setEventAt(LocalDateTime.now());
            }
        }

        if (dbObject != null) {
            dbObject.put(
                    "eventAt",
                    Date.from(
                            source.getEventAt().atZone(
                                    ZoneId.systemDefault()
                                    )
                            .toInstant())
            );
        }
    }
}
