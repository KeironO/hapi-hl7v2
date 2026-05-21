package ca.uhn.hl7v2.testpanel.ui;

import javax.swing.text.Segment;

import org.fife.ui.rsyntaxtextarea.AbstractTokenMaker;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenMap;

/**
 * Tokenizer for HL7 v2 ER7-encoded messages.
 *
 * Token type mapping (matches original er7.flex colour intent):
 *   Segment names (e.g. MSH, PID)  -> RESERVED_WORD  (blue)
 *   Delimiters | ^ & ~ \           -> OPERATOR        (gray)
 *   Numbers before a delimiter     -> LITERAL_NUMBER_DECIMAL_INT (maroon)
 *   Escape sequences \X\           -> LITERAL_STRING_DOUBLE_QUOTE (green)
 *   Comments # ...                 -> COMMENT_EOL
 *   Everything else                -> IDENTIFIER
 */
public class Er7TokenMaker extends AbstractTokenMaker {

	private static final int STATE_INITIAL = Token.NULL;
	private static final int STATE_IN_VALUE = 1;

	@Override
	public TokenMap getWordsToHighlight() {
		return new TokenMap();
	}

	@Override
	public Token getTokenList(Segment text, int initialTokenType, int startOffset) {
		resetTokenList();

		char[] array = text.array;
		int offset = text.offset;
		int count = text.count;
		int end = offset + count;

		int currentTokenStart = offset;
		int currentTokenType = Token.NULL;

		// Track whether we are at the start of a line (expecting a segment name)
		boolean atLineStart = (initialTokenType == STATE_INITIAL);

		for (int i = offset; i < end; ) {
			char c = array[i];

			if (atLineStart) {
				// Expect a 3-character segment name at line start
				if (i + 2 < end && Character.isLetter(c)
						&& Character.isLetterOrDigit(array[i + 1])
						&& Character.isLetterOrDigit(array[i + 2])) {
					// Check it's followed by | or end-of-line
					int afterSeg = i + 3;
					if (afterSeg >= end || array[afterSeg] == '|' || array[afterSeg] == '\n' || array[afterSeg] == '\r') {
						emitToken(text, currentTokenStart, i - 1, currentTokenType, startOffset + currentTokenStart - offset);
						emitToken(text, i, i + 2, Token.RESERVED_WORD, startOffset + i - offset);
						i += 3;
						currentTokenStart = i;
						currentTokenType = Token.NULL;
						atLineStart = false;
						continue;
					}
				}
				// # comment
				if (c == '#') {
					emitToken(text, currentTokenStart, i - 1, currentTokenType, startOffset + currentTokenStart - offset);
					emitToken(text, i, end - 1, Token.COMMENT_EOL, startOffset + i - offset);
					i = end;
					currentTokenStart = end;
					currentTokenType = Token.NULL;
					atLineStart = false;
					continue;
				}
				atLineStart = false;
			}

			switch (c) {
				case '|':
				case '^':
				case '&':
				case '~':
					if (currentTokenType != Token.NULL) {
						emitToken(text, currentTokenStart, i - 1, currentTokenType, startOffset + currentTokenStart - offset);
					}
					emitToken(text, i, i, Token.OPERATOR, startOffset + i - offset);
					i++;
					currentTokenStart = i;
					currentTokenType = Token.NULL;
					break;

				case '\\':
					// Escape sequence: \something\
					if (currentTokenType != Token.NULL) {
						emitToken(text, currentTokenStart, i - 1, currentTokenType, startOffset + currentTokenStart - offset);
					}
					int escEnd = i + 1;
					while (escEnd < end && array[escEnd] != '\\') {
						escEnd++;
					}
					if (escEnd < end) {
						escEnd++; // include closing backslash
					}
					emitToken(text, i, escEnd - 1, Token.LITERAL_STRING_DOUBLE_QUOTE, startOffset + i - offset);
					i = escEnd;
					currentTokenStart = i;
					currentTokenType = Token.NULL;
					break;

				case '\n':
				case '\r':
					if (currentTokenType != Token.NULL) {
						emitToken(text, currentTokenStart, i - 1, currentTokenType, startOffset + currentTokenStart - offset);
					}
					addNullToken();
					return firstToken;

				default:
					// Number detection: digit or '-' or '.' at start of a value
					if (currentTokenType == Token.NULL && (Character.isDigit(c) || c == '-' || c == '.')) {
						currentTokenStart = i;
						currentTokenType = Token.LITERAL_NUMBER_DECIMAL_INT;
					} else if (currentTokenType == Token.LITERAL_NUMBER_DECIMAL_INT
							&& !Character.isDigit(c) && c != '.' && c != '-') {
						// No longer a number
						emitToken(text, currentTokenStart, i - 1, Token.LITERAL_NUMBER_DECIMAL_INT, startOffset + currentTokenStart - offset);
						currentTokenStart = i;
						currentTokenType = Token.IDENTIFIER;
					} else if (currentTokenType == Token.NULL) {
						currentTokenStart = i;
						currentTokenType = Token.IDENTIFIER;
					}
					i++;
					break;
			}
		}

		if (currentTokenStart < end && currentTokenType != Token.NULL) {
			emitToken(text, currentTokenStart, end - 1, currentTokenType, startOffset + currentTokenStart - offset);
		}

		addNullToken();
		return firstToken;
	}

	private void emitToken(Segment text, int start, int end, int tokenType, int startOffset) {
		if (start > end || tokenType == Token.NULL) {
			return;
		}
		addToken(text.array, start, end, tokenType, startOffset);
	}

	@Override
	public void addToken(char[] array, int start, int end, int tokenType, int startOffset) {
		super.addToken(array, start, end, tokenType, startOffset);
	}

}
