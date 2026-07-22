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
package ca.uhn.hl7v2.testpanel.model.msg;

import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Composite;
import ca.uhn.hl7v2.model.Group;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.Primitive;
import ca.uhn.hl7v2.model.Segment;
import ca.uhn.hl7v2.model.Structure;
import ca.uhn.hl7v2.model.Type;
import ca.uhn.hl7v2.model.Varies;
import ca.uhn.hl7v2.model.primitive.ID;
import ca.uhn.hl7v2.model.primitive.IS;
import ca.uhn.hl7v2.parser.DefaultXMLParser;
import ca.uhn.hl7v2.parser.EncodingCharacters;
import ca.uhn.hl7v2.parser.PipeParser;
import ca.uhn.hl7v2.testpanel.model.conf.ConformanceComposite;
import ca.uhn.hl7v2.testpanel.model.conf.ConformancePrimitive;
import ca.uhn.hl7v2.testpanel.util.FieldTooltipBuilder;
import ca.uhn.hl7v2.testpanel.util.Range;
import ca.uhn.hl7v2.testpanel.util.SegmentAndComponentPath;
import ca.uhn.hl7v2.testpanel.xsd.Hl7V2EncodingTypeEnum;

public class Hl7V2MessageEr7 extends Hl7V2MessageBase {
	private static final Logger ourLog = LoggerFactory.getLogger(Hl7V2MessageEr7.class);
	private String myHighlitedPath;
	private Range myHighlitedRange;
	private ArrayList<Segment> mySegmentIndexes = new ArrayList<Segment>();
	private ArrayList<Range> mySegmentRanges = new ArrayList<Range>();
	private ArrayList<String> mySegmentTerserPaths = new ArrayList<String>();

	public Hl7V2MessageEr7() {
		super();
	}

	public Hl7V2MessageEr7(int theIndexWithinCollection) throws PropertyVetoException {
		this();

		setIndexWithinCollection(theIndexWithinCollection);
	}

	@Override
	public Hl7V2MessageBase asEncoding(Hl7V2EncodingTypeEnum theEncoding) {
		switch (theEncoding) {
		case ER_7:
			return this;
		case XML:
		default:
			Hl7V2MessageXml retVal = new Hl7V2MessageXml();
			try {
				retVal.setSourceMessage(new DefaultXMLParser().encode(getParsedMessage()));
			} catch (PropertyVetoException e) {
				ourLog.error("Failed to create XML message", e);
			} catch (HL7Exception e) {
				ourLog.error("Failed to create XML message", e);
			}

			return retVal;
		}

	}

	public void clearHighlight() {
		setHighlitedRangeBasedOnSegment((Segment[])null);
		setHighlitedPathBasedOnRange(null);
	}

	/**
	 * @return the highlitedPath
	 */
	public String getHighlitedPath() {
		return myHighlitedPath;
	}

	public Range getHighlitedRange() {
		return myHighlitedRange;
	}

	public int getLineIndex(Segment theSegment) {
		int index = 0;
		for (Segment next : mySegmentIndexes) {
			if (next == theSegment) {
				return index;
			} else {
				index++;
			}
		}

		ourLog.info("getLineIndex: segment {} not found in {} indexed segments (name={})", System.identityHashCode(theSegment), mySegmentIndexes.size(), theSegment.getName());
		return -1;
	}

	List<Range> getSegmentRanges() {
		return Collections.unmodifiableList(mySegmentRanges);
	}

	protected void recalculateIndexes() {
		String[] lines = getSourceMessage().split("\\r");
		ourLog.info("recalculateIndexes: split into {} lines, source length={}", lines.length, getSourceMessage().length());
		for (int i = 0; i < lines.length && i < 10; i++) {
			ourLog.info("  line[{}]: {} chars, starts with '{}'", i, lines[i].length(), lines[i].length() >= 3 ? lines[i].substring(0, 3) : lines[i]);
		}

		// Pre-scan all lines to build a map of segment name -> list of [startOffset, endOffset].
		// This allows segments to appear in any order in the source text, not just
		// the order declared in the message structure definition.
		Map<String, List<int[]>> segmentLocations = new LinkedHashMap<>();
		int offset = 0;
		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];
			if (line.length() >= 3) {
				String segmentName = line.substring(0, 3);
				int start = offset;
				int end = offset + line.length(); // end is inclusive, covers the trailing \r position
				segmentLocations.computeIfAbsent(segmentName, k -> new ArrayList<>())
						.add(new int[] { start, end });
			}
			offset += line.length() + 1; // +1 for \r separator
		}

		mySegmentIndexes.clear();
		mySegmentRanges.clear();
		mySegmentTerserPaths.clear();

		Map<String, int[]> segmentCounters = new HashMap<>();

		ourLog.info("Pre-scanned segment locations: {}", segmentLocations.keySet());

		try {
			recalculateIndexes(segmentLocations, segmentCounters, getParsedMessage(), "");
		} catch (HL7Exception e) {
			ourLog.error("Failed to calculate message segment indexes", e);
		}

		ourLog.info("Indexed {} segments: {}", mySegmentIndexes.size(), mySegmentTerserPaths);
	}

	private void recalculateIndexes(Map<String, List<int[]>> theSegmentLocations, Map<String, int[]> theSegmentCounters, Group theGroup, String thePath) throws HL7Exception {
		for (String nextName : theGroup.getNames()) {
			// Force creation of at least one rep so that getAll returns
			// the same Segment references the tree builder sees.
			theGroup.get(nextName);
			Structure[] reps = theGroup.getAll(nextName);

			if (theGroup.isGroup(nextName)) {
				int repIndex = 1;
				for (Structure structure : reps) {
					String nextPath = thePath + "/" + nextName + (repIndex > 1 ? "(" + repIndex + ")" : "");
					repIndex++;
					recalculateIndexes(theSegmentLocations, theSegmentCounters, (Group) structure, nextPath);
				}
				continue;
			}

			// It's a segment - look up locations from the pre-scanned map
			List<int[]> locations = theSegmentLocations.get(nextName);
			if (locations == null || locations.isEmpty()) {
				continue; // Segment not present in message text
			}

			int[] counter = theSegmentCounters.computeIfAbsent(nextName, k -> new int[] { 0 });

			for (int i = 0; i < reps.length; i++) {
				if (counter[0] >= locations.size()) {
					break; // No more occurrences of this segment in the text
				}

				int[] loc = locations.get(counter[0]);
				counter[0]++;

				int repIndex = i + 1;
				String nextPath = thePath + "/" + nextName + (repIndex > 1 ? "(" + repIndex + ")" : "");
				mySegmentTerserPaths.add(nextPath);
				mySegmentIndexes.add((Segment) reps[i]);
				mySegmentRanges.add(new Range(loc[0], loc[1]));
			}
		}
	}

	public void setHighlitedField(SegmentAndComponentPath theField) {

		if (theField == null) {
			setHighlitedPathBasedOnRange(null);
			return;
		}

		int lineIndex = getLineIndex(theField.getSegment());
		if (lineIndex == -1) {
			setHighlitedPathBasedOnRange(null);
			return;
		}

		Range segmentRange = mySegmentRanges.get(lineIndex);
		String sourceMessage = getSourceMessage();
		Message parsedMessage = getParsedMessage();

		Range currentRange = findFieldRange(theField.getComponentPath(), theField.getRepNum(), segmentRange, sourceMessage, parsedMessage);
		setHighlitedPathBasedOnRange(currentRange);
		myHighlitedRange = currentRange;

	}

	public void setHighlitedPathBasedOnRange(Range theRange) {

		if (theRange == null) {
			myHighlitedPath = null;
			return;
		}

		int dot = theRange.getStart();

		int dotIndex = -1;
		Range segmentRange = null;
		for (int i = 0; i < mySegmentRanges.size(); i++) {
			segmentRange = mySegmentRanges.get(i);
			if (segmentRange != null && segmentRange.contains(dot)) {
				dotIndex = i;
				break;
			}
		}

		if (dotIndex == -1) {
			return;
		}

		EncodingCharacters enc;
		try {
			enc = EncodingCharacters.getInstance(getParsedMessage());
		} catch (HL7Exception e) {
			ourLog.error("Failed to find field", e);
			return;
		}

		int fieldIndex = 0;
		int cmpIndex = 0;
		int subCmpIndex = 0;
		int repIndex = 0;
		for (int i = segmentRange.getStart() + 1; i <= segmentRange.getEnd() && i <= dot && i <= getSourceMessage().length(); i++) {
			char nextChar = getSourceMessage().charAt(i - 1);
			if (nextChar == enc.getRepetitionSeparator()) {
				repIndex++;
				cmpIndex = 1;
				subCmpIndex = 1;
			} else if (nextChar == enc.getFieldSeparator()) {
				fieldIndex++;
				repIndex = 0;
				cmpIndex = 1;
				subCmpIndex = 1;
			} else if (nextChar == enc.getComponentSeparator()) {
				cmpIndex++;
				subCmpIndex = 1;
			} else if (nextChar == enc.getSubcomponentSeparator()) {
				subCmpIndex++;
			}
		}

		Segment segment = mySegmentIndexes.get(dotIndex);
		if (segment.getName().equals("MSH")) {
			fieldIndex++;
			if (fieldIndex == 2) {
				cmpIndex = 1;
				subCmpIndex = 1;
				repIndex = 0;
			}
		}

		try {
			if (fieldIndex > 0) {
				Type type = segment.getField(fieldIndex, 0);
				if (type instanceof Varies) {
					type = ((Varies) type).getData();
				}
				if (type instanceof Composite) {
					Composite composite = (Composite) type;
					if (subCmpIndex == 1) {
						Type subComponent = composite.getComponent(1);
						if (subComponent instanceof Varies) {
							subComponent = ((Varies) subComponent).getData();
						}
						if (subComponent instanceof Primitive) {
							subCmpIndex = 0;
						}
					}
				} else if (cmpIndex == 1) {
					cmpIndex = 0;
				}
			}

		} catch (HL7Exception e) {
			ourLog.error("Failed to retrieve field", e);
		}

		String basePath = mySegmentTerserPaths.get(dotIndex);
		StringBuilder fullPathB = new StringBuilder(basePath);
		if (fieldIndex >= 1) {
			fullPathB.append('-').append(fieldIndex);
			if (repIndex > 0) {
				fullPathB.append('(');
				fullPathB.append(repIndex + 1);
				fullPathB.append(')');
			}
			if (cmpIndex >= 1) {
				fullPathB.append('-').append(cmpIndex);
				if (subCmpIndex >= 1) {
					fullPathB.append('-').append(subCmpIndex);
				}
			}
		}
		String fullPath = fullPathB.toString();

		/*
		 * The encoding characters in MSH-2 (^~&\ ) are indistinguishable from actual
		 * separator characters during the raw-text scanner pass above. Strip any
		 * spurious component / repetition / subcomponent indices.
		 */
		if (fullPath.startsWith("/MSH-2") && fullPath.length() > 6) {
			char next = fullPath.charAt(6);
			if (next == '-' || next == '(') {
				fullPath = "/MSH-2";
			}
		}

		ourLog.info("Highlited path is now: " + fullPath);

		myHighlitedPath = fullPath;
	}

	public String getFieldTooltipHtmlAtOffset(int dot) {
		if (dot < 0 || mySegmentRanges.isEmpty()) {
			return null;
		}

		int dotIndex = -1;
		Range segmentRange = null;
		for (int i = 0; i < mySegmentRanges.size(); i++) {
			segmentRange = mySegmentRanges.get(i);
			if (segmentRange != null && segmentRange.contains(dot)) {
				dotIndex = i;
				break;
			}
		}
		if (dotIndex == -1) {
			return null;
		}

		EncodingCharacters enc;
		try {
			enc = EncodingCharacters.getInstance(getParsedMessage());
		} catch (HL7Exception e) {
			return null;
		}

		int fieldIndex = 0;
		int cmpIndex = 0;
		int subCmpIndex = 0;
		int repIndex = 0;
		for (int i = segmentRange.getStart() + 1; i <= segmentRange.getEnd() && i <= dot && i <= getSourceMessage().length(); i++) {
			char nextChar = getSourceMessage().charAt(i - 1);
			if (nextChar == enc.getRepetitionSeparator()) {
				repIndex++;
				cmpIndex = 1;
				subCmpIndex = 1;
			} else if (nextChar == enc.getFieldSeparator()) {
				fieldIndex++;
				repIndex = 0;
				cmpIndex = 1;
				subCmpIndex = 1;
			} else if (nextChar == enc.getComponentSeparator()) {
				cmpIndex++;
				subCmpIndex = 1;
			} else if (nextChar == enc.getSubcomponentSeparator()) {
				subCmpIndex++;
			}
		}

		Segment segment = mySegmentIndexes.get(dotIndex);
		if (segment.getName().equals("MSH")) {
			fieldIndex++;
			if (fieldIndex == 2) {
				cmpIndex = 1;
				subCmpIndex = 1;
				repIndex = 0;
			}
		}

		// Hovering over the segment name itself
		if (fieldIndex == 0) {
			return "<html><b>" + FieldTooltipBuilder.escapeHtml(segment.getName()) + "</b> &mdash; Segment</html>";
		}

		try {
			int repToFetch = repIndex > 0 ? repIndex : 0;
			Type type = segment.getField(fieldIndex, repToFetch);
			if (type instanceof Varies) {
				type = ((Varies) type).getData();
			}

			// Resolve component/subcomponent
			if (type instanceof Composite) {
				Composite composite = (Composite) type;
				int effectiveCmp = cmpIndex >= 1 ? cmpIndex - 1 : 0;
				if (effectiveCmp < composite.getComponents().length) {
					Type cmpType = composite.getComponent(effectiveCmp);
					if (cmpType instanceof Varies) {
						cmpType = ((Varies) cmpType).getData();
					}
					if (cmpType instanceof Primitive) {
						subCmpIndex = 0;
					}
					type = cmpType;
				}
			} else {
				cmpIndex = 0;
				subCmpIndex = 0;
			}

			// Build terser path string
			String basePath = mySegmentTerserPaths.get(dotIndex);
			StringBuilder pathB = new StringBuilder(basePath).append('-').append(fieldIndex);
			if (cmpIndex >= 1) {
				pathB.append('-').append(cmpIndex);
				if (subCmpIndex >= 1) {
					pathB.append('-').append(subCmpIndex);
				}
			}
			String path = pathB.toString();

			// Field display name from segment
			String fieldName = null;
			String[] names = segment.getNames();
			if (fieldIndex <= names.length) {
				fieldName = names[fieldIndex - 1];
			}

			// Metadata
			String typeName;
			Integer maxLength = null;
			String table = null;

			if (type instanceof ConformancePrimitive) {
				ConformancePrimitive cp = (ConformancePrimitive) type;
				typeName = cp.getConfDefinition().getDatatype();
				long len = cp.getConfDefinition().getLength();
				if (len > 0) {
					maxLength = (int) len;
				}
				String tbl = cp.getConfDefinition().getTable();
				if (StringUtils.isNotBlank(tbl)) {
					table = tbl;
				}
			} else if (type instanceof ConformanceComposite) {
				ConformanceComposite cc = (ConformanceComposite) type;
				typeName = cc.getConfDefinition().getDatatype();
				long len = cc.getConfDefinition().getLength();
				if (len > 0) {
					maxLength = (int) len;
				}
			} else {
				typeName = type.getClass().getSimpleName();
				if (type instanceof ID) {
					int tblNum = ((ID) type).getTable();
					if (tblNum > 0) {
						table = "HL7" + StringUtils.leftPad(Integer.toString(tblNum), 4, '0');
					}
				} else if (type instanceof IS) {
					int tblNum = ((IS) type).getTable();
					if (tblNum > 0) {
						table = "HL7" + StringUtils.leftPad(Integer.toString(tblNum), 4, '0');
					}
				}
			}

			boolean required = segment.isRequired(fieldIndex);
			boolean repeating = segment.getMaxCardinality(fieldIndex) != 1;

			// Field content
			String content;
			try {
				content = PipeParser.encode(type, enc);
			} catch (Exception ex) {
				content = null;
			}

			// Build tooltip HTML
			return new FieldTooltipBuilder()
					.path(path)
					.displayName(fieldName)
					.component(cmpIndex > 0)
					.content(content)
					.typeName(typeName)
					.required(required)
					.repeating(repeating)
					.maxLength(maxLength)
					.table(table)
					.build();

		} catch (HL7Exception e) {
			return null;
		}
	}

	public void setHighlitedRangeBasedOnSegment(Segment... theSegment) {
		if (theSegment == null || theSegment.length == 0) {
			ourLog.info("setHighlitedRangeBasedOnSegment: null/empty");
			myHighlitedRange = null;
		} else {

			myHighlitedRange = null;
			for (Segment segment : theSegment) {
				ourLog.info("setHighlitedRangeBasedOnSegment: segment name={}, identity={}", segment.getName(), System.identityHashCode(segment));
				int newSelectedIndex = theSegment != null ? getLineIndex(segment) : -1;
				ourLog.info("setHighlitedRangeBasedOnSegment: getLineIndex returned {}", newSelectedIndex);
				if (newSelectedIndex != -1) {
					Range nextRange = mySegmentRanges.get(newSelectedIndex);
					if (nextRange == null) {
						// nothing
					} else if (myHighlitedRange == null) {
						myHighlitedRange = nextRange;
					} else {
						myHighlitedRange = myHighlitedRange.overlay(nextRange);
					}
				}
			}

		}

	}

}
