package edu.stanford.slac.elog_plus.service;

import edu.stanford.slac.elog_plus.api.v1.dto.LogbookDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.NewLogbookDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.NewTagDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.TagDTO;
import edu.stanford.slac.elog_plus.api.v1.mapper.LogbookMapper;
import edu.stanford.slac.elog_plus.api.v1.mapper.TagMapper;
import edu.stanford.slac.elog_plus.exception.ControllerLogicException;
import edu.stanford.slac.elog_plus.exception.LogbookAlreadyExists;
import edu.stanford.slac.elog_plus.exception.LogbookNotFound;
import edu.stanford.slac.elog_plus.exception.TagAlreadyExists;
import edu.stanford.slac.elog_plus.model.Logbook;
import edu.stanford.slac.elog_plus.model.Tag;
import edu.stanford.slac.elog_plus.repository.LogbookRepository;
import edu.stanford.slac.elog_plus.utility.StringUtilities;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static edu.stanford.slac.elog_plus.exception.Utility.assertion;
import static edu.stanford.slac.elog_plus.exception.Utility.wrapCatch;

@Service
@AllArgsConstructor
public class LogbookService {
    LogbookRepository logbookRepository;

    /**
     * Return all the logbook
     *
     * @return the lis tof all logbook
     */
    public List<LogbookDTO> getAllLogbook() {
        return wrapCatch(
                () -> logbookRepository.findAll()
                        .stream()
                        .map(
                                LogbookMapper.INSTANCE::fromModel
                        ).collect(Collectors.toList()),
                -1,
                "LogbookService::getAllLogbook"
        );
    }

    /**
     * Create a new logbook
     *
     * @param newLogbookDTO the new logbook
     * @return the id of the newly created logbook
     */
    public String createNew(NewLogbookDTO newLogbookDTO) {
        // normalize the name
        newLogbookDTO = newLogbookDTO.toBuilder()
                .name(StringUtilities.tagNameNormalization(newLogbookDTO.name()))
                .build();

        // check for tag with the same name
        NewLogbookDTO finalNewLogbookDTO = newLogbookDTO;
        boolean exists = wrapCatch(
                () -> logbookRepository.existsByName(
                        finalNewLogbookDTO.name()
                ),
                -1,
                "LogbookService::createNew");
        assertion(
                () -> !exists,
                LogbookAlreadyExists.logbookAlreadyExistsBuilder()
                        .errorCode(-2)
                        .errorDomain("LogbookService::createNew")
                        .build()
        );

        Logbook newLogbook = wrapCatch(
                () -> logbookRepository.save(
                        LogbookMapper.INSTANCE.fromDTO(finalNewLogbookDTO)
                ),
                -3,
                "LogbookService::createNew");
        return newLogbook.getId();
    }

    /**
     * Return the full logbook description
     *
     * @param logbookId the logbook id
     * @return the full logbook
     */
    public LogbookDTO getLogbook(String logbookId) {
        assertOnLogbook(logbookId, -1, "LogbookService:getLogbook");
        return logbookRepository.findById(
                logbookId
        ).map(
                LogbookMapper.INSTANCE::fromModel
        ).orElseThrow(
                () -> ControllerLogicException.builder()
                        .errorCode(-2)
                        .errorMessage("")
                        .errorDomain("")
                        .build()
        );
    }

    /**
     * Return a full log indetified by its name
     * @param logbookName the name of the logbook
     * @return the full logbook
     */
    public LogbookDTO getLogbookByName(String logbookName) {
        Optional<Logbook> lb = wrapCatch(
                () -> logbookRepository.findByName(logbookName),
                -1,
                "LogbookService:getLogbookByName"
        );
        return LogbookMapper.INSTANCE.fromModel(
                lb.orElseThrow(
                        ()-> LogbookNotFound.logbookNotFoundBuilder()
                                .errorCode(-1)
                                .errorDomain("LogbookService:getLogbookByName")
                                .build()
                )
        );

    }

    /**
     * Check if a logbook with a specific name exists
     *
     * @param logbook the name of the logbook to check
     * @return true if the logbook exists
     */
    public Boolean exist(String logbook) {
        return wrapCatch(
                () -> logbookRepository.existsByName(
                        logbook
                ),
                -1,
                "LogbookService::exist");
    }

    /**
     * Create new tag for the logbook
     *
     * @param logbookId the logbook id
     * @param newTagDTO the new tag description
     * @return the id of the newly crated tag
     */
    public String createNewTag(String logbookId, NewTagDTO newTagDTO) {
        assertOnLogbook(logbookId, -1, "LogbookService:createNewTag");
        // normalize the name
        newTagDTO = newTagDTO.toBuilder()
                .name(StringUtilities.tagNameNormalization(newTagDTO.name()))
                .build();
        NewTagDTO finalNewTagDTO = newTagDTO;

        boolean exists = wrapCatch(
                () -> logbookRepository.tagExistByName(
                        logbookId,
                        finalNewTagDTO.name()
                ),
                -1,
                "LogbookService:createNewTag"
        );
        assertion(
                () -> !exists,
                TagAlreadyExists.tagAlreadyExistsBuilder()
                        .errorCode(-2)
                        .tagName(finalNewTagDTO.name())
                        .errorDomain("LogbookService::createNewTag")
                        .build()
        );
        return wrapCatch(
                () -> logbookRepository.createNewTag(
                        logbookId,
                        TagMapper.INSTANCE.fromDTO(finalNewTagDTO)
                ),
                -3,
                "LogbookService:createNewTag"
        );
    }

    /**
     * Check if a tag exist for the log
     *
     * the name is normalized before checking
     * @param logbookId the id of the logbook
     * @param tagName the name of the tag
     * @return true if the tag exists
     */
    public Boolean tagExistForLogbook(String logbookId, String tagName) {
        return wrapCatch(
                () -> logbookRepository.tagExistByName(
                        logbookId,
                        StringUtilities.tagNameNormalization(
                                tagName
                        )
                ),
                -1,
                "LogbookService:createNewTag"
        );
    }

    /**
     * Return all tags ofr a logbook
     *
     * @param logbookId the id of the logbook
     * @return all the logbook tags
     */
    public List<TagDTO> getAllTags(String logbookId) {
        assertOnLogbook(logbookId, -1, "LogbookService:createNewTagForLogbook");
        List<Tag> allTag = wrapCatch(
                () -> logbookRepository.getAllTagFor(logbookId),
                -2,
                "LogbookService:createNewTagForLogbook"
        );
        return allTag.stream()
                .map(
                        TagMapper.INSTANCE::fromModel
                ).
                collect(Collectors.toList());
    }

    private void assertOnLogbook(String logbookId, Integer error, String domain) {
        boolean logbook = wrapCatch(
                () -> logbookRepository.existsById(logbookId),
                -1,
                "LogbookService:assertOnLogbook"
        );
        assertion(
                () -> logbook,
                LogbookNotFound.logbookNotFoundBuilder()
                        .errorCode(error)
                        .errorDomain(domain)
                        .build()
        );
    }
}
