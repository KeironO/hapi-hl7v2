package ca.uhn.hl7v2.testpanel.ui.tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.Highlighter;

import org.apache.commons.lang.StringUtils;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;

import ca.uhn.hl7v2.testpanel.controller.Hl7V2FileDiffController;
import ca.uhn.hl7v2.testpanel.controller.Prefs;
import ca.uhn.hl7v2.testpanel.ui.Er7TokenMaker;
import ca.uhn.hl7v2.testpanel.util.SimpleDocumentListener;

import java.lang.reflect.InvocationTargetException;

public class Hl7V2FileDiffDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	private static final String SYNTAX_STYLE_ER7 = "text/er7";

	private JButton myBeginButton;
	private Hl7V2FileDiffController myController;
	private RSyntaxTextArea myPane1TextArea;
	private RSyntaxTextArea myPane2TextArea;
	private JButton myStopButton;
	private JCheckBox myStopOnFirstErrorCheck;
	private JProgressBar myProgressBar;
	private JCheckBox myShowWholeMessageOnErrorCheckbox;
	private VisualDiffPanel myVisualDiffPanel;
	private OutputListModel myOutputListModel;
	private JDialog myLogWindow;

	static {
		org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory factory =
			(org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory)
				org.fife.ui.rsyntaxtextarea.TokenMakerFactory.getDefaultInstance();
		factory.putMapping(SYNTAX_STYLE_ER7, Er7TokenMaker.class.getName());
	}

	public Hl7V2FileDiffDialog(Hl7V2FileDiffController theHl7v2FileDiffController) {
		myController = theHl7v2FileDiffController;

		setTitle("HL7 Diff");
		setBounds(100, 100, 1400, 900);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setResizable(true);
		getContentPane().setLayout(new BorderLayout());

		// Main split pane: input on top, results on bottom
		JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		mainSplitPane.setResizeWeight(0.3);
		mainSplitPane.setDividerSize(5);
		getContentPane().add(mainSplitPane, BorderLayout.CENTER);

		// Top: horizontal split with left and right input panels
		JPanel topPanel = createTopInputPanel();
		mainSplitPane.setTopComponent(topPanel);

		// Bottom: visual diff panel
		myVisualDiffPanel = new VisualDiffPanel();
		mainSplitPane.setBottomComponent(myVisualDiffPanel);

		// Toolbar between inputs and results
		JPanel toolbarPanel = createToolbarPanel();
		getContentPane().add(toolbarPanel, BorderLayout.NORTH);

		// Footer: progress bar and Close button
		JPanel footerPanel = createFooterPanel();
		getContentPane().add(footerPanel, BorderLayout.SOUTH);

		initListeners();
		updateButtonStates();
	}

	private JPanel createTopInputPanel() {
		JSplitPane horizontalSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		horizontalSplit.setResizeWeight(0.5);
		horizontalSplit.setDividerSize(5);

		JPanel leftPanel = createInputPanel("Expected / Left Message", 0);
		JPanel rightPanel = createInputPanel("Actual / Right Message", 1);

		horizontalSplit.setLeftComponent(leftPanel);
		horizontalSplit.setRightComponent(rightPanel);

		JPanel wrapper = new JPanel(new BorderLayout());
		wrapper.add(horizontalSplit, BorderLayout.CENTER);
		return wrapper;
	}

	private JPanel createInputPanel(String title, int paneIndex) {
		JPanel panel = new JPanel(new BorderLayout(5, 5));
		panel.setBorder(new TitledBorder(null, title, TitledBorder.LEADING, TitledBorder.TOP, null, null));
		panel.setBorder(new EmptyBorder(10, 10, 10, 10));

		RSyntaxTextArea editorPane = new RSyntaxTextArea();
		editorPane.setSyntaxEditingStyle(SYNTAX_STYLE_ER7);
		editorPane.setCodeFoldingEnabled(false);
		editorPane.setLineWrap(false);
		editorPane.setHighlightCurrentLine(false);
		editorPane.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12));

		if (paneIndex == 0) {
			myPane1TextArea = editorPane;
		} else {
			myPane2TextArea = editorPane;
		}

		RTextScrollPane scrollPane = new RTextScrollPane(editorPane, false);

		editorPane.getDocument().addDocumentListener(new SimpleDocumentListener() {
			@Override
			public void update(DocumentEvent theE) {
				updateButtonStates();
			}
		});

		JPanel editorPanel = new JPanel(new BorderLayout());
		editorPanel.add(scrollPane, BorderLayout.CENTER);

		panel.add(editorPanel, BorderLayout.CENTER);

		JButton loadButton = new JButton("Load from file...");
		loadButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				loadFileIntoTextArea(editorPane);
			}
		});
		panel.add(loadButton, BorderLayout.SOUTH);

		return panel;
	}

	private void loadFileIntoTextArea(RSyntaxTextArea editorPane) {
		File currentDir = Prefs.getTestpanelHomeDirectory();
		JFileChooser chooser = new JFileChooser(currentDir);
		int result = chooser.showOpenDialog(Hl7V2FileDiffDialog.this);
		if (result == JFileChooser.APPROVE_OPTION) {
			File file = chooser.getSelectedFile();
			try {
				String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
				editorPane.setText(content);
			} catch (IOException e) {
				JOptionPane.showMessageDialog(Hl7V2FileDiffDialog.this,
					"Could not read file: " + e.getMessage(),
					"Error reading file",
					JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	private JPanel createToolbarPanel() {
		JPanel panel = new JPanel();
		panel.setBorder(new EmptyBorder(5, 5, 5, 5));
		GridBagLayout gbl = new GridBagLayout();
		gbl.columnWidths = new int[] { 0, 0, 0, 100, 0, 0, 0 };
		gbl.rowHeights = new int[] { 0 };
		gbl.columnWeights = new double[] { 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0 };
		gbl.rowWeights = new double[] { Double.MIN_VALUE };
		panel.setLayout(gbl);

		myBeginButton = new JButton("Begin");
		myBeginButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				myController.begin();
			}
		});
		myBeginButton.setIcon(new ImageIcon(Hl7V2FileDiffDialog.class.getResource("/ca/uhn/hl7v2/testpanel/images/start_all.png")));
		myBeginButton.setFont(myBeginButton.getFont().deriveFont(12f));
		GridBagConstraints gbc_BeginButton = new GridBagConstraints();
		gbc_BeginButton.anchor = GridBagConstraints.WEST;
		gbc_BeginButton.insets = new Insets(0, 5, 0, 10);
		gbc_BeginButton.gridx = 0;
		gbc_BeginButton.gridy = 0;
		panel.add(myBeginButton, gbc_BeginButton);

		myStopButton = new JButton("Stop");
		myStopButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				myController.cancel();
			}
		});
		myStopButton.setIcon(new ImageIcon(Hl7V2FileDiffDialog.class.getResource("/ca/uhn/hl7v2/testpanel/images/stop_all.png")));
		myStopButton.setFont(myStopButton.getFont().deriveFont(12f));
		GridBagConstraints gbc_StopButton = new GridBagConstraints();
		gbc_StopButton.insets = new Insets(0, 0, 0, 10);
		gbc_StopButton.gridx = 1;
		gbc_StopButton.gridy = 0;
		panel.add(myStopButton, gbc_StopButton);

		JLabel lblOnDifference = new JLabel("On Difference:");
		GridBagConstraints gbc_lblOnDifference = new GridBagConstraints();
		gbc_lblOnDifference.insets = new Insets(0, 10, 0, 5);
		gbc_lblOnDifference.anchor = GridBagConstraints.EAST;
		gbc_lblOnDifference.gridx = 3;
		gbc_lblOnDifference.gridy = 0;
		panel.add(lblOnDifference, gbc_lblOnDifference);

		myStopOnFirstErrorCheck = new JCheckBox("Stop on First Difference");
		myStopOnFirstErrorCheck.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				Prefs.getInstance().setHl7V2DiffStopOnFirstError(myStopOnFirstErrorCheck.isSelected());
			}
		});
		myStopOnFirstErrorCheck.setSelected(Prefs.getInstance().getHl7V2DiffStopOnFirstError());
		GridBagConstraints gbc_StopOnFirstErrorCheck = new GridBagConstraints();
		gbc_StopOnFirstErrorCheck.insets = new Insets(0, 0, 0, 10);
		gbc_StopOnFirstErrorCheck.gridx = 4;
		gbc_StopOnFirstErrorCheck.gridy = 0;
		panel.add(myStopOnFirstErrorCheck, gbc_StopOnFirstErrorCheck);

		myShowWholeMessageOnErrorCheckbox = new JCheckBox("Show Full Messages on Error");
		myShowWholeMessageOnErrorCheckbox.setSelected(Prefs.getInstance().getHl7V2DiffShowWholeMessageOnError());
		myShowWholeMessageOnErrorCheckbox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent theE) {
				Prefs.getInstance().setHl7V2DiffShowWholeMessageOnError(myShowWholeMessageOnErrorCheckbox.isSelected());
			}
		});
		GridBagConstraints gbc_ShowWholeMessageOnErrorCheckbox = new GridBagConstraints();
		gbc_ShowWholeMessageOnErrorCheckbox.insets = new Insets(0, 0, 0, 0);
		gbc_ShowWholeMessageOnErrorCheckbox.gridx = 5;
		gbc_ShowWholeMessageOnErrorCheckbox.gridy = 0;
		panel.add(myShowWholeMessageOnErrorCheckbox, gbc_ShowWholeMessageOnErrorCheckbox);

		return panel;
	}

	private JPanel createFooterPanel() {
		JPanel panel = new JPanel(new BorderLayout(10, 5));
		panel.setBorder(new EmptyBorder(5, 5, 5, 5));

		myProgressBar = new JProgressBar();
		myProgressBar.setStringPainted(true);
		myProgressBar.setOpaque(true);
		myProgressBar.setValue(0);
		myProgressBar.setMaximum(100);
		myProgressBar.setPreferredSize(new Dimension(400, 20));
		panel.add(myProgressBar, BorderLayout.CENTER);

		JPanel closePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
		JButton logButton = new JButton("View Log");
		logButton.addActionListener(e -> openLogWindow());
		closePanel.add(logButton);

		JButton closeButton = new JButton("Close");
		closeButton.addActionListener(e -> dispose());
		closePanel.add(closeButton);
		panel.add(closePanel, BorderLayout.EAST);

		return panel;
	}

	public boolean isShowWholeMessageOnError() {
		return myShowWholeMessageOnErrorCheckbox.isSelected();
	}

	public Hl7V2FileDiffController.InputMode getInputMode() {
		return Hl7V2FileDiffController.InputMode.TEXT;
	}

	public String getFile1Text() {
		return "";
	}

	public String getFile2Text() {
		return "";
	}

	public String getPane1Text() {
		return getEditorPaneText(myPane1TextArea);
	}

	public String getPane2Text() {
		return getEditorPaneText(myPane2TextArea);
	}

	public VisualDiffPanel getVisualDiffPanel() {
		return myVisualDiffPanel;
	}

	private void initListeners() {
		myOutputListModel = new OutputListModel();

		myController.addPropertyChangeListener(Hl7V2FileDiffController.PROP_RUNNING, new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent theEvt) {
				updateButtonStates();
				updateProgress();
			}
		});
		myController.addPropertyChangeListener(Hl7V2FileDiffController.PROP_FAILED, new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent theEvt) {
				updateProgress();
			}
		});
		myController.addPropertyChangeListener(Hl7V2FileDiffController.PROP_PERCENT_DONE, new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent theEvt) {
				updateProgress();
			}
		});
	}

	private void updateProgress() {
		int percentDone = myController.getPercentDone();
		myProgressBar.setValue(percentDone);

		if (myController.isFailed()) {
			myProgressBar.setString("Failed - " + percentDone + "%");
		} else if (myController.isRunning()) {
			myProgressBar.setString("Working - " + percentDone + "%");
		} else {
			myProgressBar.setString("");
		}
	}

	public boolean isStopOnFirstError() {
		return myStopOnFirstErrorCheck.isSelected();
	}

	private void updateButtonStates() {
		boolean running = myController.isRunning();
		myShowWholeMessageOnErrorCheckbox.setEnabled(!running);
		myStopButton.setEnabled(running);
		myStopOnFirstErrorCheck.setEnabled(!running);

		String text1 = getEditorPaneText(myPane1TextArea);
		String text2 = getEditorPaneText(myPane2TextArea);
		boolean haveText = (StringUtils.isNotBlank(text1) && StringUtils.isNotBlank(text2));
		boolean canBegin = haveText && !running;

		myBeginButton.setEnabled(canBegin);
	}

	private String getEditorPaneText(RSyntaxTextArea pane) {
		if (pane == null) {
			return "";
		}
		String text = pane.getText();
		return text != null ? text : "";
	}

	private void openLogWindow() {
		if (myLogWindow != null && myLogWindow.isVisible()) {
			myLogWindow.toFront();
			return;
		}

		myLogWindow = new JDialog(this, "HL7 Diff Log", false);
		myLogWindow.setSize(600, 400);
		myLogWindow.setLocationRelativeTo(this);

		javax.swing.JList<String> logList = new javax.swing.JList<>(myOutputListModel);
		logList.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 11));
		logList.setCellRenderer(new ColoredOutputRenderer());

		JScrollPane scrollPane = new JScrollPane(logList);
		myLogWindow.add(scrollPane, BorderLayout.CENTER);

		myLogWindow.setVisible(true);
	}

	public class OutputListModel extends javax.swing.AbstractListModel<String> {
		private static final long serialVersionUID = 1L;

		OutputListModel() {
			myController.addPropertyChangeListener(Hl7V2FileDiffController.PROP_OUTPUT, new PropertyChangeListener() {
				public void propertyChange(PropertyChangeEvent theEvt) {
					fireContentsChanged(OutputListModel.this, 0, getSize());
				}
			});
		}

		@Override
		public int getSize() {
			return myController.getOutputSize();
		}

		@Override
		public String getElementAt(int theIndex) {
			String retVal = myController.getOutputLine(theIndex);
			return retVal != null ? retVal : "";
		}
	}

	private static class ColoredOutputRenderer extends javax.swing.DefaultListCellRenderer {
		private static final long serialVersionUID = 1L;

		@Override
		public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			javax.swing.JLabel comp = (javax.swing.JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

			if (value != null && !isSelected) {
				String text = value.toString();

				if (text.contains("No differences found")) {
					comp.setForeground(new Color(0, 128, 0));
				} else if (text.startsWith("< ")) {
					comp.setForeground(new Color(178, 34, 34));
				} else if (text.startsWith("> ")) {
					comp.setForeground(new Color(34, 139, 34));
				} else if (text.contains("Difference Found") || text.contains("FAILED") || text.contains("error:")) {
					comp.setForeground(new Color(204, 0, 0));
				} else if (text.contains("Beginning comparison")) {
					comp.setForeground(new Color(0, 102, 204));
				} else if (text.contains("Control ID") || text.contains("Message")) {
					comp.setForeground(new Color(204, 102, 0));
				} else if (text.startsWith("<html>")) {
					comp.setForeground(new Color(204, 0, 0));
				} else {
					comp.setForeground(list.getForeground());
				}
			}

			return comp;
		}
	}
}
