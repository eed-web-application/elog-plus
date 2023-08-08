package edu.stanford.slac.elog_plus.service;

import edu.stanford.slac.elog_plus.api.v1.dto.*;
import edu.stanford.slac.elog_plus.api.v1.mapper.LogbookMapper;
import edu.stanford.slac.elog_plus.api.v1.mapper.ShiftMapper;
import edu.stanford.slac.elog_plus.api.v1.mapper.TagMapper;
import edu.stanford.slac.elog_plus.exception.*;
import edu.stanford.slac.elog_plus.model.Logbook;
import edu.stanford.slac.elog_plus.model.Shift;
import edu.stanford.slac.elog_plus.model.Tag;
import edu.stanford.slac.elog_plus.repository.EntryRepository;
import edu.stanford.slac.elog_plus.repository.LogbookRepository;
import edu.stanford.slac.elog_plus.utility.StringUtilities;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static edu.stanford.slac.elog_plus.exception.Utility.assertion;
import static edu.stanford.slac.elog_plus.exception.Utility.wrapCatch;

@Log4j2
@Service
@AllArgsConstructor
public class LogbookService {
    EntryRepository entryRepository;
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

        // check for logbook with the same name
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
        log.info("New logbook '{}' created", newLogbook.getName());
        return newLogbook.getId();
    }


    /**
     * Update an existing logbook
     *
     * @param logbookDTO the updated logbook
     */
    @Transactional
    public void update(String logbookId, UpdateLogbookDTO logbookDTO) {
        // check if id exists
        Optional<Logbook> logBook = wrapCatch(
                () -> logbookRepository.findById(logbookId),
                -1,
                "LogbookService::update"
        );

        // assert on logbook presence
        assertion(
                logBook::isPresent,
                LogbookNotFound.logbookNotFoundBuilder()
                        .errorCode(-2)
                        .errorDomain("LogbookService::update")
                        .build()
        );
        assertion(
                () -> logBook.get().getName() != null && !logBook.get().getName().isEmpty(),
                ControllerLogicException.builder()
                        .errorCode(-3)
                        .errorMessage("The name field is mandatory")
                        .errorDomain("LogbookService::update")
                        .build()
        );
        assertion(
                () -> logBook.get().getTags() != null,
                ControllerLogicException.builder()
                        .errorCode(-3)
                        .errorMessage("The tags field is mandatory")
                        .errorDomain("LogbookService::update")
                        .build()
        );
        assertion(
                () -> logBook.get().getTags() != null,
                ControllerLogicException.builder()
                        .errorCode(-3)
                        .errorMessage("The shift field is mandatory")
                        .errorDomain("LogbookService::update")
                        .build()
        );
        Logbook updateLogbookInfo = LogbookMapper.INSTANCE.fromDTO(logbookDTO);
        Logbook lbToUpdated = logBook.get();
        if (lbToUpdated.getShifts() == null) {
            lbToUpdated.setShifts(new ArrayList<>());
        }
        if (lbToUpdated.getTags() == null) {
            lbToUpdated.setTags(new ArrayList<>());
        }
        // normalize name and shift
        lbToUpdated.setName(
                StringUtilities.tagNameNormalization(logbookDTO.name())
        );
        // check that all shifts id are present, the shift with no id it's ok because he will be created
        verifyShiftAndUpdate(
                updateLogbookInfo.getShifts(),
                lbToUpdated.getShifts(),
                -4,
                "LogbookService:update");

        // check that all tags id are present, the tag with no id it's ok because he will be created
        verifyTagAndUpdate(
                updateLogbookInfo.getTags(),
                lbToUpdated.getTags(),
                -5,
                "LogbookService:update"
        );

        //we can save the logbook
        wrapCatch(
                () -> logbookRepository.save(lbToUpdated),
                -6,
                "LogbookService:update"
        );
        log.info("Logbook '{}' has been updated", lbToUpdated.getName());
    }

    /**
     * verify and updates the tags
     *
     * @param updateTagList the list of the tags updates
     * @param actualTagList the list of actual tags
     */
    private void verifyTagAndUpdate(List<Tag> updateTagList, List<Tag> actualTagList, int errorCode, String errorDomain) {
        //normalize tag
        updateTagList.forEach(
                t -> t.setName(
                        StringUtilities.tagNameNormalization(
                                t.getName()
                        )
                )
        );

        for (Tag tagToUpdate :
                updateTagList) {
            if (tagToUpdate.getId() == null) {
                // generate new ID
                tagToUpdate.setId(UUID.randomUUID().toString());
                continue;
            }
            boolean exists = actualTagList.stream().anyMatch(
                    s -> s.getId().compareTo(tagToUpdate.getId()) == 0
            );
            // check if the script with the same id exists, in case fire exception
            assertion(
                    () -> exists,
                    TagNotFound.tagNotFoundBuilder()
                            .errorCode(errorCode)
                            .tagName(tagToUpdate.getName())
                            .errorDomain(errorDomain)
                            .build()
            );
        }

        //check which shift should be removed
        for (Tag t :
                actualTagList) {
            boolean willBeUpdated = updateTagList.stream().anyMatch(
                    ut -> ut.getId() != null && ut.getId().compareTo(t.getId()) == 0
            );
            if (willBeUpdated) continue;

            // in this case the shift will be deleted
            // now if we need to check if the shift is used
            long summariesForTagName = wrapCatch(
                    () -> entryRepository.countByTagsContains(t.getName()),
                    -1,
                    "LogbookService:verifyTagAndUpdate"
            );

            assertion(
                    () -> summariesForTagName == 0,
                    ControllerLogicException.of(
                            -2,
                            String.format("The tag with the id '%s' cannot be deleted because has associated summaries", t.getId()),
                            "LogbookService:verifyTagAndUpdate"
                    )
            );
        }

        // we can update the tags
        actualTagList.clear();
        actualTagList.addAll(updateTagList);
    }

    /**
     * Update the actual list of shift with the updated shift list
     *
     * @param updatedShifts a list that contains the shift updates
     * @param actualShift   a list that contains the actual shift list
     * @param errorCode     is the error code to generate in case of fails
     * @param errorDomain   is the error domain in case of fail
     */
    private void verifyShiftAndUpdate(List<Shift> updatedShifts, List<Shift> actualShift, int errorCode, String errorDomain) {
        //normalize tag
        updatedShifts.forEach(
                s -> s.setName(
                        StringUtilities.shiftNameNormalization(
                                s.getName()
                        )
                )
        );

        // check if the shift to update exists and have a valid id
        for (Shift shiftToUpdate :
                updatedShifts) {
            if (shiftToUpdate.getId() == null) {
                // generate new ID
                shiftToUpdate.setId(UUID.randomUUID().toString());
                continue;
            }
            boolean exists = actualShift.stream().anyMatch(
                    s -> s.getId().compareTo(shiftToUpdate.getId()) == 0
            );
            // check if the script with the same id exists, in case fire exception
            assertion(
                    () -> exists,
                    ShiftNotFound.shiftNotFoundBuilder()
                            .errorCode(errorCode)
                            .shiftName(shiftToUpdate.getName())
                            .errorDomain(errorDomain)
                            .build()
            );
        }

        //check which shift should be removed
        for (Shift s :
                actualShift) {
            boolean willBeUpdated = updatedShifts.stream().anyMatch(
                    us -> us.getId() != null && us.getId().compareTo(s.getId()) == 0
            );
            if (willBeUpdated) continue;

            // in this case the shift will be deleted
            // now if we need to check if the shift is used
            long summariesForShift = wrapCatch(
                    () -> entryRepository.countBySummarizes_ShiftId(s.getId()),
                    -1,
                    "LogbookService:verifyShiftAndUpdate"
            );

            assertion(
                    () -> summariesForShift == 0,
                    ControllerLogicException.of(
                            -2,
                            String.format("The shift with the id '%s' cannot be deleted because has associated summaries", s.getId()),
                            "LogbookService:verifyShiftAndUpdate"
                    )
            );
        }

        actualShift.clear();
        for (Shift shift :
                updatedShifts) {
            Shift shiftToAdd = validateShift(
                    shift,
                    errorCode,
                    errorDomain
            );
            checkShiftAmongAllOther(shiftToAdd, actualShift, errorCode, errorDomain);

            if (shiftToAdd.getId() == null) {
                shiftToAdd.setId(UUID.randomUUID().toString());
            }
            actualShift.add(shiftToAdd);
        }
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
     *
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
                        () -> LogbookNotFound.logbookNotFoundBuilder()
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
     * Create new tag just in case it doesn't exist
     *
     * @param logbookId the logbook id
     * @param tagName   the tag name
     */
    public String ensureTag(String logbookId, String tagName) {
        assertOnLogbook(logbookId, -1, "LogbookService:ensureTag");
        return wrapCatch(
                () ->
                        logbookRepository.ensureTag(
                                logbookId,
                                TagMapper.INSTANCE.fromDTO(
                                        NewTagDTO
                                                .builder()
                                                .name(StringUtilities.tagNameNormalization(tagName))
                                                .build()
                                )
                        )
                ,
                -2,
                "LogbookService:ensureTag"
        );
    }

    /**
     * Check if a tag exist for the log
     * <p>
     * the name is normalized before checking
     *
     * @param logbookId the id of the logbook
     * @param tagName   the name of the tag
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
                "LogbookService:tagExistForLogbook"
        );
    }

    /**
     * Return all tags ofr a logbook
     *
     * @param logbookId the id of the logbook
     * @return all the logbook tags
     */
    public List<TagDTO> getAllTags(String logbookId) {
        assertOnLogbook(logbookId, -1, "LogbookService:getAllTags");
        List<Tag> allTag = wrapCatch(
                () -> logbookRepository.getAllTagFor(logbookId),
                -2,
                "LogbookService:getAllTags"
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

    public List<TagDTO> getAllTagsByLogbooksName(List<String> logbooks) {
        List<TagDTO> result = new ArrayList<>();

        if (logbooks == null || logbooks.isEmpty()) {
            logbooks = wrapCatch(
                    () -> logbookRepository.getAllLogbook(),
                    -1,
                    "LogbookService:getAllTagsByLogbooksName"
            );
        }

        for (String logbookName :
                logbooks) {
            if (!exist(logbookName)) {
                continue;
            }
            LogbookDTO lbDTO = getLogbookByName(logbookName);
            result.removeAll(lbDTO.tags());
            result.addAll(lbDTO.tags());
        }
        return result;
    }

    /**
     * Replace the shift in this way
     * <p>
     * if a shift as no id it will be created as new, if it has an ID,
     * it will be used for find it within all the shift, in case is not found an exception will be thrown
     *
     * @param logbookId   the logbook id
     * @param allNewShift all the new shift
     */
    @Transactional()
    public void replaceShift(String logbookId, List<ShiftDTO> allNewShift) {
        Optional<Logbook> lb =
                wrapCatch(
                        () -> logbookRepository.findById(
                                logbookId
                        ),
                        -1,
                        "LogbookService:replaceShift"
                );
        assertion(
                lb::isPresent,
                LogbookNotFound.logbookNotFoundBuilder()
                        .errorCode(-2)
                        .errorDomain("LogbookService:replaceShift")
                        .build()
        );

        Logbook lbToSave = lb.get();

        // verify and update the shifts
        verifyShiftAndUpdate(
                ShiftMapper.INSTANCE.fromDTO(allNewShift),
                lbToSave.getShifts(),
                -3,
                "LogbookService:replaceShift"
        );

        wrapCatch(
                () -> logbookRepository.save(
                        lbToSave
                ),
                -3,
                "LogbookService:addShift"
        );
    }

    /**
     * Add new shift checking for the correctness
     *
     * @param logbookId   the id of the logbook from which we want to add the shift
     * @param newShiftDTO the shift description
     */
    @Transactional(propagation = Propagation.NESTED)
    public String addShift(String logbookId, NewShiftDTO newShiftDTO) {
        // validate the shift
        Shift shiftToAdd = validateShift(ShiftMapper.INSTANCE.fromDTO(newShiftDTO), -1, "LogbookService:addShift");

        // normalize shift name
        shiftToAdd.setName(
                StringUtilities.shiftNameNormalization(
                        shiftToAdd.getName()
                )
        );

        Optional<Logbook> lb =
                wrapCatch(
                        () -> logbookRepository.findById(
                                logbookId
                        ),
                        -3,
                        "LogbookService:addShift"
                );
        assertion(
                lb::isPresent,
                LogbookNotFound.logbookNotFoundBuilder()
                        .errorCode(-2)
                        .errorDomain("LogbookService:addShift")
                        .build()
        );

        Logbook lbToSave = lb.get();

        //create ID
        shiftToAdd.setId(UUID.randomUUID().toString());

        // get all shift already saved
        List<Shift> allShift = lbToSave.getShifts();

        // check if the new shift is not overlapped with the other
        checkShiftAmongAllOther(
                shiftToAdd,
                allShift,
                -3,
                "LogbookService:addShift");

        // we can add the shift
        lbToSave.getShifts().add(shiftToAdd);

        // save shift
        wrapCatch(
                () -> logbookRepository.save(
                        lbToSave
                ),
                -3,
                "LogbookService:addShift"
        );
        return shiftToAdd.getId();
    }

    @Transactional(propagation = Propagation.NESTED)
    public void updateShift(String logbookId, ShiftDTO shiftDTO) {
        // validate the shift
        Shift shiftToUpdate = validateShift(
                ShiftMapper.INSTANCE.fromDTO(shiftDTO),
                -1,
                "LogbookService:updateShift"
        );

        // normalize shift name
        shiftToUpdate.setName(
                StringUtilities.shiftNameNormalization(
                        shiftToUpdate.getName()
                )
        );

        Optional<Logbook> lb =
                wrapCatch(
                        () -> logbookRepository.findById(
                                logbookId
                        ),
                        -3,
                        "LogbookService:addShift"
                );
        assertion(
                lb::isPresent,
                LogbookNotFound.logbookNotFoundBuilder()
                        .errorCode(-2)
                        .errorDomain("LogbookService:addShift")
                        .build()
        );

        Logbook lbToSave = lb.get();

        //create ID
        shiftToUpdate.setId(UUID.randomUUID().toString());

        // get all shift already saved
        List<Shift> allShift = lbToSave.getShifts()
                .stream().filter(
                        s -> s.getId().compareTo(shiftToUpdate.getId()) == 0
                ).collect(Collectors.toList());


        // check if the new shift is not overlapped with the other
        checkShiftAmongAllOther(
                shiftToUpdate,
                allShift,
                -3,
                "LogbookService:addShift");

        // we can add the shift
        lbToSave.getShifts().add(shiftToUpdate);

        // save shift
        wrapCatch(
                () -> logbookRepository.save(
                        lbToSave
                ),
                -3,
                "LogbookService:addShift"
        );
    }

    private static void checkShiftAmongAllOther(
            Shift newShift,
            List<Shift> allShift,
            Integer errorCode,
            String errorDomain) {
        for (Shift savedShift :
                allShift) {
            // check from field
            if (
                    (
                            newShift.getFromTime().equals(savedShift.getFromTime()) ||
                                    newShift.getFromTime().equals(savedShift.getToTime())
                    ) ||

                            (
                                    newShift.getToTime().equals(savedShift.getFromTime()) ||
                                            newShift.getToTime().equals(savedShift.getToTime())
                            )
            ) {
                throw ControllerLogicException.of(
                        errorCode,
                        String.format("New shift 'from' field overlap with the shift %s", savedShift.getName()),
                        errorDomain
                );
            }

            if (
                    newShift.getFromTime().isBefore(savedShift.getToTime()) &&
                            newShift.getFromTime().isAfter(savedShift.getFromTime())
            ) {
                throw ControllerLogicException.of(
                        errorCode,
                        String.format("New shift 'from' field overlap with the shift %s", savedShift.getName()),
                        errorDomain
                );
            }

            if (
                    newShift.getToTime().isBefore(savedShift.getToTime()) &&
                            newShift.getToTime().isAfter(savedShift.getFromTime())
            ) {
                throw ControllerLogicException.of(
                        errorCode,
                        String.format("New shift 'to' field overlap with the shif %s", savedShift.getName()),
                        errorDomain
                );
            }
        }
    }

    /**
     * Validate the shift
     *
     * @param shiftToAdd is the shift to validate
     */
    private Shift validateShift(Shift shiftToAdd, Integer errorCode, String errorDomain) {
        final String timeRegex = "^(0?[0-9]|1[0-9]|2[0-3]):([0-5][0-9])$";
        final Pattern pattern = Pattern.compile(timeRegex, Pattern.MULTILINE);

        assertion(
                () -> shiftToAdd != null,
                ControllerLogicException.of(
                        errorCode,
                        "The shift cannot be null",
                        errorDomain
                )
        );

        // check 'name'
        assertion(
                () -> shiftToAdd.getName() != null && !shiftToAdd.getName().isEmpty(),
                ControllerLogicException.of(
                        errorCode,
                        "The shift 'name' is mandatory",
                        errorDomain
                )
        );

        //check 'from' field
        assertion(
                () -> shiftToAdd.getFrom() != null && !shiftToAdd.getFrom().isEmpty(),
                ControllerLogicException.of(
                        errorCode,
                        "The shift 'from' field is mandatory",
                        errorDomain
                )
        );
        Matcher fromMatcher = pattern.matcher(shiftToAdd.getFrom());
        assertion(
                () -> fromMatcher.matches() && fromMatcher.groupCount() == 2,
                ControllerLogicException.of(
                        errorCode,
                        "The shift 'from' field need to be a time in the range 00:01-23:59",
                        errorDomain
                )
        );

        //check 'to' field
        assertion(
                () -> shiftToAdd.getTo() != null && !shiftToAdd.getTo().isEmpty(),
                ControllerLogicException.of(
                        errorCode,
                        "The shift 'to' field is mandatory",
                        errorDomain
                )
        );
        Matcher toMatcher = pattern.matcher(shiftToAdd.getTo());
        assertion(
                () -> toMatcher.matches() && toMatcher.groupCount() == 2,
                ControllerLogicException.of(
                        errorCode,
                        "The shift 'to' field need to be a time in the range 00:01-23:59",
                        errorDomain
                )
        );

        assertion(
                () -> shiftToAdd.getFromTime().isBefore(shiftToAdd.getToTime()),
                ControllerLogicException.of(
                        errorCode,
                        "The shift 'from' time should be before 'to' time",
                        errorDomain
                )
        );
        shiftToAdd.setFromMinutesSinceMidnight(
                shiftToAdd.getFromTime().getHour() * 60 + shiftToAdd.getFromTime().getMinute()
        );
        shiftToAdd.setToMinutesSinceMidnight(
                shiftToAdd.getToTime().getHour() * 60 + shiftToAdd.getToTime().getMinute()
        );
        //shiftToAdd.setFromTime(fromTime);
        //.setToTime(toTime);
        return shiftToAdd;
    }

    /**
     * Return the shift which the date fall in its range
     *
     * @param logbookId the logbook unique identifier
     * @param localTime the time of the event in the day
     * @return the found shift, if eny matches
     */
    public Optional<ShiftDTO> findShiftByLocalTime(String logbookId, LocalTime localTime) {
        assertOnLogbook(logbookId, -1, "LogbookService:getShiftByLocalTime");
        return wrapCatch(
                () -> logbookRepository.findShiftFromLocalTime(
                        logbookId,
                        localTime
                ).map(
                        ShiftMapper.INSTANCE::fromModel
                ),
                -2,
                "LogbookService:getShiftByLocalTime"
        );
    }

    /**
     * Return the shift which the date fall in its range
     *
     * @param logbookName the logbook unique name identifier
     * @param localTime   the time of the event in the day
     * @return the found shift, if eny matches
     */
    public Optional<ShiftDTO> findShiftByLocalTimeWithLogbookName(String logbookName, LocalTime localTime) {
        return wrapCatch(
                () -> logbookRepository.findShiftFromLocalTimeWithLogbookName(
                        logbookName,
                        localTime
                ).map(
                        ShiftMapper.INSTANCE::fromModel
                ),
                -2,
                "LogbookService:getShiftByLocalTime"
        );
    }
}
