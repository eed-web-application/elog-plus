package edu.stanford.slac.elog_plus.repository;

import edu.stanford.slac.elog_plus.model.Attachment;

public interface AttachmentRepositoryCustom {
    /**
     * Set the storage id for the preview of an attachment
     *
     * @param id attachment id
     * @param previewID the newly uploaded preview storage unique identifier
     */
    void setPreviewID(String id, String previewID);

    /**
     * set the mini-preview
     * @param id the id of the attachment
     * @param byteArray the mini preview information
     */
    void setMiniPreview(String id, byte[] byteArray);

    /**
     * Set the preview processing state for an attachment
     *
     * @param id the attachment id
     * @param state is the current state of the preview
     */
    void setPreviewState(String id, Attachment.PreviewProcessingState state);

    /**
     * Return the current processing state of a preview
     * @param id attachment id
     * @return the current state of the processing of the preview
     */
    Attachment.PreviewProcessingState getPreviewState(String id);

    /**
     * Set the in use state of the attachment
     *
     * this field is set when an entry is created with this attachment
     * @param id the attachment id
     * @param inUse if true meaning that the attachment is used by some entry
     */
    void setInUseState(String id, Boolean inUse);
}
