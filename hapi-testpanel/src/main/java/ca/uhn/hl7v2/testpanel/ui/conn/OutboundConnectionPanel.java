package ca.uhn.hl7v2.testpanel.ui.conn;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import ca.uhn.hl7v2.testpanel.controller.Controller;
import ca.uhn.hl7v2.testpanel.model.conn.AbstractConnection;
import ca.uhn.hl7v2.testpanel.model.conn.AbstractConnection.StatusEnum;
import ca.uhn.hl7v2.testpanel.model.conn.OutboundConnection;
import ca.uhn.hl7v2.testpanel.ui.ActivityTable;
import ca.uhn.hl7v2.testpanel.ui.BaseMainPanel;
import ca.uhn.hl7v2.testpanel.ui.IDestroyable;

public class OutboundConnectionPanel extends BaseMainPanel implements IDestroyable {

	private ActivityTable myActivityTable;
	private OutboundConnection myConnection;
	private PropertyChangeListener myNameListener;
	private Hl7ConnectionPanel mySettingPanel;
	private JTabbedPane myTabbedPane;
	private Hl7ConnectionPanelHeader myHeaderPanel;
	private PropertyChangeListener myStatusPropertyChangeListener;
	private Controller myController;

	public OutboundConnectionPanel(Controller theController) {
		myController = theController;
		setLayout(new BorderLayout(0, 0));

		myHeaderPanel = new Hl7ConnectionPanelHeader();
		add(myHeaderPanel, BorderLayout.NORTH);

		myTabbedPane = new JTabbedPane(JTabbedPane.TOP);
		myTabbedPane.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
		add(myTabbedPane, BorderLayout.CENTER);

		// Settings tab
		mySettingPanel = new Hl7ConnectionPanel(myController);
		mySettingPanel.setBorder(null);
		myTabbedPane.addTab("Settings", mySettingPanel);

		// Activity tab
		JPanel activityTab = new JPanel(new BorderLayout(0, 0));
		activityTab.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		myActivityTable = new ActivityTable();
		activityTab.add(myActivityTable, BorderLayout.CENTER);
		myTabbedPane.addTab("Activity", activityTab);
	}

	public void destroy() {
		mySettingPanel.destroy();
		myActivityTable.destroy();
		myHeaderPanel.destroy();
		myConnection.removePropertyChangeListener(OutboundConnection.NAME_PROPERTY, myNameListener);
		myConnection.removePropertyChangeListener(AbstractConnection.STATUS_PROPERTY, myStatusPropertyChangeListener);
	}

	public void setConnection(OutboundConnection theConnection) {
		mySettingPanel.setConnection(theConnection);
		myHeaderPanel.setConnection(theConnection);
		myActivityTable.setConnection(theConnection);
		myConnection = theConnection;

		myNameListener = new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent theEvt) {
				updateWindowTitle();
			}
		};
		theConnection.addPropertyChangeListener(OutboundConnection.NAME_PROPERTY, myNameListener);
		updateWindowTitle();

		myTabbedPane.setSelectedIndex(
				theConnection.getStatus() == StatusEnum.STARTED
						|| theConnection.getStatus() == StatusEnum.TRYING_TO_START ? 1 : 0);

		myStatusPropertyChangeListener = new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent theEvt) {
				StatusEnum oldVal = (StatusEnum) theEvt.getOldValue();
				StatusEnum newVal = (StatusEnum) theEvt.getNewValue();
				if (oldVal == StatusEnum.STOPPED
						&& (newVal == StatusEnum.TRYING_TO_START || newVal == StatusEnum.STARTED)) {
					myTabbedPane.setSelectedIndex(1);
				} else if ((oldVal == StatusEnum.TRYING_TO_START || oldVal == StatusEnum.STARTED)
						&& newVal == StatusEnum.STOPPED) {
					myTabbedPane.setSelectedIndex(0);
				}
			}
		};
		myConnection.addPropertyChangeListener(AbstractConnection.STATUS_PROPERTY, myStatusPropertyChangeListener);
	}

	public void setController(Controller theController) {
		myActivityTable.setController(theController);
	}

	private void updateWindowTitle() {
		setWindowTitle(myConnection.getName());
	}

}
