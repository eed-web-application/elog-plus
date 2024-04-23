package edu.stanford.slac.elog_plus.api.v2.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import edu.stanford.slac.elog_plus.api.v1.dto.EntryImportDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
/**
 * Is the dto for the import entry

 */
public record ImportEntryDTO(
        /**
         * The list of the user ids that can read the entry
         * each user will be granted as read on the entry logbook
         */
        List<String> readerUserIds,
        /**
         * Is the entry to import
         */
        @NotNull
        @Valid
        EntryImportDTO entry
) {
}
