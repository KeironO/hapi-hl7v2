package ca.uhn.hl7v2.testpanel.ui.tools;

import java.util.ArrayList;
import java.util.List;

public class Hl7DiffComparator {

	public static class DiffLine {
		public String line;
		public boolean isDifferent;
		public List<DiffSegment> diffSegments;

		public DiffLine(String line, boolean isDifferent) {
			this.line = line;
			this.isDifferent = isDifferent;
			this.diffSegments = new ArrayList<>();
		}
	}

	public static class DiffSegment {
		public String text;
		public boolean isDifferent;

		public DiffSegment(String text, boolean isDifferent) {
			this.text = text;
			this.isDifferent = isDifferent;
		}
	}

	public static DiffLine[] compareSides(String[] leftLines, String[] rightLines) {
		DiffLine[] result = new DiffLine[leftLines.length];

		for (int i = 0; i < leftLines.length; i++) {
			String leftLine = leftLines[i];
			String rightLine = i < rightLines.length ? rightLines[i] : "";

			boolean lineDifferent = !leftLine.equals(rightLine);
			DiffLine diffLine = new DiffLine(leftLine, lineDifferent);

			if (lineDifferent) {
				diffLine.diffSegments = parseAndCompareSegments(leftLine, rightLine);
			} else {
				diffLine.diffSegments.add(new DiffSegment(leftLine, false));
			}

			result[i] = diffLine;
		}

		return result;
	}

	private static List<DiffSegment> parseAndCompareSegments(String leftLine, String rightLine) {
		List<DiffSegment> segments = new ArrayList<>();

		String[] leftFields = leftLine.split("\\|", -1);
		String[] rightFields = rightLine.split("\\|", -1);

		for (int i = 0; i < Math.max(leftFields.length, rightFields.length); i++) {
			String leftField = i < leftFields.length ? leftFields[i] : "";
			String rightField = i < rightFields.length ? rightFields[i] : "";

			if (leftField.equals(rightField)) {
				segments.add(new DiffSegment(leftField, false));
			} else {
				segments.add(new DiffSegment(leftField, true));
			}

			if (i < Math.max(leftFields.length, rightFields.length) - 1) {
				segments.add(new DiffSegment("|", false));
			}
		}

		return segments;
	}

	public static String toHtml(DiffLine diffLine) {
		StringBuilder html = new StringBuilder("<html><code>");

		for (DiffSegment seg : diffLine.diffSegments) {
			if (seg.isDifferent) {
				html.append("<span style='background-color: yellow; text-decoration: underline;'>");
				html.append(escapeHtml(seg.text));
				html.append("</span>");
			} else {
				html.append(escapeHtml(seg.text));
			}
		}

		html.append("</code></html>");
		return html.toString();
	}

	private static String escapeHtml(String text) {
		return text.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;");
	}
}
