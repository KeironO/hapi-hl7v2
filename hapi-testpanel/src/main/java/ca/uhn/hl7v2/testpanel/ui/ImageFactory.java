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
package ca.uhn.hl7v2.testpanel.ui;

import java.awt.Color;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.UIManager;

import com.formdev.flatlaf.extras.FlatSVGIcon;

/**
 * Factory for application icons. SVG icons are loaded via FlatSVGIcon so that
 * FlatLaf can apply its colour filter for dark-mode support automatically.
 */
public class ImageFactory {

	private static Map<String, Icon> ourIcons = new HashMap<String, Icon>();

	public static void clearCache() {
		ourIcons.clear();
	}

	// -------------------------------------------------------------------------
	// Helper methods
	// -------------------------------------------------------------------------

	private static final FlatSVGIcon.ColorFilter FOREGROUND_FILTER = new FlatSVGIcon.ColorFilter() {
		@Override
		public Color filter(Color color) {
			Color fg = UIManager.getColor("Label.foreground");
			return fg != null ? fg : color;
		}
	};

	private static Icon getSVGIcon(String path) {
		Icon retVal = ourIcons.get(path);
		if (retVal == null) {
			FlatSVGIcon icon = new FlatSVGIcon(path, 18, 18);
			icon.setColorFilter(FOREGROUND_FILTER);
			retVal = icon;
			ourIcons.put(path, retVal);
		}
		return retVal;
	}

	/** Kept for hapi_64.png which is used as an AWT window icon (must be raster). */
	public static ImageIcon getHapi64() {
		String location = "ca/uhn/hl7v2/testpanel/images/hapi_64.png";
		Icon cached = ourIcons.get(location);
		if (cached instanceof ImageIcon) {
			return (ImageIcon) cached;
		}
		URL resource = ImageFactory.class.getClassLoader().getResource(location);
		if (resource == null) {
			throw new Error(location);
		}
		ImageIcon icon = new ImageIcon(resource);
		ourIcons.put(location, icon);
		return icon;
	}

	// -------------------------------------------------------------------------
	// Original mapped methods (PNG -> SVG)
	// -------------------------------------------------------------------------

	public static Icon getButtonExecute() {
		return getSVGIcon("ca/uhn/hl7v2/testpanel/icons/player-play.svg");
	}

	public static Icon getProfile() {
		return getSVGIcon("ca/uhn/hl7v2/testpanel/icons/id-badge-2.svg");
	}

	public static Icon getProfileGroup() {
		return getSVGIcon("ca/uhn/hl7v2/testpanel/icons/folder-plus.svg");
	}

	public static Icon getFile() {
		return getSVGIcon("ca/uhn/hl7v2/testpanel/icons/file.svg");
	}

	public static Icon getTable() {
		return getSVGIcon("ca/uhn/hl7v2/testpanel/icons/table.svg");
	}

	public static Icon getNo() {
		return getSVGIcon("ca/uhn/hl7v2/testpanel/icons/ban.svg");
	}

	public static Icon getInterfaceOff() {
		return getSVGIcon("ca/uhn/hl7v2/testpanel/icons/plug-x.svg");
	}

	public static Icon getInterfaceOn() {
		return getSVGIcon("ca/uhn/hl7v2/testpanel/icons/plug-connected.svg");
	}

	public static Icon getInterfaceStarting() {
		return getSVGIcon("ca/uhn/hl7v2/testpanel/icons/loader.svg");
	}

	public static Icon getMessageHl7() {
		return getSVGIcon("ca/uhn/hl7v2/testpanel/icons/file-text.svg");
	}

	public static Icon getMessageIn() {
		return getSVGIcon("ca/uhn/hl7v2/testpanel/icons/inbox.svg");
	}

	public static Icon getMessageOut() {
		return getSVGIcon("ca/uhn/hl7v2/testpanel/icons/send.svg");
	}

	public static Icon getMessageXml() {
		return getSVGIcon("ca/uhn/hl7v2/testpanel/icons/file-code.svg");
	}

	public static Icon getTabLog() {
		return getSVGIcon("ca/uhn/hl7v2/testpanel/icons/terminal.svg");
	}

	public static Icon getTest() {
		return getSVGIcon("ca/uhn/hl7v2/testpanel/icons/test-pipe.svg");
	}

	public static Icon getTestFailed() {
		return getSVGIcon("ca/uhn/hl7v2/testpanel/icons/test-pipe-off.svg");
	}

	public static Icon getTestPassed() {
		return getSVGIcon("ca/uhn/hl7v2/testpanel/icons/circle-check.svg");
	}

	public static Icon getTestRunning() {
		return getSVGIcon("ca/uhn/hl7v2/testpanel/icons/loader.svg");
	}

	public static Icon getTreeBundle() {
		return getSVGIcon("ca/uhn/hl7v2/testpanel/icons/folder.svg");
	}

	public static Icon getTreeLeaf() {
		return getSVGIcon("ca/uhn/hl7v2/testpanel/icons/file.svg");
	}

	public static Icon getValFailed() {
		return getSVGIcon("ca/uhn/hl7v2/testpanel/icons/circle-x.svg");
	}

	public static Icon getValFailedChild() {
		return getSVGIcon("ca/uhn/hl7v2/testpanel/icons/alert-circle.svg");
	}

	public static Icon getValPassed() {
		return getSVGIcon("ca/uhn/hl7v2/testpanel/icons/circle-check.svg");
	}

	public static Icon getValPassedGreen() {
		return getSVGIcon("ca/uhn/hl7v2/testpanel/icons/circle-check.svg");
	}

	// -------------------------------------------------------------------------
	// New methods
	// -------------------------------------------------------------------------

	public static Icon getAdd() {
		return getSVGIcon("ca/uhn/hl7v2/testpanel/icons/plus.svg");
	}

	public static Icon getDelete() {
		return getSVGIcon("ca/uhn/hl7v2/testpanel/icons/trash.svg");
	}

	public static Icon getClose() {
		return getSVGIcon("ca/uhn/hl7v2/testpanel/icons/x.svg");
	}

	public static Icon getRename() {
		return getSVGIcon("ca/uhn/hl7v2/testpanel/icons/pencil.svg");
	}

	public static Icon getSave() {
		return getSVGIcon("ca/uhn/hl7v2/testpanel/icons/device-floppy.svg");
	}

	public static Icon getSaveAll() {
		return getSVGIcon("ca/uhn/hl7v2/testpanel/icons/files.svg");
	}

	public static Icon getOpen() {
		return getSVGIcon("ca/uhn/hl7v2/testpanel/icons/folder-open.svg");
	}

	public static Icon getStartOne() {
		return getSVGIcon("ca/uhn/hl7v2/testpanel/icons/player-play.svg");
	}

	public static Icon getStartAll() {
		return getSVGIcon("ca/uhn/hl7v2/testpanel/icons/player-play.svg");
	}

	public static Icon getStop() {
		return getSVGIcon("ca/uhn/hl7v2/testpanel/icons/player-stop.svg");
	}

	public static Icon getStopAll() {
		return getSVGIcon("ca/uhn/hl7v2/testpanel/icons/player-stop.svg");
	}

	public static Icon getClear() {
		return getSVGIcon("ca/uhn/hl7v2/testpanel/icons/trash.svg");
	}

	public static Icon getEditOne() {
		return getSVGIcon("ca/uhn/hl7v2/testpanel/icons/pencil.svg");
	}

	public static Icon getEditAll() {
		return getSVGIcon("ca/uhn/hl7v2/testpanel/icons/edit.svg");
	}

	public static Icon getNewMessage() {
		return getSVGIcon("ca/uhn/hl7v2/testpanel/icons/file-text.svg");
	}

	public static Icon getMoveTaskUp() {
		return getSVGIcon("ca/uhn/hl7v2/testpanel/icons/arrow-up.svg");
	}

	public static Icon getMoveTaskDown() {
		return getSVGIcon("ca/uhn/hl7v2/testpanel/icons/arrow-down.svg");
	}

	public static Icon getFollow() {
		return getSVGIcon("ca/uhn/hl7v2/testpanel/icons/arrows-up-down.svg");
	}

	public static Icon getWrap() {
		return getSVGIcon("ca/uhn/hl7v2/testpanel/icons/text-wrap.svg");
	}

	public static Icon getCollapseAll() {
		return getSVGIcon("ca/uhn/hl7v2/testpanel/icons/fold.svg");
	}

	public static Icon getExpandAll() {
		return getSVGIcon("ca/uhn/hl7v2/testpanel/icons/unfold.svg");
	}

	public static Icon getInfoOk() {
		return getSVGIcon("ca/uhn/hl7v2/testpanel/icons/circle-check.svg");
	}

	public static Icon getInfoWarning() {
		return getSVGIcon("ca/uhn/hl7v2/testpanel/icons/alert-triangle.svg");
	}

	public static Icon getInfoWorking() {
		return getSVGIcon("ca/uhn/hl7v2/testpanel/icons/loader.svg");
	}

}
