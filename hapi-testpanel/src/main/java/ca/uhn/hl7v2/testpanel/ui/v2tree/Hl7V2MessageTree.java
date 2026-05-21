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
/*
 * Created on October 17, 2001, 11:44 AM
 */
package ca.uhn.hl7v2.testpanel.ui.v2tree;

import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.BorderLayout;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.conf.ProfileException;
import ca.uhn.hl7v2.conf.check.DefaultValidator;
import ca.uhn.hl7v2.model.AbstractGroup;
import ca.uhn.hl7v2.model.Composite;
import ca.uhn.hl7v2.model.Group;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.Primitive;
import ca.uhn.hl7v2.model.Segment;
import ca.uhn.hl7v2.model.Structure;
import ca.uhn.hl7v2.model.Type;
import ca.uhn.hl7v2.model.Varies;
import ca.uhn.hl7v2.model.primitive.AbstractNumericPrimitive;
import ca.uhn.hl7v2.model.primitive.ID;
import ca.uhn.hl7v2.model.primitive.IS;
import ca.uhn.hl7v2.parser.EncodingCharacters;
import ca.uhn.hl7v2.parser.PipeParser;
import ca.uhn.hl7v2.testpanel.controller.Controller;
import ca.uhn.hl7v2.testpanel.model.UnknownMessage;
import ca.uhn.hl7v2.testpanel.model.conf.ConformanceComposite;
import ca.uhn.hl7v2.testpanel.model.conf.ConformanceGroup;
import ca.uhn.hl7v2.testpanel.model.conf.ConformanceMessage;
import ca.uhn.hl7v2.testpanel.model.conf.ConformancePrimitive;
import ca.uhn.hl7v2.testpanel.model.conf.ConformanceSegment;
import ca.uhn.hl7v2.testpanel.model.conf.TableFile;
import ca.uhn.hl7v2.testpanel.model.msg.AbstractMessage;
import ca.uhn.hl7v2.testpanel.model.msg.Comment;
import ca.uhn.hl7v2.testpanel.model.msg.Hl7V2MessageBase;
import ca.uhn.hl7v2.testpanel.model.msg.Hl7V2MessageCollection;
import ca.uhn.hl7v2.testpanel.ui.IDestroyable;
import ca.uhn.hl7v2.testpanel.ui.ImageFactory;
import ca.uhn.hl7v2.testpanel.ui.ShowEnum;
import ca.uhn.hl7v2.testpanel.util.SegmentAndComponentPath;
import ca.uhn.hl7v2.util.StringUtil;
import ca.uhn.hl7v2.validation.PrimitiveTypeRule;
import ca.uhn.hl7v2.validation.impl.DefaultValidation;
import ca.uhn.hl7v2.validation.impl.ValidationContextImpl;

/**
 * This is a Swing panel that displays the contents of a Message object in a
 * JTree. The tree currently only expands to the field level (components shown
 * as one node).
 *
 * @author Bryan Tripp (bryan_tripp@sourceforge.net)
 */
public class Hl7V2MessageTree extends JPanel implements IDestroyable {
	private static final DefaultValidation ourDefaultValidation = new DefaultValidation();

	private static final Logger ourLog = LoggerFactory.getLogger(Hl7V2MessageTree.class);
	private static final String TABLE_NAMESPACE_HL7 = "HL7";
	private static final String TBL = " ";
	private Controller myController;
	private boolean myCurrentlyEditing;
	private PropertyChangeListener myHighlitedPathListener;
	private PropertyChangeListener myMessageEncodingListener;
	private Hl7V2MessageCollection myMessages;
	private PropertyChangeListener myParsedMessagesListener;
	private PipeParser myPipeParser;
	private boolean myRespondingToManualRangeChange;
	private DefaultValidator myRuntimeProfileValidator;
	private boolean mySelectionHandlingDisabled;
	private boolean myShouldOpenDefaultPaths = true;

	private boolean myShowRep0 = true;

	private JTree myTree;

	private TreeNodeRoot myTop;

	private DefaultTreeModel myTreeModel;

	private ShowEnum myUnitTestShowMode;

	private UpdaterThread myUpdaterThread;

	private PropertyChangeListener myValidationContextListener;

	private IWorkingListener myWorkingListener;

	/** Creates new TreePanel */
	public Hl7V2MessageTree(Controller theController) {
		myController = theController;

		myPipeParser = new PipeParser();
		myPipeParser.setValidationContext(new ValidationContextImpl());

		setLayout(new BorderLayout());

		myTree = new JTree();
		myTree.setRootVisible(false);
		myTree.setRowHeight(20);
		myTree.setCellRenderer(new Hl7TreeCellRenderer());
		myTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		myTree.addTreeSelectionListener(new TreeSelectionListener() {
			@Override
			public void valueChanged(TreeSelectionEvent e) {
				if (!mySelectionHandlingDisabled) {
					TreePath path = e.getPath();
					if (path != null) {
						handleNewSelectedPath(path);
					}
				}
			}
		});
		myTree.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				handleKeyPress(e);
			}
		});

		myTree.setComponentPopupMenu(createTreeContextMenu());

		add(new JScrollPane(myTree), BorderLayout.CENTER);

		myHighlitedPathListener = new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent theEvt) {
				if (myController.isMessageEditorInFollowMode()) {
					if (Hl7V2MessageTree.this.hasFocus() == false) {
						synchronizeTreeWithHighlitedPath();
					}
				}
			}
		};

		myParsedMessagesListener = new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent theEvt) {
				myUpdaterThread.scheduleUpdate();
			}
		};

		myValidationContextListener = new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent theEvt) {
				myUpdaterThread.scheduleUpdate();
			}
		};

		myMessageEncodingListener = new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent theEvt) {
				myUpdaterThread.scheduleUpdate();
			}
		};

		myUpdaterThread = new UpdaterThread();
		myUpdaterThread.start();
	}

	private int addChidrenExtra(String theParentName, Type thePrimitive, TreeNodeBase treeParent, Segment theSegment, List<Integer> theComponentPath, String theTerserPath, int cpIndex, int index) throws InterruptedException, InvocationTargetException {
		// Extra components
		for (int i = 0; i < thePrimitive.getExtraComponents().numComponents(); i++) {
			Type nextType = thePrimitive.getExtraComponents().getComponent(i);
			String nextParentName = theParentName + "-" + (i + 1);

			// theComponentPath.set(cpIndex, Integer.valueOf(i + 1));
			String terserPath = theTerserPath + "-" + (i + 1);

			index = addChildren(nextParentName, treeParent, false, false, null, i, nextType, theSegment, theComponentPath, index, terserPath);
		}
		return index;
	}

	void addChildren() throws InterruptedException, InvocationTargetException {
		if (myMessages != null && myMessages.getRuntimeProfile() != null) {
			myRuntimeProfileValidator = new DefaultValidator();
			myRuntimeProfileValidator.setValidateChildren(false);
		}

		final Set<String> openPaths = getOpenPaths();

		int selectedRow = getSelectedRow();
		final String selectedPath = getPathAtRow(selectedRow);

		if (myMessages != null) {
			try {
				addChildren(myMessages.getMessages(), myTop, "");
			} catch (InterruptedException e) {
				ourLog.info("Interrupted during an update loop, going to schedule another pass");
				myUpdaterThread.scheduleUpdate();
			} catch (InvocationTargetException e) {
				ourLog.error("Failed up update message tree", e);
			}

			myTop.validate();

			EventQueue.invokeLater(new Runnable() {
				public void run() {
					myTreeModel.nodeStructureChanged(myTop);
				}
			});

		}

		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					mySelectionHandlingDisabled = true;
					ourLog.debug("Open paths are: {}", openPaths);
					if (openPaths.isEmpty() && myShouldOpenDefaultPaths) {
						ourLog.info("Opening default paths");
						for (int row = 0; row < myTree.getRowCount(); row++) {
							TreePath path = myTree.getPathForRow(row);
							Object component = path.getLastPathComponent();
							if (component instanceof TreeNodeMessage || component instanceof TreeNodeUnknown || component instanceof TreeNodeGroup) {
								myTree.expandPath(path);
							}
						}
						myShouldOpenDefaultPaths = false;
					} else {
						ourLog.info("Opening pre-existing paths: {} and selected path: {}", openPaths, selectedPath);
						expandPaths(openPaths, selectedPath);
					}
				} finally {
					mySelectionHandlingDisabled = false;
				}
			}
		});
		// if (selectedRow != -1) {
		// getSelectionModel().setSelectionInterval(selectedRow, selectedRow);
		// handleNewSelectedIndex(selectedRow);
		// }

	}

	/**
	 * Adds the children of the given group under the given tree node.
	 */
	void addChildren(Group messParent, TreeNodeBase treeParent, String theTerserPath) throws InterruptedException, InvocationTargetException {

		String[] childNames = messParent.getNames();
		int currChild = 0;
		for (int i = 0; i < childNames.length; i++) {

			try {
				String nextName = childNames[i];

				switch (getShowMode()) {
				case ALL:
				case ERROR:
					// case POPULATED:
					/*
					 * this creates at least one rep if there are none, since
					 * these modes want to show the node in the tree regardless
					 * of whether or not it has content
					 */
					messParent.get(nextName);
				default:
					// nothing
				}

				Structure[] childReps = messParent.getAll(nextName);
				boolean repeating = messParent.isRepeating(nextName);
				boolean required = messParent.isRequired(nextName);

				for (int j = 0; j < childReps.length; j++) {

					TreeNodeBase newNode = null;
					Structure nextStructure = childReps[j];
					String groupName = nextStructure.getName();

					String nextTerserPath = theTerserPath + "/" + groupName + (j > 0 ? ("(" + (j + 1) + ")") : "");

					if (nextStructure instanceof Group) {

						if (nextStructure instanceof ConformanceGroup) {
							newNode = new TreeNodeGroupConf((ConformanceGroup) nextStructure, groupName, j, repeating, required, nextTerserPath);
						} else {
							newNode = new TreeNodeGroup((Group) nextStructure, groupName, j, repeating, required, nextTerserPath);
						}

						addChildren((Group) nextStructure, newNode, nextTerserPath);

						newNode = insertOrReplaceWithExisting(treeParent, currChild, newNode);

					} else if (nextStructure instanceof Segment) {

						if (nextStructure instanceof ConformanceSegment) {
							newNode = new TreeNodeSegmentConf((ConformanceSegment) nextStructure, groupName, j, repeating, required, nextTerserPath);
						} else {
							newNode = new TreeNodeSegment((Segment) nextStructure, groupName, j, repeating, required, nextTerserPath);
						}

						addChildren((Segment) nextStructure, newNode, nextTerserPath);

						newNode = insertOrReplaceWithExisting(treeParent, currChild, newNode);

					}

					currChild++;
					// treeParent.insert(newNode, currChild++);
				}
			} catch (HL7Exception e) {
				ourLog.error("Failed to add group to tree", e);
			}
		}
	}

	void addChildren(List<AbstractMessage<?>> theMessages, TreeNodeRoot theTop, String theTerserPath) throws InterruptedException, InvocationTargetException {
		int index = 0;
		for (AbstractMessage<?> abstractMessage : theMessages) {

			if (abstractMessage instanceof Hl7V2MessageBase) {

				Hl7V2MessageBase message = (Hl7V2MessageBase) abstractMessage;
				TreeNodeMessage node;
				if (message.getParsedMessage() instanceof ConformanceMessage) {
					node = new TreeNodeMessageConf(index, message);
				} else {
					node = new TreeNodeMessage(index, message);
				}
				insertOrReplaceWithExisting(theTop, index, node);

				addChildren(node.getMessage().getParsedMessage(), node, "");

			} else if (abstractMessage instanceof UnknownMessage) {

				UnknownMessage unknownMessage = (UnknownMessage) abstractMessage;
				TreeNodeUnknown node = new TreeNodeUnknown(unknownMessage);
				insertOrReplaceWithExisting(theTop, index, node);

				String message = unknownMessage.getParsedMessage();
				for (String line : message.split("(\\n|\\r)+")) {
					line = StringUtil.chomp(line);
					node.add(new TreeNodeUnknownLine(line));
				}

			} else if (abstractMessage instanceof Comment) {

				TreeNodeComment node = new TreeNodeComment((Comment) abstractMessage);
				insertOrReplaceWithExisting(theTop, index, node);

			} else {

				throw new IllegalStateException("Unknown type: " + abstractMessage.getClass());

			}

			index++;
		}
	}

	/**
	 * Add fields of a segment to the tree ...
	 */
	void addChildren(Segment messParent, TreeNodeBase treeParent, String theTerserPath) throws InterruptedException, InvocationTargetException {

		int n = messParent.numFields();
		String[] names = messParent.getNames();
		int index = 0;
		for (int i = 1; i <= n; i++) {
			try {

				List<Integer> components = new ArrayList<Integer>();
				components.add(Integer.valueOf(i));

				switch (getShowMode()) {
				case ALL:
				case ERROR:
					// case POPULATED:
					/*
					 * this creates at least one rep if there are none, since
					 * these modes want to show the node in the tree regardless
					 * of whether or not it has content
					 */
					messParent.getField(i, 0);
				default:
					// nothing
				}

				Type[] reps = messParent.getField(i);
				boolean repeating = messParent.getMaxCardinality(i) != 1;
				boolean required = messParent.isRequired(i);
				String name = i <= names.length ? names[i - 1] : "Unknown";

				for (int j = 0; j < reps.length; j++) {

					// String field = PipeParser.encode(reps[j], encChars);

					Type type = reps[j];
					String parentName = messParent.getName() + "-" + (i);

					StringBuilder b = new StringBuilder();
					b.append(theTerserPath);
					b.append("-");
					b.append((i));
					if (repeating) {
						b.append('(');
						b.append(j + 1);
						b.append(')');
					}
					String terserPath = b.toString();

					index = addChildren(parentName, treeParent, repeating, required, name, j, type, messParent, components, index, terserPath);

				}

			} catch (HL7Exception e) {
				ourLog.error("Failed to add child to tree", e);
			}
		}
	}

	/**
	 * Adds components of a composite to the tree ...
	 * 
	 */
	void addChildren(String theParentName, Composite messParent, TreeNodeBase treeParent, Segment theSegment, List<Integer> theComponentPath, String theTerserPath) throws InterruptedException, InvocationTargetException {
		Type[] components = messParent.getComponents();

		int cpIndex = theComponentPath.size();
		theComponentPath.add(null);

		int index = 0;
		for (int i = 0; i < components.length; i++) {
			Type nextType = components[i];
			String nextParentName = theParentName + "-" + (i + 1);

			theComponentPath.set(cpIndex, Integer.valueOf(i + 1));
			String terserPath = theTerserPath + "-" + (i + 1);

			index = addChildren(nextParentName, treeParent, false, false, null, i, nextType, theSegment, theComponentPath, index, terserPath);
		}

		index = addChidrenExtra(theParentName, messParent, treeParent, theSegment, theComponentPath, theTerserPath, cpIndex, index);

		theComponentPath.remove(cpIndex);

	}

	int addChildren(String theParentName, TreeNodeBase theTreeParent, boolean theRepeating, boolean theRequired, String theName, int theRepNum, Type theType, Segment theParent, List<Integer> theComponentNumbers, int theIndex, String theTerserPath) throws InterruptedException,
			InvocationTargetException {
		if (theType instanceof Varies) {
			theType = ((Varies) theType).getData();
		}

		if (theType instanceof Composite) {
			Composite composite = (Composite) theType;
			TreeNodeType newNode;
			if (composite instanceof ConformanceComposite) {
				newNode = new TreeNodeCompositeConf(theParentName, composite, theName, theRepNum, theRepeating, theRequired, theParent, theComponentNumbers, theTerserPath);
			} else {
				newNode = new TreeNodeType(theParentName, composite, theName, theRepNum, theRepeating, theRequired, theParent, theComponentNumbers, theTerserPath);
			}

			addChildren(theParentName, composite, newNode, theParent, theComponentNumbers, theTerserPath);

			newNode = (TreeNodeType) insertOrReplaceWithExisting(theTreeParent, theIndex, newNode);

		} else {

			Primitive primitive = (Primitive) theType;
			TreeNodeType newNode;
			if (primitive instanceof ConformancePrimitive) {
				newNode = new TreeNodePrimitiveConf(theParentName, (ConformancePrimitive) primitive, theName, theRepNum, theRepeating, theRequired, theParent, theComponentNumbers, theTerserPath);
			} else {
				newNode = new TreeNodePrimitive(theParentName, primitive, theName, theRepNum, theRepeating, theRequired, theParent, theComponentNumbers, theTerserPath);
			}

			addChidrenExtra(theParentName, primitive, newNode, theParent, theComponentNumbers, theTerserPath, theComponentNumbers.size(), 0);

			newNode = (TreeNodeType) insertOrReplaceWithExisting(theTreeParent, theIndex, newNode);

		}

		return theIndex + 1;
	}


	public void collapseAll() {
		for (int i = myTree.getRowCount() - 1; i >= 0; i--) {
			TreePath path = myTree.getPathForRow(i);
			if (path != null) {
				myTree.collapsePath(path);
			}
		}
	}

	public void destroy() {
		removeMessageListeners();

		myTop.destroy();
		myUpdaterThread.stopThread();
	}

	public TreeNodeBase getRootNode() {
		return myTop;
	}

	private void doSynchronizeTreeWithHighlitedPath() {
		String highlitedPath = myMessages.getHighlitedPath();
		if (highlitedPath == null) {
			return;
		}

		int currentMessageIndex = -1;
		int bestMatchRow = -1;
		int bestMatchScore = -1;
		for (int row = 0; row < myTree.getRowCount(); row++) {
			TreePath path = myTree.getPathForRow(row);
			if (path == null) {
				continue;
			}

			Object component = path.getLastPathComponent();
			if (component instanceof TreeNodeMessage) {
				currentMessageIndex = ((TreeNodeMessage) component).getMessageIndex();
				if (highlitedPath.startsWith(currentMessageIndex + "/")) {
					myTree.expandPath(path);
				}
				continue;
			}

			if (component instanceof TreeNodeUnknown) {
				continue;
			}

			if (component instanceof TreeNodeBase) {
				TreeNodeBase node = (TreeNodeBase) component;
				String terserPath = (currentMessageIndex) + node.getTerserPath();

				int matchScore = calculateMatchScore(highlitedPath, terserPath);

				if (matchScore > bestMatchScore) {
					bestMatchScore = matchScore;
					bestMatchRow = row;
				}

				if (matchScore > 0) {
					myTree.expandPath(path);
				}
			}
		}

		if (bestMatchRow != -1 && !myRespondingToManualRangeChange) {
			myTree.setSelectionRow(bestMatchRow);
			myTree.scrollRowToVisible(bestMatchRow);
		}
	}

	private int calculateMatchScore(String highlitedPath, String terserPath) {
		if (highlitedPath.equals(terserPath)) {
			return 100;
		}

		int parenIndex = terserPath.indexOf('(');
		String baseField = parenIndex > 0 ? terserPath.substring(0, parenIndex) : terserPath;

		if (highlitedPath.equals(baseField)) {
			return 75;
		}
		if (highlitedPath.startsWith(terserPath + "(")) {
			return 50;
		}
		if (highlitedPath.startsWith(terserPath)) {
			return 25;
		}
		if (highlitedPath.startsWith(baseField + "-")) {
			int highlitedDepth = countDashes(highlitedPath);
			int terserDepth = countDashes(terserPath);
			if (highlitedDepth == terserDepth) {
				return 90;
			} else if (highlitedDepth > terserDepth) {
				return 85 - Math.min(10, (highlitedDepth - terserDepth) * 2);
			}
		}
		return 0;
	}

	private int countDashes(String path) {
		int count = 0;
		for (int i = 0; i < path.length(); i++) {
			if (path.charAt(i) == '-') {
				count++;
			}
		}
		return count;
	}

	public void expandAll() {
		for (int i = 0; i < myTree.getRowCount(); i++) {
			TreePath path = myTree.getPathForRow(i);
			if (path != null) {
				myTree.expandPath(path);
			}
		}
	}

	private void expandPaths(Set<String> theOpenPaths, String theSelectedPath) {
		int messageIndex = -1;
		for (int i = 0; i < myTree.getRowCount(); i++) {
			TreePath path = myTree.getPathForRow(i);
			if (path == null) continue;

			Object baseObj = path.getLastPathComponent();
			String pathString = null;
			if (baseObj instanceof TreeNodeMessage || baseObj instanceof TreeNodeUnknown) {
				messageIndex++;
				pathString = Integer.toString(messageIndex);
			} else if (baseObj instanceof TreeNodeBase) {
				pathString = (Integer.toString(messageIndex) + ((TreeNodeBase) baseObj).getTerserPath());
			}

			if (pathString != null) {
				if (theOpenPaths.contains(pathString)) {
					myTree.expandPath(path);
				} else {
					myTree.collapsePath(path);
				}
				if (pathString.equals(theSelectedPath)) {
					myTree.setSelectionRow(i);
				}
			}

		}

	}

	private Set<String> getOpenPaths() {
		Set<String> retVal = new HashSet<String>();

		int messageIndex = -1;
		for (int i = 0; i < myTree.getRowCount(); i++) {
			TreePath path = myTree.getPathForRow(i);
			if (path == null) continue;

			Object baseObj = path.getLastPathComponent();
			if (baseObj instanceof TreeNodeMessage || baseObj instanceof TreeNodeUnknown) {

				messageIndex++;

				if (myTree.isExpanded(path)) {
					retVal.add(Integer.toString(messageIndex));
				}

			} else if (baseObj instanceof TreeNodeBase) {
				if (myTree.isExpanded(path)) {
					retVal.add(Integer.toString(messageIndex) + ((TreeNodeBase) baseObj).getTerserPath());
				}
			}

		}

		return retVal;
	}

	private String getPathAtRow(int theRowIndex) {

		int messageIndex = -1;
		for (int i = 0; i < myTree.getRowCount(); i++) {
			TreePath path = myTree.getPathForRow(i);
			if (path == null) continue;

			Object baseObj = path.getLastPathComponent();
			if (baseObj instanceof TreeNodeMessage || baseObj instanceof TreeNodeUnknown) {

				messageIndex++;
				if (i == theRowIndex) {
					return Integer.toString(messageIndex);
				}

			} else {

				if (i == theRowIndex && baseObj instanceof TreeNodeBase) {
					return (Integer.toString(messageIndex) + ((TreeNodeBase) baseObj).getTerserPath());
				}

			}

		}

		return null;
	}

	private ShowEnum getShowMode() {
		if (myUnitTestShowMode != null) {
			return myUnitTestShowMode;
		}
		ShowEnum showMode = myMessages != null ? myMessages.getEditorShowMode() : ShowEnum.POPULATED;
		return showMode;
	}

	private void handleKeyPress(KeyEvent theE) {
		// Tree doesn't support in-place editing in the same way; no-op for now
	}

	private JPopupMenu createTreeContextMenu() {
		JPopupMenu menu = new JPopupMenu();
		JMenuItem editMenuItem = new JMenuItem("Edit...");
		editMenuItem.addActionListener(e -> handleEditMenuClick());
		menu.add(editMenuItem);
		return menu;
	}

	private void handleEditMenuClick() {
		int selectedRow = myTree.getLeadSelectionRow();
		if (selectedRow < 0) {
			return;
		}

		TreePath path = myTree.getPathForRow(selectedRow);
		if (path == null) {
			return;
		}

		Object component = path.getLastPathComponent();
		if (component instanceof TreeNodeType) {
			TreeNodeType node = (TreeNodeType) component;
			showEditDialog(node);
		} else if (component instanceof TreeNodeSegment) {
			// Could add segment editing here in the future
			javax.swing.JOptionPane.showMessageDialog(myTree, "Segment editing not yet supported", "Info", javax.swing.JOptionPane.INFORMATION_MESSAGE);
		}
	}

	private void showEditDialog(TreeNodeType node) {
		Type type = node.getType();
		String fieldName = node.getName();

		// Unwrap Varies types to get the actual type
		if (type instanceof Varies) {
			type = ((Varies) type).getData();
		}

		String newValue = null;
		if (type instanceof Composite) {
			newValue = showEditCompositeDialog(fieldName, (Composite) type);
		} else {
			String currentValue = node.getPipeEncodedValue();
			Primitive primitive = (Primitive) type;
			String typeName = primitive.getName();
			if (isDateTimeType(typeName)) {
				newValue = showEditDateTimeDialog(fieldName, currentValue, typeName);
			} else {
				newValue = showEditValueDialog(fieldName, currentValue, type);
			}
		}

		if (newValue != null) {
			try {
				if (type instanceof Composite) {
					// Composite was already updated in the dialog
					newValue = node.getPipeEncodedValue();
				} else if (!newValue.equals(node.getPipeEncodedValue())) {
					updateNodeValue(node, newValue);
				} else {
					return; // No change
				}

				// Find the message this node belongs to and update it
				TreeNodeBase base = node;
				while (!(base instanceof TreeNodeMessage)) {
					base = (TreeNodeBase) base.getParent();
				}

				if (base instanceof TreeNodeMessage) {
					TreeNodeMessage msgNode = (TreeNodeMessage) base;
					int messageIndex = msgNode.getMessageIndex();
					Message msg = type.getMessage();

					// This syncs the parsed message back to source and triggers the display update
					myMessages.updateSourceMessageBasedOnParsedMessage(messageIndex, msg);
				}

				myUpdaterThread.scheduleUpdateNow();
			} catch (Exception e) {
				ourLog.error("Failed to update node value", e);
				javax.swing.JOptionPane.showMessageDialog(myTree, "Failed to update value: " + e.getMessage(), "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	private String showEditDateTimeDialog(String fieldName, String currentValue, String typeName) {
		javax.swing.JDialog dialog = new javax.swing.JDialog();
		dialog.setTitle("Edit: " + fieldName);
		dialog.setDefaultCloseOperation(javax.swing.JDialog.DISPOSE_ON_CLOSE);
		dialog.setModal(true);
		dialog.setLocationRelativeTo(myTree);

		javax.swing.JPanel mainPanel = new javax.swing.JPanel();
		mainPanel.setLayout(new java.awt.GridBagLayout());
		mainPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));

		java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();
		gbc.anchor = java.awt.GridBagConstraints.WEST;
		gbc.insets = new java.awt.Insets(0, 0, 8, 10);

		java.util.Map<String, String> components = parseDateTime(currentValue, typeName);

		final javax.swing.JTextField[] fields = new javax.swing.JTextField[8];
		int currentRow = 0;
		int fieldIdx = 0;

		// Date fields (for DT, DTM, and TS)
		if ("DT".equals(typeName) || "DTM".equals(typeName) || "TS".equals(typeName)) {
			gbc.gridx = 0;
			gbc.gridy = currentRow;
			mainPanel.add(new javax.swing.JLabel("Date (YYYY-MM-DD):"), gbc);

			javax.swing.JPanel datePanel = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 3, 0));
			fields[fieldIdx] = new javax.swing.JTextField(components.get("year"), 4);
			fields[fieldIdx].setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12));
			datePanel.add(fields[fieldIdx]);
			datePanel.add(new javax.swing.JLabel("-"));
			fieldIdx++;

			fields[fieldIdx] = new javax.swing.JTextField(components.get("month"), 2);
			fields[fieldIdx].setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12));
			datePanel.add(fields[fieldIdx]);
			datePanel.add(new javax.swing.JLabel("-"));
			fieldIdx++;

			fields[fieldIdx] = new javax.swing.JTextField(components.get("day"), 2);
			fields[fieldIdx].setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12));
			datePanel.add(fields[fieldIdx]);
			fieldIdx++;

			gbc.gridx = 1;
			gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
			gbc.weightx = 1.0;
			mainPanel.add(datePanel, gbc);
			currentRow++;
		}

		// Time fields (for DTM, TS, and TM)
		if ("DTM".equals(typeName) || "TS".equals(typeName) || "TM".equals(typeName)) {
			gbc.gridx = 0;
			gbc.gridy = currentRow;
			gbc.fill = java.awt.GridBagConstraints.NONE;
			gbc.weightx = 0;
			mainPanel.add(new javax.swing.JLabel("Time (HH:MM:SS):"), gbc);

			javax.swing.JPanel timePanel = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 3, 0));
			fields[fieldIdx] = new javax.swing.JTextField(components.get("hour"), 2);
			fields[fieldIdx].setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12));
			timePanel.add(fields[fieldIdx]);
			timePanel.add(new javax.swing.JLabel(":"));
			fieldIdx++;

			fields[fieldIdx] = new javax.swing.JTextField(components.get("minute"), 2);
			fields[fieldIdx].setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12));
			timePanel.add(fields[fieldIdx]);
			timePanel.add(new javax.swing.JLabel(":"));
			fieldIdx++;

			fields[fieldIdx] = new javax.swing.JTextField(components.get("second"), 2);
			fields[fieldIdx].setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12));
			timePanel.add(fields[fieldIdx]);
			fieldIdx++;

			gbc.gridx = 1;
			gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
			gbc.weightx = 1.0;
			mainPanel.add(timePanel, gbc);
			currentRow++;
		}

		// Milliseconds field (optional for DTM, TS, and TM)
		if ("DTM".equals(typeName) || "TS".equals(typeName) || "TM".equals(typeName)) {
			gbc.gridx = 0;
			gbc.gridy = currentRow;
			gbc.fill = java.awt.GridBagConstraints.NONE;
			gbc.weightx = 0;
			mainPanel.add(new javax.swing.JLabel("Milliseconds (optional):"), gbc);

			fields[fieldIdx] = new javax.swing.JTextField(components.get("millis"), 3);
			fields[fieldIdx].setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12));

			gbc.gridx = 1;
			gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
			gbc.weightx = 1.0;
			mainPanel.add(fields[fieldIdx], gbc);
			fieldIdx++;
			currentRow++;
		}

		// Timezone field (optional for DTM, TS, and TM)
		if ("DTM".equals(typeName) || "TS".equals(typeName) || "TM".equals(typeName)) {
			gbc.gridx = 0;
			gbc.gridy = currentRow;
			gbc.fill = java.awt.GridBagConstraints.NONE;
			gbc.weightx = 0;
			mainPanel.add(new javax.swing.JLabel("Timezone (e.g., -0500):"), gbc);

			fields[fieldIdx] = new javax.swing.JTextField(components.get("timezone"), 5);
			fields[fieldIdx].setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12));

			gbc.gridx = 1;
			gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
			gbc.weightx = 1.0;
			mainPanel.add(fields[fieldIdx], gbc);
			currentRow++;
		}

		// Preview label
		gbc.gridx = 0;
		gbc.gridy = currentRow;
		gbc.fill = java.awt.GridBagConstraints.NONE;
		gbc.weightx = 0;
		gbc.insets = new java.awt.Insets(10, 0, 5, 10);
		mainPanel.add(new javax.swing.JLabel("Preview:"), gbc);

		javax.swing.JLabel previewLabel = new javax.swing.JLabel(currentValue != null ? currentValue : "");
		previewLabel.setFont(new java.awt.Font("Monospaced", java.awt.Font.BOLD, 12));
		previewLabel.setForeground(java.awt.Color.BLUE);

		gbc.gridx = 1;
		gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1.0;
		mainPanel.add(previewLabel, gbc);

		// Update preview as fields change
		javax.swing.event.DocumentListener docListener = new javax.swing.event.DocumentListener() {
			public void insertUpdate(javax.swing.event.DocumentEvent e) {
				updatePreview();
			}
			public void removeUpdate(javax.swing.event.DocumentEvent e) {
				updatePreview();
			}
			public void changedUpdate(javax.swing.event.DocumentEvent e) {
				updatePreview();
			}
			private void updatePreview() {
				String preview = formatDateTime(fields, typeName);
				previewLabel.setText(preview != null ? preview : "");
			}
		};

		for (javax.swing.JTextField field : fields) {
			if (field != null) {
				field.getDocument().addDocumentListener(docListener);
			}
		}

		// Buttons
		gbc.gridx = 0;
		gbc.gridy = currentRow + 1;
		gbc.gridwidth = 2;
		gbc.anchor = java.awt.GridBagConstraints.WEST;
		gbc.fill = java.awt.GridBagConstraints.NONE;
		gbc.weightx = 1.0;
		gbc.insets = new java.awt.Insets(10, 0, 0, 0);

		javax.swing.JPanel buttonPanel = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 5, 0));
		javax.swing.JButton setNowButton = new javax.swing.JButton("Set to Now");

		final String[] result = new String[1];

		setNowButton.addActionListener(e -> {
			java.time.LocalDateTime now = java.time.LocalDateTime.now();
			if ("DT".equals(typeName)) {
				fields[0].setText(String.format("%04d", now.getYear()));
				fields[1].setText(String.format("%02d", now.getMonthValue()));
				fields[2].setText(String.format("%02d", now.getDayOfMonth()));
			} else if ("DTM".equals(typeName) || "TS".equals(typeName)) {
				fields[0].setText(String.format("%04d", now.getYear()));
				fields[1].setText(String.format("%02d", now.getMonthValue()));
				fields[2].setText(String.format("%02d", now.getDayOfMonth()));
				fields[3].setText(String.format("%02d", now.getHour()));
				fields[4].setText(String.format("%02d", now.getMinute()));
				fields[5].setText(String.format("%02d", now.getSecond()));
				fields[6].setText("");
				fields[7].setText("");
			} else if ("TM".equals(typeName)) {
				fields[0].setText(String.format("%02d", now.getHour()));
				fields[1].setText(String.format("%02d", now.getMinute()));
				fields[2].setText(String.format("%02d", now.getSecond()));
				fields[3].setText("");
				fields[4].setText("");
			}
		});

		buttonPanel.add(setNowButton);

		gbc.anchor = java.awt.GridBagConstraints.EAST;
		gbc.weightx = 0;
		javax.swing.JPanel rightButtonPanel = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 5, 0));
		javax.swing.JButton okButton = new javax.swing.JButton("OK");
		javax.swing.JButton cancelButton = new javax.swing.JButton("Cancel");

		okButton.addActionListener(e -> {
			String formatted = formatDateTime(fields, typeName);
			if (formatted != null) {
				result[0] = formatted;
				dialog.dispose();
			} else {
				javax.swing.JOptionPane.showMessageDialog(dialog, "Invalid date/time format", "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
			}
		});

		cancelButton.addActionListener(e -> {
			result[0] = null;
			dialog.dispose();
		});

		rightButtonPanel.add(okButton);
		rightButtonPanel.add(cancelButton);
		buttonPanel.add(rightButtonPanel);
		mainPanel.add(buttonPanel, gbc);

		dialog.getContentPane().add(mainPanel);
		dialog.setSize(450, 300);
		dialog.setVisible(true);

		return result[0];
	}

	private String formatDateTime(javax.swing.JTextField[] fields, String typeName) {
		if ("DT".equals(typeName)) {
			String year = fields[0] != null ? fields[0].getText() : "";
			String month = fields[1] != null ? fields[1].getText() : "";
			String day = fields[2] != null ? fields[2].getText() : "";
			if (year.length() != 4 || month.length() != 2 || day.length() != 2) {
				return null;
			}
			return year + month + day;
		} else if ("DTM".equals(typeName) || "TS".equals(typeName)) {
			String year = fields[0] != null ? fields[0].getText() : "";
			String month = fields[1] != null ? fields[1].getText() : "";
			String day = fields[2] != null ? fields[2].getText() : "";
			String hour = fields[3] != null ? fields[3].getText() : "";
			String minute = fields[4] != null ? fields[4].getText() : "";
			String second = fields[5] != null ? fields[5].getText() : "";
			String millis = fields[6] != null ? fields[6].getText().trim() : "";
			String timezone = fields[7] != null ? fields[7].getText().trim() : "";

			if (year.length() != 4 || month.length() != 2 || day.length() != 2) {
				return null;
			}

			// For DTM/TS: either no time, or full time (HHMMSS)
			boolean hasTime = !hour.isEmpty() || !minute.isEmpty() || !second.isEmpty();
			if (hasTime) {
				if (hour.length() != 2 || minute.length() != 2 || second.length() != 2) {
					return null;
				}
			}

			StringBuilder result = new StringBuilder();
			result.append(year).append(month).append(day);
			if (hasTime) {
				result.append(hour).append(minute).append(second);
				if (!millis.isEmpty()) {
					result.append(".").append(millis);
				}
				if (!timezone.isEmpty()) {
					result.append(timezone);
				}
			}
			return result.toString();
		} else if ("TM".equals(typeName)) {
			String hour = fields[0] != null ? fields[0].getText() : "";
			String minute = fields[1] != null ? fields[1].getText() : "";
			String second = fields[2] != null ? fields[2].getText() : "";
			String millis = fields[3] != null ? fields[3].getText().trim() : "";
			String timezone = fields[4] != null ? fields[4].getText().trim() : "";

			if (hour.length() != 2 || minute.length() != 2 || second.length() != 2) {
				return null;
			}

			StringBuilder result = new StringBuilder();
			result.append(hour).append(minute).append(second);
			if (!millis.isEmpty()) {
				result.append(".").append(millis);
			}
			if (!timezone.isEmpty()) {
				result.append(timezone);
			}
			return result.toString();
		}
		return null;
	}

	private String showEditCompositeDialog(String fieldName, Composite composite) {
		javax.swing.JDialog dialog = new javax.swing.JDialog();
		dialog.setTitle("Edit: " + fieldName);
		dialog.setDefaultCloseOperation(javax.swing.JDialog.DISPOSE_ON_CLOSE);
		dialog.setModal(true);
		dialog.setLocationRelativeTo(myTree);

		javax.swing.JPanel mainPanel = new javax.swing.JPanel();
		mainPanel.setLayout(new java.awt.BorderLayout());
		mainPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// Header panel with field info
		javax.swing.JPanel headerPanel = new javax.swing.JPanel();
		headerPanel.setLayout(new java.awt.BorderLayout());
		headerPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 10, 0));
		javax.swing.JLabel headerLabel = new javax.swing.JLabel("<html><b>" + fieldName + "</b> (" + composite.getClass().getSimpleName() + ")</html>");
		headerPanel.add(headerLabel, java.awt.BorderLayout.WEST);
		mainPanel.add(headerPanel, java.awt.BorderLayout.NORTH);

		// Scrollable components panel
		javax.swing.JPanel scrollablePanel = new javax.swing.JPanel();
		scrollablePanel.setLayout(new java.awt.GridBagLayout());

		java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();
		gbc.anchor = java.awt.GridBagConstraints.NORTHWEST;
		gbc.insets = new java.awt.Insets(8, 0, 8, 10);

		Type[] components = composite.getComponents();
		javax.swing.JTextField[] componentFields = new javax.swing.JTextField[components.length];

		for (int i = 0; i < components.length; i++) {
			Type component = components[i];
			int position = i + 1;
			String componentName = "Component " + position;
			String componentType = component != null ? component.getClass().getSimpleName() : "Unknown";

			// Try to get name from conformance profile first
			if (composite instanceof ConformanceComposite) {
				ConformanceComposite confComposite = (ConformanceComposite) composite;
				try {
					String confName = confComposite.getName(position);
					if (confName != null && !confName.isEmpty()) {
						componentName = confName;
					}
				} catch (Exception e) {
					// Use default
				}
			}

			// If still default, try to extract from getter methods via reflection
			if (componentName.startsWith("Component")) {
				try {
					java.lang.reflect.Method[] methods = composite.getClass().getDeclaredMethods();

					for (java.lang.reflect.Method method : methods) {
						String methodName = method.getName();
						// Match pattern like "get[Type][Position]_[Description]"
						if (methodName.matches("get.*" + position + "_.*")) {
							// Extract the description part after the underscore
							String[] parts = methodName.split("_");
							if (parts.length > 1) {
								String desc = parts[1];
								// Convert camelCase to readable text
								desc = desc.replaceAll("([a-z])([A-Z])", "$1 $2");
								componentName = desc;
								break;
							}
						}
					}
				} catch (Exception e) {
					// Use default
				}
			}

			// Try to get name from the component itself as fallback
			if (componentName.startsWith("Component")) {
				if (component != null) {
					try {
						String typeName = component.getName();
						if (typeName != null && !typeName.isEmpty()) {
							componentName = typeName;
						}
					} catch (Exception e) {
						// Use default
					}
				}
			}

			// Add label with position, name, and type
			gbc.gridx = 0;
			gbc.gridy = i;
			gbc.gridwidth = 1;
			gbc.weightx = 0.0;
			gbc.fill = java.awt.GridBagConstraints.NONE;
			gbc.insets = new java.awt.Insets(8, 0, 8, 10);

			String labelText = "<html><b>" + position + ".</b> " + componentName + "<br/><font color='#666666' size='-1'>(" + componentType + ")</font></html>";
			scrollablePanel.add(new javax.swing.JLabel(labelText), gbc);

			// Add text field next to label
			gbc.gridx = 1;
			gbc.weightx = 1.0;
			gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
			gbc.insets = new java.awt.Insets(8, 0, 8, 0);

			componentFields[i] = new javax.swing.JTextField(30);
			componentFields[i].setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 11));

			// Get current value for this component
			try {
				String value = component.encode();
				componentFields[i].setText(value != null ? value : "");
			} catch (Exception e) {
				ourLog.warn("Could not encode component", e);
			}

			scrollablePanel.add(componentFields[i], gbc);
		}

		// Add scrollable panel to main panel
		javax.swing.JScrollPane scrollPane = new javax.swing.JScrollPane(scrollablePanel);
		mainPanel.add(scrollPane, java.awt.BorderLayout.CENTER);

		// Button panel
		javax.swing.JPanel buttonPanel = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 5, 5));
		buttonPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 0, 0, 0));
		javax.swing.JButton okButton = new javax.swing.JButton("OK");
		javax.swing.JButton cancelButton = new javax.swing.JButton("Cancel");

		final boolean[] result = new boolean[1];

		okButton.addActionListener(e -> {
			try {
				// Update each component with the new value
				for (int i = 0; i < components.length; i++) {
					String newValue = componentFields[i].getText();
					Type component = components[i];

					if (component instanceof Primitive) {
						((Primitive) component).setValue(newValue);
					} else {
						// For nested composites, parse the value
						EncodingCharacters enc;
						try {
							enc = EncodingCharacters.getInstance(component.getMessage());
						} catch (HL7Exception ex) {
							enc = new EncodingCharacters('|', null);
						}
						component.clear();
						myPipeParser.parse(component, newValue, enc);
					}
				}
				result[0] = true;
				dialog.dispose();
			} catch (Exception ex) {
				ourLog.error("Failed to update composite", ex);
				javax.swing.JOptionPane.showMessageDialog(dialog, "Failed to update: " + ex.getMessage(), "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
			}
		});

		cancelButton.addActionListener(e -> {
			result[0] = false;
			dialog.dispose();
		});

		okButton.setPreferredSize(new java.awt.Dimension(75, 30));
		cancelButton.setPreferredSize(new java.awt.Dimension(75, 30));
		buttonPanel.add(okButton);
		buttonPanel.add(cancelButton);
		mainPanel.add(buttonPanel, java.awt.BorderLayout.SOUTH);

		dialog.getContentPane().add(mainPanel);
		dialog.setSize(700, Math.min(150 + (components.length * 50), 600));
		dialog.setVisible(true);

		return result[0] ? "composite_updated" : null;
	}

	private String showEditValueDialog(String fieldName, String currentValue, Type type) {
		javax.swing.JDialog dialog = new javax.swing.JDialog();
		dialog.setTitle("Edit: " + fieldName);
		dialog.setDefaultCloseOperation(javax.swing.JDialog.DISPOSE_ON_CLOSE);
		dialog.setModal(true);
		dialog.setLocationRelativeTo(myTree);

		javax.swing.JPanel panel = new javax.swing.JPanel();
		panel.setLayout(new java.awt.GridBagLayout());
		panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));

		java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.anchor = java.awt.GridBagConstraints.WEST;
		gbc.insets = new java.awt.Insets(0, 0, 5, 10);
		panel.add(new javax.swing.JLabel("Value:"), gbc);

		javax.swing.JTextArea textArea = new javax.swing.JTextArea(5, 40);
		textArea.setText(currentValue != null ? currentValue : "");
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);
		textArea.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12));

		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.fill = java.awt.GridBagConstraints.BOTH;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		panel.add(new javax.swing.JScrollPane(textArea), gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridwidth = 2;
		gbc.anchor = java.awt.GridBagConstraints.EAST;
		gbc.fill = java.awt.GridBagConstraints.NONE;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.insets = new java.awt.Insets(10, 0, 0, 0);

		javax.swing.JPanel buttonPanel = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 5, 0));
		javax.swing.JButton okButton = new javax.swing.JButton("OK");
		javax.swing.JButton cancelButton = new javax.swing.JButton("Cancel");

		final String[] result = new String[1];

		okButton.addActionListener(e -> {
			result[0] = textArea.getText();
			dialog.dispose();
		});

		cancelButton.addActionListener(e -> {
			result[0] = null;
			dialog.dispose();
		});

		buttonPanel.add(okButton);
		buttonPanel.add(cancelButton);
		panel.add(buttonPanel, gbc);

		dialog.getContentPane().add(panel);
		dialog.setSize(500, 300);
		dialog.setVisible(true);

		return result[0];
	}

	private boolean isDateTimeType(String typeName) {
		return "DT".equals(typeName) || "DTM".equals(typeName) || "TM".equals(typeName) || "TS".equals(typeName);
	}

	private java.util.Map<String, String> parseDateTime(String value, String typeName) {
		java.util.Map<String, String> components = new java.util.HashMap<>();
		java.time.LocalDateTime now = java.time.LocalDateTime.now();
		java.time.format.DateTimeFormatter dateFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
		java.time.format.DateTimeFormatter timeFormatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss");

		String year = String.format("%04d", now.getYear());
		String month = String.format("%02d", now.getMonthValue());
		String day = String.format("%02d", now.getDayOfMonth());
		String hour = String.format("%02d", now.getHour());
		String minute = String.format("%02d", now.getMinute());
		String second = String.format("%02d", now.getSecond());
		String millis = "";
		String timezone = "";

		if (value != null && !value.isEmpty()) {
			try {
				if ("DT".equals(typeName)) {
					if (value.length() >= 8) {
						year = value.substring(0, 4);
						month = value.substring(4, 6);
						day = value.substring(6, 8);
					}
				} else if ("DTM".equals(typeName) || "TS".equals(typeName)) {
					if (value.length() >= 8) {
						year = value.substring(0, 4);
						month = value.substring(4, 6);
						day = value.substring(6, 8);
					}
					if (value.length() >= 14) {
						hour = value.substring(8, 10);
						minute = value.substring(10, 12);
						second = value.substring(12, 14);
					}
					if (value.contains(".")) {
						int dotIdx = value.indexOf('.');
						int endIdx = value.length();
						for (int i = dotIdx + 1; i < value.length(); i++) {
							char c = value.charAt(i);
							if (c == '-' || c == '+') {
								endIdx = i;
								break;
							}
						}
						millis = value.substring(dotIdx + 1, endIdx);
					}
					if (value.contains("-") || value.contains("+")) {
						int tzIdx = Math.max(value.lastIndexOf('-'), value.lastIndexOf('+'));
						timezone = value.substring(tzIdx);
					}
				} else if ("TM".equals(typeName)) {
					if (value.length() >= 6) {
						hour = value.substring(0, 2);
						minute = value.substring(2, 4);
						second = value.substring(4, 6);
					}
					if (value.contains(".")) {
						int dotIdx = value.indexOf('.');
						int endIdx = value.length();
						for (int i = dotIdx + 1; i < value.length(); i++) {
							char c = value.charAt(i);
							if (c == '-' || c == '+') {
								endIdx = i;
								break;
							}
						}
						millis = value.substring(dotIdx + 1, endIdx);
					}
					if (value.contains("-") || value.contains("+")) {
						int tzIdx = Math.max(value.lastIndexOf('-'), value.lastIndexOf('+'));
						timezone = value.substring(tzIdx);
					}
				}
			} catch (Exception e) {
				ourLog.debug("Could not parse datetime value: " + value, e);
			}
		}

		components.put("year", year);
		components.put("month", month);
		components.put("day", day);
		components.put("hour", hour);
		components.put("minute", minute);
		components.put("second", second);
		components.put("millis", millis);
		components.put("timezone", timezone);

		return components;
	}

	private void updateNodeValue(TreeNodeType node, String newValue) throws HL7Exception {
		Type type = node.getType();
		if (type instanceof Primitive) {
			Primitive primitive = (Primitive) type;
			if (node.isMsh1orMsh2()) {
				// MSH-1 and MSH-2 are special cases and not parsed normally
				primitive.setValue(newValue);
			} else {
				// Parse the new value properly (handles escaping, etc.)
				EncodingCharacters enc;
				try {
					enc = EncodingCharacters.getInstance(type.getMessage());
				} catch (HL7Exception e) {
					ourLog.error("Could not get encoding chars", e);
					enc = new EncodingCharacters('|', null);
				}
				type.clear();
				myPipeParser.parse(type, newValue, enc);
			}
		} else {
			throw new HL7Exception("Cannot edit composite types yet");
		}
	}

	private void handleNewSelectedPath(TreePath path) {
		if (mySelectionHandlingDisabled) {
			return;
		}

		if (myCurrentlyEditing) {
			ourLog.info("Not responding to new selection because we are marked as editing right now");
			return;
		}

		if (path == null) {
			return;
		}

		DefaultMutableTreeNode lead = (DefaultMutableTreeNode) path.getLastPathComponent();
		if (lead instanceof TreeNodeSegment) {
			TreeNodeSegment segmentNode = (TreeNodeSegment) lead;
			ourLog.info("Selected segment: " + segmentNode.getTerserPath());
			myMessages.setHighlitedRangeBasedOnSegment(segmentNode.getSegment());
		} else if (lead instanceof TreeNodeGroup) {
			TreeNodeGroup type = (TreeNodeGroup) lead;
			ourLog.info("Selected group: " + type.getTerserPath());
			try {
				List<Segment> segments = type.getSegments();
				myMessages.setHighlitedRangeBasedOnSegment(segments.toArray(new Segment[segments.size()]));
			} catch (HL7Exception e) {
				e.printStackTrace();
			}
		} else if (lead instanceof TreeNodeType) {
			TreeNodeType type = (TreeNodeType) lead;
			ourLog.info("Selected field: " + type.getTerserPath());
			myMessages.setHighlitedRangeBasedOnField(type.getSegmentAndComponentPath());
		} else {
			ourLog.info("Selected node: " + lead.getClass().getSimpleName());
			myMessages.clearHighlight();
		}
	}

	private void handleNewSelectedIndex(int theNewIndex) {
		if (mySelectionHandlingDisabled) {
			return;
		}
		ourLog.info("New selection index: " + theNewIndex);

		if (myCurrentlyEditing) {
			ourLog.info("Not responding to new selection index because we are marked as editing right now");
			return;
		}

		TreePath path = myTree.getPathForRow(theNewIndex);
		if (path == null) {
			return;
		}

		handleNewSelectedPath(path);
	}

	private int getSelectedRow() {
		return myTree.getLeadSelectionRow();
	}

	private void removeMessageListeners() {
		if (myMessages != null) {
			myMessages.removePropertyChangeListener(Hl7V2MessageCollection.PROP_HIGHLITED_PATH, myHighlitedPathListener);
			myMessages.removePropertyChangeListener(Hl7V2MessageCollection.PARSED_MESSAGES_PROPERTY, myParsedMessagesListener);
			myMessages.removePropertyChangeListener(Hl7V2MessageCollection.PROP_VALIDATIONCONTEXT_OR_PROFILE, myValidationContextListener);
			myMessages.removePropertyChangeListener(Hl7V2MessageCollection.PROP_ENCODING, myMessageEncodingListener);
		}
	}

	public void scheduleNewValidationPass() {
		myUpdaterThread.scheduleUpdate();
	}

	public void setEditingRow(int theARow) {
		if (theARow == -1) {
			myCurrentlyEditing = false;
		} else {
			ourLog.info("Beginning editing row " + theARow);
			myCurrentlyEditing = true;
		}
	}

	public void setEditorShowModeAndUpdateAccordingly(ShowEnum theValue) {
		if (myMessages != null && theValue != myMessages.getEditorShowMode()) {
			myMessages.setEditorShowMode(theValue);
			myUpdaterThread.scheduleUpdateNow();
		}
	}

	/**
	 * Updates the panel with a new Message.
	 */
	public void setMessage(Hl7V2MessageCollection theMessage) {

		removeMessageListeners();

		myMessages = theMessage;

		myMessages.addPropertyChangeListener(Hl7V2MessageCollection.PROP_HIGHLITED_PATH, myHighlitedPathListener);
		myMessages.addPropertyChangeListener(Hl7V2MessageCollection.PARSED_MESSAGES_PROPERTY, myParsedMessagesListener);
		myMessages.addPropertyChangeListener(Hl7V2MessageCollection.PROP_VALIDATIONCONTEXT_OR_PROFILE, myValidationContextListener);
		myMessages.addPropertyChangeListener(Hl7V2MessageCollection.PROP_ENCODING, myMessageEncodingListener);

		myTop = new TreeNodeRoot();
		myTreeModel = new DefaultTreeModel(myTop, false);

		myTree.setModel(myTreeModel);

		myUpdaterThread.scheduleUpdateNow();

	}

	void setTreeModel(DefaultTreeModel theModel) {
		myTreeModel = theModel;
		myTree.setModel(theModel);
	}

	void setMessageForUnitTest(Hl7V2MessageCollection theMessageModel) {
		myMessages = theMessageModel;
	}

	void setRuntimeProfileValidator(DefaultValidator theRuntimeProfileValidator) {
		myRuntimeProfileValidator = theRuntimeProfileValidator;
	}

	public void setUnitTestShowMode(ShowEnum theUnitTestShowMode) {
		myUnitTestShowMode = theUnitTestShowMode;
		myUpdaterThread.scheduleUpdateNow();
	}

	/**
	 * @param theWorkingListener
	 *            the workingListener to set
	 */
	public void setWorkingListener(IWorkingListener theWorkingListener) {
		myWorkingListener = theWorkingListener;
	}

	public void synchronizeTreeWithHighlitedPath() {
		try {
			mySelectionHandlingDisabled = true;
			doSynchronizeTreeWithHighlitedPath();
		} finally {
			mySelectionHandlingDisabled = false;
		}
	}

	private String xmlEncode(String theValue) {
		if (theValue == null) {
			return null;
		}

		StringBuilder b = new StringBuilder();
		for (int i = 0; i < theValue.length(); i++) {
			char nextChar = theValue.charAt(i);
			switch (nextChar) {
			case '\r':
			case '\n':
				b.append("<br>");
				break;
			case ' ':
				b.append("&nbsp;");
				break;
			case '&':
				b.append("&amp;");
				break;
			case '<':
				b.append("&lt;");
				break;
			case '>':
				b.append("&gt;");
				break;
			default:
				b.append(nextChar);
			}
		}
		return b.toString();
	}

	private static TreeNodeBase insertOrReplaceWithExisting(final TreeNodeBase theTreeParent, final int theIndex, final TreeNodeBase theNewNode) throws InterruptedException, InvocationTargetException {

		if (theTreeParent.getChildCount() <= theIndex) {
			EventQueue.invokeAndWait(new Runnable() {
				public void run() {
					theTreeParent.insert(theNewNode, theIndex);
				}
			});
			return theNewNode;
		}

		// if (theTreeParent.getChildAt(theIndex).equals(theNewNode)) {
		// return (TreeNodeBase) theTreeParent.getChildAt(theIndex);
		// }

		while (theTreeParent.getChildCount() > (theIndex)) {
			TreeNode node = theTreeParent.getChildAt(theIndex);
			if (node instanceof IDestroyable) {
				((IDestroyable) node).destroy();
			}

			EventQueue.invokeAndWait(new Runnable() {
				public void run() {
					theTreeParent.remove(theIndex);
				}
			});
			if (theTreeParent.getChildCount() > (theIndex) && theTreeParent.getChildAt(theIndex).equals(theNewNode)) {
				return (TreeNodeBase) theTreeParent.getChildAt(theIndex);
			}
		}

		EventQueue.invokeAndWait(new Runnable() {
			public void run() {
				theTreeParent.insert(theNewNode, theIndex);
			}
		});

		return theNewNode;
	}

	/**
	 * Left pads a string representation of the integer to make it 4 digits long
	 */
	public static String toHl7Table(int theTable) {
		return StringUtils.leftPad(Integer.toString(theTable), 4, '0');
	}

	public interface IWorkingListener {
		void finishedWorking(String theStatus);

		void startedWorking();

	}

	private class Hl7TreeCellRenderer extends DefaultTreeCellRenderer {
		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
				boolean leaf, int row, boolean hasFocus) {
			super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
			if (value instanceof TreeNodeMessage) {
				TreeNodeMessage tnm = (TreeNodeMessage) value;
				String text = tnm.getMessage().getMessageDescription();
				if (selected) {
					text = "<html><font color=\"white\">" + text + "</font></html>";
				} else {
					text = "<html>" + text + "</html>";
				}
				setText(text);
				setIcon(ImageFactory.getTreeBundle());
			} else if (value instanceof TreeNodeBase) {
				TreeNodeBase node = (TreeNodeBase) value;
				String nodeText = node.getNodeText().toString();
				if (selected) {
					nodeText = nodeText.replaceAll("color=\"[^\"]*\"", "color=\"white\"");
				}
				setText("<html>" + nodeText + "</html>");
				if (node instanceof TreeNodeGroup) {
					setIcon(ImageFactory.getTreeBundle());
				} else if (node instanceof TreeNodeSegment) {
					setIcon(ImageFactory.getTreeLeaf());
				} else if (node.getChildCount() > 0) {
					setIcon(ImageFactory.getTreeBundle());
				} else {
					setIcon(ImageFactory.getTreeLeaf());
				}
			}

			if (selected) {
				setForeground(Color.WHITE);
			} else {
				setForeground(Color.BLACK);
			}
			return this;
		}
	}

	/**
	 * Custom selection model for JTree that hooks selection changes to handleNewSelectedIndex
	 */
	public class MySelectionModel extends DefaultTreeSelectionModel {
		@Override
		public void setSelectionPath(TreePath path) {
			myRespondingToManualRangeChange = true;
			try {
				super.setSelectionPath(path);
				if (path != null) {
					int row = myTree.getRowForPath(path);
					if (row >= 0) {
						handleNewSelectedIndex(row);
					}
				}
			} finally {
				myRespondingToManualRangeChange = false;
			}
		}
	}

	private class NodeValidationFailure {
		private Icon myIcon;
		private String myMessage;

		public NodeValidationFailure(Icon theIcon, String theMessage) {
			super();
			myIcon = theIcon;
			myMessage = theMessage;
		}

		/**
		 * @return the icon
		 */
		public Icon getIcon() {
			return myIcon;
		}

		/**
		 * @return the message
		 */
		public String getMessage() {
			return myMessage;
		}
	}

	public abstract class TreeNodeBase extends DefaultMutableTreeNode {
		protected static final String COLOR_REPNUM = "#808000";

		private Boolean myContainError;
		private String myErrorDescription;
		private Boolean myHasContent;
		private final String myName;
		private final Boolean myRepeating;
		private final int myRepNum;
		private final Boolean myRequired;
		private final String myTerserPath;
		private List<HL7Exception> myValidationExceptions = new ArrayList<HL7Exception>();

		public TreeNodeBase(Object theStructure) {
			super(theStructure);
			assert theStructure != null || this instanceof TreeNodeRoot;

			myName = null;
			myTerserPath = null;
			myRepNum = 0;
			myRepeating = null;
			myRequired = null;
		}

		public TreeNodeBase(Object theStructure, String theName, int theRepNum, Boolean theRepeating, Boolean theRequired, String theTerserPath) {
			super(theStructure);
			assert theStructure != null;

			myName = theName;
			myRepNum = theRepNum;
			myRepeating = theRepeating;
			myRequired = theRequired;
			myTerserPath = theTerserPath;
		}

		public void addValidationExceptions(List<HL7Exception> theProblems) {
			addValidationExceptions(theProblems.toArray(new HL7Exception[theProblems.size()]));
		}

		public void addValidationExceptions(HL7Exception... theExceptions) {
			for (HL7Exception hl7Exception : theExceptions) {
				myValidationExceptions.add(hl7Exception);
			}
		}

		public int countExceptions() {
			int retVal = 0;

			for (int i = 0; i < getChildCount(); i++) {
				TreeNodeBase next = (TreeNodeBase) getChildAt(i);
				retVal += next.countExceptions();
			}

			retVal += myValidationExceptions.size();
			return retVal;
		}

		public void collectValidationExceptions(java.util.List<HL7Exception> theList) {
			theList.addAll(myValidationExceptions);
			for (int i = 0; i < getChildCount(); i++) {
				TreeNodeBase next = (TreeNodeBase) getChildAt(i);
				next.collectValidationExceptions(theList);
			}
		}

		public void collectValidationExceptionsWithPath(java.util.List<java.util.AbstractMap.SimpleEntry<String, HL7Exception>> theList) {
			for (HL7Exception ex : myValidationExceptions) {
				theList.add(new java.util.AbstractMap.SimpleEntry<>(getTerserPath(), ex));
			}
			for (int i = 0; i < getChildCount(); i++) {
				TreeNodeBase next = (TreeNodeBase) getChildAt(i);
				next.collectValidationExceptionsWithPath(theList);
			}
		}

		/**
		 * Subclasses may override if validation is possible
		 */
		public void doValidate() {
			// nothing
		}

		@Override
		public boolean equals(Object theObj) {
			if (theObj == null || !getClass().equals(theObj.getClass())) {
				return false;
			}
			return ((TreeNodeBase) theObj).getUserObject() == getUserObject();
		}

		public String getDisplayName() {
			return null;
		}

		public String getNodeCode() {
			return myName != null ? myName : "";
		}

		@Override
		public String toString() {
			String name = myName != null ? myName : "";
			String displayName = getDisplayName();
			if (displayName != null && !displayName.isEmpty()) {
				return name + ": " + displayName;
			}
			return name;
		}

		/**
		 * @return the errorDescription
		 */
		public String getErrorDescription() {
			if (myErrorDescription == null && myValidationExceptions.size() > 0) {
				StringBuilder b = new StringBuilder();
				b.append("<html><ul>");
				for (HL7Exception next : myValidationExceptions) {
					b.append("<li>");
					b.append(next.getMessage());
				}
				b.append("</ul></html>");

				myErrorDescription = b.toString();
			}
			return myErrorDescription;
		}

		public Integer getMaxLength() {
			return null;
		}

		public Short getMaxReps() {
			if (isRepeating() == null) {
				return null;
			} else if (isRepeating()) {
				return -1;
			} else {
				return 1;
			}
		}

		public Short getMinReps() {
			if (isRequired() == null) {
				return null;
			} else if (isRequired()) {
				return 1;
			} else {
				return 0;
			}
		}

		/**
		 * @return the groupName
		 */
		public String getName() {
			return myName;
		}

		public StringBuilder getNodeText() {
			StringBuilder b = new StringBuilder();

			b.append("<font color=\"" + getNodeTextColor() + "\">");
			b.append(myName);
			b.append("</font>");

			if (myRepeating != null && myRepeating && (myShowRep0 || getRepNum() > 0)) {
				b.append("<font color=\"" + COLOR_REPNUM + "\">");
				b.append(" (rep");
				if (myRepNum > 0) {
					b.append(' ');
					b.append(myRepNum + 1);
				}
				b.append(")");
				b.append("</font>");
			}

			if (StringUtils.isNotBlank(getDisplayName())) {
				b.append("<font color=\"#00A000\">");
				b.append(" - ");
				b.append(getDisplayName());
				b.append("</font>");
			}

			return b;
		}

		public String getNodeTextColor() {
			return "#000000";
		}

		public String getPipeEncodedValue() {
			return null;
		}

		/**
		 * @return the repNum
		 */
		public int getRepNum() {
			return myRepNum;
		}

		/**
		 * @return the terserPath
		 */
		public String getTerserPath() {
			return myTerserPath;
		}

		@Override
		public int hashCode() {
			Object object = getUserObject();
			if (object != null) {
				return object.hashCode();
			} else {
				return super.hashCode();
			}
		}

		/**
		 * @return the containError
		 */
		public boolean isContainError() {
			if (myContainError == null) {
				if (isHasContent() == false) {
					myContainError = false;
				} else if (getErrorDescription() != null) {
					myContainError = true;
				} else {
					for (int i = 0; i < getChildCount(); i++) {
						TreeNodeBase nextChild = (TreeNodeBase) getChildAt(i);
						if (nextChild.isHasContent() && nextChild.isContainError()) {
							myContainError = true;
							break;
						}
					}
					if (myContainError == null) {
						myContainError = false;
					}
				}
			}
			return myContainError;
		}

		public Boolean isHasContent() {
			if (myHasContent == null) {
				for (int i = 0; i < getChildCount(); i++) {
					TreeNodeBase next = (TreeNodeBase) getChildAt(i);
					if (next.isHasContent() == Boolean.TRUE) {
						myHasContent = Boolean.TRUE;
						break;
					}
				}

				if (myHasContent == null) {
					myHasContent = Boolean.FALSE;
				}
			}
			return myHasContent;
		}

		/**
		 * @return the repeating
		 */
		public Boolean isRepeating() {
			return myRepeating;
		}

		/**
		 * @return the required
		 */
		public Boolean isRequired() {
			return myRequired;
		}

		/**
		 * Subclasses may override
		 */
		protected boolean isSupported() {
			return true;
		}

		/**
		 * @param theErrorDescription
		 *            the errorDescription to set
		 */
		public void setErrorDescription(String theErrorDescription) {
			myErrorDescription = theErrorDescription;
		}

		public final void validate() throws InterruptedException, InvocationTargetException {
			for (int i = 0; i < getChildCount(); i++) {
				TreeNodeBase next = (TreeNodeBase) getChildAt(i);

				if (next.isHasContent()) {
					next.validate();
				}

				ShowEnum showMode = myMessages.getEditorShowMode();
				if ((next.getErrorDescription() == null && showMode == ShowEnum.ERROR) || (next.isHasContent() == false && showMode == ShowEnum.POPULATED) || (next.isSupported() == false && next.getErrorDescription() == null && showMode == ShowEnum.SUPPORTED)) {
					final int index = i;
					EventQueue.invokeAndWait(new Runnable() {
						public void run() {
							remove(index);
						}
					});
					i--;
					continue;
				}

			}

			doValidate();
		}
	}

	public class TreeNodeComment extends TreeNodeBase implements IDestroyable {
		private PropertyChangeListener myListener;
		private Comment myMessage;

		public TreeNodeComment(Comment theMessage) {
			super(theMessage);

			myMessage = theMessage;

			myListener = new PropertyChangeListener() {

				public void propertyChange(PropertyChangeEvent theEvt) {
					myTreeModel.nodeStructureChanged(myTop);
				}
			};
			myMessage.addPropertyChangeListener(UnknownMessage.PARSED_MESSAGE_PROPERTY, myListener);

		}

		/**
		 * {@inheritDoc}
		 */
		public void destroy() {
			myMessage.addPropertyChangeListener(UnknownMessage.PARSED_MESSAGE_PROPERTY, myListener);
		}

		@Override
		public void doValidate() {
			// nothing
		}

		@Override
		public StringBuilder getNodeText() {
			StringBuilder retVal = new StringBuilder();
			retVal.append("<html><font color=\"#00FF00\">");
			retVal.append(myMessage.getSourceMessage());
			retVal.append("</font></html>");
			return retVal;
		}

	}

	public class TreeNodeCompositeConf extends TreeNodeType {

		public TreeNodeCompositeConf(String theParentName, Type theComposite, String theGroupName, int theRepNum, boolean theRepeating, boolean theRequired, Segment theParent, List<Integer> theComponentPath, String theTerserPath) {
			super(theParentName, theComposite, theGroupName, theRepNum, theRepeating, theRequired, theParent, theComponentPath, theTerserPath);
		}

		@Override
		public void doValidate() {
			EncodingCharacters enc;
			try {
				enc = EncodingCharacters.getInstance(getComposite().getMessage());
			} catch (HL7Exception e) {
				ourLog.error("Could not get encoding chars", e);
				enc = new EncodingCharacters('|', null);
			}

			String encoded = PipeParser.encode(getComposite(), enc);
			List<HL7Exception> problems = myRuntimeProfileValidator.testType(getComposite(), getComposite().getConfDefinition(), encoded, "");
			addValidationExceptions(problems);
		}

		public ConformanceComposite getComposite() {
			return (ConformanceComposite) super.getUserObject();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected String getDataTypeDescription() {
			String retVal = getComposite().getConfDefinition().getDatatype();
			return retVal;
		}

		@Override
		public String getDisplayName() {
			return getComposite().getConfDefinition().getName();
		}

		@Override
		public Integer getMaxLength() {
			return (int) getComposite().getConfDefinition().getLength();
		}

		protected boolean isSupported() {
			return !"X".equals(getComposite().getConfDefinition().getUsage());
		}

	}

	public class TreeNodeGroup extends TreeNodeGroupBase {

		public TreeNodeGroup(Group theGroup, String theGroupName, int theRepNum, boolean theRepeating, boolean theRequired, String theTerserPath) {
			super(theGroup, theGroupName, theRepNum, theRepeating, theRequired, theTerserPath);
		}

		private void addToSegList(List<Segment> retVal, Group group) throws HL7Exception {
			for (String next : group.getNames()) {
				for (Structure nextStructure : group.getAll(next)) {
					if (nextStructure instanceof Segment) {
						retVal.add((Segment) nextStructure);
					} else {
						addToSegList(retVal, (Group) nextStructure);
					}
				}
			}
		}

		public Group getGroup() {
			return (Group) getUserObject();
		}

		@Override
		public String getNodeTextColor() {
			return "#404000";
		}

		public List<Segment> getSegments() throws HL7Exception {

			List<Segment> retVal = new ArrayList<Segment>();

			Group group = getGroup();
			addToSegList(retVal, group);

			return retVal;
		}

	}

	public class TreeNodeGroupBase extends TreeNodeBase {
		public TreeNodeGroupBase(Group theGroup, String theGroupName, int theRepNum, boolean theRepeating, boolean theRequired, String theTerserPath) {
			super(theGroup, theGroupName, theRepNum, theRepeating, theRequired, theTerserPath);
		}

		public TreeNodeGroupBase(Hl7V2MessageBase theMessage) {
			super(theMessage);
		}

		public int countPopulatedSegments() {
			int retVal = 0;

			for (int i = 0; i < getChildCount(); i++) {
				TreeNode nextStructure = getChildAt(i);
				if (nextStructure instanceof TreeNodeSegment) {
					if (((TreeNodeSegment) nextStructure).isHasContent()) {
						retVal++;
					}
				} else if (nextStructure instanceof TreeNodeGroup) {
					retVal += ((TreeNodeGroup) nextStructure).countPopulatedSegments();
				}
			}

			return retVal;
		}

	}

	public class TreeNodeGroupConf extends TreeNodeGroup {

		public TreeNodeGroupConf(ConformanceGroup theGroup, String theGroupName, int theRepNum, boolean theRepeating, boolean theRequired, String theTerserPath) {
			super(theGroup, theGroupName, theRepNum, theRepeating, theRequired, theTerserPath);
		}

		@Override
		public void doValidate() {
			try {
				List<HL7Exception> problems = myRuntimeProfileValidator.testGroup(getGroup(), getGroup().getConfDefinition(), "");
				addValidationExceptions(problems);
			} catch (ProfileException e) {
				addValidationExceptions(new HL7Exception(e));
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getDisplayName() {
			return getGroup().getConfDefinition().getLongName();
		}

		public ConformanceGroup getGroup() {
			return (ConformanceGroup) super.getGroup();
		}

		protected boolean isSupported() {
			return !"X".equals(getGroup().getConfDefinition().getUsage());
		}

	}

	public class TreeNodeMessage extends TreeNodeGroupBase implements IDestroyable {
		private int myMessageIndex;
		private PropertyChangeListener myParsedMessageListener;

		public TreeNodeMessage(int theMessageIndex, final Hl7V2MessageBase theMessage) {
			super(theMessage);

			myMessageIndex = theMessageIndex;

			myParsedMessageListener = new PropertyChangeListener() {

				public void propertyChange(PropertyChangeEvent theEvt) {
					myUpdaterThread.scheduleUpdate();
				}
			};
			theMessage.addPropertyChangeListener(Hl7V2MessageBase.PARSED_MESSAGE_PROPERTY, myParsedMessageListener);

		}

		public void destroy() {
			((Hl7V2MessageBase) getUserObject()).removePropertyChangeListener(Hl7V2MessageBase.PARSED_MESSAGE_PROPERTY, myParsedMessageListener);
		}

		public Hl7V2MessageBase getMessage() {
			return (Hl7V2MessageBase) getUserObject();
		}

		/**
		 * @return the messageIndex
		 */
		public int getMessageIndex() {
			return myMessageIndex;
		}

		public Message getParsedMessage() {
			return getMessage().getParsedMessage();
		}
	}

	public class TreeNodeMessageConf extends TreeNodeMessage {

		public TreeNodeMessageConf(int theIndex, Hl7V2MessageBase theMessage) {
			super(theIndex, theMessage);
		}

		@Override
		public void doValidate() {
			try {
				HL7Exception[] problems = myRuntimeProfileValidator.validate(getParsedMessage(), getParsedMessage().getConfDefinition());
				addValidationExceptions(problems);
			} catch (ProfileException e) {
				addValidationExceptions(new HL7Exception(e));
			} catch (HL7Exception e) {
				addValidationExceptions(e);
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getDisplayName() {
			return getParsedMessage().getConfDefinition().getDescription();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public ConformanceMessage getParsedMessage() {
			return (ConformanceMessage) super.getParsedMessage();
		}

	}

	public class TreeNodePrimitive extends TreeNodeType {

		public TreeNodePrimitive(String theParentName, Primitive theGroup, String theGroupName, int theRepNum, boolean theRepeating, boolean theRequired, Segment theParent, List<Integer> theComponentPath, String theTerserPath) {
			super(theParentName, theGroup, theGroupName, theRepNum, theRepeating, theRequired, theParent, theComponentPath, theTerserPath);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void doValidate() {
			super.doValidate();

			Primitive primitive = getPrimitive();
			if (myMessages != null) {
				if (myMessages.getRuntimeProfile() != null) {

					// If we're using a conformance profile, also
					// use datatype validation as well
					String version = primitive.getMessage().getVersion();
					String typeName = primitive.getName();
					Collection<PrimitiveTypeRule> rules = ourDefaultValidation.getPrimitiveRules(version, typeName, primitive);
					for (PrimitiveTypeRule rule : rules) {
						if (!rule.test(primitive.getValue())) {
							addValidationExceptions(new HL7Exception(rule.getDescription()));
						}
					}

				} else if (myMessages.getValidationContext() != null) {

					String version = primitive.getMessage().getVersion();
					String type = primitive.getName();
					Collection<PrimitiveTypeRule> rules = myMessages.getValidationContext().getPrimitiveRules(version, type, primitive);
					for (PrimitiveTypeRule primitiveTypeRule : rules) {
						boolean test = primitiveTypeRule.test(primitive.getValue());
						if (!test) {
							// setErrorDescription(primitiveTypeRule.getDescription());
							addValidationExceptions(new HL7Exception(primitiveTypeRule.getDescription()));
						}
					}

				}

			}

		}

		@Override
		protected String getDataTypeDescription() {
			Primitive primitive = getPrimitive();
			if (primitive instanceof ID) {
				return super.getDataTypeDescription() + TBL + toHl7Table(((ID) primitive).getTable());
			}
			if (primitive instanceof IS) {
				return super.getDataTypeDescription() + TBL + toHl7Table(((IS) primitive).getTable());
			}
			return super.getDataTypeDescription();
		}

		public Primitive getPrimitive() {
			return (Primitive) getUserObject();
		}

		protected String getTable() {
			Primitive prim = getPrimitive();
			String namespace = TABLE_NAMESPACE_HL7;
			int retVal = 0;
			if (prim instanceof IS) {
				retVal = (((IS) prim).getTable());
			} else if (prim instanceof ID) {
				retVal = (((ID) prim).getTable());
			}
			return retVal > 0 ? namespace + toHl7Table(retVal) : null;
		}

		@Override
		public Boolean isHasContent() {
			Primitive p = (Primitive) getUserObject();
			String value = p.getValue();
			boolean retVal = value != null && value.length() > 0;
			if (retVal) {
				return retVal;
			}

			for (int i = 0; i < p.getExtraComponents().numComponents(); i++) {
				try {
					value = p.getExtraComponents().getComponent(i).encode();
				} catch (HL7Exception e) {
					return false;
				}
				retVal = value != null && value.length() > 0;
				if (retVal) {
					return retVal;
				}
			}

			return false;
		}

	}

	public class TreeNodePrimitiveConf extends TreeNodePrimitive {

		public TreeNodePrimitiveConf(String theParentName, ConformancePrimitive thePrimitive, String theGroupName, int theRepNum, boolean theRepeating, boolean theRequired, Segment theParent, List<Integer> theComponentPath, String theTerserPath) {
			super(theParentName, thePrimitive, theGroupName, theRepNum, theRepeating, theRequired, theParent, theComponentPath, theTerserPath);

		}

		@Override
		public void doValidate() {
			ConformancePrimitive primitive = getPrimitive();
			String tp = getTerserPath();

			EncodingCharacters enc;
			try {
				enc = EncodingCharacters.getInstance(primitive.getMessage());
			} catch (HL7Exception e) {
				ourLog.error("Could not get encoding chars", e);
				enc = new EncodingCharacters('|', null);
			}

			String encoded = PipeParser.encode(primitive, enc);
			if (tp.endsWith("MSH-1") || tp.endsWith("MSH-2")) {
				encoded = primitive.getValue();
			}

			List<HL7Exception> problems = myRuntimeProfileValidator.testType(getPrimitive(), getPrimitive().getConfDefinition(), encoded, "");
			addValidationExceptions(problems);

			if (myMessages.getRuntimeProfile() != null) {
				String table = getTable();
				if (table != null) {

					ConformanceMessage msg = getPrimitive().getMessage();
					String tablesId = msg.getTablesId();
					if (StringUtils.isNotBlank(tablesId)) {
						TableFile tableFile = myController.getTableFileList().getTableFile(tablesId);
						if (tableFile != null) {
							if (tableFile.knowsCodes(table)) {
								String value = StringUtils.defaultString(primitive.getValue());
								if (!tableFile.isValidCode(table, value)) {
									addValidationExceptions(new HL7Exception("Not a valid value in table '" + table + "': " + value));
								}
							}
						}
					}
				}
			}

			super.doValidate();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		protected String getDataTypeDescription() {
			String retVal = getPrimitive().getConfDefinition().getDatatype();
			String table = getPrimitive().getConfDefinition().getTable();
			if (StringUtils.isNotBlank(table)) {
				return retVal + TBL + table;
			} else {
				return retVal;
			}
		}

		@Override
		public String getDisplayName() {
			return getPrimitive().getConfDefinition().getName();
		}

		@Override
		public Integer getMaxLength() {
			return (int) getPrimitive().getConfDefinition().getLength();
		}

		public ConformancePrimitive getPrimitive() {
			return (ConformancePrimitive) super.getPrimitive();
		}

		@Override
		protected String getTable() {
			String retVal = getPrimitive().getConfDefinition().getTable();
			if (StringUtils.isBlank(retVal)) {
				return null;
			} else {
				if (AbstractNumericPrimitive.isInteger(retVal)) {
					return TABLE_NAMESPACE_HL7 + retVal;
				} else {
					return retVal;
				}
			}
		}

		protected boolean isSupported() {
			return !"X".equals(getPrimitive().getConfDefinition().getUsage());
		}

	}

	public class TreeNodeRoot extends TreeNodeBase implements IDestroyable {

		public TreeNodeRoot() {
			super(null);
		}

		public int countMessages() {
			int retVal = 0;
			for (int i = 0; i < getChildCount(); i++) {
				if (getChildAt(i) instanceof TreeNodeMessage) {
					retVal++;
				}
			}
			return retVal;
		}

		public void destroy() {
			for (int i = 0; i < getChildCount(); i++) {
				TreeNode next = getChildAt(i);
				if (next instanceof IDestroyable) {
					((IDestroyable) next).destroy();
				}
			}
		}

	}

	public class TreeNodeSegment extends TreeNodeBase {
		public TreeNodeSegment(Segment theSegment, String theGroupName, int theRepNum, boolean theRepeating, boolean theRequired, String theTerserPath) {
			super(theSegment, theGroupName, theRepNum, theRepeating, theRequired, theTerserPath);

			Validate.notNull(theTerserPath);
			Validate.isTrue(theTerserPath.startsWith("/"));
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public StringBuilder getNodeText() {
			StringBuilder retVal = super.getNodeText();

			if (isNonStandard()) {
				retVal.append("<font color=\"#A0A000\">");
				retVal.append(" (non standard)");
				retVal.append("</font>");
			}

			return retVal;
		}

		@Override
		public String getNodeTextColor() {
			return "#006000";
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getPipeEncodedValue() {
			EncodingCharacters enc;
			try {
				enc = EncodingCharacters.getInstance(getSegment().getMessage());
			} catch (HL7Exception e) {
				ourLog.error("Could not get encoding chars", e);
				enc = new EncodingCharacters('|', null);
			}
			return PipeParser.encode(getSegment(), enc);
		}

		public Segment getSegment() {
			return (Segment) getUserObject();
		}

		@Override
		public Boolean isHasContent() {
			return getPipeEncodedValue().length() > 3;
		}

		public boolean isNonStandard() {
			AbstractGroup parent = (AbstractGroup) getSegment().getParent();
			Set<String> nonStandardNames = parent.getNonStandardNames();
			String segmentName = getSegment().getName();
			return nonStandardNames.contains(segmentName);
		}

	}

	public class TreeNodeSegmentConf extends TreeNodeSegment {
		public TreeNodeSegmentConf(ConformanceSegment theSegment, String theGroupName, int theRepNum, boolean theRepeating, boolean theRequired, String theTerserPath) {
			super(theSegment, theGroupName, theRepNum, theRepeating, theRequired, theTerserPath);
		}

		@Override
		public void doValidate() {
			try {
				List<HL7Exception> problems = myRuntimeProfileValidator.testSegment(getSegment(), getSegment().getConfDefinition(), "");
				addValidationExceptions(problems);
			} catch (ProfileException e) {
				addValidationExceptions(new HL7Exception(e));
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getDisplayName() {
			return getSegment().getConfDefinition().getLongName();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Short getMaxReps() {
			return getSegment().getMaxReps();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Short getMinReps() {
			return getSegment().getMinReps();
		}

		public ConformanceSegment getSegment() {
			return (ConformanceSegment) getUserObject();
		}

		protected boolean isSupported() {
			return !"X".equals(getSegment().getConfDefinition().getUsage());
		}
	}

	public class TreeNodeType extends TreeNodeBase {
		private ArrayList<Integer> myComponentPath;
		private String myParentName;
		private Segment mySegment;

		public TreeNodeType(String theParentName, Type theGroup, String theGroupName, int theRepNum, boolean theRepeating, boolean theRequired, Segment theParent, List<Integer> theComponentPath, String theTerserPath) {
			super(theGroup, theGroupName, theRepNum, theRepeating, theRequired, theTerserPath);

			Validate.notNull(theParent);
			Validate.notNull(theComponentPath);
			Validate.notEmpty(theComponentPath);

			mySegment = theParent;
			myParentName = theParentName;
			myComponentPath = new ArrayList<Integer>(theComponentPath);
		}

		@Override
		public String getDisplayName() {
			// The field name/description is stored in myName from the constructor's theGroupName parameter
			// This gets passed through the parent TreeNodeBase constructor
			return getName();
		}

		protected String getDataTypeDescription() {
			return getType().getClass().getSimpleName();
		}

		public StringBuilder getNodeText() {
			StringBuilder b = new StringBuilder();

			b.append(myParentName);

			if (isRepeating() && (myShowRep0 || getRepNum() > 0)) {
				b.append("<font color=\"" + COLOR_REPNUM + "\">");
				b.append(" (rep");
				if (getRepNum() > 0) {
					b.append(' ');
					b.append(getRepNum() + 1);
				}
				b.append(")");
				b.append("</font>");
			}

			b.append("<font color=\"#00A000\">");

			// Try to get component name from display name
			String displayName = getDisplayName();
			if (StringUtils.isNotBlank(displayName)) {
				b.append(" - ");
				b.append(displayName);
				b.append(" ");
			} else if (myComponentPath.size() > 1) {
				// For components without a display name, try to extract from parent composite
				String componentName = extractComponentName();
				if (StringUtils.isNotBlank(componentName)) {
					b.append(" - ");
					b.append(componentName);
					b.append(" ");
				}
			}

			b.append(" (");
			b.append(getDataTypeDescription());
			b.append(")");
			b.append("</font>");

			return b;
		}

		private String extractComponentName() {
			try {
				// Get the parent composite
				if (myComponentPath.size() < 2) {
					return null;
				}

				int componentPosition = myComponentPath.get(myComponentPath.size() - 1);
				Type parentType = getType();

				// Walk up to find the composite
				Object current = getParent();
				while (current instanceof TreeNodeType) {
					TreeNodeType nodeParent = (TreeNodeType) current;
					Type type = nodeParent.getType();
					if (type instanceof Composite) {
						Composite composite = (Composite) type;
						java.lang.reflect.Method[] methods = composite.getClass().getDeclaredMethods();

						for (java.lang.reflect.Method method : methods) {
							String methodName = method.getName();
							if (methodName.matches("get.*" + componentPosition + "_.*")) {
								String[] parts = methodName.split("_");
								if (parts.length > 1) {
									String desc = parts[1];
									desc = desc.replaceAll("([a-z])([A-Z])", "$1 $2");
									return desc;
								}
							}
						}
						break;
					}
					current = nodeParent.getParent();
				}
			} catch (Exception e) {
				// Use default
			}
			return null;
		}


		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getPipeEncodedValue() {
			// Don't encode MSH-1 or 2 since this will escape them
			if (isMsh1orMsh2()) {
				return ((Primitive) getType()).getValue();
			}

			EncodingCharacters enc;
			try {
				enc = EncodingCharacters.getInstance(getType().getMessage());
			} catch (HL7Exception e) {
				ourLog.error("Could not get encoding chars", e);
				enc = new EncodingCharacters('|', null);
			}
			return PipeParser.encode(getType(), enc);
		}

		public SegmentAndComponentPath getSegmentAndComponentPath() {
			return new SegmentAndComponentPath(mySegment, myComponentPath, getRepNum() + 1);
		}

		public Type getType() {
			return (Type) getUserObject();
		}

		public boolean isMsh1orMsh2() {
			return "MSH-1".equals(myParentName) || "MSH-2".equals(myParentName);
		}

	}

	public class TreeNodeUnknown extends TreeNodeBase implements IDestroyable {
		private PropertyChangeListener myListener;
		private UnknownMessage myMessage;

		public TreeNodeUnknown(UnknownMessage theMessage) {
			super(theMessage);

			myMessage = theMessage;

			myListener = new PropertyChangeListener() {

				public void propertyChange(PropertyChangeEvent theEvt) {
					myTreeModel.nodeStructureChanged(myTop);
				}
			};
			myMessage.addPropertyChangeListener(UnknownMessage.PARSED_MESSAGE_PROPERTY, myListener);

		}

		/**
		 * {@inheritDoc}
		 */
		public void destroy() {
			myMessage.addPropertyChangeListener(UnknownMessage.PARSED_MESSAGE_PROPERTY, myListener);
		}

		@Override
		public StringBuilder getNodeText() {
			StringBuilder retVal = new StringBuilder();
			retVal.append("<html><font color=\"#FF0000\">Unknown</font><font color=\"#A0A0A0\"> ");

			int countLines = StringUtil.countLines(myMessage.getSourceMessage().trim());
			retVal.append(countLines);
			retVal.append(" Line");

			if (countLines != 1) {
				retVal.append("s");
			}

			retVal.append("</font></html>");
			return retVal;
		}

	}

	public class TreeNodeUnknownLine extends TreeNodeBase {
		public TreeNodeUnknownLine(Object theLine) {
			super(theLine);
		}

		@Override
		public StringBuilder getNodeText() {
			StringBuilder retVal = new StringBuilder();
			retVal.append("<html><font color=\"#4040A0\">");

			Object object = getUserObject();
			if (object != null) {
				retVal.append(xmlEncode(object.toString()));
			}

			retVal.append("</font></html>");
			return retVal;
		}

	}

	private class UpdaterThread extends Thread {
		private long myNextUpdate = 0;

		@Override
		public void run() {

			while (myNextUpdate > -1) {

				try {
					long sleepTime = myNextUpdate > 0 ? myNextUpdate - System.currentTimeMillis() : 5000;
					sleepTime = Math.max(0, sleepTime);
					sleepTime = Math.min(5000, sleepTime);

					try {
						Thread.sleep(sleepTime);
					} catch (InterruptedException e) {
						// ignore
					}

					if (myNextUpdate > 0 && myNextUpdate <= System.currentTimeMillis()) {

						try {
							addChildren();
						} catch (InterruptedException e) {
							ourLog.info("Interrupted during addChildren");
						} catch (InvocationTargetException e) {
							ourLog.error("Error during addChildren", e);
						}

						int messages = myTop.countMessages();
						int exceptions = myTop.countExceptions();

						if (myWorkingListener != null) {
							myWorkingListener.finishedWorking(messages + " messages, " + exceptions + " validation failures");
						}

						myNextUpdate = 0;
					}
				} catch (Exception e) {
					ourLog.error("Unexpected error in updater thread", e);
				}

			}

		}

		public void scheduleUpdate() {
			myNextUpdate = System.currentTimeMillis() + 2000;
			interrupt();

			if (myWorkingListener != null) {
				myWorkingListener.startedWorking();
			}
		}

		public void scheduleUpdateNow() {
			myNextUpdate = System.currentTimeMillis();
			interrupt();

			if (myWorkingListener != null) {
				myWorkingListener.startedWorking();
			}
		}

		public void stopThread() {
			myNextUpdate = -1;
			interrupt();
		}

	}

}
