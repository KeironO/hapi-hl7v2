package ca.uhn.hl7v2.testpanel.util;

import org.apache.commons.lang.StringUtils;

/**
 * Builds consistent HTML tooltip content for HL7 field/component nodes,
 * shared between the message tree and the text editor.
 */
public class FieldTooltipBuilder {

	private String myPath;
	private String myDisplayName;
	private boolean myComponent;
	private String myContent;
	private String myTypeName;
	private Boolean myRequired;
	private Boolean myRepeating;
	private Integer myMaxLength;
	private String myTable;

	public FieldTooltipBuilder path(String path) {
		myPath = path;
		return this;
	}

	public FieldTooltipBuilder displayName(String displayName) {
		myDisplayName = displayName;
		return this;
	}

	public FieldTooltipBuilder component(boolean component) {
		myComponent = component;
		return this;
	}

	public FieldTooltipBuilder content(String content) {
		myContent = content;
		return this;
	}

	public FieldTooltipBuilder typeName(String typeName) {
		myTypeName = typeName;
		return this;
	}

	public FieldTooltipBuilder required(Boolean required) {
		myRequired = required;
		return this;
	}

	public FieldTooltipBuilder repeating(Boolean repeating) {
		myRepeating = repeating;
		return this;
	}

	public FieldTooltipBuilder maxLength(Integer maxLength) {
		myMaxLength = maxLength;
		return this;
	}

	public FieldTooltipBuilder table(String table) {
		myTable = table;
		return this;
	}

	public String build() {
		StringBuilder sb = new StringBuilder("<html><table cellpadding='2' cellspacing='0'>");

		sb.append("<tr><td colspan='2'><b>");
		if (StringUtils.isNotBlank(myPath)) {
			sb.append(escapeHtml(myPath));
		}
		if (StringUtils.isNotBlank(myDisplayName)) {
			sb.append(" ").append(escapeHtml(myDisplayName));
		}
		sb.append(" (").append(myComponent ? "Component" : "Field").append(")");
		sb.append("</b></td></tr>");

		sb.append("<tr><td><b>Content:</b></td><td>");
		if (StringUtils.isBlank(myContent)) {
			sb.append("<i>empty</i>");
		} else {
			String escaped = escapeHtml(myContent);
			if (escaped.length() > 80) {
				escaped = escaped.substring(0, 77) + "...";
			}
			sb.append(escaped);
		}
		sb.append("</td></tr>");

		if (StringUtils.isNotBlank(myTypeName)) {
			sb.append("<tr><td><b>Type:</b></td><td>").append(escapeHtml(myTypeName)).append("</td></tr>");
		}

		sb.append("<tr><td><b>Required:</b></td><td>")
				.append(myRequired == null ? "Unknown" : (myRequired ? "Yes" : "No"))
				.append("</td></tr>");

		sb.append("<tr><td><b>Repeating:</b></td><td>")
				.append(myRepeating == null ? "Unknown" : (myRepeating ? "Yes" : "No"))
				.append("</td></tr>");

		if (myMaxLength != null) {
			sb.append("<tr><td><b>Max Length:</b></td><td>").append(myMaxLength).append("</td></tr>");
		}

		sb.append("<tr><td><b>Table:</b></td><td>")
				.append(StringUtils.isNotBlank(myTable) ? escapeHtml(myTable) : "No")
				.append("</td></tr>");

		sb.append("</table></html>");
		return sb.toString();
	}

	public static String escapeHtml(String s) {
		if (s == null) return "";
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
}
