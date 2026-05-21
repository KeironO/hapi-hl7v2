package ca.uhn.hl7v2.testpanel.controller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.hl7v2.testpanel.model.conf.ProfileFileList;
import ca.uhn.hl7v2.testpanel.model.conn.InboundConnectionList;
import ca.uhn.hl7v2.testpanel.model.conn.OutboundConnectionList;
import ca.uhn.hl7v2.testpanel.model.msg.Hl7V2MessageCollection;

public class WorkspaceController {

    private static final Logger ourLog = LoggerFactory.getLogger(WorkspaceController.class);
    private static JAXBContext ourJaxbContext;

    static {
        try {
            ourJaxbContext = JAXBContext.newInstance(WorkspaceModel.class);
        } catch (JAXBException e) {
            throw new Error("Failed to create JAXB context for WorkspaceModel", e);
        }
    }

    private File myWorkspaceFile;
    private WorkspaceModel myModel;
    private final Controller myController;

    public WorkspaceController(Controller theController) {
        myController = theController;
    }

    public boolean hasWorkspace() {
        return myWorkspaceFile != null && myModel != null;
    }

    public File getWorkspaceFile() {
        return myWorkspaceFile;
    }

    public WorkspaceModel getModel() {
        return myModel;
    }

    public File getRootFolder() {
        if (myModel == null || myModel.getRootFolder() == null) return null;
        return new File(myModel.getRootFolder());
    }

    /**
     * Creates a new workspace rooted at the given folder. Writes the workspace
     * file and updates Prefs with the last-used path.
     */
    public void createWorkspace(File theRootFolder, OutboundConnectionList outbound, InboundConnectionList inbound) {
        myModel = new WorkspaceModel();
        myModel.setRootFolder(theRootFolder.getAbsolutePath());
        myModel.setOutboundConnectionList(outbound.exportConfigToXml());
        myModel.setInboundConnectionList(inbound.exportConfigToXml());

        String folderName = theRootFolder.getName();
        myWorkspaceFile = new File(theRootFolder, folderName + ".hapi-workspace.xml");

        save();
        Prefs.getInstance().setLastWorkspacePath(myWorkspaceFile.getAbsolutePath());
        ourLog.info("Created new workspace at {}", myWorkspaceFile);
    }

    /**
     * Opens an existing workspace file. Returns true on success.
     */
    public boolean openWorkspace(File theWorkspaceFile) {
        try {
            Unmarshaller u = ourJaxbContext.createUnmarshaller();
            myModel = (WorkspaceModel) u.unmarshal(theWorkspaceFile);
            myWorkspaceFile = theWorkspaceFile;
            Prefs.getInstance().setLastWorkspacePath(theWorkspaceFile.getAbsolutePath());
            ourLog.info("Opened workspace from {}", theWorkspaceFile);
            return true;
        } catch (JAXBException e) {
            ourLog.error("Failed to open workspace file: {}", theWorkspaceFile, e);
            return false;
        }
    }

    public void closeWorkspace() {
        myModel = null;
        myWorkspaceFile = null;
    }

    /**
     * Persists the current state of connections and open files to the workspace
     * file. Should be called whenever the user saves or the app closes.
     */
    public void save(OutboundConnectionList outbound, InboundConnectionList inbound,
                     List<Hl7V2MessageCollection> openMessages, String activeFilePath,
                     ProfileFileList profileFileList) {
        if (myModel == null) return;

        myModel.setOutboundConnectionList(outbound.exportConfigToXml());
        myModel.setInboundConnectionList(inbound.exportConfigToXml());

        List<String> openFiles = new ArrayList<>();
        for (Hl7V2MessageCollection msg : openMessages) {
            if (msg.getSaveFileName() != null && !msg.getSaveFileName().isEmpty()) {
                openFiles.add(msg.getSaveFileName());
            }
        }
        myModel.setOpenFiles(openFiles);
        myModel.setActiveFile(activeFilePath);

        List<String> profilePaths = new ArrayList<>();
        for (ca.uhn.hl7v2.testpanel.model.conf.ProfileGroup pg : profileFileList.getProfiles()) {
            if (pg.getSourceUrl() != null && !pg.getSourceUrl().isEmpty()) {
                profilePaths.add(pg.getSourceUrl());
            }
        }
        myModel.setOpenProfileFiles(profilePaths);

        save();
    }

    private void save() {
        if (myWorkspaceFile == null || myModel == null) return;
        myController.invokeInBackground(() -> {
            try {
                Marshaller m = ourJaxbContext.createMarshaller();
                m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
                try (Writer w = new OutputStreamWriter(new FileOutputStream(myWorkspaceFile), StandardCharsets.UTF_8)) {
                    m.marshal(myModel, w);
                }
                ourLog.debug("Workspace saved to {}", myWorkspaceFile);
            } catch (JAXBException | IOException e) {
                ourLog.error("Failed to save workspace", e);
            }
        });
    }
}
