package ca.uhn.hl7v2.testpanel.ui.tools;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;

import org.apache.commons.lang.StringUtils;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;

import ca.uhn.hl7v2.testpanel.App;
import ca.uhn.hl7v2.testpanel.controller.Hl7V2FileDiffController;
import ca.uhn.hl7v2.testpanel.controller.Prefs;
import ca.uhn.hl7v2.testpanel.ui.ImageFactory;
import ca.uhn.hl7v2.testpanel.ui.Er7TokenMaker;
import ca.uhn.hl7v2.testpanel.util.SimpleDocumentListener;

public class Hl7V2FileDiffDialog extends JDialog {

	private static final long serialVersionUID = 1L;
	private static final String SYNTAX_STYLE_ER7 = "text/er7";
	private static final String CARD_INPUT  = "input";
	private static final String CARD_FILE   = "file";
	private static final String CARD_RESULT = "result";

	private JButton myBeginButton;
	private JButton myEditAgainButton;
	private JButton myPrevDiffButton;
	private JButton myNextDiffButton;
	private Hl7V2FileDiffController myController;
	private RSyntaxTextArea myPane1TextArea;
	private RSyntaxTextArea myPane2TextArea;
	private JTextField myFile1PathField;
	private JTextField myFile2PathField;
	private JRadioButton myTextModeButton;
	private JRadioButton myFileModeButton;
	private Hl7V2FileDiffController.InputMode myInputMode = Hl7V2FileDiffController.InputMode.TEXT;
	private JButton myStopButton;
	private JCheckBox myStopOnFirstErrorCheck;
	private JProgressBar myProgressBar;
	private JCheckBox myShowWholeMessageOnErrorCheckbox;
	private OutputListModel myOutputListModel;
	private JDialog myLogWindow;

	private CardLayout myLeftCardLayout;
	private JPanel myLeftCardPanel;
	private CardLayout myRightCardLayout;
	private JPanel myRightCardPanel;
	private DiffResultPane myLeftResultPane;
	private DiffResultPane myRightResultPane;

	private boolean mySyncingScroll;
	private boolean myInResultMode;
	private int myTotalMsgDiffs;
	private int myTotalFieldDiffs;

	static {
		org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory factory =
			(org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory)
				org.fife.ui.rsyntaxtextarea.TokenMakerFactory.getDefaultInstance();
		factory.putMapping(SYNTAX_STYLE_ER7, Er7TokenMaker.class.getName());
	}

	// -------------------------------------------------------------------------
	// Row types stored in the list model
	// -------------------------------------------------------------------------

	/** A numbered HL7 diff line (wraps DiffLine with a display line number). */
	private static class NumberedDiffLine {
		final int lineNumber;
		final Hl7DiffComparator.DiffLine diff;
		NumberedDiffLine(int n, Hl7DiffComparator.DiffLine d) { lineNumber = n; diff = d; }
	}

	/** A separator row shown between message pairs. Its toString is the display label. */
	private static class SeparatorRow {
		final String label;
		SeparatorRow(String label) { this.label = label; }
		@Override public String toString() { return label; }
	}

	// -------------------------------------------------------------------------
	// Constructor
	// -------------------------------------------------------------------------

	public Hl7V2FileDiffDialog(Hl7V2FileDiffController theHl7v2FileDiffController) {
		myController = theHl7v2FileDiffController;

		setTitle("HL7 Diff");
		setBounds(100, 100, 1400, 900);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setResizable(true);
		getContentPane().setLayout(new BorderLayout());

		JSplitPane editorSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		editorSplit.setResizeWeight(0.5);
		editorSplit.setDividerSize(5);
		editorSplit.setLeftComponent(createSidePanel("Expected / Left Message", 0));
		editorSplit.setRightComponent(createSidePanel("Actual / Right Message", 1));
		getContentPane().add(editorSplit, BorderLayout.CENTER);

		getContentPane().add(createToolbarPanel(), BorderLayout.NORTH);
		getContentPane().add(createFooterPanel(), BorderLayout.SOUTH);

		myLeftResultPane.getVerticalScrollBar().addAdjustmentListener(e -> {
			if (mySyncingScroll) return;
			mySyncingScroll = true;
			myRightResultPane.getVerticalScrollBar().setValue(e.getValue());
			mySyncingScroll = false;
		});
		myRightResultPane.getVerticalScrollBar().addAdjustmentListener(e -> {
			if (mySyncingScroll) return;
			mySyncingScroll = true;
			myLeftResultPane.getVerticalScrollBar().setValue(e.getValue());
			mySyncingScroll = false;
		});

		initListeners();
		updateButtonStates();
	}

	// -------------------------------------------------------------------------
	// Side panels (CardLayout: input editor / diff result list)
	// -------------------------------------------------------------------------

	private JPanel createSidePanel(String title, int paneIndex) {
		RSyntaxTextArea editorPane = new RSyntaxTextArea();
		editorPane.setSyntaxEditingStyle(SYNTAX_STYLE_ER7);
		editorPane.setCodeFoldingEnabled(false);
		editorPane.setLineWrap(false);
		editorPane.setHighlightCurrentLine(false);
		editorPane.setFont(new Font("Monospaced", Font.PLAIN, 12));

		if (paneIndex == 0) myPane1TextArea = editorPane;
		else                myPane2TextArea = editorPane;

		editorPane.getDocument().addDocumentListener(new SimpleDocumentListener() {
			@Override public void update(DocumentEvent e) { updateButtonStates(); }
		});

		DiffResultPane resultPane = new DiffResultPane();
		if (paneIndex == 0) myLeftResultPane  = resultPane;
		else                myRightResultPane = resultPane;

		CardLayout cardLayout = new CardLayout();
		JPanel cardPanel = new JPanel(cardLayout);
		cardPanel.add(new RTextScrollPane(editorPane, true), CARD_INPUT);
		cardPanel.add(createFilePathPanel(paneIndex), CARD_FILE);
		cardPanel.add(resultPane, CARD_RESULT);

		if (paneIndex == 0) { myLeftCardLayout  = cardLayout; myLeftCardPanel  = cardPanel; }
		else                { myRightCardLayout = cardLayout; myRightCardPanel = cardPanel; }

		JPanel editorPanel = new JPanel(new BorderLayout());
		editorPanel.setBorder(new TitledBorder(null, title, TitledBorder.LEADING, TitledBorder.TOP, null, null));
		editorPanel.add(cardPanel, BorderLayout.CENTER);

		JPanel outer = new JPanel(new BorderLayout(5, 5));
		outer.setBorder(new EmptyBorder(10, 10, 10, 10));
		outer.add(editorPanel, BorderLayout.CENTER);
		return outer;
	}

	private JPanel createFilePathPanel(int paneIndex) {
		JTextField pathField = new JTextField();
		if (paneIndex == 0) myFile1PathField = pathField;
		else                myFile2PathField = pathField;

		pathField.getDocument().addDocumentListener(new SimpleDocumentListener() {
			@Override public void update(DocumentEvent e) { updateButtonStates(); }
		});

		JButton browseButton = new JButton("Browse...");
		browseButton.addActionListener(e -> {
			JFileChooser chooser = new JFileChooser(Prefs.getTestpanelHomeDirectory());
			if (chooser.showOpenDialog(Hl7V2FileDiffDialog.this) == JFileChooser.APPROVE_OPTION) {
				pathField.setText(chooser.getSelectedFile().getAbsolutePath());
			}
		});

		JPanel row = new JPanel(new BorderLayout(5, 0));
		row.add(new JLabel("File: "), BorderLayout.WEST);
		row.add(pathField, BorderLayout.CENTER);
		row.add(browseButton, BorderLayout.EAST);

		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(new EmptyBorder(20, 10, 10, 10));
		panel.add(row, BorderLayout.NORTH);
		return panel;
	}

	// -------------------------------------------------------------------------
	// Public API called from the controller
	// -------------------------------------------------------------------------

	public void displayDiff(int msgNum, String[] leftLines, String[] rightLines) {
		Hl7DiffComparator.DiffLine[] leftDiffs  = Hl7DiffComparator.compareSides(leftLines, rightLines);
		Hl7DiffComparator.DiffLine[] rightDiffs = Hl7DiffComparator.compareSides(rightLines, leftLines);

		myTotalMsgDiffs++;
		for (Hl7DiffComparator.DiffLine diff : leftDiffs) {
			if (diff.isDifferent && diff.diffSegments != null) {
				for (Hl7DiffComparator.DiffSegment seg : diff.diffSegments) {
					if (seg.isDifferent && !seg.text.isEmpty() && !"|".equals(seg.text))
						myTotalFieldDiffs++;
				}
			}
		}

		String separatorLabel = "Difference " + msgNum;
		myLeftResultPane.appendDiffs(separatorLabel, leftDiffs);
		myRightResultPane.appendDiffs(separatorLabel, rightDiffs);

		myLeftCardLayout.show(myLeftCardPanel, CARD_RESULT);
		myRightCardLayout.show(myRightCardPanel, CARD_RESULT);
		myInResultMode = true;
		myEditAgainButton.setVisible(true);
		updateNavButtons();

		// Auto-jump to first diff in this batch if it is the first pair
		if (myTotalMsgDiffs == 1) navigateDiff(true);
	}

	private void switchToInputMode() {
		String card = myInputMode == Hl7V2FileDiffController.InputMode.FILE ? CARD_FILE : CARD_INPUT;
		myLeftCardLayout.show(myLeftCardPanel, card);
		myRightCardLayout.show(myRightCardPanel, card);
		myInResultMode = false;
		myEditAgainButton.setVisible(false);
		updateNavButtons();
	}

	private void onModeChanged() {
		myInputMode = myFileModeButton.isSelected()
			? Hl7V2FileDiffController.InputMode.FILE
			: Hl7V2FileDiffController.InputMode.TEXT;
		if (!myInResultMode) {
			String card = myInputMode == Hl7V2FileDiffController.InputMode.FILE ? CARD_FILE : CARD_INPUT;
			myLeftCardLayout.show(myLeftCardPanel, card);
			myRightCardLayout.show(myRightCardPanel, card);
		}
		updateButtonStates();
	}

	private void clearResults() {
		myLeftResultPane.clearDiffs();
		myRightResultPane.clearDiffs();
		myTotalMsgDiffs   = 0;
		myTotalFieldDiffs = 0;
	}

	// -------------------------------------------------------------------------
	// Diff navigation
	// -------------------------------------------------------------------------

	private void navigateDiff(boolean forward) {
		int current = myLeftResultPane.getSelectedIndex();
		int next    = myLeftResultPane.findNextDiff(current, forward);
		if (next < 0) return;
		mySyncingScroll = true;
		myLeftResultPane.scrollToAndSelect(next);
		myRightResultPane.scrollToAndSelect(next);
		mySyncingScroll = false;
	}

	private void updateNavButtons() {
		myPrevDiffButton.setEnabled(myInResultMode);
		myNextDiffButton.setEnabled(myInResultMode);
	}

	// -------------------------------------------------------------------------
	// Diff result list
	// -------------------------------------------------------------------------

	private static class DiffResultPane extends JScrollPane {
		private final DefaultListModel<Object> myModel;
		private final JList<Object> myList;

		DiffResultPane() {
			myModel = new DefaultListModel<>();
			myList  = new JList<>(myModel);
			myList.setFont(new Font("Monospaced", Font.PLAIN, 12));
			myList.setCellRenderer(new DiffLineCellRenderer());
			myList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			setViewportView(myList);
			setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		}

		void appendDiffs(String separatorLabel, Hl7DiffComparator.DiffLine[] diffs) {
			// Count existing numbered rows to continue line numbering
			int lineNum = 0;
			for (int i = 0; i < myModel.size(); i++) {
				if (myModel.get(i) instanceof NumberedDiffLine) lineNum++;
			}
			// Add separator if this is not the first block
			if (!myModel.isEmpty()) myModel.addElement(new SeparatorRow(separatorLabel));
			// Add numbered diff lines
			for (Hl7DiffComparator.DiffLine d : diffs) {
				myModel.addElement(new NumberedDiffLine(++lineNum, d));
			}
		}

		void clearDiffs() {
			myModel.clear();
		}

		int getSelectedIndex() {
			return myList.getSelectedIndex();
		}

		void scrollToAndSelect(int index) {
			if (index >= 0 && index < myModel.size()) {
				myList.setSelectedIndex(index);
				myList.ensureIndexIsVisible(index);
			}
		}

		/** Returns the model index of the next/prev differing line, or -1. Wraps around. */
		int findNextDiff(int current, boolean forward) {
			int size = myModel.size();
			if (size == 0) return -1;
			int start = (current < 0) ? (forward ? -1 : size) : current;
			if (forward) {
				for (int i = start + 1; i < size; i++) {
					if (isDiffRow(i)) return i;
				}
				for (int i = 0; i <= start && i < size; i++) {
					if (isDiffRow(i)) return i;
				}
			} else {
				for (int i = start - 1; i >= 0; i--) {
					if (isDiffRow(i)) return i;
				}
				for (int i = size - 1; i >= start && i >= 0; i--) {
					if (isDiffRow(i)) return i;
				}
			}
			return -1;
		}

		private boolean isDiffRow(int index) {
			Object obj = myModel.get(index);
			return obj instanceof NumberedDiffLine && ((NumberedDiffLine) obj).diff.isDifferent;
		}
	}

	private static class DiffLineCellRenderer extends DefaultListCellRenderer {
		private static final long serialVersionUID = 1L;

		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index,
				boolean isSelected, boolean cellHasFocus) {
			JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			boolean dark = App.isCurrentlyDark();

			if (value instanceof SeparatorRow) {
				label.setHorizontalAlignment(SwingConstants.CENTER);
				label.setFont(label.getFont().deriveFont(Font.ITALIC));
				label.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
				if (!isSelected) {
					label.setBackground(dark ? new Color(55, 55, 55) : new Color(190, 190, 190));
					label.setForeground(dark ? new Color(190, 190, 190) : new Color(50, 50, 50));
				}
				label.setText(((SeparatorRow) value).label);
				return label;
			}

			NumberedDiffLine ndl = (NumberedDiffLine) value;
			Hl7DiffComparator.DiffLine diff = ndl.diff;

			label.setHorizontalAlignment(SwingConstants.LEFT);
			label.setBorder(BorderFactory.createEmptyBorder(1, 6, 1, 6));
			if (!isSelected) {
				label.setBackground(diff.isDifferent
					? (dark ? new Color(70, 25, 25) : new Color(255, 232, 232))
					: list.getBackground());
			}

			String numColor = dark ? "#666666" : "#aaaaaa";
			String fieldBg  = dark ? "#c07000" : "#ffcc00";

			StringBuilder html = new StringBuilder("<html><body>");
			html.append("<font color='").append(numColor).append("'>")
				.append(String.format("%4d", ndl.lineNumber))
				.append("&nbsp;&nbsp;</font>");

			if (diff.isDifferent && diff.diffSegments != null && !diff.diffSegments.isEmpty()) {
				for (Hl7DiffComparator.DiffSegment seg : diff.diffSegments) {
					String e = esc(seg.text);
					if (seg.isDifferent && !seg.text.isEmpty()) {
						html.append("<span style='background-color:").append(fieldBg)
							.append(";font-weight:bold;'>").append(e).append("</span>");
					} else {
						html.append(e);
					}
				}
			} else {
				html.append(esc(diff.line));
			}

			html.append("</body></html>");
			label.setText(html.toString());
			return label;
		}

		private static String esc(String s) {
			return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
		}
	}

	// -------------------------------------------------------------------------
	// Toolbar / footer / plumbing
	// -------------------------------------------------------------------------

	private JPanel createToolbarPanel() {
		JPanel panel = new JPanel();
		panel.setBorder(new EmptyBorder(5, 5, 5, 5));
		GridBagLayout gbl = new GridBagLayout();
		gbl.columnWidths  = new int[]    { 0, 0, 0, 0, 0, 0, 0, 0, 100, 0, 0, 0 };
		gbl.rowHeights    = new int[]    { 0 };
		gbl.columnWeights = new double[] { 0, 0, 0, 0, 0, 0, 0, 0, 1.0, 0, 0, 0 };
		gbl.rowWeights    = new double[] { Double.MIN_VALUE };
		panel.setLayout(gbl);

		myBeginButton = new JButton("Begin");
		myBeginButton.setIcon(ImageFactory.getStartAll());
		myBeginButton.setFont(myBeginButton.getFont().deriveFont(12f));
		myBeginButton.addActionListener(e -> { clearResults(); switchToInputMode(); myController.begin(); });
		panel.add(myBeginButton, gbc(0, new Insets(0, 5, 0, 10)));

		myStopButton = new JButton("Stop");
		myStopButton.setIcon(ImageFactory.getStopAll());
		myStopButton.setFont(myStopButton.getFont().deriveFont(12f));
		myStopButton.addActionListener(e -> myController.cancel());
		panel.add(myStopButton, gbc(1, new Insets(0, 0, 0, 10)));

		myEditAgainButton = new JButton("Edit Again");
		myEditAgainButton.addActionListener(e -> switchToInputMode());
		myEditAgainButton.setVisible(false);
		panel.add(myEditAgainButton, gbc(2, new Insets(0, 0, 0, 16)));

		myPrevDiffButton = new JButton("▲ Prev");
		myPrevDiffButton.setToolTipText("Jump to previous difference");
		myPrevDiffButton.setEnabled(false);
		myPrevDiffButton.addActionListener(e -> navigateDiff(false));
		panel.add(myPrevDiffButton, gbc(3, new Insets(0, 0, 0, 2)));

		myNextDiffButton = new JButton("▼ Next");
		myNextDiffButton.setToolTipText("Jump to next difference");
		myNextDiffButton.setEnabled(false);
		myNextDiffButton.addActionListener(e -> navigateDiff(true));
		panel.add(myNextDiffButton, gbc(4, new Insets(0, 0, 0, 16)));

		panel.add(new JLabel("Mode:"), gbc(5, new Insets(0, 0, 0, 4)));

		myTextModeButton = new JRadioButton("Text");
		myTextModeButton.setSelected(true);
		myTextModeButton.addActionListener(e -> onModeChanged());
		panel.add(myTextModeButton, gbc(6, new Insets(0, 0, 0, 2)));

		myFileModeButton = new JRadioButton("File");
		myFileModeButton.addActionListener(e -> onModeChanged());
		panel.add(myFileModeButton, gbc(7, new Insets(0, 0, 0, 0)));

		ButtonGroup modeGroup = new ButtonGroup();
		modeGroup.add(myTextModeButton);
		modeGroup.add(myFileModeButton);

		// column 8: spacer, takes remaining width

		panel.add(new JLabel("On Difference:"), gbc(9, new Insets(0, 10, 0, 5)));

		myStopOnFirstErrorCheck = new JCheckBox("Stop on First Difference");
		myStopOnFirstErrorCheck.setSelected(Prefs.getInstance().getHl7V2DiffStopOnFirstError());
		myStopOnFirstErrorCheck.addChangeListener(e ->
			Prefs.getInstance().setHl7V2DiffStopOnFirstError(myStopOnFirstErrorCheck.isSelected()));
		panel.add(myStopOnFirstErrorCheck, gbc(10, new Insets(0, 0, 0, 10)));

		myShowWholeMessageOnErrorCheckbox = new JCheckBox("Show Full Messages on Error");
		myShowWholeMessageOnErrorCheckbox.setSelected(Prefs.getInstance().getHl7V2DiffShowWholeMessageOnError());
		myShowWholeMessageOnErrorCheckbox.addActionListener(e ->
			Prefs.getInstance().setHl7V2DiffShowWholeMessageOnError(myShowWholeMessageOnErrorCheckbox.isSelected()));
		panel.add(myShowWholeMessageOnErrorCheckbox, gbc(11, new Insets(0, 0, 0, 5)));

		return panel;
	}

	private static GridBagConstraints gbc(int x, Insets insets) {
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = x; c.gridy = 0; c.anchor = GridBagConstraints.WEST; c.insets = insets;
		return c;
	}

	private JPanel createFooterPanel() {
		JPanel panel = new JPanel(new BorderLayout(10, 5));
		panel.setBorder(new EmptyBorder(5, 5, 5, 5));

		myProgressBar = new JProgressBar();
		myProgressBar.setStringPainted(true);
		myProgressBar.setOpaque(true);
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

	public boolean isShowWholeMessageOnError()              { return myShowWholeMessageOnErrorCheckbox.isSelected(); }
	public Hl7V2FileDiffController.InputMode getInputMode() { return myInputMode; }
	public String getFile1Text()                            { return myFile1PathField != null ? myFile1PathField.getText().trim() : ""; }
	public String getFile2Text()                            { return myFile2PathField != null ? myFile2PathField.getText().trim() : ""; }
	public String getPane1Text()                            { return myPane1TextArea != null && myPane1TextArea.getText() != null ? myPane1TextArea.getText() : ""; }
	public String getPane2Text()                            { return myPane2TextArea != null && myPane2TextArea.getText() != null ? myPane2TextArea.getText() : ""; }
	public boolean isStopOnFirstError()                     { return myStopOnFirstErrorCheck.isSelected(); }

	private void initListeners() {
		myOutputListModel = new OutputListModel();
		myController.addPropertyChangeListener(Hl7V2FileDiffController.PROP_RUNNING,      e -> { updateButtonStates(); updateProgress(); });
		myController.addPropertyChangeListener(Hl7V2FileDiffController.PROP_FAILED,       e -> updateProgress());
		myController.addPropertyChangeListener(Hl7V2FileDiffController.PROP_PERCENT_DONE, e -> updateProgress());
	}

	private void updateProgress() {
		int pct = myController.getPercentDone();
		myProgressBar.setValue(pct);
		if (myController.isRunning()) {
			myProgressBar.setString("Working - " + pct + "%");
		} else if (myController.isFailed() && myTotalMsgDiffs == 0) {
			myProgressBar.setString("Failed");
		} else if (pct > 0 && !myController.isRunning()) {
			// Comparison finished - show summary
			int total = myController.getNumMessagesTotal();
			if (myTotalMsgDiffs == 0) {
				myProgressBar.setString("No differences found  (" + total + " message" + (total == 1 ? "" : "s") + " compared)");
			} else {
				myProgressBar.setString(myTotalMsgDiffs + " of " + total + " message" + (total == 1 ? "" : "s") + " differ  ·  "
					+ myTotalFieldDiffs + " field difference" + (myTotalFieldDiffs == 1 ? "" : "s"));
			}
		} else {
			myProgressBar.setString("");
		}
	}

	private void updateButtonStates() {
		boolean running = myController.isRunning();
		myShowWholeMessageOnErrorCheckbox.setEnabled(!running);
		myStopButton.setEnabled(running);
		myStopOnFirstErrorCheck.setEnabled(!running);
		myTextModeButton.setEnabled(!running);
		myFileModeButton.setEnabled(!running);
		boolean hasInput = myInputMode == Hl7V2FileDiffController.InputMode.FILE
			? StringUtils.isNotBlank(getFile1Text()) && StringUtils.isNotBlank(getFile2Text())
			: StringUtils.isNotBlank(getPane1Text()) && StringUtils.isNotBlank(getPane2Text());
		myBeginButton.setEnabled(hasInput && !running);
	}

	private void openLogWindow() {
		if (myLogWindow != null && myLogWindow.isVisible()) { myLogWindow.toFront(); return; }
		myLogWindow = new JDialog(this, "HL7 Diff Log", false);
		myLogWindow.setSize(600, 400);
		myLogWindow.setLocationRelativeTo(this);
		JList<String> logList = new JList<>(myOutputListModel);
		logList.setFont(new Font("Monospaced", Font.PLAIN, 11));
		logList.setCellRenderer(new ColoredOutputRenderer());
		myLogWindow.add(new JScrollPane(logList), BorderLayout.CENTER);
		myLogWindow.setVisible(true);
	}

	public class OutputListModel extends AbstractListModel<String> {
		private static final long serialVersionUID = 1L;
		OutputListModel() {
			myController.addPropertyChangeListener(Hl7V2FileDiffController.PROP_OUTPUT,
				e -> fireContentsChanged(OutputListModel.this, 0, getSize()));
		}
		@Override public int    getSize()           { return myController.getOutputSize(); }
		@Override public String getElementAt(int i) { String s = myController.getOutputLine(i); return s != null ? s : ""; }
	}

	private static class ColoredOutputRenderer extends DefaultListCellRenderer {
		private static final long serialVersionUID = 1L;
		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			JLabel comp = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			if (value != null && !isSelected) {
				String t = value.toString();
				if      (t.contains("No differences found"))                                          comp.setForeground(new Color(0, 128, 0));
				else if (t.startsWith("< "))                                                          comp.setForeground(new Color(178, 34, 34));
				else if (t.startsWith("> "))                                                          comp.setForeground(new Color(34, 139, 34));
				else if (t.contains("Difference Found")||t.contains("FAILED")||t.contains("error:")) comp.setForeground(new Color(204, 0, 0));
				else if (t.contains("Beginning comparison"))                                          comp.setForeground(new Color(0, 102, 204));
				else if (t.contains("Control ID")||t.contains("Message"))                            comp.setForeground(new Color(204, 102, 0));
				else if (t.startsWith("<html>"))                                                      comp.setForeground(new Color(204, 0, 0));
				else                                                                                  comp.setForeground(list.getForeground());
			}
			return comp;
		}
	}
}
