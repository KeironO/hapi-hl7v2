package ca.uhn.hl7v2.testpanel.ui.tools;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;

import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;

public class LineNumberPanel extends JPanel implements DocumentListener {
	private static final long serialVersionUID = 1L;
	private JEditorPane myEditorPane;
	private int myDigits = 1;
	private static final int MARGIN = 4;

	public LineNumberPanel(JEditorPane editorPane) {
		myEditorPane = editorPane;
		myEditorPane.getDocument().addDocumentListener(this);
		updateUI();
		setPreferredSize(new Dimension(50, 0));
	}

	@Override
	public void updateUI() {
		super.updateUI();
		Color bg = UIManager.getColor("EditorPane.background");
		if (bg != null) {
			float[] hsb = Color.RGBtoHSB(bg.getRed(), bg.getGreen(), bg.getBlue(), null);
			float adj = hsb[2] > 0.5f ? -0.06f : 0.06f;
			setBackground(Color.getHSBColor(hsb[0], hsb[1], Math.max(0f, Math.min(1f, hsb[2] + adj))));
		}
		Color fg = UIManager.getColor("Label.foreground");
		if (fg != null) setForeground(fg);
		Font labelFont = UIManager.getFont("Label.font");
		int uiFontSize = labelFont != null ? labelFont.getSize() : 12;
		setFont(new Font(Font.MONOSPACED, Font.PLAIN, uiFontSize));
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		FontMetrics fm = g.getFontMetrics();
		int fontHeight = fm.getHeight();
		int baseline = fm.getAscent();

		int lineCount = getLineCount();
		myDigits = String.valueOf(lineCount).length();
		int width = fm.charWidth('0') * Math.max(myDigits, 2) + MARGIN * 2;
		setPreferredSize(new Dimension(width, 0));

		int y = baseline;
		for (int line = 1; line <= lineCount; line++) {
			String lineNumber = String.valueOf(line);
			g.drawString(lineNumber, MARGIN, y);
			y += fontHeight;
		}
	}

	private int getLineCount() {
		int count = 1;
		String text = myEditorPane.getText();
		for (int i = 0; i < text.length(); i++) {
			if (text.charAt(i) == '\n') {
				count++;
			}
		}
		return count;
	}

	@Override
	public void insertUpdate(DocumentEvent e) {
		repaint();
	}

	@Override
	public void removeUpdate(DocumentEvent e) {
		repaint();
	}

	@Override
	public void changedUpdate(DocumentEvent e) {
		repaint();
	}
}
