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
import java.awt.Component;

import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;

public class ActivityCellRendererBase extends DefaultTableCellRenderer {
	private ActivityTable myTablePanel;

	public ActivityCellRendererBase(ActivityTable theTablePanel) {
		myTablePanel = theTablePanel;
	}
	
	/**
	 * @return the tablePanel
	 */
	public ActivityTable getTablePanel() {
		return myTablePanel;
	}

	/* (non-Javadoc)
	 * @see javax.swing.table.DefaultTableCellRenderer#getTableCellRendererComponent(javax.swing.JTable, java.lang.Object, boolean, boolean, int, int)
	 */
	@Override
	public Component getTableCellRendererComponent(JTable theTable, Object theValue, boolean theIsSelected, boolean theHasFocus, int theRow, int theColumn) {
		Component theComponent = super.getTableCellRendererComponent(theTable, theValue, theIsSelected, theHasFocus, theRow, theColumn);
		ActivityTable theTablePanel = myTablePanel;
		
		adjustBackground(theIsSelected, theRow, theComponent, theTablePanel);
		
		return theComponent;
	}

	public static void adjustBackground(boolean theIsSelected, int theRow, Component retVal, ActivityTable theTablePanel) {
		if (!theIsSelected) {
			Color tableBg = UIManager.getColor("Table.background");
			if (tableBg == null) tableBg = Color.WHITE;
			if (theTablePanel.isResponseAtRow(theRow)) {
				retVal.setBackground(responseRowColor(tableBg));
			} else {
				retVal.setBackground(tableBg);
			}
		}
	}

	private static Color responseRowColor(Color base) {
		return new Color(
			Math.max(0, base.getRed() - 15),
			Math.max(0, base.getGreen() - 15),
			Math.min(255, base.getBlue() + 30)
		);
	}

}
