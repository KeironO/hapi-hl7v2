package ca.uhn.hl7v2.testpanel.ui.tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

public class VisualDiffPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	private JList<String> leftList;
	private JList<String> rightList;
	private SimpleListModel leftModel;
	private SimpleListModel rightModel;

	public VisualDiffPanel() {
		setLayout(new BorderLayout(5, 5));
		setBorder(new EmptyBorder(10, 10, 10, 10));

		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		splitPane.setResizeWeight(0.5);
		splitPane.setDividerSize(5);

		JPanel leftPanel = new JPanel(new BorderLayout());
		leftPanel.setBorder(new TitledBorder(null, "Expected / Left", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		leftModel = new SimpleListModel();
		leftList = new JList<>(leftModel);
		leftList.setFont(new Font("Monospaced", Font.PLAIN, 12));
		leftList.setCellRenderer(new DiffCellRenderer());
		JScrollPane leftScroll = new JScrollPane(leftList);
		LineNumberListPanel leftLineNumbers = new LineNumberListPanel(leftList);
		leftScroll.setRowHeaderView(leftLineNumbers);
		leftPanel.add(leftScroll, BorderLayout.CENTER);

		JPanel rightPanel = new JPanel(new BorderLayout());
		rightPanel.setBorder(new TitledBorder(null, "Actual / Right", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		rightModel = new SimpleListModel();
		rightList = new JList<>(rightModel);
		rightList.setFont(new Font("Monospaced", Font.PLAIN, 12));
		rightList.setCellRenderer(new DiffCellRenderer());
		JScrollPane rightScroll = new JScrollPane(rightList);
		LineNumberListPanel rightLineNumbers = new LineNumberListPanel(rightList);
		rightScroll.setRowHeaderView(rightLineNumbers);
		rightPanel.add(rightScroll, BorderLayout.CENTER);

		splitPane.setLeftComponent(leftPanel);
		splitPane.setRightComponent(rightPanel);

		add(splitPane, BorderLayout.CENTER);
	}

	public void displayDiff(String[] leftLines, String[] rightLines, boolean[] differences) {
		Hl7DiffComparator.DiffLine[] leftDiffs = Hl7DiffComparator.compareSides(leftLines, rightLines);
		Hl7DiffComparator.DiffLine[] rightDiffs = Hl7DiffComparator.compareSides(rightLines, leftLines);

		leftModel.setDiffLines(leftDiffs);
		rightModel.setDiffLines(rightDiffs);
	}

	public void clear() {
		leftModel.clear();
		rightModel.clear();
	}

	private static class SimpleListModel extends javax.swing.AbstractListModel<String> {
		private static final long serialVersionUID = 1L;
		private Hl7DiffComparator.DiffLine[] diffLines = new Hl7DiffComparator.DiffLine[0];

		void setDiffLines(Hl7DiffComparator.DiffLine[] newDiffLines) {
			this.diffLines = newDiffLines != null ? newDiffLines : new Hl7DiffComparator.DiffLine[0];
			fireContentsChanged(this, 0, this.diffLines.length);
		}

		void clear() {
			this.diffLines = new Hl7DiffComparator.DiffLine[0];
			fireContentsChanged(this, 0, 0);
		}

		@Override
		public int getSize() {
			return diffLines.length;
		}

		@Override
		public String getElementAt(int index) {
			if (index >= 0 && index < diffLines.length) {
				return diffLines[index].line;
			}
			return "";
		}

		Hl7DiffComparator.DiffLine getDiffLine(int index) {
			if (index >= 0 && index < diffLines.length) {
				return diffLines[index];
			}
			return null;
		}
	}

	private static class DiffCellRenderer extends DefaultListCellRenderer {
		private static final long serialVersionUID = 1L;
		private static final Color DIFF_COLOR = new Color(255, 220, 220);
		private static final Color SAME_COLOR = Color.WHITE;

		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			JLabel comp = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

			if (!isSelected && list.getModel() instanceof SimpleListModel) {
				SimpleListModel model = (SimpleListModel) list.getModel();
				Hl7DiffComparator.DiffLine diffLine = model.getDiffLine(index);

				if (diffLine != null && diffLine.isDifferent) {
					String htmlText = Hl7DiffComparator.toHtml(diffLine);
					comp.setText(htmlText);
					comp.setBackground(DIFF_COLOR);
					comp.setOpaque(true);
				} else {
					comp.setBackground(SAME_COLOR);
					comp.setOpaque(true);
				}
			}

			return comp;
		}
	}

	private static class LineNumberListPanel extends JPanel implements ListDataListener {
		private static final long serialVersionUID = 1L;
		private JList<?> myList;
		private static final int MARGIN = 4;

		LineNumberListPanel(JList<?> list) {
			myList = list;
			myList.getModel().addListDataListener(this);
			setBackground(new Color(240, 240, 240));
			setForeground(Color.BLACK);
			setFont(new Font("Monospaced", Font.PLAIN, 12));
			setPreferredSize(new Dimension(50, 0));
		}

		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);

			FontMetrics fm = g.getFontMetrics();
			int fontHeight = fm.getHeight();
			int baseline = fm.getAscent();

			int lineCount = myList.getModel().getSize();
			if (lineCount == 0) return;

			int width = fm.charWidth('0') * String.valueOf(lineCount).length() + MARGIN * 2;
			setPreferredSize(new Dimension(width, 0));

			int y = baseline;
			for (int line = 1; line <= lineCount; line++) {
				g.drawString(String.valueOf(line), MARGIN, y);
				y += fontHeight;
			}
		}

		@Override
		public void intervalAdded(ListDataEvent e) {
			repaint();
		}

		@Override
		public void intervalRemoved(ListDataEvent e) {
			repaint();
		}

		@Override
		public void contentsChanged(ListDataEvent e) {
			repaint();
		}
	}
}
