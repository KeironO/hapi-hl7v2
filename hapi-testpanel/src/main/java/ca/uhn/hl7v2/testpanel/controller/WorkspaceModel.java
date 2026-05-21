package ca.uhn.hl7v2.testpanel.controller;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "HapiWorkspace", namespace = "urn:ca.uhn.hapi:testpanel:workspace")
@XmlAccessorType(XmlAccessType.FIELD)
public class WorkspaceModel {

    @XmlElement(name = "root_folder")
    private String myRootFolder;

    @XmlElement(name = "outbound_connection_list")
    private String myOutboundConnectionList;

    @XmlElement(name = "inbound_connection_list")
    private String myInboundConnectionList;

    @XmlElement(name = "open_profile_files")
    private List<String> myOpenProfileFiles = new ArrayList<>();

    @XmlElement(name = "open_file")
    private List<String> myOpenFiles = new ArrayList<>();

    @XmlElement(name = "active_file")
    private String myActiveFile;

    public String getRootFolder() {
        return myRootFolder;
    }

    public void setRootFolder(String theRootFolder) {
        myRootFolder = theRootFolder;
    }

    public String getOutboundConnectionList() {
        return myOutboundConnectionList;
    }

    public void setOutboundConnectionList(String theList) {
        myOutboundConnectionList = theList;
    }

    public String getInboundConnectionList() {
        return myInboundConnectionList;
    }

    public void setInboundConnectionList(String theList) {
        myInboundConnectionList = theList;
    }

    public List<String> getOpenProfileFiles() {
        if (myOpenProfileFiles == null) myOpenProfileFiles = new ArrayList<>();
        return myOpenProfileFiles;
    }

    public void setOpenProfileFiles(List<String> theFiles) {
        myOpenProfileFiles = theFiles;
    }

    public List<String> getOpenFiles() {
        if (myOpenFiles == null) myOpenFiles = new ArrayList<>();
        return myOpenFiles;
    }

    public void setOpenFiles(List<String> theFiles) {
        myOpenFiles = theFiles;
    }

    public String getActiveFile() {
        return myActiveFile;
    }

    public void setActiveFile(String theActiveFile) {
        myActiveFile = theActiveFile;
    }
}
