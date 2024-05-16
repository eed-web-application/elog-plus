package edu.stanford.slac.elog_plus.service.authorization;

import edu.stanford.slac.ad.eed.baselib.exception.NotAuthorized;
import edu.stanford.slac.ad.eed.baselib.service.AuthService;
import edu.stanford.slac.elog_plus.api.v1.dto.EntrySummaryDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.LogbookSummaryDTO;
import edu.stanford.slac.elog_plus.service.EntryService;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

import static edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationTypeDTO.Read;
import static edu.stanford.slac.ad.eed.baselib.api.v1.dto.AuthorizationTypeDTO.Write;
import static edu.stanford.slac.ad.eed.baselib.exception.Utility.assertion;

@Service
@AllArgsConstructor
public class AttachmentAuthorizationService {
    private final AuthService authService;
    private final EntryService entryService;

    /**
     * Check if the user can create an attachment
     *
     * substantially is check if the user has some write authorization on some logbook
     * @return true if the user can create an attachment
     */
    public boolean canCreate(Authentication authentication) {
        // check the authorization
        assertion(
                NotAuthorized.notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("AttachmentAuthorizationService::canCreateAttachment")
                        .build(),
                // should be authenticated
                () -> authService.checkAuthentication(authentication),
                // should be able to write on some logbook
                () -> authService.checkAuthorizationForOwnerAuthTypeAndResourcePrefix(
                        authentication,
                        Write,
                        "/"
                )
        );
        return true;
    }

    /**
     * Check if the user can read an attachment
     *
     * check if the user has authorization to read on logbook where each entry that own the attachment belongs
     * @return true if the user can read an attachment
     */
    public boolean canRead(Authentication authentication, String attachmentId) {
        // fetch all the entries that are parent of the attachment and filter all those are readable by the user
        List<EntrySummaryDTO> entryThatOwnTheAttachment = entryService.getEntriesThatOwnTheAttachment(attachmentId)
                .stream()
                .map(
                        summary -> {
                            List<LogbookSummaryDTO> filteredLogbook = summary.logbooks().stream()
                                    .filter(
                                            lbSummary -> authService.checkAuthorizationForOwnerAuthTypeAndResourcePrefix(
                                                    authentication,
                                                    Read,
                                                    "/logbook/%s".formatted(lbSummary.id())
                                            )
                                    ).toList();
                            return summary.toBuilder()
                                    .logbooks(
                                            filteredLogbook
                                    ).build();
                        }
                )
                .filter(
                        summary -> !summary.logbooks().isEmpty()
                )
                .toList();
        assertion(
                NotAuthorized.notAuthorizedBuilder()
                        .errorCode(-1)
                        .errorDomain("")
                        .build(),
                () -> !entryThatOwnTheAttachment.isEmpty()
        );
        return true;
    }
}
