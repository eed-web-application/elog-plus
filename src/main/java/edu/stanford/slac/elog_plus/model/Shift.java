package edu.stanford.slac.elog_plus.model;

import edu.stanford.slac.ad.eed.baselib.exception.ControllerLogicException;
import edu.stanford.slac.elog_plus.utility.DateUtilities;
import lombok.*;
import org.springframework.data.annotation.Transient;

import java.time.LocalTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static edu.stanford.slac.ad.eed.baselib.exception.Utility.assertion;

/**
 * Represent the shift during the day
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder(toBuilder = true)
public class Shift {
    final static String timeRegex = "^(0?[0-9]|1[0-9]|2[0-3]):([0-5][0-9])$";
    final static Pattern pattern = Pattern.compile(timeRegex, Pattern.MULTILINE);

    private String id;
    private String name;
    private String from;
    private String to;
    private Integer fromMinutesSinceMidnight;
    private Integer toMinutesSinceMidnight;
    // calculated fields
    @Transient
    @Builder.Default
    private LocalTime fromTime = null;
    @Transient
    @Builder.Default
    private LocalTime toTime = null;

    public LocalTime getFromTime() {
        if (from != null && fromTime == null) {
            Matcher m = pattern.matcher(from);
            assertion(
                    () -> m.matches() && m.groupCount() == 2,
                    ControllerLogicException.of(
                            -1,
                            "The shift 'from' field need to be a time in the range 00:01-23:59",
                            "Shift"
                    )
            );
            fromTime = DateUtilities.fromUTCString(
                    String.format("%s:%s",
                            m.group(1),
                            m.group(2)
                    )
            );
        }
        return fromTime;
    }

    public LocalTime getToTime() {
        if (to != null && toTime == null) {
            Matcher m = pattern.matcher(to);
            assertion(
                    () -> m.matches() && m.groupCount() == 2,
                    ControllerLogicException.of(
                            -1,
                            "The shift 'to' field need to be a time in the range 00:01-23:59",
                            String.format("Shift %s", name)
                    )
            );
            toTime = DateUtilities.fromUTCString(
                    String.format("%s:%s",
                            m.group(1),
                            m.group(2)
                    )
            );
        }
        return toTime;
    }
}
