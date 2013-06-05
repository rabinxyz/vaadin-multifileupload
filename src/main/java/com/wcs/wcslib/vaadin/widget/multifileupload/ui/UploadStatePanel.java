package com.wcs.wcslib.vaadin.widget.multifileupload.ui;

import com.vaadin.server.StreamVariable;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;
import com.wcs.wcslib.vaadin.widget.multifileupload.component.FileDetail;
import com.wcs.wcslib.vaadin.widget.multifileupload.component.MultiUploadHandler;
import com.wcs.wcslib.vaadin.widget.multifileupload.component.SmartMultiUpload;
import com.wcs.wcslib.vaadin.widget.multifileupload.receiver.DefaultUploadReceiver;
import com.wcs.wcslib.vaadin.widget.multifileupload.receiver.UploadReceiver;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author gergo
 */
public class UploadStatePanel extends Panel implements MultiUploadHandler {

    private static final String PANEL_STLYE_CLASS = "multiple-upload-state-panel";
    private List<FileDetailBean> uploadQueue = new ArrayList<FileDetailBean>();
    private UploadStateLayout currentUploadingLayout;
    private UploadStateWindow window;
    private SmartMultiUpload multiUpload;
    private UploadReceiver receiver;
    private UploadFinishedHandler finishedHandler;
    private UploadQueueTable table = new UploadQueueTable();

    public UploadStatePanel(UploadStateWindow window) {
        this(window, new DefaultUploadReceiver());
    }

    public UploadStatePanel(UploadStateWindow window, UploadReceiver uploadReceiver) {
        this.window = window;
        this.receiver = uploadReceiver;
        setVisible(false);
        setStyleName(PANEL_STLYE_CLASS);
        window.addPanel(this);

        createLayout();
    }

    private void createLayout() {
        VerticalLayout panelLayout = new VerticalLayout();
        setContent(panelLayout);
        panelLayout.setMargin(false);
        currentUploadingLayout = new UploadStateLayout(this);
        panelLayout.addComponent(currentUploadingLayout);
        panelLayout.addComponent(table);
    }

    @Override
    public void streamingStarted(StreamVariable.StreamingStartEvent event) {
        if (!uploadQueue.isEmpty()) {
            currentUploadingLayout.startStreaming(uploadQueue.get(0));
            show();
        }
    }

    @Override
    public void streamingFinished(StreamVariable.StreamingEndEvent event) {
        removeFromQueue(currentUploadingLayout.getFileDetailBean());
        InputStream stream = receiver.getStream();
        //"simple" Upload fires Upload.FinishedEvent on interruptUpload()
        if (stream != null) {
            finishedHandler.handleFile(stream, event.getFileName(), event.getMimeType(), event.getBytesReceived());
            receiver.deleteTempFile();
        }
    }

    @Override
    public OutputStream getOutputStream() {
        return receiver.receiveUpload();
    }

    @Override
    public void streamingFailed(StreamVariable.StreamingErrorEvent event) {
//        Logger.getLogger(getClass().getName()).log(Level.FINE,
//                "Streaming failed", event.getException());
        receiver.deleteTempFile();
    }

    @Override
    public void onProgress(StreamVariable.StreamingProgressEvent event) {
        currentUploadingLayout.setProgress(event.getBytesReceived(), event.getContentLength());
    }

    @Override
    public void filesQueued(Collection<FileDetail> pendingFileNames) {
        for (FileDetail fileDetail : pendingFileNames) {
            uploadQueue.add(new FileDetailBean(fileDetail, this));
        }
        table.refreshContainerDatasource(uploadQueue);
    }

    private void show() {
        setVisible(true);
        window.refreshVisibility();
    }

    public void removeFromQueue(FileDetailBean fileDetail) {
        uploadQueue.remove(fileDetail);
        table.refreshContainerDatasource(uploadQueue);
        if (uploadQueue.isEmpty()) {
            setVisible(false);
            window.refreshVisibility();
        }
    }

    public SmartMultiUpload getMultiUpload() {
        return multiUpload;
    }

    public void setMultiUpload(SmartMultiUpload multiUpload) {
        this.multiUpload = multiUpload;
    }

    public void setFinishedHandler(UploadFinishedHandler finishedHandler) {
        this.finishedHandler = finishedHandler;
    }

    public UploadStateWindow getWindow() {
        return window;
    }

    public void interruptAll() {
        for (int i = uploadQueue.size() - 1; i >= 0; i--) {
            multiUpload.interruptUpload(uploadQueue.get(i).getId());
        }
        window.removePanel(this);
        window.refreshVisibility();
    }

    public UploadQueueTable getTable() {
        return table;
    }
}