package ca.uhn.hl7v2.testpanel.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileBrowserPanel extends JPanel {

    private static final Logger ourLog = LoggerFactory.getLogger(FileBrowserPanel.class);

    public interface FileOpenListener {
        void openFile(File theFile);
    }

    private final JTree myTree;
    private final DefaultTreeModel myTreeModel;
    private DefaultMutableTreeNode myRoot;
    private FileOpenListener myListener;
    private File myRootFolder;

    public FileBrowserPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder());

        myRoot = new DefaultMutableTreeNode("No workspace open");
        myTreeModel = new DefaultTreeModel(myRoot);
        myTree = new JTree(myTreeModel);
        myTree.setRootVisible(true);
        myTree.setShowsRootHandles(true);
        myTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        myTree.setCellRenderer(new FileBrowserCellRenderer());
        myTree.setToggleClickCount(1);

        myTree.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    TreePath path = myTree.getPathForLocation(e.getX(), e.getY());
                    if (path == null) return;
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                    Object userObj = node.getUserObject();
                    if (userObj instanceof File) {
                        File f = (File) userObj;
                        if (f.isFile() && myListener != null) {
                            myListener.openFile(f);
                        }
                    }
                }
            }
        });

        JScrollPane scroll = new JScrollPane(myTree);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        add(scroll, BorderLayout.CENTER);
    }

    public void setFileOpenListener(FileOpenListener theListener) {
        myListener = theListener;
    }

    public void setRootFolder(File theFolder) {
        myRootFolder = theFolder;
        refresh();
    }

    public File getRootFolder() {
        return myRootFolder;
    }

    public void clearWorkspace() {
        myRootFolder = null;
        myRoot = new DefaultMutableTreeNode("No workspace open");
        myTreeModel.setRoot(myRoot);
        myTreeModel.reload();
    }

    public void refresh() {
        if (myRootFolder == null || !myRootFolder.isDirectory()) return;
        SwingUtilities.invokeLater(() -> {
            myRoot = buildNode(myRootFolder);
            myTreeModel.setRoot(myRoot);
            myTreeModel.reload();
            myTree.expandRow(0);
        });
    }

    private DefaultMutableTreeNode buildNode(File theFile) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(theFile);

        if (theFile.isDirectory()) {
            File[] children = theFile.listFiles();
            if (children != null) {
                Arrays.sort(children, Comparator
                    .comparing(File::isFile) // directories first
                    .thenComparing(f -> f.getName().toLowerCase()));
                for (File child : children) {
                    if (child.isDirectory()) {
                        DefaultMutableTreeNode childNode = buildNode(child);
                        if (childNode.getChildCount() > 0 || child.isDirectory()) {
                            node.add(childNode);
                        }
                    } else if (isAccepted(child)) {
                        node.add(new DefaultMutableTreeNode(child));
                    }
                }
            }
        }
        return node;
    }

    private boolean isAccepted(File f) {
        String name = f.getName().toLowerCase();
        return name.endsWith(".hl7") || name.endsWith(".xml");
    }

    private class FileBrowserCellRenderer extends DefaultTreeCellRenderer {

        private final ImageIcon myFolderIcon;
        private final ImageIcon myHl7Icon;
        private final ImageIcon myXmlIcon;

        FileBrowserCellRenderer() {
            myFolderIcon = loadIcon("/ca/uhn/hl7v2/testpanel/images/file.png");
            myHl7Icon = loadIcon("/ca/uhn/hl7v2/testpanel/images/message_hl7.png");
            myXmlIcon = loadIcon("/ca/uhn/hl7v2/testpanel/images/file.png");
        }

        private ImageIcon loadIcon(String path) {
            try {
                java.net.URL url = getClass().getResource(path);
                return url != null ? new ImageIcon(url) : null;
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
                                                       boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

            Object userObj = ((DefaultMutableTreeNode) value).getUserObject();
            if (userObj instanceof File) {
                File f = (File) userObj;
                setText(f.getName());
                if (f.isDirectory()) {
                    if (myFolderIcon != null) setIcon(myFolderIcon);
                } else if (f.getName().toLowerCase().endsWith(".hl7")) {
                    if (myHl7Icon != null) setIcon(myHl7Icon);
                } else {
                    if (myXmlIcon != null) setIcon(myXmlIcon);
                }
            } else {
                // Root label when no workspace
                setText(userObj.toString());
                setIcon(null);
                setForeground(Color.GRAY);
            }
            return this;
        }
    }
}
