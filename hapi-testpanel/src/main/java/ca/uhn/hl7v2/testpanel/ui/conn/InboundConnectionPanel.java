package ca.uhn.hl7v2.testpanel.ui.conn;

import java.awt.Color;
import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import ca.uhn.hl7v2.testpanel.controller.Controller;
import ca.uhn.hl7v2.testpanel.model.conn.AbstractConnection;
import ca.uhn.hl7v2.testpanel.model.conn.AbstractConnection.StatusEnum;
import ca.uhn.hl7v2.testpanel.model.conn.InboundConnection;
import ca.uhn.hl7v2.testpanel.ui.ActivityTable;
import ca.uhn.hl7v2.testpanel.ui.BaseMainPanel;
import ca.uhn.hl7v2.testpanel.ui.IDestroyable;
import net.miginfocom.swing.MigLayout;

public class InboundConnectionPanel extends BaseMainPanel implements IDestroyable {

	private ActivityTable myActivityTable;
	private InboundConnection myConnection;
	private PropertyChangeListener myConnectionsListener;
	private JTable myConnectionsTable;
	private ConnectionsTableModel myConnectionsTableModel;
	private Hl7ConnectionPanel mySettingPanelTab;
	private PropertyChangeListener myNameListener;
	private Hl7ConnectionPanelHeader myHeaderPanel;
	private JSplitPane myActivitySplitPaneTab;
	private JTabbedPane myTabbedPane;
	private PropertyChangeListener myStatusPropertyChangeListener;
	private ValidationHeaderPanel myValidationPanel;
	private VetoableChangeListener myNewMessagesPropertyListener;

	public InboundConnectionPanel(Controller theController) {
		setLayout(new MigLayout("wrap 1, insets 0", "[grow]", "[][grow]"));

		// ── Top: header + validation ───────────────────────────────────────────
		JPanel topPanel = new JPanel(new MigLayout("wrap 1, insets 0", "[grow]"));
		topPanel.setOpaque(false);

		myHeaderPanel = new Hl7ConnectionPanelHeader();
		topPanel.add(myHeaderPanel, "growx");

		myValidationPanel = new ValidationHeaderPanel(theController);
		topPanel.add(myValidationPanel, "growx");

		add(topPanel, "growx");

		// ── Centre: tabbed content ─────────────────────────────────────────────
		myTabbedPane = new JTabbedPane(JTabbedPane.TOP);
		myTabbedPane.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
		add(myTabbedPane, "grow, push");

		// Settings tab
		mySettingPanelTab = new Hl7ConnectionPanel(theController);
		mySettingPanelTab.setBorder(null);
		myTabbedPane.addTab("Connection Settings", mySettingPanelTab);
		myTabbedPane.setToolTipTextAt(0, "Configure transport, validation, security, and message settings");

		// Activity tab
		myActivitySplitPaneTab = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		myActivitySplitPaneTab.setResizeWeight(0.3);
		myActivitySplitPaneTab.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		myTabbedPane.addTab("Activity", myActivitySplitPaneTab);
		myTabbedPane.setToolTipTextAt(1, "View connected clients and recent receiving activity");

		// Left: connected clients
		JPanel connectionsPanel = new JPanel(new MigLayout("wrap 1, insets 0", "[grow]", "[][grow]"));
		connectionsPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 4));
		connectionsPanel.setOpaque(false);
		connectionsPanel.add(sectionLabel("Connections"), "growx");

		myConnectionsTableModel = new ConnectionsTableModel();
		myConnectionsTable = new JTable(myConnectionsTableModel);
		myConnectionsTable.setFillsViewportHeight(true);
		JScrollPane connectionsScroll = new JScrollPane(myConnectionsTable);
		connectionsScroll.setBorder(BorderFactory.createLineBorder(
				UIManager.getColor("Separator.foreground") != null
						? UIManager.getColor("Separator.foreground")
						: new Color(0xD1, 0xD5, 0xDB)));
		connectionsPanel.add(connectionsScroll, "grow, push");
		myActivitySplitPaneTab.setLeftComponent(connectionsPanel);

		// Right: activity log
		JPanel activityPanel = new JPanel(new MigLayout("wrap 1, insets 0", "[grow]", "[][grow]"));
		activityPanel.setOpaque(false);
		activityPanel.add(sectionLabel("Activity"), "growx");

		myActivityTable = new ActivityTable();
		myActivityTable.getScrollPane().setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		myActivityTable.getScrollPane().setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		myActivityTable.setController(theController);
		activityPanel.add(myActivityTable, "grow, push");
		myActivitySplitPaneTab.setRightComponent(activityPanel);
	}

	private static JLabel sectionLabel(String text) {
		JLabel label = new JLabel(text);
		label.setFont(label.getFont().deriveFont(Font.BOLD, 11f));
		label.setForeground(UIManager.getColor("Label.disabledForeground") != null
				? UIManager.getColor("Label.disabledForeground")
				: new Color(0x6B, 0x72, 0x80));
		label.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
		return label;
	}

	public void destroy() {
		mySettingPanelTab.destroy();
		myConnection.removePropertyChangeListener(InboundConnection.CONNECTIONS_PROPERTY, myConnectionsListener);
		myConnection.removePropertyChangeListener(InboundConnection.NAME_PROPERTY, myNameListener);
		myActivityTable.destroy();
		myHeaderPanel.destroy();
		myValidationPanel.destroy();
		myConnection.removePropertyChangeListener(AbstractConnection.STATUS_PROPERTY, myStatusPropertyChangeListener);
		myConnection.removeVetoableChangeListener(AbstractConnection.NEW_MESSAGES_PROPERTY, myNewMessagesPropertyListener);
	}

	public void setConnection(InboundConnection theConnection) {
		mySettingPanelTab.setConnection(theConnection);
		myHeaderPanel.setConnection(theConnection);
		myConnection = theConnection;
		myActivityTable.setConnection(theConnection);
		myValidationPanel.setConnection(theConnection);

		theConnection.clearNewMessages();
		myNewMessagesPropertyListener = new VetoableChangeListener() {
			public void vetoableChange(PropertyChangeEvent theEvt) throws PropertyVetoException {
				if (theEvt.getPropertyName() == AbstractConnection.NEW_MESSAGES_PROPERTY) {
					Integer oldValue = (Integer) theEvt.getOldValue();
					Integer newValue = (Integer) theEvt.getNewValue();
					if (oldValue != null && newValue != null && newValue > oldValue) {
						throw new PropertyVetoException("", theEvt);
					}
				}
			}
		};
		myConnection.addVetoableyChangeListener(AbstractConnection.NEW_MESSAGES_PROPERTY, myNewMessagesPropertyListener);

		myConnectionsListener = new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent theEvt) {
				myConnectionsTableModel.update();
			}
		};
		theConnection.addPropertyChangeListener(InboundConnection.CONNECTIONS_PROPERTY, myConnectionsListener);
		myConnectionsTableModel.update();

		myNameListener = new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent theEvt) {
				updateWindowTitle();
			}
		};
		theConnection.addPropertyChangeListener(InboundConnection.NAME_PROPERTY, myNameListener);
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

	private void updateWindowTitle() {
		setWindowTitle(myConnection.getName());
	}

	private class ConnectionsTableModel implements TableModel {

		private List<TableModelListener> myTableListeners = new ArrayList<TableModelListener>();

		public void addTableModelListener(TableModelListener theL) {
			myTableListeners.add(theL);
		}

		public Class<?> getColumnClass(int theColumnIndex) {
			return String.class;
		}

		public int getColumnCount() {
			return 1;
		}

		public String getColumnName(int theColumnIndex) {
			return "Address";
		}

		public int getRowCount() {
			return myConnection != null ? myConnection.getConnections().size() : 0;
		}

		public Object getValueAt(int theRowIndex, int theColumnIndex) {
			InetAddress address = myConnection.getConnections().get(theRowIndex).getRemoteAddress();
			int port = myConnection.getConnections().get(theRowIndex).getRemotePort();
			return address.getCanonicalHostName() + ":" + port;
		}

		public boolean isCellEditable(int theRowIndex, int theColumnIndex) {
			return false;
		}

		public void removeTableModelListener(TableModelListener theL) {
			myTableListeners.remove(theL);
		}

		public void setValueAt(Object theAValue, int theRowIndex, int theColumnIndex) {
			throw new UnsupportedOperationException();
		}

		public void update() {
			for (TableModelListener next : myTableListeners) {
				next.tableChanged(new TableModelEvent(this));
			}
		}
	}

}
