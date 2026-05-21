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
package ca.uhn.hl7v2.testpanel.util;

public class FormatUtil {

	private static final String COLOR_KEYWORD  = "#3333EE"; // segment names
	private static final String COLOR_DELIM    = "#A0A0A0"; // delimiters
	private static final String COLOR_NUMBER   = "#990033"; // numbers
	private static final String COLOR_ESCAPE   = "#00A000"; // escape sequences

	public static String formatEr7(String theEr7, boolean theIsType) {
		StringBuilder b = new StringBuilder();
		b.append("<html>");

		char[] chars = theEr7.toCharArray();
		int i = 0;
		boolean atLineStart = !theIsType;

		while (i < chars.length) {
			char c = chars[i];

			// Newline — reset to line-start state
			if (c == '\r' || c == '\n') {
				b.append("<br>");
				i++;
				atLineStart = true;
				continue;
			}

			// Segment name at start of line: 3 letter/digit chars followed by | or end
			if (atLineStart && i + 2 < chars.length
					&& Character.isLetter(c)
					&& Character.isLetterOrDigit(chars[i + 1])
					&& Character.isLetterOrDigit(chars[i + 2])) {
				int after = i + 3;
				if (after >= chars.length || chars[after] == '|' || chars[after] == '\r' || chars[after] == '\n') {
					b.append("<span style=\"color: ").append(COLOR_KEYWORD).append(";\">");
					b.append(chars, i, 3);
					b.append("</span>");
					i += 3;
					atLineStart = false;
					continue;
				}
			}
			atLineStart = false;

			// Delimiter
			if (c == '|' || c == '^' || c == '&' || c == '~') {
				b.append("<span style=\"color: ").append(COLOR_DELIM).append(";\">");
				b.append(c);
				b.append("</span>");
				i++;
				continue;
			}

			// Escape sequence \...\
			if (c == '\\') {
				int escEnd = i + 1;
				while (escEnd < chars.length && chars[escEnd] != '\\') {
					escEnd++;
				}
				if (escEnd < chars.length) {
					escEnd++; // include closing backslash
				}
				b.append("<span style=\"color: ").append(COLOR_ESCAPE).append(";\">");
				b.append(theEr7, i, escEnd);
				b.append("</span>");
				i = escEnd;
				continue;
			}

			// Number: leading digit or '-' followed by digits/dots until a delimiter or end
			if (Character.isDigit(c) || (c == '-' && i + 1 < chars.length && Character.isDigit(chars[i + 1]))) {
				int numEnd = i;
				while (numEnd < chars.length && (Character.isDigit(chars[numEnd]) || chars[numEnd] == '.' || chars[numEnd] == '-')) {
					numEnd++;
				}
				b.append("<span style=\"color: ").append(COLOR_NUMBER).append(";\">");
				b.append(theEr7, i, numEnd);
				b.append("</span>");
				i = numEnd;
				continue;
			}

			// Plain text
			b.append(c);
			i++;
		}

		b.append("</html>");
		return b.toString();
	}

}
