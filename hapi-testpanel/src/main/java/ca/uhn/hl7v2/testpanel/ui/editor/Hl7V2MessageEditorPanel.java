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
package ca.uhn.hl7v2.testpanel.ui.editor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.SystemColor;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollBar;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Highlighter;

import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.rsta.ui.search.FindDialog;
import org.fife.rsta.ui.search.ReplaceDialog;
import org.fife.rsta.ui.GoToDialog;
import org.fife.rsta.ui.search.SearchEvent;
import org.fife.rsta.ui.search.SearchListener;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;
import org.fife.ui.rtextarea.SearchResult;

import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.testpanel.model.msg.AbstractMessage;
import ca.uhn.hl7v2.testpanel.ui.Er7TokenMaker;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.hl7v2.conf.ProfileException;
import ca.uhn.hl7v2.testpanel.App;
import ca.uhn.hl7v2.testpanel.controller.Controller;
import ca.uhn.hl7v2.testpanel.controller.Prefs;
import ca.uhn.hl7v2.testpanel.model.conf.ProfileFileList;
import ca.uhn.hl7v2.testpanel.model.conf.ProfileGroup;
import ca.uhn.hl7v2.testpanel.model.conn.OutboundConnection;
import ca.uhn.hl7v2.testpanel.model.conn.OutboundConnectionList;
import ca.uhn.hl7v2.testpanel.model.msg.Hl7V2MessageCollection;
import ca.uhn.hl7v2.testpanel.ui.ActivityTable;
import ca.uhn.hl7v2.testpanel.ui.BaseMainPanel;
import ca.uhn.hl7v2.testpanel.ui.HoverButtonMouseAdapter;
import ca.uhn.hl7v2.testpanel.ui.IDestroyable;
import ca.uhn.hl7v2.testpanel.ui.ImageFactory;
import ca.uhn.hl7v2.testpanel.ui.ShowEnum;
import ca.uhn.hl7v2.testpanel.ui.TestPanelWindow;
import ca.uhn.hl7v2.testpanel.ui.v2tree.Hl7V2MessageTree;
import ca.uhn.hl7v2.testpanel.ui.v2tree.Hl7V2MessageTree.IWorkingListener;
import ca.uhn.hl7v2.testpanel.util.IOkCancelCallback;
import ca.uhn.hl7v2.testpanel.util.Range;
import ca.uhn.hl7v2.testpanel.xsd.Hl7V2EncodingTypeEnum;
import ca.uhn.hl7v2.validation.impl.DefaultValidation;

public class Hl7V2MessageEditorPanel extends BaseMainPanel implements IDestroyable, SearchListener {
	private static final String CREATE_NEW_CONNECTION = "Create New Connection...";
	private static final String NO_CONNECTIONS = "No Connections";
	private static final Logger ourLog = LoggerFactory.getLogger(Hl7V2MessageEditorPanel.class);

	private static final String SYNTAX_STYLE_ER7 = "text/er7";

	static {
		AbstractTokenMakerFactory factory = (AbstractTokenMakerFactory) TokenMakerFactory.getDefaultInstance();
		factory.putMapping(SYNTAX_STYLE_ER7, Er7TokenMaker.class.getName());
	}

	private boolean myDontRespondToSourceMessageChanges;
	private JPanel messageEditorContainerPanel;
	private JComboBox myShowCombo;
	private Controller myController;
	private boolean myDisableCaretUpdateHandling;
	private DocumentListener myDocumentListener;
	private JToggleButton myFollowToggle;
	private JToggleButton myWrapToggle;
	private FindDialog myFindDialog;
	private ReplaceDialog myReplaceDialog;
	private JLabel mylabel_1;
	private JLabel mylabel_2;
	private JLabel mylabel_3;
	private Hl7V2MessageCollection myMessage;
	private RSyntaxTextArea myMessageEditor;
	private PropertyChangeListener myMessageListeneer;
	private RTextScrollPane myMessageScrollPane;
	private PropertyChangeListener myOutboundConnectionsListener;
	private JComboBox myOutboundInterfaceCombo;
	private DefaultComboBoxModel myOutboundInterfaceComboModel;
	private JComboBox myProfileCombobox;
	private ProfileComboModel myProfileComboboxModel;
	private PropertyChangeListener myRangeListener;
	private JRadioButton myRdbtnEr7;
	private JRadioButton myRdbtnXml;
	private PropertyChangeListener mySelectedPathListener;
	private JButton mySendButton;
	private JSplitPane mysplitPane;
	private JLabel myTerserPathTextField;
	private JToolBar mytoolBar;
	private Hl7V2MessageTree myTreePanel;
	private JPanel treeContainerPanel;
	// private JPanel mySendingPanel;
	private ActivityTable mySendingActivityTable;
	private PropertyChangeListener myWindowTitleListener;
	private Component myhorizontalStrut;
	private Component myhorizontalStrut_1;
	private boolean myOutboundInterfaceComboModelIsUpdating;
	private ArrayList<OutboundConnection> myOutboundInterfaceComboModelShadow;
	private final JPopupMenu myTerserPathPopupMenu = new JPopupMenu();
	private boolean myHandlingProfileComboboxChange;
	private JLabel mylabel_4;
	private JToolBar mytoolBar_1;
	private TablesComboModel myTablesComboModel;
	private PropertyChangeListener myProfilesListener;
	private PropertyChangeListener myProfilesNamesListener;
	private JButton mySpinner;
	private Component myhorizontalGlue;
	private ImageIcon mySpinnerIconOn;
	private ImageIcon mySpinnerIconOff;
	private JButton collapseAllButton;
	private JButton expandAllButton;
	private Component myhorizontalStrut_2;
	private javax.swing.JSpinner mySendTimesSpinner;
	private JLabel mySendTimesLabel;
	private TestPanelWindow myTestPanelWindow;
	private Hl7V2MessageTree myTreeForValidation;

	/**
	 * Create the panel.
	 */
	public Hl7V2MessageEditorPanel(final Controller theController) {
		setBorder(null);
		myController = theController;

		ButtonGroup encGrp = new ButtonGroup();
		setLayout(new BorderLayout(0, 0));

		mysplitPane = new JSplitPane();
		mysplitPane.setResizeWeight(0.75);
		mysplitPane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
		add(mysplitPane);

		mysplitPane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent theEvt) {
				double ratio = (double) mysplitPane.getDividerLocation() / mysplitPane.getHeight();
				ourLog.debug("Resizing split to ratio: {}", ratio);
				Prefs.getInstance().setHl7EditorSplit(ratio);
			}
		});

		EventQueue.invokeLater(new Runnable() {
			public void run() {
				mysplitPane.setDividerLocation(0.25);
			}
		});

		messageEditorContainerPanel = new JPanel();
		messageEditorContainerPanel.setBorder(null);
		mysplitPane.setRightComponent(messageEditorContainerPanel);
		messageEditorContainerPanel.setLayout(new BorderLayout(0, 0));

		myMessageEditor = new RSyntaxTextArea();
		myMessageEditor.setSyntaxEditingStyle(SYNTAX_STYLE_ER7);
		myMessageEditor.setCodeFoldingEnabled(false);
		myMessageEditor.setLineWrap(false);
		myMessageEditor.setHighlightCurrentLine(false);
		myMessageEditor.setSelectionColor(new Color(100, 150, 255, 150));
		myMessageEditor.setSelectedTextColor(null);
		myMessageEditor.setFont(resolveEditorFont(14));
		applyEditorTheme();

		myMessageEditor.addCaretListener(new javax.swing.event.CaretListener() {
			public void caretUpdate(javax.swing.event.CaretEvent e) {
				if (myFollowToggle.isSelected()) {
					highlightFieldAtCursorPosition(e.getDot());
				}
			}
		});

		myMessageScrollPane = new RTextScrollPane(myMessageEditor, true);
		messageEditorContainerPanel.add(myMessageScrollPane);

		JToolBar toolBar = new JToolBar();
		messageEditorContainerPanel.add(toolBar, BorderLayout.NORTH);
		toolBar.setFloatable(false);
		toolBar.setRollover(true);

		myFollowToggle = new JToggleButton("Follow");
		myFollowToggle.setToolTipText("Keep the message tree (above) and the message editor (below) in sync");
		myFollowToggle.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				theController.setMessageEditorInFollowMode(myFollowToggle.isSelected());
			}
		});

		myFollowToggle.setIcon(ImageFactory.getFollow());
		myFollowToggle.setSelected(theController.isMessageEditorInFollowMode());
		toolBar.add(myFollowToggle);

		myWrapToggle = new JToggleButton("Wrap");
		myWrapToggle.setToolTipText("Toggle line wrapping in the message editor");
		myWrapToggle.setIcon(ImageFactory.getWrap());
		myWrapToggle.setSelected(false);
		myWrapToggle.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				myMessageEditor.setLineWrap(myWrapToggle.isSelected());
			}
		});
		toolBar.add(myWrapToggle);

		myhorizontalStrut = Box.createHorizontalStrut(20);
		toolBar.add(myhorizontalStrut);

		mylabel_4 = new JLabel("Encoding");
		toolBar.add(mylabel_4);

		myRdbtnEr7 = new JRadioButton("ER7");
		myRdbtnEr7.setMargin(new Insets(1, 2, 0, 1));
		toolBar.add(myRdbtnEr7);

		myRdbtnXml = new JRadioButton("XML");
		myRdbtnXml.setMargin(new Insets(1, 5, 0, 1));
		toolBar.add(myRdbtnXml);
		encGrp.add(myRdbtnEr7);
		encGrp.add(myRdbtnXml);

		Component horizontalGlue = Box.createHorizontalGlue();
		toolBar.add(horizontalGlue);

		mySpinner = new JButton("");
		mySpinner.setForeground(Color.DARK_GRAY);
		mySpinner.setHorizontalAlignment(SwingConstants.RIGHT);
		mySpinner.setMaximumSize(new Dimension(200, 15));
		mySpinner.setPreferredSize(new Dimension(200, 15));
		mySpinner.setMinimumSize(new Dimension(200, 15));
		mySpinner.setBorderPainted(false);
		mySpinner.setSize(new Dimension(16, 16));
		toolBar.add(mySpinner);

		treeContainerPanel = new JPanel();
		mysplitPane.setLeftComponent(treeContainerPanel);
		treeContainerPanel.setLayout(new BorderLayout(0, 0));

		mySpinnerIconOn = new ImageIcon(Hl7V2MessageEditorPanel.class.getResource("/ca/uhn/hl7v2/testpanel/images/spinner.gif"));
		mySpinnerIconOff = new ImageIcon();

		myTreePanel = new Hl7V2MessageTree(theController);
		myTreeForValidation = myTreePanel;
		myTreePanel.setWorkingListener(new IWorkingListener() {

			public void startedWorking() {
				mySpinner.setText("");
				mySpinner.setIcon(mySpinnerIconOn);
				mySpinnerIconOn.setImageObserver(mySpinner);
			}


			public void finishedWorking(String theStatus) {
				mySpinner.setText(theStatus);

				mySpinner.setIcon(mySpinnerIconOff);
				mySpinnerIconOn.setImageObserver(null);

				if (myTestPanelWindow != null) {
					myTestPanelWindow.updateValidationErrors(theStatus, myTreeForValidation);
				}
			}
		});

		JPanel treeContainer = new JPanel();
		treeContainer.setLayout(new BorderLayout(0, 0));
		treeContainerPanel.add(treeContainer);
		treeContainer.add(myTreePanel);

		mytoolBar_1 = new JToolBar();
		mytoolBar_1.setFloatable(false);
		mytoolBar_1.setBorder(javax.swing.BorderFactory.createEmptyBorder(3, 4, 3, 4));
		treeContainer.add(mytoolBar_1, BorderLayout.NORTH);

		mylabel_3 = new JLabel("Show");
		mylabel_3.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 2, 0, 6));
		mytoolBar_1.add(mylabel_3);

		myShowCombo = new JComboBox();
		mytoolBar_1.add(myShowCombo);
		myShowCombo.setPreferredSize(new Dimension(130, 27));
		myShowCombo.setMinimumSize(new Dimension(130, 27));
		myShowCombo.setMaximumSize(new Dimension(130, 32767));

		mytoolBar_1.add(Box.createHorizontalStrut(6));

		collapseAllButton = new JButton();
		collapseAllButton.setBorderPainted(false);
		collapseAllButton.addMouseListener(new HoverButtonMouseAdapter(collapseAllButton));
		collapseAllButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				myTreePanel.collapseAll();
			}
		});
		collapseAllButton.setToolTipText("Collapse All");
		collapseAllButton.setIcon(ImageFactory.getCollapseAll());
		mytoolBar_1.add(collapseAllButton);

		expandAllButton = new JButton();
		expandAllButton.setBorderPainted(false);
		expandAllButton.addMouseListener(new HoverButtonMouseAdapter(expandAllButton));
		expandAllButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				myTreePanel.expandAll();
			}
		});
		expandAllButton.setToolTipText("Expand All");
		expandAllButton.setIcon(ImageFactory.getExpandAll());
		mytoolBar_1.add(expandAllButton);

		myhorizontalGlue = Box.createHorizontalGlue();
		mytoolBar_1.add(myhorizontalGlue);
		myProfileComboboxModel = new ProfileComboModel();

		myTablesComboModel = new TablesComboModel(myController);

		mytoolBar = new JToolBar();
		mytoolBar.setFloatable(false);
		mytoolBar.setRollover(true);
		add(mytoolBar, BorderLayout.NORTH);

		JLabel sendLabel = new JLabel("Send");
		sendLabel.setBorder(new javax.swing.border.EmptyBorder(0, 5, 0, 10));
		mytoolBar.add(sendLabel);

		myOutboundInterfaceComboModel = new DefaultComboBoxModel();
		myOutboundInterfaceCombo = new JComboBox<>(myOutboundInterfaceComboModel);
		myOutboundInterfaceCombo.setMaximumSize(new Dimension(200, 32767));
		mytoolBar.add(myOutboundInterfaceCombo);

		mySendButton = new JButton("Send");
		mySendButton.addMouseListener(new HoverButtonMouseAdapter(mySendButton));
		mySendButton.setBorderPainted(false);
		mySendButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int selectedIndex = myOutboundInterfaceCombo.getSelectedIndex();
				OutboundConnection connection = myController.getOutboundConnectionList().getConnections().get(selectedIndex);
				activateSendingActivityTabForConnection(connection);
				myController.sendMessages(connection, myMessage, mySendingActivityTable.provideTransmissionCallback());
			}
		});
		// I AM GOING TO KILL MYSELF
		myhorizontalStrut_2 = Box.createHorizontalStrut(10);
		myhorizontalStrut_2.setMaximumSize(new Dimension(10, 32767));
		mytoolBar.add(myhorizontalStrut_2);

		mySendTimesLabel = new JLabel("Times:");
		mySendTimesLabel.setBorder(new javax.swing.border.EmptyBorder(0, 5, 0, 5));
		mytoolBar.add(mySendTimesLabel);

		mySendTimesSpinner = new javax.swing.JSpinner(new javax.swing.SpinnerNumberModel(1, 1, 1000, 1));
		mySendTimesSpinner.setMaximumSize(new Dimension(60, 32767));
		mySendTimesSpinner.setPreferredSize(new Dimension(60, 27));
		mytoolBar.add(mySendTimesSpinner);
		mySendTimesSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
			public void stateChanged(javax.swing.event.ChangeEvent e) {
				if (myMessage != null) {
					int value = (Integer) mySendTimesSpinner.getValue();
					myMessage.setSendNumberOfTimes(value);
				}
			}
		});

		mySendButton.setIcon(ImageFactory.getButtonExecute());
		mytoolBar.add(mySendButton);

		myhorizontalStrut_1 = Box.createHorizontalStrut(15);
		myhorizontalStrut_1.setMaximumSize(new Dimension(15, 32767));
		mytoolBar.add(myhorizontalStrut_1);

		mylabel_2 = new JLabel("Validate");
		mylabel_2.setBorder(new javax.swing.border.EmptyBorder(0, 5, 0, 10));
		mytoolBar.add(mylabel_2);

		myProfileCombobox = new JComboBox();
		mytoolBar.add(myProfileCombobox);
		myProfileCombobox.setPreferredSize(new Dimension(200, 27));
		myProfileCombobox.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				if (myHandlingProfileComboboxChange) {
					return;
				}

				myHandlingProfileComboboxChange = true;
				try {
					if (myProfileCombobox.getSelectedIndex() == 0) {
						myMessage.setValidationContext(null);
					} else if (myProfileCombobox.getSelectedIndex() == 1) {
						myMessage.setValidationContext(new DefaultValidation());
					} else if (myProfileCombobox.getSelectedIndex() > 0) {
						ProfileGroup profile = myProfileComboboxModel.myProfileGroups.get(myProfileCombobox.getSelectedIndex());
						myMessage.setRuntimeProfile(profile);

						// } else if (myProfileCombobox.getSelectedItem() ==
						// ProfileComboModel.APPLY_CONFORMANCE_PROFILE) {
						// IOkCancelCallback<Void> callback = new
						// IOkCancelCallback<Void>() {
						// public void ok(Void theArg) {
						// myProfileComboboxModel.update();
						// }
						//
						// public void cancel(Void theArg) {
						// myProfileCombobox.setSelectedIndex(0);
						// }
						// };
						// myController.chooseAndLoadConformanceProfileForMessage(myMessage,
						// callback);
					}
				} catch (ProfileException e2) {
					ourLog.error("Failed to load profile", e2);
				} finally {
					myHandlingProfileComboboxChange = false;
				}
			}
		});
		myProfileCombobox.setMaximumSize(new Dimension(300, 32767));
		myProfileCombobox.setModel(myProfileComboboxModel);

		mySendingActivityTable = new ActivityTable();
		mySendingActivityTable.setController(myController);
		// Note: mySendingActivityTable is added to the bottom connections tabs in TestPanelWindow

		myTerserPathTextField = new JLabel();
		myTerserPathTextField.setForeground(Color.BLUE);
		myTerserPathTextField.setFont(new Font("Lucida Console", Font.PLAIN, 11));
		myTerserPathTextField.setBorder(null);
		myTerserPathTextField.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (StringUtils.isNotEmpty(myTerserPathTextField.getText())) {
					myTerserPathPopupMenu.show(myTerserPathTextField, 0, 0);
				}
			}
		});

		initLocal();

	}


	@Override
	public void updateUI() {
		super.updateUI();
		if (myMessageEditor != null) {
			applyEditorTheme();
		}
	}

	private void applyEditorTheme() {
		boolean dark = App.isCurrentlyDark();
		String themePath = dark
			? "/org/fife/ui/rsyntaxtextarea/themes/dark.xml"
			: "/org/fife/ui/rsyntaxtextarea/themes/default.xml";
		try (InputStream in = getClass().getResourceAsStream(themePath)) {
			if (in != null) {
				Theme.load(in).apply(myMessageEditor);
			}
		} catch (Exception ignored) {}
	}

	public void destroy() {
		myMessage.removePropertyChangeListener(Hl7V2MessageCollection.SOURCE_MESSAGE_PROPERTY, myMessageListeneer);
		myMessage.removePropertyChangeListener(Hl7V2MessageCollection.PROP_HIGHLITED_RANGE, myRangeListener);
		myMessage.removePropertyChangeListener(Hl7V2MessageCollection.SAVED_PROPERTY, myWindowTitleListener);
		myMessage.removePropertyChangeListener(Hl7V2MessageCollection.PROP_SAVE_FILENAME, myWindowTitleListener);

		myTreePanel.destroy();
		myController.getOutboundConnectionList().addPropertyChangeListener(OutboundConnectionList.PROP_LIST, myOutboundConnectionsListener);

		myTablesComboModel.destroy();
		unregisterProfileNamesListeners();
	}


	private void initLocal() {

		myDocumentListener = new DocumentListener() {

			public void changedUpdate(DocumentEvent theE) {
				ourLog.info("Document change: " + theE);
				handleChange(theE);
			}


			private void handleChange(DocumentEvent theE) {
				myDontRespondToSourceMessageChanges = true;
				try {
					long start = System.currentTimeMillis();

					String newSource = myMessageEditor.getText();
					int changeStart = theE.getOffset();
					int changeEnd = changeStart + theE.getLength();
					myMessage.updateSourceMessage(newSource, changeStart, changeEnd);

					ourLog.info("Handled document update in {} ms", System.currentTimeMillis() - start);
				} catch (Exception e) {
					ourLog.warn("Error parsing message: {}", e.getMessage());
				} finally {
					myDontRespondToSourceMessageChanges = false;
				}
			}


			public void insertUpdate(DocumentEvent theE) {
				ourLog.info("Document insert: " + theE);
				handleChange(theE);
			}


			public void removeUpdate(DocumentEvent theE) {
				ourLog.info("Document removed: " + theE);
				handleChange(theE);
			}
		};
		myMessageEditor.getDocument().addDocumentListener(myDocumentListener);

		myMessageEditor.addCaretListener(new CaretListener() {

			public void caretUpdate(final CaretEvent theE) {
				removeMostHighlights();
				if (!myDisableCaretUpdateHandling) {
					myController.invokeInBackground(new Runnable() {
						public void run() {
							myMessage.setHighlitedPathBasedOnRange(new Range(theE.getDot(), theE.getMark()));
							final boolean shouldFollow = myFollowToggle.isSelected();
							SwingUtilities.invokeLater(new Runnable() {
								public void run() {
									if (shouldFollow) {
										myTreePanel.synchronizeTreeWithHighlitedPath();
									}
									myTreePanel.repaint();
								}
							});
						}});
				}
			}
		});

		updateOutboundConnectionsBox();
		myOutboundConnectionsListener = new PropertyChangeListener() {

			public void propertyChange(PropertyChangeEvent theEvt) {
				updateOutboundConnectionsBox();
			}
		};
		myController.getOutboundConnectionList().addPropertyChangeListener(OutboundConnectionList.PROP_LIST, myOutboundConnectionsListener);

		myOutboundInterfaceCombo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent theE) {
				if (!myOutboundInterfaceComboModelIsUpdating) {
					updateSendButton();
				}
			}

		});

		JMenuItem copyMenuItem = new JMenuItem("Copy to Clipboard");
		copyMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent theE) {
				String selection = myTerserPathTextField.getText();
				StringSelection data = new StringSelection(selection);
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				clipboard.setContents(data, data);
			}
		});
		myTerserPathPopupMenu.add(copyMenuItem);

		myProfilesListener = new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent theEvt) {
				myProfileComboboxModel.update();
				registerProfileNamesListeners();
			}
		};
		myController.getProfileFileList().addPropertyChangeListener(ProfileFileList.PROP_FILES, myProfilesListener);
		registerProfileNamesListeners();

		myProfilesNamesListener = new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent theEvt) {
				myProfileComboboxModel.update();
			}
		};

	}


	private void registerProfileNamesListeners() {
		unregisterProfileNamesListeners();
		for (ProfileGroup next : myController.getProfileFileList().getProfiles()) {
			next.addPropertyChangeListener(ProfileGroup.PROP_NAME, myProfilesNamesListener);
		}
	}


	private void unregisterProfileNamesListeners() {
		for (ProfileGroup next : myController.getProfileFileList().getProfiles()) {
			next.removePropertyChangeListener(ProfileGroup.PROP_NAME, myProfilesNamesListener);
		}
	}


	private void updateSendButton() {
		String selected = (String) myOutboundInterfaceCombo.getSelectedItem();
		if (selected == null || selected == NO_CONNECTIONS) {
			mySendButton.setEnabled(false);
			mySendTimesSpinner.setEnabled(false);
			mySendTimesLabel.setEnabled(false);
			if (myMessage != null) {
				myMessage.setLastSendToInterfaceId(null);
			}
			return;
		}

		if (selected == CREATE_NEW_CONNECTION) {
			IOkCancelCallback<OutboundConnection> handler = new IOkCancelCallback<OutboundConnection>() {
				public void ok(OutboundConnection theArg) {
					myMessage.setLastSendToInterfaceId(theArg.getId());
					myOutboundInterfaceCombo.setSelectedIndex(myOutboundInterfaceComboModel.getSize() - 2);
				}


				public void cancel(OutboundConnection theArg) {
					myOutboundInterfaceCombo.setSelectedIndex(0);
				}
			};
			myController.addOutboundConnectionToSendTo(handler);
			return;
		}

		if (myMessage != null) {
			int selectedIndex = myOutboundInterfaceCombo.getSelectedIndex();
			OutboundConnection connection = myController.getOutboundConnectionList().getConnections().get(selectedIndex);
			myMessage.setLastSendToInterfaceId(connection.getId());
		}

		mySendButton.setEnabled(true);
		mySendTimesSpinner.setEnabled(true);
		mySendTimesLabel.setEnabled(true);
	}


	/**
	 * @param theMessage
	 *            the message to set
	 */
	public void setMessage(Hl7V2MessageCollection theMessage) {
		Validate.isTrue(myMessage == null);

		myMessage = theMessage;

		myShowCombo.setModel(new ShowComboModel());

		// Prepopulate the "send to interface" combo to the last value it had
		if (StringUtils.isNotBlank(myMessage.getLastSendToInterfaceId())) {
			for (int i = 0; i < myOutboundInterfaceComboModelShadow.size(); i++) {
				OutboundConnection conn = myOutboundInterfaceComboModelShadow.get(i);
				if (conn != null && conn.getId().equals(myMessage.getLastSendToInterfaceId())) {
					myOutboundInterfaceCombo.setSelectedIndex(i);
					break;
				}
			}
		}

		updateEncodingButtons();
		myRdbtnEr7.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent theE) {
				removeHighlights();
				myMessage.setEncoding(Hl7V2EncodingTypeEnum.ER_7);
			}
		});
		myRdbtnXml.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent theE) {
				removeHighlights();
				myMessage.setEncoding(Hl7V2EncodingTypeEnum.XML);
			}
		});

		try {
			myDisableCaretUpdateHandling = true;
			updateMessageEditor();
		} finally {
			myDisableCaretUpdateHandling = false;
		}

		myMessageListeneer = new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent theEvt) {
				if (myDontRespondToSourceMessageChanges) {
					return;
				}
				try {
					myDisableCaretUpdateHandling = true;
					updateMessageEditor();
				} finally {
					myDisableCaretUpdateHandling = false;
				}
			}
		};
		myMessage.addPropertyChangeListener(Hl7V2MessageCollection.SOURCE_MESSAGE_PROPERTY, myMessageListeneer);

		mySelectedPathListener = new PropertyChangeListener() {

			public void propertyChange(PropertyChangeEvent theEvt) {
				String path = myMessage.getHighlitedPath();
				if (path == null) {
					path = "";
				}

				int index = path.indexOf('/');
				if (index > -1) {
					path = path.substring(index);
				}

				myTerserPathTextField.setText(path);
			}
		};
		myMessage.addPropertyChangeListener(Hl7V2MessageCollection.PROP_HIGHLITED_PATH, mySelectedPathListener);

		myTreePanel.setMessage(myMessage);

		myRangeListener = new PropertyChangeListener() {

			public void propertyChange(PropertyChangeEvent theEvt) {
				if (theEvt.getNewValue() == null || !myFollowToggle.isSelected()) {
					removeHighlights();
					return;
				}
				Range range = (Range) theEvt.getNewValue();

				myMessageEditor.setCaretPosition(range.getStart());
				myMessageEditor.moveCaretPosition(range.getEnd());

				myMessageEditor.setCaretPosition(range.getEnd());
				myMessageEditor.moveCaretPosition(range.getStart());

				removeHighlights();
				try {
					Highlighter hilite = myMessageEditor.getHighlighter();
					hilite.addHighlight(range.getStart(), range.getEnd(), new javax.swing.text.DefaultHighlighter.DefaultHighlightPainter(new Color(255, 200, 50, 100)));
				} catch (javax.swing.text.BadLocationException e) {
					ourLog.error("Failed to highlight range", e);
				}

				myMessageEditor.repaint();

				String substring = myMessage.getSourceMessage().substring(range.getStart(), range.getEnd());
				ourLog.info("Selected range set to " + range + " which is " + substring);
			}
		};
		myMessage.addPropertyChangeListener(Hl7V2MessageCollection.PROP_HIGHLITED_RANGE, myRangeListener);

		myProfileComboboxModel.update();

		// Window Title
		myWindowTitleListener = new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent theEvt) {
				updateWindowTitle();
			}
		};
		myMessage.addPropertyChangeListener(Hl7V2MessageCollection.SAVED_PROPERTY, myWindowTitleListener);
		myMessage.addPropertyChangeListener(Hl7V2MessageCollection.PROP_SAVE_FILENAME, myWindowTitleListener);
		updateWindowTitle();

		EventQueue.invokeLater(new Runnable() {

			public void run() {
				myMessageEditor.setCaretPosition(0);
				myMessageEditor.grabFocus();
			}
		});

	}


	private void removeHighlights() {
		Highlighter hilite = myMessageEditor.getHighlighter();
		Highlighter.Highlight[] hilites = hilite.getHighlights();
		for (int i = 0; i < hilites.length; i++) {
			hilite.removeHighlight(hilites[i]);
		}
	}

	private void highlightFieldAtCursorPosition(int cursorPos) {
		if (myMessage == null) {
			return;
		}

		String sourceMessage = myMessage.getSourceMessage();
		if (sourceMessage == null || cursorPos < 0 || cursorPos > sourceMessage.length()) {
			removeHighlights();
			return;
		}

		// Get encoding characters from the parsed message
		ca.uhn.hl7v2.parser.EncodingCharacters enc = null;
		try {
			java.util.List<AbstractMessage<?>> messages = myMessage.getMessages();
			if (messages != null && messages.size() > 0 && messages.get(0) instanceof Message) {
				enc = ca.uhn.hl7v2.parser.EncodingCharacters.getInstance((Message) messages.get(0));
			}
		} catch (Exception e) {
			// Fall back to default
		}

		if (enc == null) {
			enc = new ca.uhn.hl7v2.parser.EncodingCharacters('|', '^', '&', '\\', '~');
		}

		char fieldSep = enc.getFieldSeparator();
		char componentSep = enc.getComponentSeparator();
		char subcomponentSep = enc.getSubcomponentSeparator();
		char escapeChar = enc.getEscapeCharacter();
		char repetitionSep = enc.getRepetitionSeparator();

		int fieldStart = cursorPos;
		int fieldEnd = cursorPos;

		char[] chars = sourceMessage.toCharArray();

		// Find start of field (go back until we hit field separator, line break, or start of string)
		while (fieldStart > 0) {
			char c = chars[fieldStart - 1];
			if (c == fieldSep || c == '\n') {
				break;
			}
			fieldStart--;
		}

		// Find end of field (go forward until we hit field separator, line break, or end of string)
		while (fieldEnd < chars.length) {
			char c = chars[fieldEnd];
			if (c == fieldSep || c == '\n') {
				break;
			}
			fieldEnd++;
		}

		// Remove old highlights
		removeHighlights();

		// Add new highlight
		try {
			Highlighter hilite = myMessageEditor.getHighlighter();
			hilite.addHighlight(fieldStart, fieldEnd, new javax.swing.text.DefaultHighlighter.DefaultHighlightPainter(new Color(255, 200, 50, 100)));
		} catch (javax.swing.text.BadLocationException e) {
			ourLog.error("Failed to highlight field at cursor", e);
		}
	}


	// Removes all but the 2 most recent highlights - the last tag pair
	// selected.
	private void removeMostHighlights() {
		Highlighter hilite = myMessageEditor.getHighlighter();
		Highlighter.Highlight[] hilites = hilite.getHighlights();
		for (int i = 0; i < hilites.length - 2; i++) {
			hilite.removeHighlight(hilites[i]);
		}
	}


	private void updateWindowTitle() {
		StringBuilder b = new StringBuilder();

		if (myMessage.isSaved() == false) {
			b.append("Unsaved");
		}

		if (b.length() > 0) {
			b.append(" - ");
		}
		if (StringUtils.isNotBlank(myMessage.getSaveFileName())) {
			b.append(myMessage.getSaveFileName());
		} else {
			b.append("New File");
		}

		setWindowTitle(b.toString());
	}


	private void updateEncodingButtons() {
		switch (myMessage.getEncoding()) {
			case XML:
				myRdbtnXml.setSelected(true);
				myRdbtnEr7.setSelected(false);
				break;
			case ER_7:
				myRdbtnXml.setSelected(false);
				myRdbtnEr7.setSelected(true);
		}
	}

	

	private void updateMessageEditor() {

		final JScrollBar vsb = myMessageScrollPane.getVerticalScrollBar();
		int initialVerticalValue = vsb.getValue();

		myMessageEditor.getDocument().removeDocumentListener(myDocumentListener);

		String sourceMessage = myMessage.getSourceMessage();

		if (myMessage.getEncoding() == Hl7V2EncodingTypeEnum.XML) {
			myMessageEditor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML);
		} else {
			myMessageEditor.setSyntaxEditingStyle(SYNTAX_STYLE_ER7);
			sourceMessage = sourceMessage.replace('\r', '\n');
		}

		myMessageEditor.setText(sourceMessage);

		myMessageEditor.getDocument().addDocumentListener(myDocumentListener);

		final int verticalValue = Math.min(initialVerticalValue, vsb.getMaximum());

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				vsb.setValue(verticalValue);
			}
		});

	}


	private void updateOutboundConnectionsBox() {
		int currentSelection = myOutboundInterfaceCombo.getSelectedIndex();

		List<OutboundConnection> conn = myController.getOutboundConnectionList().getConnections();

		myOutboundInterfaceComboModelIsUpdating = true;

		myOutboundInterfaceComboModel.removeAllElements();
		myOutboundInterfaceComboModelShadow = new ArrayList<OutboundConnection>();
		if (conn.isEmpty()) {
			myOutboundInterfaceComboModel.addElement(NO_CONNECTIONS);
			myOutboundInterfaceComboModelShadow.add(null);
		} else {
			for (OutboundConnection next : conn) {
				myOutboundInterfaceComboModel.addElement(next.getName());
				myOutboundInterfaceComboModelShadow.add(next);
			}
		}

		myOutboundInterfaceComboModel.addElement(CREATE_NEW_CONNECTION);
		myOutboundInterfaceComboModelShadow.add(null);

		if (currentSelection != -1 && currentSelection < myOutboundInterfaceComboModel.getSize()) {
			myOutboundInterfaceCombo.setSelectedIndex(currentSelection);
		} else {
			myOutboundInterfaceCombo.setSelectedIndex(0);
		}

		myOutboundInterfaceComboModelIsUpdating = false;

		updateSendButton();
	}

	private class ProfileComboModel extends DefaultComboBoxModel {

		// private static final String APPLY_CONFORMANCE_PROFILE =
		// "Apply Conformance Profile...";
		private ArrayList<ProfileGroup> myProfileGroups;


		public void update() {
			myHandlingProfileComboboxChange = true;
			try {
				super.removeAllElements();
				myProfileGroups = new ArrayList<ProfileGroup>();

				addElement("No Profile/Validation");
				myProfileGroups.add(null);

				addElement("Default Datatype Validation (HAPI)");
				myProfileGroups.add(null);

				// ProfileGroup profile = myMessage.getRuntimeProfile();
				// if (profile != null) {
				// String name = "Conf Profile (" +
				// profile.getMessage().getMsgType() + "^" +
				// profile.getMessage().getEventType() + ")";
				// String profName = myMessage.getRuntimeProfile().getName();
				// if (StringUtils.isNotBlank(profName)) {
				// name = name + ":" + profName;
				// }
				//
				// addElement(name);
				// }

				// addElement(APPLY_CONFORMANCE_PROFILE);

				for (ProfileGroup nextProfile : myController.getProfileFileList().getProfiles()) {
					String nextString = "Profile Group: " + nextProfile.getName();
					addElement(nextString);
					myProfileGroups.add(nextProfile);
					if (myMessage.getRuntimeProfile() == nextProfile) {
						setSelectedItem(nextString);
					}
				}

				if (myMessage.getValidationContext() instanceof DefaultValidation) {
					setSelectedItem(getElementAt(1));
				} else if (getSelectedItem() == null) {
					setSelectedItem(getElementAt(0));
				}
			} finally {
				myHandlingProfileComboboxChange = false;
			}
		}
	}

	public class ShowComboModel extends DefaultComboBoxModel implements ActionListener {
		private static final String ALL = "All";
		private static final String ERRORS = "Errors";
		private static final String POPULATED = "Populated";
		private static final String SUPPORTED = "Supported";


		public ShowComboModel() {
			addElement(POPULATED);
			addElement(ALL);
			addElement(ERRORS);
			addElement(SUPPORTED);

			switch (myMessage.getEditorShowMode()) {
				case ALL:
					setSelectedItem(ALL);
					break;
				case ERROR:
					setSelectedItem(ERRORS);
					break;
				case POPULATED:
					setSelectedItem(POPULATED);
					break;
				case SUPPORTED:
					setSelectedItem(SUPPORTED);
					break;
			}

			myShowCombo.addActionListener(this);
		}


		public void actionPerformed(ActionEvent theE) {
			String value = (String) myShowCombo.getSelectedItem();

			if (value == ALL) {
				myTreePanel.setEditorShowModeAndUpdateAccordingly(ShowEnum.ALL);
			} else if (value == ERRORS) {
				myTreePanel.setEditorShowModeAndUpdateAccordingly(ShowEnum.ERROR);
			} else if (value == SUPPORTED) {
				myTreePanel.setEditorShowModeAndUpdateAccordingly(ShowEnum.SUPPORTED);
			} else {
				myTreePanel.setEditorShowModeAndUpdateAccordingly(ShowEnum.POPULATED);
			}

		}
	}


	private void activateSendingActivityTabForConnection(OutboundConnection theConnection) {
		mySendingActivityTable.setConnection(theConnection, false);
	}

	public Frame getWindow() {
		return myController.getWindow();
	}

	public ActivityTable getSendingActivityTable() {
		return mySendingActivityTable;
	}

	public JLabel getTerserPathLabel() {
		return myTerserPathTextField;
	}

	public void setTestPanelWindow(TestPanelWindow theWindow) {
		myTestPanelWindow = theWindow;
	}

	public void openFindDialog() {
		if (myReplaceDialog != null && myReplaceDialog.isVisible()) {
			myReplaceDialog.setVisible(false);
		}
		if (myFindDialog == null) {
			myFindDialog = new FindDialog((java.awt.Frame) SwingUtilities.getWindowAncestor(this), this);
		}
		myFindDialog.setVisible(true);
	}

	public void openReplaceDialog() {
		if (myFindDialog != null && myFindDialog.isVisible()) {
			myFindDialog.setVisible(false);
		}
		if (myReplaceDialog == null) {
			myReplaceDialog = new ReplaceDialog((java.awt.Frame) SwingUtilities.getWindowAncestor(this), this);
		}
		myReplaceDialog.setVisible(true);
	}

	public void openGoToDialog() {
		GoToDialog dialog = new GoToDialog((java.awt.Frame) SwingUtilities.getWindowAncestor(this));
		dialog.setMaxLineNumberAllowed(myMessageEditor.getLineCount());
		dialog.setVisible(true);
		int line = dialog.getLineNumber();
		if (line > 0) {
			try {
				myMessageEditor.setCaretPosition(myMessageEditor.getLineStartOffset(line - 1));
			} catch (javax.swing.text.BadLocationException e) {
				// ignore
			}
		}
	}

	@Override
	public void searchEvent(SearchEvent e) {
		SearchEvent.Type type = e.getType();
		SearchContext context = e.getSearchContext();
		SearchResult result;
		switch (type) {
			case MARK_ALL:
				result = SearchEngine.markAll(myMessageEditor, context);
				break;
			case FIND:
				result = SearchEngine.find(myMessageEditor, context);
				if (!result.wasFound()) {
					javax.swing.UIManager.getLookAndFeel().provideErrorFeedback(myMessageEditor);
				}
				break;
			case REPLACE:
				result = SearchEngine.replace(myMessageEditor, context);
				if (!result.wasFound()) {
					javax.swing.UIManager.getLookAndFeel().provideErrorFeedback(myMessageEditor);
				}
				break;
			case REPLACE_ALL:
				result = SearchEngine.replaceAll(myMessageEditor, context);
				javax.swing.JOptionPane.showMessageDialog(this, result.getCount() + " replacements made.");
				break;
			default:
				break;
		}
	}

	@Override
	public String getSelectedText() {
		return myMessageEditor.getSelectedText();
	}

	private static Font resolveEditorFont(int size) {
		String[] preferred = { "JetBrains Mono", "Fira Code", "Cascadia Code" };
		java.awt.GraphicsEnvironment ge = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment();
		java.util.Set<String> available = new java.util.HashSet<>(java.util.Arrays.asList(ge.getAvailableFontFamilyNames()));
		for (String name : preferred) {
			if (available.contains(name)) {
				return new Font(name, Font.PLAIN, size);
			}
		}
		return new Font(Font.MONOSPACED, Font.PLAIN, size);
	}

}
