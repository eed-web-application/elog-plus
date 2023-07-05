package edu.stanford.slac.elog_plus.service;

import edu.stanford.slac.elog_plus.api.v1.dto.NewTagDTO;
import edu.stanford.slac.elog_plus.api.v1.dto.TagDTO;
import edu.stanford.slac.elog_plus.api.v1.mapper.TagMapper;
import edu.stanford.slac.elog_plus.exception.ControllerLogicException;
import edu.stanford.slac.elog_plus.model.Tag;
import edu.stanford.slac.elog_plus.repository.TagRepository;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static edu.stanford.slac.elog_plus.exception.Utility.assertion;
import static edu.stanford.slac.elog_plus.exception.Utility.wrapCatch;

@Log4j2
@Service
@AllArgsConstructor
public class TagService {
    private TagRepository tagRepository;

    /**
     * Create new tag
     *
     * @param tag the new tag
     * @return the id of the new tag
     */
    public String createTag(NewTagDTO tag) {
        assertion(
                () -> tag.name() != null && !tag.name().isBlank(),
                -1,
                "The tag name is mandatory",
                "TagService::createTag"
        );


        Tag newTag = wrapCatch(
                () -> tagRepository.save(
                        TagMapper.INSTANCE.fromDTO(
                                NewTagDTO
                                        .builder()
                                        .name(tagNameNormalization(tag.name()))
                                        .build()
                        )
                ),
                -2,
                "TagService::createTag"
        );
        return newTag.getId();
    }

    public TagDTO getByID(String id) {
        Optional<Tag> tag = wrapCatch(
                () -> tagRepository.findById(id),
                -1,
                "TagService::getByID"
        );
        return TagMapper.INSTANCE.fromModel(
                tag.orElseThrow(
                        () -> ControllerLogicException.of(
                                -2,
                                "Tag not found",
                                "TagService::getByID"
                        )
                )
        );
    }

    /**
     * Check if a tag exists
     *
     * @param tagName, is the name of tag to search
     * @return true if the tag exits
     */
    public boolean exist(String tagName) {
        return wrapCatch(
                () -> tagRepository.existsByName(tagName),
                -1,
                "TagService::exist"
        );
    }

    /**
     * Return all the tags
     *
     * @return the list of all the tags
     */
    public List<TagDTO> getAllTags() {
        return wrapCatch(
                () -> tagRepository.findAll(),
                -1,
                "TagService::getAllTags"
        )
                .stream()
                .map(
                        TagMapper.INSTANCE::fromModel
                )
                .collect(Collectors.toList());
    }

    /**
     *
     * @param tagName
     * @return
     */
    public String tagNameNormalization(String tagName) {
        return Normalizer
                .normalize(tagName, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "");
    }

    /**
     *
     * @param tagName
     * @return
     */
    public boolean existsByName(String tagName) {
        return wrapCatch(
                () -> tagRepository.existsByName(tagName),
                -1,
                "TagService::existsByName"
        );
    }
}
