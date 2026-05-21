/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.mozilla.org/MPL/
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for the
 * specific language governing rights and limitations under the License.
 *
 * The Original Code is ""  Description:
 * ""
 *
 * The Initial Developer of the Original Code is University Health Network. Copyright (C)
 * 2001.  All Rights Reserved.
 *
 * Contributor(s): ______________________________________.
 *
 * Alternatively, the contents of this file may be used under the terms of the
 * GNU General Public License (the  "GPL"), in which case the provisions of the GPL are
 * applicable instead of those above.  If you wish to allow use of your version of this
 * file only under the terms of the GPL and not to allow others to use your version
 * of this file under the MPL, indicate your decision by deleting  the provisions above
 * and replace  them with the notice and other provisions required by the GPL License.
 * If you do not delete the provisions above, a recipient may use your version of
 * this file under either the MPL or the GPL.
 */
package ca.uhn.hl7v2.testpanel.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.io.File;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.testpanel.controller.Controller;
import ca.uhn.hl7v2.testpanel.controller.Hl7V2FileDiffController;
import ca.uhn.hl7v2.testpanel.controller.Hl7V2FileSortController;
import ca.uhn.hl7v2.testpanel.controller.Prefs;
import ca.uhn.hl7v2.testpanel.model.MessagesList;
import ca.uhn.hl7v2.testpanel.model.conn.AbstractConnection;
import ca.uhn.hl7v2.testpanel.model.conn.AbstractConnection.StatusEnum;
import ca.uhn.hl7v2.testpanel.model.conn.InboundConnection;
import ca.uhn.hl7v2.testpanel.model.conn.InboundConnectionList;
import ca.uhn.hl7v2.testpanel.model.conn.OutboundConnection;
import ca.uhn.hl7v2.testpanel.model.conn.OutboundConnectionList;
import ca.uhn.hl7v2.testpanel.model.msg.Hl7V2MessageCollection;
import ca.uhn.hl7v2.testpanel.ui.ActivityTable;
import ca.uhn.hl7v2.testpanel.ui.editor.Hl7V2MessageEditorPanel;
import ca.uhn.hl7v2.testpanel.ui.v2tree.Hl7V2MessageTree;
import ca.uhn.hl7v2.testpanel.util.ScreenBoundsUtil;
import ca.uhn.hl7v2.testpanel.util.SwingLogAppender;

/**
 * This is the main outer window for the TestPanel
 */
public class TestPanelWindow implements IDestroyable {

	private Hl7V2FileDiffController myHl7V2FileDiff;
	private Controller myController;
	private JFrame myframe;
	private JTabbedPane myMessagesTabPane;
	private JTabbedPane myConnectionsTabPane;
	private boolean myUpdatingTabs = false;
	private PropertyChangeListener myTabTitleListener;
	private final PropertyChangeListener myMessageDescriptionListener;
	private MyOutboundConnectionsListModel myOutboundConnectionsListModel;
	private MyInboundConnectionsListModel myInboundConnectionsListModel;
	private JButton myMsgSaveButton;
	private PropertyChangeListener myOutboundConnectionsListListener;
	private PropertyChangeListener myInboundConnectionsListListener;
	private JButton myDeleteOutboundConnectionButton;
	private JButton myAddInboundConnectionButton;
	private PropertyChangeListener myPanelTitleListener;
	private AboutDialog myAboutDialog;
	private JButton myStartOneOutboundButton;
	private JButton myStartAllOutboundButton;
	private JButton myStopAllOutboundButton;
	private Hl7V2FileSortController myHl7V2FileSort;

	/**
	 * Create the application.
	 */
	public TestPanelWindow(Controller theController) {
		myController = theController;

		// myMessageDescriptionListener is only used for connections lists now
		myMessageDescriptionListener = null;
		new SwingLogAppender();

		initialize();
		initializeLocal();
		initWindowPosition();

		if (myController.getLeftSelectedItem() instanceof Hl7V2MessageCollection) {
			int idx = myController.getMessagesList().getMessages()
				.indexOf(myController.getLeftSelectedItem());
			if (idx >= 0) myMessagesTabPane.setSelectedIndex(idx);
		} else if (myController.getLeftSelectedItem() instanceof InboundConnection) {
			myInboundConnectionsList.setSelectedValue(myController.getLeftSelectedItem(), true);
		} else if (myController.getLeftSelectedItem() instanceof OutboundConnection) {
			myOutboundConnectionsList.setSelectedValue(myController.getLeftSelectedItem(), true);
		} else {
			ourLog.warn("Unknown type is selected: {}", myController.getLeftSelectedItem());
		}

		myPanelTitleListener = new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent theEvt) {
				updateWindowTitle();
			}
		};


	}

	
	/**
	 * @return the controller
	 */
	public Controller getController() {
		return myController;
	}

	private void initWindowPosition() {
		if (Prefs.getInstance().getWindowMaximized()) {
			myframe.setExtendedState(JFrame.MAXIMIZED_BOTH);
			return;
		}
		
		Rectangle screenBounds = ScreenBoundsUtil.getScreenBounds(myframe);
		int maxWidth = screenBounds.width;
		int maxHeight = screenBounds.height;
		int width;
		int height;

		Point position = Prefs.getInstance().getWindowPosition();
		Dimension dimension = Prefs.getInstance().getWindowDimension();
		if (dimension.width > 600 && dimension.height > 500) {
			if (position.x >= 0 && position.y >= 0) {
				if (dimension.width + position.x < maxWidth) {
					if (dimension.height + position.y < maxHeight) {
						ourLog.info("Restoring window size to {} and location to {}", dimension, position);
						myframe.setLocation(position);
						myframe.setSize(dimension);
						return;
					}
				}
			}
		}
		
		width = (int) (maxWidth * 0.7);
		if (width < 1024) {
			width = maxWidth;
		}
		width = Math.min(width, 1600);

		height = (int) (maxHeight * 0.7);
		if (height < 600) {
			height = maxHeight;
		}
		height = Math.min(height, 1000);

		if (width == maxWidth && height == maxHeight) {
			ourLog.info("Maximizing window");
			myframe.setExtendedState(Frame.MAXIMIZED_BOTH);
		} else {
			ourLog.info("Setting window size to {} x {}", width, height);
			myframe.setSize(width, height);
		}

		myframe.setLocationByPlatform(true);
	}

	private void updateWindowTitle() {
		String title = myMainPanel != null ? myMainPanel.getWindowTitle() : null;
		if (StringUtils.isNotBlank(title)) {
			myframe.setTitle("HAPI TestPanel " + myController.getAppVersionString() + " - " + title);
		} else {
			myframe.setTitle("HAPI TestPanel " + myController.getAppVersionString());
		}
	}

	public void clearMessagesListSelection() {
		// JTabbedPane doesn't support deselection; this is a no-op
	}

	private void updateLeftToolbarButtons() {

		boolean isMsg = (myController.getLeftSelectedItem() instanceof Hl7V2MessageCollection);
		mySaveMenuItem.setEnabled(isMsg);
		mySaveAsMenuItem.setEnabled(isMsg);
		myRevertToSavedMenuItem.setEnabled(leftMessageHasSaveFilename());
		
		
		if (myController.getLeftSelectedItem() instanceof OutboundConnection) {
			myDeleteOutboundConnectionButton.setEnabled(true);
			myStartOneOutboundButton.setEnabled(true);
		} else {
			myDeleteOutboundConnectionButton.setEnabled(false);
			myStartOneOutboundButton.setEnabled(false);
		}

		if (myController.getLeftSelectedItem() instanceof InboundConnection) {
			myDeleteInboundConnectionButton.setEnabled(true);
			myStartOneInboundButton.setEnabled(true);
		} else {
			myDeleteInboundConnectionButton.setEnabled(false);
			myStartOneInboundButton.setEnabled(false);
		}

	}
	
	private boolean leftMessageHasSaveFilename() {
		if (myController.getLeftSelectedItem() instanceof Hl7V2MessageCollection) {
			Hl7V2MessageCollection left = (Hl7V2MessageCollection) myController.getLeftSelectedItem();
			return StringUtils.isNotBlank(left.getSaveFileName());
		}
		return false;
	}

	private void updateLeftToolbarInboundStatusButtons() {
		boolean haveStarted = false;
		boolean haveStopped = false;
		for (InboundConnection next : myController.getInboundConnectionList().getConnections()) {
			switch (next.getStatus()) {
			case FAILED:
			case STOPPED:
				haveStopped = true;
				break;
			case STARTED:
			case TRYING_TO_START:
				haveStarted = true;
				break;
								
			}
		}
		
		myStopAllInboundButton.setEnabled(haveStarted);
		myStartAllInboundButton.setEnabled(haveStopped);
	}

	private void updateLeftToolbarOutboundStatusButtons() {
		boolean haveStarted = false;
		boolean haveStopped = false;
		for (OutboundConnection next : myController.getOutboundConnectionList().getConnections()) {
			switch (next.getStatus()) {
			case FAILED:
			case STOPPED:
				haveStopped = true;
				break;
			case STARTED:
			case TRYING_TO_START:
				haveStarted = true;
				break;
								
			}
		}
		
		myStopAllOutboundButton.setEnabled(haveStarted);
		myStartAllOutboundButton.setEnabled(haveStopped);
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		myframe = new JFrame();
		myframe.setVisible(false);
		
		List<Image> l = new ArrayList<Image>();
		l.add(Toolkit.getDefaultToolkit().getImage(TestPanelWindow.class.getResource("/ca/uhn/hl7v2/testpanel/images/hapi_16.png")));
		l.add(Toolkit.getDefaultToolkit().getImage(TestPanelWindow.class.getResource("/ca/uhn/hl7v2/testpanel/images/hapi_64.png")));
		
		myframe.setIconImages(l);
		myframe.setTitle("HAPI TestPanel");
		myframe.setBounds(100, 100, 796, 603);
		myframe.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		myframe.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent theE) {
				myController.close();
			}
		});

		myframe.getRootPane().getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW)
			.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, java.awt.event.InputEvent.CTRL_DOWN_MASK), "newMessage");
		myframe.getRootPane().getActionMap().put("newMessage", new javax.swing.AbstractAction() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent e) {
				myController.addMessage();
			}
		});

		JMenuBar menuBar = new JMenuBar();
		myframe.setJMenuBar(menuBar);
		initializeMenuBar(menuBar);
		myframe.getContentPane().setLayout(new BorderLayout(0, 0));

		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());
		myframe.getContentPane().add(mainPanel);


		myMessagesTabPane = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
		myMessagesTabPane.addChangeListener(e -> {
			if (myUpdatingTabs) return;
			int idx = myMessagesTabPane.getSelectedIndex();
			if (idx >= 0) {
				List<Hl7V2MessageCollection> messages = myController.getMessagesList().getMessages();
				if (idx < messages.size()) {
					Object selected = messages.get(idx);
					myController.setLeftSelectedItem(selected);
					myOutboundConnectionsList.clearSelection();
					myInboundConnectionsList.clearSelection();
				}
			}
			updateLeftToolbarButtons();
		});

		myMessagesTabPane.addMouseListener(new java.awt.event.MouseAdapter() {
			@Override
			public void mousePressed(java.awt.event.MouseEvent e) {
				int idx = myMessagesTabPane.getSelectedIndex();
				if (idx >= 0) {
					List<Hl7V2MessageCollection> messages = myController.getMessagesList().getMessages();
					if (idx < messages.size()) {
						myController.setLeftSelectedItem(messages.get(idx));
					}
				}
			}
		});

		myConnectionsTabPane = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.WRAP_TAB_LAYOUT);
		myConnectionsTabPane.setPreferredSize(new Dimension(200, 150));

		JPanel sendingConnectionsPanel = new JPanel();
		myConnectionsTabPane.addTab("Sending Connections", sendingConnectionsPanel);

		JPanel receivingConnectionsPanel = new JPanel();
		myConnectionsTabPane.addTab("Receiving Connections", receivingConnectionsPanel);

		JPanel sendingActivityPanel = new JPanel();
		myConnectionsTabPane.addTab("Sending", sendingActivityPanel);

		JPanel connectionsPanel = sendingConnectionsPanel;
		GridBagLayout gbl_connectionsPanel = new GridBagLayout();
		gbl_connectionsPanel.columnWidths = new int[] { 194, 0 };
		gbl_connectionsPanel.rowHeights = new int[] { 30, 0, 0 };
		gbl_connectionsPanel.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
		gbl_connectionsPanel.rowWeights = new double[] { 0.0, 0.0, 1.0, Double.MIN_VALUE };
		connectionsPanel.setLayout(gbl_connectionsPanel);

		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		GridBagConstraints gbc_toolBar = new GridBagConstraints();
		gbc_toolBar.insets = new Insets(0, 0, 5, 0);
		gbc_toolBar.anchor = GridBagConstraints.NORTH;
		gbc_toolBar.fill = GridBagConstraints.HORIZONTAL;
		gbc_toolBar.gridx = 0;
		gbc_toolBar.gridy = 0;
		connectionsPanel.add(toolBar, gbc_toolBar);

		myAddConnectionButton = new JButton("New");
		myAddConnectionButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				myController.addOutboundConnection();
			}
		});
		myAddConnectionButton.setBorderPainted(false);
		myAddConnectionButton.addMouseListener(new HoverButtonMouseAdapter(myAddConnectionButton));
		myAddConnectionButton.setIcon(new ImageIcon(TestPanelWindow.class.getResource("/ca/uhn/hl7v2/testpanel/images/add.png")));
		toolBar.add(myAddConnectionButton);

		myDeleteOutboundConnectionButton = new JButton("Delete");
		myDeleteOutboundConnectionButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (myController.getLeftSelectedItem() instanceof OutboundConnection) {
					myController.removeOutboundConnection((OutboundConnection) myController.getLeftSelectedItem());
				}
			}
		});
		myDeleteOutboundConnectionButton.setBorderPainted(false);
		myDeleteOutboundConnectionButton.addMouseListener(new HoverButtonMouseAdapter(myDeleteOutboundConnectionButton));
		myDeleteOutboundConnectionButton.setIcon(new ImageIcon(TestPanelWindow.class.getResource("/ca/uhn/hl7v2/testpanel/images/delete.png")));
		toolBar.add(myDeleteOutboundConnectionButton);

		myStartOneOutboundButton = new JButton("Start");
		myStartOneOutboundButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (myController.getLeftSelectedItem() instanceof OutboundConnection) {
					myController.startOutboundConnection((OutboundConnection) myController.getLeftSelectedItem());
				}
			}
		});
		myStartOneOutboundButton.setBorderPainted(false);
		myStartOneOutboundButton.setIcon(new ImageIcon(TestPanelWindow.class.getResource("/ca/uhn/hl7v2/testpanel/images/start_one.png")));
		myStartOneOutboundButton.addMouseListener(new HoverButtonMouseAdapter(myStartOneOutboundButton));
		toolBar.add(myStartOneOutboundButton);

		myStartAllOutboundButton = new JButton("Start All");
		myStartAllOutboundButton.setBorderPainted(false);
		myStartAllOutboundButton.setIcon(new ImageIcon(TestPanelWindow.class.getResource("/ca/uhn/hl7v2/testpanel/images/start_all.png")));
		myStartAllOutboundButton.addMouseListener(new HoverButtonMouseAdapter(myStartAllOutboundButton));
		myStartAllOutboundButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent theE) {
				myController.startAllOutboundConnections();
			}
		});
		toolBar.add(myStartAllOutboundButton);

		myStopAllOutboundButton = new JButton("Stop All");
		myStopAllOutboundButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				myController.stopAllOutboundConnections();
			}
		});
		myStopAllOutboundButton.setIcon(new ImageIcon(TestPanelWindow.class.getResource("/ca/uhn/hl7v2/testpanel/images/stop_all.png")));
		myStopAllOutboundButton.setBorderPainted(false);
		myStopAllOutboundButton.addMouseListener(new HoverButtonMouseAdapter(myStopAllOutboundButton));
		toolBar.add(myStopAllOutboundButton);
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBorder(null);
		GridBagConstraints gbc_scrollPane = new GridBagConstraints();
		gbc_scrollPane.fill = GridBagConstraints.BOTH;
		gbc_scrollPane.weighty = 1.0;
		gbc_scrollPane.gridx = 0;
		gbc_scrollPane.gridy = 2;
		connectionsPanel.add(scrollPane, gbc_scrollPane);

		myOutboundConnectionsList = new JList();
		myOutboundConnectionsList.setBorder(null);
		myOutboundConnectionsList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if (myOutboundConnectionsList.getSelectedIndex() >= 0) {
					ourLog.debug("New outbound connection selection " + myOutboundConnectionsList.getSelectedIndex());
					myController.setLeftSelectedItem(myOutboundConnectionsList.getSelectedValue());
					myMessagesTabPane.repaint();
					myInboundConnectionsList.clearSelection();
					myInboundConnectionsList.repaint();
				}
				updateLeftToolbarButtons();
			}
		});
		scrollPane.setViewportView(myOutboundConnectionsList);

		GridBagLayout gbl_receivingConnectionsPanel = new GridBagLayout();
		gbl_receivingConnectionsPanel.columnWidths = new int[] { 194, 0 };
		gbl_receivingConnectionsPanel.rowHeights = new int[] { 30, 0, 0 };
		gbl_receivingConnectionsPanel.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
		gbl_receivingConnectionsPanel.rowWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
		receivingConnectionsPanel.setLayout(gbl_receivingConnectionsPanel);

		JToolBar toolBar_1 = new JToolBar();
		toolBar_1.setFloatable(false);
		GridBagConstraints gbc_toolBar_1 = new GridBagConstraints();
		gbc_toolBar_1.anchor = GridBagConstraints.WEST;
		gbc_toolBar_1.insets = new Insets(0, 0, 5, 0);
		gbc_toolBar_1.fill = GridBagConstraints.HORIZONTAL;
		gbc_toolBar_1.gridx = 0;
		gbc_toolBar_1.gridy = 0;
		receivingConnectionsPanel.add(toolBar_1, gbc_toolBar_1);

		myAddInboundConnectionButton = new JButton("New");
		myAddInboundConnectionButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				myController.addInboundConnection();
			}
		});
		myAddInboundConnectionButton.setIcon(new ImageIcon(TestPanelWindow.class.getResource("/ca/uhn/hl7v2/testpanel/images/add.png")));
		myAddInboundConnectionButton.setBorderPainted(false);
		myAddInboundConnectionButton.addMouseListener(new HoverButtonMouseAdapter(myAddInboundConnectionButton));
		toolBar_1.add(myAddInboundConnectionButton);

		myDeleteInboundConnectionButton = new JButton("Delete");
		myDeleteInboundConnectionButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (myController.getLeftSelectedItem() instanceof InboundConnection) {
					myController.removeInboundConnection((InboundConnection) myController.getLeftSelectedItem());
				}
			}
		});
		myDeleteInboundConnectionButton.setBorderPainted(false);
		myDeleteInboundConnectionButton.addMouseListener(new HoverButtonMouseAdapter(myDeleteInboundConnectionButton));
		myDeleteInboundConnectionButton.setIcon(new ImageIcon(TestPanelWindow.class.getResource("/ca/uhn/hl7v2/testpanel/images/delete.png")));
		toolBar_1.add(myDeleteInboundConnectionButton);

		myStartOneInboundButton = new JButton("Start");
		myStartOneInboundButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (myController.getLeftSelectedItem() instanceof InboundConnection) {
					myController.startInboundConnection((InboundConnection) myController.getLeftSelectedItem());
				}
			}
		});
		myStartOneInboundButton.setBorderPainted(false);
		myStartOneInboundButton.setIcon(new ImageIcon(TestPanelWindow.class.getResource("/ca/uhn/hl7v2/testpanel/images/start_one.png")));
		myStartOneInboundButton.addMouseListener(new HoverButtonMouseAdapter(myStartOneInboundButton));
		toolBar_1.add(myStartOneInboundButton);
		
		myStartAllInboundButton = new JButton("");
		myStartAllInboundButton.setBorderPainted(false);
		myStartAllInboundButton.setText("Start All");
		myStartAllInboundButton.setIcon(new ImageIcon(TestPanelWindow.class.getResource("/ca/uhn/hl7v2/testpanel/images/start_all.png")));
		myStartAllInboundButton.addMouseListener(new HoverButtonMouseAdapter(myStartAllInboundButton));
		myStartAllInboundButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent theE) {
				myController.startAllInboundConnections();
			}
		});
		toolBar_1.add(myStartAllInboundButton);

		myStopAllInboundButton = new JButton("Stop All");
		myStopAllInboundButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				myController.stopAllInboundConnections();
			}
		});
		myStopAllInboundButton.setIcon(new ImageIcon(TestPanelWindow.class.getResource("/ca/uhn/hl7v2/testpanel/images/stop_all.png")));
		myStopAllInboundButton.setBorderPainted(false);
		myStopAllInboundButton.addMouseListener(new HoverButtonMouseAdapter(myStopAllInboundButton));
		toolBar_1.add(myStopAllInboundButton);

		JScrollPane scrollPane_1 = new JScrollPane();
		scrollPane_1.setBorder(null);
		GridBagConstraints gbc_scrollPane_1 = new GridBagConstraints();
		gbc_scrollPane_1.fill = GridBagConstraints.BOTH;
		gbc_scrollPane_1.weighty = 1.0;
		gbc_scrollPane_1.gridx = 0;
		gbc_scrollPane_1.gridy = 1;
		receivingConnectionsPanel.add(scrollPane_1, gbc_scrollPane_1);

		myInboundConnectionsList = new JList();
		myInboundConnectionsList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				if (myInboundConnectionsList.getSelectedIndex() >= 0) {
					ourLog.debug("New inbound connection selection " + myInboundConnectionsList.getSelectedIndex());
					myController.setLeftSelectedItem(myInboundConnectionsList.getSelectedValue());
					myMessagesTabPane.repaint();
					myOutboundConnectionsList.clearSelection();
					myOutboundConnectionsList.repaint();
					myInboundConnectionsList.repaint();
				}
				updateLeftToolbarButtons();
			}
		});
		scrollPane_1.setViewportView(myInboundConnectionsList);

		myWorkspacePanel = new JPanel();
		myWorkspacePanel.setBorder(null);
		myWorkspacePanel.setLayout(new BorderLayout(0, 0));
		mainPanel.add(myWorkspacePanel, BorderLayout.CENTER);

		// Status bar footer
		JPanel statusBarPanel = new JPanel(new BorderLayout());
		statusBarPanel.setBorder(javax.swing.BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(200, 200, 200)));

		myStatusBar = new JLabel(" No workspace open");
		myStatusBar.setBorder(javax.swing.BorderFactory.createEmptyBorder(3, 8, 3, 8));
		myStatusBar.setFont(myStatusBar.getFont().deriveFont(11f));
		myStatusBar.setForeground(new Color(100, 100, 100));
		statusBarPanel.add(myStatusBar, BorderLayout.WEST);

		myTerserPathStatusLabel = new JLabel();
		myTerserPathStatusLabel.setBorder(javax.swing.BorderFactory.createEmptyBorder(3, 8, 3, 8));
		myTerserPathStatusLabel.setFont(myTerserPathStatusLabel.getFont().deriveFont(11f));
		myTerserPathStatusLabel.setForeground(new Color(60, 100, 180));
		statusBarPanel.add(myTerserPathStatusLabel, BorderLayout.EAST);

		mainPanel.add(statusBarPanel, BorderLayout.SOUTH);

		// Create container for tabs and editor
		JPanel centerPanel = new JPanel();
		centerPanel.setLayout(new BorderLayout(0, 0));
		myWorkspacePanel.add(centerPanel, BorderLayout.CENTER);

		// Outer editor panel: tabs fixed at top, swappable content below
		myEditorContentPanel = new JPanel();
		myEditorContentPanel.setBorder(null);
		myEditorContentPanel.setLayout(new BorderLayout(0, 0));
		myEditorContentPanel.add(myMessagesTabPane, BorderLayout.NORTH);

		// Inner panel — only this gets swapped by setMainPanel
		myEditorInnerPanel = new JPanel();
		myEditorInnerPanel.setBorder(null);
		myEditorInnerPanel.setLayout(new BorderLayout(0, 0));
		myEditorContentPanel.add(myEditorInnerPanel, BorderLayout.CENTER);

		centerPanel.add(myEditorContentPanel, BorderLayout.CENTER);

		// Add log as a tab in the connections pane
		myLogScrollPane = new LogTable();
		myLogTabIndex = 3;
		myConnectionsTabPane.addTab("Log", myLogScrollPane);

		// Add validation errors tab
		JPanel validationPanel = new JPanel();
		validationPanel.setLayout(new BorderLayout());
		myValidationTable = new javax.swing.JTable();
		myValidationTable.setModel(new javax.swing.table.DefaultTableModel(
			new Object[][] {},
			new String[] { "Field", "Error" }
		));
		javax.swing.JScrollPane validationScrollPane = new javax.swing.JScrollPane(myValidationTable);
		validationPanel.add(validationScrollPane, BorderLayout.CENTER);
		myValidationTabIndex = 4;
		myConnectionsTabPane.addTab("Validation Errors (0)", validationPanel);

		// Add connections tabs to the bottom
		centerPanel.add(myConnectionsTabPane, BorderLayout.SOUTH);

		updateLogScrollPaneVisibility();

		updateLeftToolbarButtons();
	}

	private void updateLogScrollPaneVisibility() {
		if (Prefs.getInstance().getShowLogConsole()) {
			myLogScrollPane.setVisible(true);
		} else {
			myLogScrollPane.setVisible(false);
		}
	}

	public void updateValidationErrors(String statusMessage, Hl7V2MessageTree theTree) {
		javax.swing.table.DefaultTableModel model = (javax.swing.table.DefaultTableModel) myValidationTable.getModel();
		model.setRowCount(0);

		if (statusMessage == null || statusMessage.isEmpty() || theTree == null) {
			myConnectionsTabPane.setTitleAt(myValidationTabIndex, "Validation Errors (0)");
			return;
		}

		// Collect all validation exceptions with their field paths from the tree
		List<java.util.AbstractMap.SimpleEntry<String, HL7Exception>> exceptionsWithPath = new ArrayList<>();
		Hl7V2MessageTree.TreeNodeBase root = theTree.getRootNode();
		if (root != null) {
			root.collectValidationExceptionsWithPath(exceptionsWithPath);
		}

		// Populate the table with validation errors
		for (java.util.AbstractMap.SimpleEntry<String, HL7Exception> entry : exceptionsWithPath) {
			java.util.Vector<Object> row = new java.util.Vector<>();
			row.add(entry.getKey());
			row.add(entry.getValue().getMessage());
			model.addRow(row);
		}

		myConnectionsTabPane.setTitleAt(myValidationTabIndex, "Validation Errors (" + exceptionsWithPath.size() + ")");
	}

	public void displayValidationError(String errorMessage) {
		java.util.Vector<Object> row = new java.util.Vector<>();
		row.add("Parse Error");
		row.add(errorMessage);

		javax.swing.table.DefaultTableModel model = (javax.swing.table.DefaultTableModel) myValidationTable.getModel();
		model.addRow(row);

		myConnectionsTabPane.setTitleAt(myValidationTabIndex, "Validation Errors (" + model.getRowCount() + ")");
		myConnectionsTabPane.setSelectedIndex(myValidationTabIndex);
	}

	private void initializeMenuBar(JMenuBar menuBar) {
		createFileMenu(menuBar);
		createEditMenu(menuBar);
		createToolsMenu(menuBar);
		createHelpMenu(menuBar);
	}

	private void createEditMenu(JMenuBar menuBar) {
		JMenu editMenu = new JMenu("Edit");
		editMenu.setMnemonic('e');
		menuBar.add(editMenu);

		JMenuItem findMenuItem = new JMenuItem("Find...");
		findMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		findMenuItem.addActionListener(e -> {
			if (myMainPanel instanceof Hl7V2MessageEditorPanel) {
				((Hl7V2MessageEditorPanel) myMainPanel).openFindDialog();
			}
		});
		editMenu.add(findMenuItem);

		JMenuItem replaceMenuItem = new JMenuItem("Replace...");
		replaceMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		replaceMenuItem.addActionListener(e -> {
			if (myMainPanel instanceof Hl7V2MessageEditorPanel) {
				((Hl7V2MessageEditorPanel) myMainPanel).openReplaceDialog();
			}
		});
		editMenu.add(replaceMenuItem);

		JMenuItem goToMenuItem = new JMenuItem("Go To Line...");
		goToMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		goToMenuItem.addActionListener(e -> {
			if (myMainPanel instanceof Hl7V2MessageEditorPanel) {
				((Hl7V2MessageEditorPanel) myMainPanel).openGoToDialog();
			}
		});
		editMenu.add(goToMenuItem);
	}

	private void createFileMenu(JMenuBar menuBar) {
		JMenu fileMenu = new JMenu("File");
		fileMenu.setMnemonic('f');
		menuBar.add(fileMenu);

		myNewWorkspaceMenuItem = new JMenuItem("New Workspace...");
		myNewWorkspaceMenuItem.addActionListener(e -> myController.newWorkspace());
		fileMenu.add(myNewWorkspaceMenuItem);

		myOpenWorkspaceMenuItem = new JMenuItem("Open Workspace...");
		myOpenWorkspaceMenuItem.addActionListener(e -> myController.openWorkspace());
		fileMenu.add(myOpenWorkspaceMenuItem);

		myCloseWorkspaceMenuItem = new JMenuItem("Close Workspace");
		myCloseWorkspaceMenuItem.addActionListener(e -> myController.closeWorkspace());
		fileMenu.add(myCloseWorkspaceMenuItem);

		fileMenu.addSeparator();

		JMenuItem newMessageMenuItem = new JMenuItem("New Message...");
		newMessageMenuItem.setIcon(new ImageIcon(TestPanelWindow.class.getResource("/ca/uhn/hl7v2/testpanel/images/message_hl7.png")));
		newMessageMenuItem.addActionListener(e -> myController.addMessage());
		fileMenu.add(newMessageMenuItem);

		mySaveMenuItem = new JMenuItem("Save");
		mySaveMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		mySaveMenuItem.addActionListener(e -> doSaveMessages());
		fileMenu.add(mySaveMenuItem);

		mySaveAsMenuItem = new JMenuItem("Save As...");
		mySaveAsMenuItem.addActionListener(e -> doSaveMessagesAs());
		fileMenu.add(mySaveAsMenuItem);

		openMenuItem = new JMenuItem("Open");
		openMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		openMenuItem.addActionListener(e -> myController.openMessages());
		fileMenu.add(openMenuItem);

		myRevertToSavedMenuItem = new JMenuItem("Revert to Saved");
		myRevertToSavedMenuItem.addActionListener(e -> myController.revertMessage((Hl7V2MessageCollection) myController.getLeftSelectedItem()));
		fileMenu.add(myRevertToSavedMenuItem);

		myRecentFilesMenu = new JMenu("Open Recent");
		fileMenu.add(myRecentFilesMenu);

		fileMenu.addSeparator();

		JMenuItem exitMenuItem = new JMenuItem("Exit");
		exitMenuItem.addActionListener(e -> myController.close());
		fileMenu.add(exitMenuItem);
	}


	private void createToolsMenu(JMenuBar menuBar) {
		toolsMenu = new JMenu("Tools");
		menuBar.add(toolsMenu);

		fileDiffMenuItem = new JMenuItem("HL7 v2 File Diff...");
		fileDiffMenuItem.addActionListener(e -> {
			if (myHl7V2FileDiff == null) {
				myHl7V2FileDiff = new Hl7V2FileDiffController(myController);
			}
			myHl7V2FileDiff.show();
		});
		toolsMenu.add(fileDiffMenuItem);

		fileSortMenuItem = new JMenuItem("HL7 v2 File Sort...");
		fileSortMenuItem.addActionListener(e -> {
			if (myHl7V2FileSort == null) {
				myHl7V2FileSort = new Hl7V2FileSortController(myController);
			}
			myHl7V2FileSort.show();
		});
		toolsMenu.add(fileSortMenuItem);

		toolsMenu.addSeparator();

		JMenu conformanceSubMenu = new JMenu("Conformance");
		profilesAndTablesMenuItem = new JMenuItem("Profiles and Tables...");
		profilesAndTablesMenuItem.addActionListener(e -> myController.showProfilesAndTablesEditor());
		conformanceSubMenu.add(profilesAndTablesMenuItem);
		toolsMenu.add(conformanceSubMenu);

		toolsMenu.addSeparator();

		populateSampleMenuItem = new JMenuItem("Populate TestPanel with Sample Message and Connections...");
		populateSampleMenuItem.addActionListener(e -> myController.populateWithSampleMessageAndConnections());
		toolsMenu.add(populateSampleMenuItem);
	}


	private void createHelpMenu(JMenuBar menuBar) {
		helpMenu = new JMenu("Help");
		helpMenu.setMnemonic('H');
		menuBar.add(helpMenu);

		aboutMenuItem = new JMenuItem("About HAPI TestPanel...");
		aboutMenuItem.setIcon(new ImageIcon(TestPanelWindow.class.getResource("/ca/uhn/hl7v2/testpanel/images/hapi_16.png")));
		aboutMenuItem.addActionListener(e -> showAboutDialog());
		helpMenu.add(aboutMenuItem);

		licensesMenuItem = new JMenuItem("Licenses...");
		licensesMenuItem.addActionListener(e -> new LicensesDialog().setVisible(true));
		helpMenu.add(licensesMenuItem);
	}

	private void initializeLocal() {
		myTabTitleListener = evt -> {
			Hl7V2MessageCollection source = (Hl7V2MessageCollection) evt.getSource();
			List<Hl7V2MessageCollection> messages = myController.getMessagesList().getMessages();
			int i = messages.indexOf(source);
			if (i >= 0) myMessagesTabPane.setTitleAt(i, buildTabTitle(source));
		};
		myController.getMessagesList().addPropertyChangeListener(
			MessagesList.PROP_LIST,
			evt -> updateMessagesList()
		);
		updateMessagesList();

		myOutboundConnectionsListModel = new MyOutboundConnectionsListModel();
		myOutboundConnectionsList.setModel(myOutboundConnectionsListModel);
		myOutboundConnectionsList.setCellRenderer(new MyOutboundConnectionsListCellRenderer());
		updateOutboundConnectionsList();

		myOutboundConnectionsListListener = new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent theEvt) {
				updateOutboundConnectionsList();
			}
		};
		myController.getOutboundConnectionList().addPropertyChangeListener(OutboundConnectionList.PROP_LIST, myOutboundConnectionsListListener);

		myInboundConnectionsListModel = new MyInboundConnectionsListModel();
		myInboundConnectionsList.setModel(myInboundConnectionsListModel);
		myInboundConnectionsList.setCellRenderer(new MyInboundConnectionsListCellRenderer());
		updateInboundConnectionsList();

		myInboundConnectionsListListener = new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent theEvt) {
				updateInboundConnectionsList();
			}
		};
		myController.getInboundConnectionList().addPropertyChangeListener(InboundConnectionList.PROP_LIST, myInboundConnectionsListListener);

		updateLeftToolbarInboundStatusButtons();
		updateLeftToolbarOutboundStatusButtons();

		myCloseWorkspaceMenuItem.setEnabled(myController.getWorkspaceController().hasWorkspace());
		if (myController.getWorkspaceController().hasWorkspace()) {
			myStatusBar.setText(" Workspace: " + myController.getWorkspaceController().getWorkspaceFile().getAbsolutePath());
		}
	}

	public void rebindConnectionLists() {
		// Detach old listeners (they may be attached to now-replaced list instances)
		myController.getOutboundConnectionList().removePropertyChangeListener(OutboundConnectionList.PROP_LIST, myOutboundConnectionsListListener);
		myController.getInboundConnectionList().removePropertyChangeListener(InboundConnectionList.PROP_LIST, myInboundConnectionsListListener);

		// Clear the UI list models
		myOutboundConnectionsListModel.clear();
		myInboundConnectionsListModel.clear();

		// Reattach to the current (possibly new) list instances
		myController.getOutboundConnectionList().addPropertyChangeListener(OutboundConnectionList.PROP_LIST, myOutboundConnectionsListListener);
		myController.getInboundConnectionList().addPropertyChangeListener(InboundConnectionList.PROP_LIST, myInboundConnectionsListListener);

		updateOutboundConnectionsList();
		updateInboundConnectionsList();
		updateLeftToolbarOutboundStatusButtons();
		updateLeftToolbarInboundStatusButtons();
	}

	public void updateOutboundConnectionsList() {

		int index = 0;
		myOutboundConnectionsList.clearSelection();
		for (OutboundConnection next : myController.getOutboundConnectionList().getConnections()) {

			if (myOutboundConnectionsListModel.size() <= index) {

				myOutboundConnectionsListModel.addElement(next);
				next.addPropertyChangeListener(OutboundConnection.NAME_PROPERTY, new MyOutboundConnectionDescriptionListener(next));
				next.addPropertyChangeListener(OutboundConnection.STATUS_PROPERTY, new MyOutboundConnectionDescriptionListener(next));
				next.addPropertyChangeListener(OutboundConnection.PERSISTENT_PROPERTY, new MyOutboundConnectionDescriptionListener(next));

			} else if (myOutboundConnectionsListModel.getElementAt(index) != next) {

				myOutboundConnectionsListModel.add(index, next);
				next.addPropertyChangeListener(OutboundConnection.NAME_PROPERTY, new MyOutboundConnectionDescriptionListener(next));
				next.addPropertyChangeListener(OutboundConnection.STATUS_PROPERTY, new MyOutboundConnectionDescriptionListener(next));
				next.addPropertyChangeListener(OutboundConnection.PERSISTENT_PROPERTY, new MyOutboundConnectionDescriptionListener(next));

			}

			if (next == myController.getLeftSelectedItem()) {
				myOutboundConnectionsList.setSelectedIndex(index);
			}

			index++;
		}

		while (myOutboundConnectionsListModel.size() > index) {
			OutboundConnection obj = (OutboundConnection) myOutboundConnectionsListModel.remove(index);
			obj.destroy();
			obj.removePropertyChangeListener(Hl7V2MessageCollection.PROP_DESCRIPTION, myMessageDescriptionListener);
		}

	}

	/**
	 * Save the currently selected message
	 */
	private void doSaveMessages() {
		ourLog.info("Selected index: {}", myMessagesTabPane.getSelectedIndex());
		myController.saveMessages((Hl7V2MessageCollection) myController.getLeftSelectedItem());
	}

	private void doSaveMessagesAs() {
		ourLog.info("Selected index: {}", myMessagesTabPane.getSelectedIndex());
		myController.saveMessagesAs((Hl7V2MessageCollection) myController.getLeftSelectedItem());
	}
	
	public void updateInboundConnectionsList() {

		int index = 0;
		myInboundConnectionsList.clearSelection();
		for (InboundConnection next : myController.getInboundConnectionList().getConnections()) {

			if (myInboundConnectionsListModel.size() <= index) {

				myInboundConnectionsListModel.addElement(next);
				next.addPropertyChangeListener(InboundConnection.NAME_PROPERTY, new MyInboundConnectionDescriptionListener(next));
				next.addPropertyChangeListener(InboundConnection.STATUS_PROPERTY, new MyInboundConnectionDescriptionListener(next));
				next.addPropertyChangeListener(InboundConnection.NEW_MESSAGES_PROPERTY, new MyInboundConnectionDescriptionListener(next));

			} else if (myInboundConnectionsListModel.getElementAt(index) != next) {

				myInboundConnectionsListModel.add(index, next);
				next.addPropertyChangeListener(InboundConnection.NAME_PROPERTY, new MyInboundConnectionDescriptionListener(next));
				next.addPropertyChangeListener(InboundConnection.STATUS_PROPERTY, new MyInboundConnectionDescriptionListener(next));
				next.addPropertyChangeListener(InboundConnection.NEW_MESSAGES_PROPERTY, new MyInboundConnectionDescriptionListener(next));

			}

			if (next == myController.getLeftSelectedItem()) {
				myInboundConnectionsList.setSelectedIndex(index);
			}

			index++;
		}

		while (myInboundConnectionsListModel.size() > index) {
			InboundConnection obj = (InboundConnection) myInboundConnectionsListModel.remove(index);
			obj.destroy();
			obj.removePropertyChangeListener(Hl7V2MessageCollection.PROP_DESCRIPTION, myMessageDescriptionListener);
		}

	}

	private class MyInboundConnectionDescriptionListener implements PropertyChangeListener {

		private AbstractConnection myConnection;

		public MyInboundConnectionDescriptionListener(AbstractConnection theConnection) {
			myConnection = theConnection;
		}

		public void propertyChange(PropertyChangeEvent theEvt) {
			String propertyName = theEvt.getPropertyName();

			int rowIndex = myInboundConnectionsListModel.indexOf(myConnection);
			myInboundConnectionsListModel.fireChangeAtRow(rowIndex);
			
			if (propertyName == InboundConnection.STATUS_PROPERTY) {
				updateLeftToolbarInboundStatusButtons();
			}
		}

		
	}


	private class MyOutboundConnectionDescriptionListener implements PropertyChangeListener {

		private AbstractConnection myConnection;

		public MyOutboundConnectionDescriptionListener(AbstractConnection theConnection) {
			myConnection = theConnection;
		}

		public void propertyChange(PropertyChangeEvent theEvt) {
			int rowIndex = myOutboundConnectionsListModel.indexOf(myConnection);
			myOutboundConnectionsListModel.fireChangeAtRow(rowIndex);
			
			if (theEvt.getPropertyName() == InboundConnection.STATUS_PROPERTY) {
				updateLeftToolbarOutboundStatusButtons();
			}
		}

	}

	public void updateMessagesList() {
		myUpdatingTabs = true;
		try {
			List<Hl7V2MessageCollection> messages = myController.getMessagesList().getMessages();

			// Remove listeners from tabs being removed
			for (int i = myMessagesTabPane.getTabCount() - 1; i >= messages.size(); i--) {
				Hl7V2MessageCollection old = (Hl7V2MessageCollection) myMessagesTabPane.getClientProperty("msg_" + i);
				if (old != null) {
					old.removePropertyChangeListener(Hl7V2MessageCollection.PROP_DESCRIPTION, myTabTitleListener);
					old.removePropertyChangeListener(Hl7V2MessageCollection.SAVED_PROPERTY, myTabTitleListener);
				}
				myMessagesTabPane.removeTabAt(i);
			}

			for (int i = 0; i < messages.size(); i++) {
				Hl7V2MessageCollection msg = messages.get(i);
				String title = buildTabTitle(msg);
				if (i < myMessagesTabPane.getTabCount()) {
					myMessagesTabPane.setTitleAt(i, title);
					myMessagesTabPane.putClientProperty("msg_" + i, msg);
				} else {
					myMessagesTabPane.addTab(title, new JPanel());
					myMessagesTabPane.putClientProperty("msg_" + i, msg);
					msg.addPropertyChangeListener(Hl7V2MessageCollection.PROP_DESCRIPTION, myTabTitleListener);
					msg.addPropertyChangeListener(Hl7V2MessageCollection.SAVED_PROPERTY, myTabTitleListener);
				}

				if (msg == myController.getLeftSelectedItem()) {
					myMessagesTabPane.setSelectedIndex(i);
				}
			}
			setupTabCloseButtons();
		} finally {
			myUpdatingTabs = false;
		}
	}

	private String buildTabTitle(Hl7V2MessageCollection collection) {
		if (collection.isSaved()) {
			return collection.getMessageDescription();
		}
		return "<html><font color='red'>" + collection.getMessageDescription() + "*</font></html>";
	}

	private void setupTabCloseButtons() {
		for (int i = 0; i < myMessagesTabPane.getTabCount(); i++) {
			JPanel tabComponent = new JPanel(new BorderLayout(5, 0));
			tabComponent.setOpaque(false);

			String title = myMessagesTabPane.getTitleAt(i);
			// Strip HTML tags for display in label
			String plainText = title.replaceAll("<[^>]*>", "");
			JLabel label = new JLabel(plainText);
			tabComponent.add(label, BorderLayout.CENTER);

			JButton closeButton = new JButton("×");
			closeButton.setMargin(new Insets(0, 3, 0, 3));
			closeButton.setFont(closeButton.getFont().deriveFont(12f));
			closeButton.setFocusPainted(false);
			closeButton.setContentAreaFilled(false);
			closeButton.setBorderPainted(false);

			final int tabIndex = i;
			closeButton.addActionListener(e -> {
				Hl7V2MessageCollection msg = (Hl7V2MessageCollection) myMessagesTabPane.getClientProperty("msg_" + tabIndex);
				if (msg != null) {
					try {
						myController.closeMessage(msg);
					} catch (Exception ex) {
						ourLog.warn("Error closing message", ex);
						// Try to close anyway by removing from the list
						myController.getMessagesList().removeMessage(msg);
					}
				}
			});

			tabComponent.add(closeButton, BorderLayout.EAST);
			myMessagesTabPane.setTabComponentAt(i, tabComponent);
		}
	}

	private static final Logger ourLog = LoggerFactory.getLogger(TestPanelWindow.class);
	private static final Color BG_SELECTED = new Color(0.8f, 0.8f, 1.0f);
	private static final Color BG_NOT_SELECTED = Color.white;
	private JPanel myWorkspacePanel;
	private JPanel myEditorContentPanel;
	private JPanel myEditorInnerPanel;
	private JLabel myStatusBar;
	private JLabel myTerserPathStatusLabel;
	private JMenuItem myNewWorkspaceMenuItem;
	private JMenuItem myOpenWorkspaceMenuItem;
	private JMenuItem myCloseWorkspaceMenuItem;
	private JButton myAddConnectionButton;
	private JList myOutboundConnectionsList;
	private JList myInboundConnectionsList;
	private JScrollPane myLogScrollPane;
	private int myLogTabIndex;
	private javax.swing.JTable myValidationTable;
	private int myValidationTabIndex;
	private BaseMainPanel myMainPanel;
	private JButton myDeleteInboundConnectionButton;
	private JMenuItem mySaveMenuItem;
	private JMenuItem mySaveAsMenuItem;
	private JButton myStartAllInboundButton;
	private JButton myStartOneInboundButton;
	private JButton myStopAllInboundButton;
	private JMenu helpMenu;
	private JMenuItem aboutMenuItem;
	private JMenuItem populateSampleMenuItem;
	private JMenuItem profilesAndTablesMenuItem;
	private JMenu myRecentFilesMenu;
	private JMenuItem openMenuItem;
	private JMenuItem licensesMenuItem;
	private JMenu toolsMenu;
	private JMenuItem fileDiffMenuItem;
	private JMenuItem myRevertToSavedMenuItem;
	private JMenuItem fileSortMenuItem;


	private class MyOutboundConnectionsListCellRenderer extends DefaultListCellRenderer {

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * javax.swing.DefaultListCellRenderer#getListCellRendererComponent(
		 * javax.swing.JList, java.lang.Object, int, boolean, boolean)
		 */
		@Override
		public Component getListCellRendererComponent(JList theList, Object theValue, int theIndex, boolean theIsSelected, boolean theCellHasFocus) {
			OutboundConnection obj = (OutboundConnection) theValue;
			switch (obj.getStatus()) {
			case STARTED:
				setIcon(ImageFactory.getInterfaceOn());
				break;
			case STOPPED:
			case FAILED:
				setIcon(ImageFactory.getInterfaceOff());
				break;
			case TRYING_TO_START:
			default:
				setIcon(ImageFactory.getInterfaceStarting());
				break;
			}

			StringBuilder b = new StringBuilder();
			b.append(obj.getName());
			boolean html = false;
			
			if (!obj.isPersistent()) {
				b.insert(0, "<font color=\\\"red\\\" size=\\\"2\\\">temp</font> ");
				html = true;
			}

			if (obj.getNewMessages() > 0) {
				b.append(" - <font color=\\\"red\\\">").append(obj.getNewMessages()).append(" new</font> ");
				html = true;
			}
			
			if (obj.getStatus() == StatusEnum.FAILED) {
				b.append(" <font color=\"red\" size=\"2\">(failed)</font> ");
				html = true;
			}
			

			if (html) {
				setText("<html><nobr>" + b.toString()+"</nobr></html>");
			}else {
				setText(b.toString());
			}
			
			if (theValue == myController.getLeftSelectedItem()) {
				setBackground(BG_SELECTED);
			} else {
				setBackground(BG_NOT_SELECTED);
			}

			return this;
		}

	}

	private class MyInboundConnectionsListCellRenderer extends DefaultListCellRenderer {

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * javax.swing.DefaultListCellRenderer#getListCellRendererComponent(
		 * javax.swing.JList, java.lang.Object, int, boolean, boolean)
		 */
		@Override
		public Component getListCellRendererComponent(JList theList, Object theValue, int theIndex, boolean theIsSelected, boolean theCellHasFocus) {
			InboundConnection obj = (InboundConnection) theValue;
			switch (obj.getStatus()) {
			case STARTED:
				setIcon(ImageFactory.getInterfaceOn());
				break;
			case STOPPED:
			case FAILED:
				setIcon(ImageFactory.getInterfaceOff());
				break;
			case TRYING_TO_START:
			default:
				setIcon(ImageFactory.getInterfaceStarting());
				break;
			}

			StringBuilder b = new StringBuilder();
			b.append(obj.getName());
			boolean html = false;
			
			if (!obj.isPersistent()) {
				b.insert(0, "<font color=\"red\" size=\"2\">temp</font> ");
				html = true;
			}

			if (obj.getNewMessages() > 0) {
				b.append(" <font color=\"red\" size=\"2\">(").append(obj.getNewMessages()).append(" new)</font> ");
				html = true;
			}
			
			if (obj.getStatus() == StatusEnum.FAILED) {
				b.append(" <font color=\"red\" size=\"2\">(failed)</font> ");
				html = true;
			}
			
			if (html) {
				setText("<html><nobr>" + b.toString()+"</nobr></html>");
			}else {
				setText(b.toString());
			}
			
			if (theValue == myController.getLeftSelectedItem()) {
				setBackground(BG_SELECTED);
			} else {
				setBackground(BG_NOT_SELECTED);
			}

			return this;
		}

	}

	public void setMainPanel(BaseMainPanel theOutboundPanel) {
		Validate.notNull(theOutboundPanel);

		if (myMainPanel != null) {
			if (myMainPanel instanceof IDestroyable) {
				((IDestroyable) myMainPanel).destroy();
			}
			myMainPanel.removePropertyChangeListener(BaseMainPanel.PROP_WINDOWTITLE, myPanelTitleListener);
		}

		myMainPanel = theOutboundPanel;
		myMainPanel.addPropertyChangeListener(BaseMainPanel.PROP_WINDOWTITLE, myPanelTitleListener);

		myEditorInnerPanel.removeAll();
		myEditorInnerPanel.add(theOutboundPanel, BorderLayout.CENTER);
		myEditorInnerPanel.validate();

		// Update the Sending activity table and terser path label for editor panels
		if (theOutboundPanel instanceof Hl7V2MessageEditorPanel) {
			Hl7V2MessageEditorPanel editorPanel = (Hl7V2MessageEditorPanel) theOutboundPanel;
			editorPanel.setTestPanelWindow(this);
			ActivityTable sendingTable = editorPanel.getSendingActivityTable();
			int sendingTabIndex = 2;
			if (sendingTabIndex < myConnectionsTabPane.getTabCount()) {
				myConnectionsTabPane.setComponentAt(sendingTabIndex, sendingTable);
			}
			// Mirror the terser path label into the status bar
			JLabel terserLabel = editorPanel.getTerserPathLabel();
			terserLabel.addPropertyChangeListener("text", evt ->
				myTerserPathStatusLabel.setText((String) evt.getNewValue()));
			myTerserPathStatusLabel.setText(terserLabel.getText());
		} else {
			myTerserPathStatusLabel.setText(null);
		}

		myMessagesTabPane.repaint();
		myInboundConnectionsList.repaint();
		myOutboundConnectionsList.repaint();

		updateLeftToolbarButtons();
		updateWindowTitle();
	}


	private class MyOutboundConnectionsListModel extends DefaultListModel {

		public void fireChangeAtRow(int theI) {
			fireContentsChanged(this, theI, theI);
		}

	}

	private class MyInboundConnectionsListModel extends DefaultListModel {

		public void fireChangeAtRow(int theI) {
			fireContentsChanged(this, theI, theI);
		}

	}

	public JFrame getFrame() {
		return myframe;
	}

	/**
	 * {@inheritDoc}
	 */
	public void destroy() {
		
		// For some reason, on OSX once a window has been maximized it will keep reporting
		// that it is even once it no longer is
		int extState = myframe.getExtendedState();
		if (extState == JFrame.MAXIMIZED_BOTH && !System.getProperty("os.name").contains("Mac")) { 
			Prefs.getInstance().setWindowMaximized(true);
		} else {			
			Point location = myframe.getLocation();
			Dimension size = myframe.getSize();
			ourLog.info("Saving window location of {} and size of {}", location, size);
			Prefs.getInstance().setWindowPosition(location);
			Prefs.getInstance().setWindowDimension(size);
			Prefs.getInstance().setWindowMaximized(false);
		}
	}

	public void showAboutDialog() {
		if (myAboutDialog == null) {
			myAboutDialog = new AboutDialog();
		}
		myAboutDialog.setVisible(true);
	}

	public void onWorkspaceChanged() {
		ca.uhn.hl7v2.testpanel.controller.WorkspaceController wsc = myController.getWorkspaceController();
		boolean hasWorkspace = wsc.hasWorkspace();
		myCloseWorkspaceMenuItem.setEnabled(hasWorkspace);

		if (hasWorkspace) {
			File root = wsc.getRootFolder();
			myStatusBar.setText(" Workspace: " + wsc.getWorkspaceFile().getAbsolutePath());
			myframe.setTitle("HAPI TestPanel - " + root.getName());
		} else {
			myStatusBar.setText(" No workspace open");
			updateWindowTitle();
		}
	}

	public void setRecentMessageFiles(List<Hl7V2MessageCollection> theList) {
		myRecentFilesMenu.removeAll();
		for (final Hl7V2MessageCollection nextFile : theList) {
			JMenuItem nextItem = new JMenuItem(nextFile.getSaveFileName());
			myRecentFilesMenu.add(nextItem);
			nextItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent theE) {
					myController.openOrSwitchToMessage(nextFile);
				}
			});
		}
	}

}
