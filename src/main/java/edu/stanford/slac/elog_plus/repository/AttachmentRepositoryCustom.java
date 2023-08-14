package edu.stanford.slac.elog_plus.repository;

import edu.stanford.slac.elog_plus.model.Attachment;

public interface AttachmentRepositoryCustom {
    /**
     * Set the storage id for the preview of an attachment
     *
     * @param id        attachement id
     * @param previewID the newly uploaded preview storage unique identifier
     * @return
     */
    void setPreviewID(String id, String previewID);

    void setMiniPreview(String id, byte[] byteArray);

    /**
     * Set the preview processing state for an attachment
     *
     * @param id    attachement id
     * @param state is the current state of the preview
     * @return
     */
    void setPreviewState(String id, Attachment.PreviewProcessingState state);

    /**
     * retrn the current processing state of a preview
     * @param id attachement id
     * @return the current state of the processing of the preview
     */
    Attachment.PreviewProcessingState getPreviewState(String id);
}
