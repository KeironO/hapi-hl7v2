package ca.uhn.hl7v2.testpanel.ui.conn;

import java.awt.CardLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;

import ca.uhn.hl7v2.testpanel.controller.Controller;
import ca.uhn.hl7v2.testpanel.model.conn.AbstractConnection;
import ca.uhn.hl7v2.testpanel.model.conn.AbstractConnection.StatusEnum;
import ca.uhn.hl7v2.testpanel.model.conn.OutboundConnection;
import ca.uhn.hl7v2.testpanel.ui.IDestroyable;
import net.miginfocom.swing.MigLayout;

public class Hl7ConnectionPanel extends JPanel implements IDestroyable {

	private static final String IFACE_TYPE_CARD_HOH = "hohCard";
	private static final String IFACE_TYPE_CARD_MLLP = "mllpCard";

	private TransportPanel myTransportPanel;
	private EncodingPanel myEncodingPanel;
	private CharsetPanel myCharsetPanel;
	private DebugPanel myDebugPanel;
	private MllpCardPanel myMllpCardPanel;
	private HohCardPanel myHohCardPanel;
	private JPanel myInterfaceTypeCardPanel;

	private AbstractConnection myConnection;
	private PropertyChangeListener myStatusPropertyChangeListener;
	private PropertyChangeListener myStatusLinePropertyChangeListener;
	private PropertyChangeListener myTlsPropertyChangeListener;

	public Hl7ConnectionPanel(Controller theController) {
		setBorder(new EtchedBorder(EtchedBorder.LOWERED));
		setLayout(new MigLayout("wrap 2, insets 5", "[150!][grow]", "[][][][][][grow]"));

		// Row 0: Transport (radio buttons + port fields)
		myTransportPanel = new TransportPanel(this::onTransportChanged);
		add(myTransportPanel, "span 2, growx");

		// Row 1: Encoding
		add(new JLabel("Encoding"));
		myEncodingPanel = new EncodingPanel();
		add(myEncodingPanel, "growx");

		// Row 2: Charset
		add(new JLabel("Charset"));
		myCharsetPanel = new CharsetPanel();
		add(myCharsetPanel, "growx");

		// Row 3: Debug
		add(new JLabel("Debug"));
		myDebugPanel = new DebugPanel();
		add(myDebugPanel, "growx");

		// Row 4: CardLayout for MLLP / HoH cards
		myInterfaceTypeCardPanel = new JPanel(new CardLayout(0, 0));
		add(myInterfaceTypeCardPanel, "span 2, growx, growy");

		myMllpCardPanel = new MllpCardPanel();
		myInterfaceTypeCardPanel.add(myMllpCardPanel, IFACE_TYPE_CARD_MLLP);

		myHohCardPanel = new HohCardPanel();
		myInterfaceTypeCardPanel.add(myHohCardPanel, IFACE_TYPE_CARD_HOH);
	}

	private void onTransportChanged() {
		CardLayout cl = (CardLayout) myInterfaceTypeCardPanel.getLayout();
		if (myTransportPanel.isHoHSelected()) {
			cl.show(myInterfaceTypeCardPanel, IFACE_TYPE_CARD_HOH);
		} else {
			cl.show(myInterfaceTypeCardPanel, IFACE_TYPE_CARD_MLLP);
		}
	}

	public void setConnection(AbstractConnection theConnection) {
		if (myConnection != null) {
			destroy();
		}

		myConnection = theConnection;

		myTransportPanel.setConnection(theConnection);
		myEncodingPanel.setConnection(theConnection);
		myCharsetPanel.setConnection(theConnection);
		myDebugPanel.setConnection(theConnection);
		myMllpCardPanel.setConnection(theConnection);
		myHohCardPanel.setConnection(theConnection);

		onTransportChanged();

		myStatusPropertyChangeListener = new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent theEvt) {
				updateStatus();
			}
		};
		myConnection.addPropertyChangeListener(
				OutboundConnection.STATUS_PROPERTY, myStatusPropertyChangeListener);

		myStatusLinePropertyChangeListener = new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent theEvt) {
				updateStatus();
			}
		};
		myConnection.addPropertyChangeListener(
				OutboundConnection.STATUS_LINE_PROPERTY, myStatusLinePropertyChangeListener);

		myTlsPropertyChangeListener = new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent theEvt) {
				synchronizeTlsUi();
			}
		};
		myConnection.addPropertyChangeListener(
				AbstractConnection.TLS_PROPERTY, myTlsPropertyChangeListener);

		updateStatus();
	}

	private void updateStatus() {
		boolean changesAllowed = myConnection.getStatus() == StatusEnum.STOPPED
				|| myConnection.getStatus() == StatusEnum.FAILED;

		synchronizeTlsUi();

		myTransportPanel.updateStatus();
		myEncodingPanel.updateStatus();
		myCharsetPanel.updateStatus();
		myDebugPanel.updateStatus();
		myMllpCardPanel.updateStatus();
		myHohCardPanel.updateStatus();
	}

	private void synchronizeTlsUi() {
		myMllpCardPanel.setTlsSelected(myConnection.isTls());
		myHohCardPanel.setTlsSelected(myConnection.isTls());
	}

	public void setValidationErrors(List<ConnectionValidator.ValidationError> theErrors) {
		myTransportPanel.setValidationErrors(theErrors);
		myMllpCardPanel.setValidationErrors(theErrors);
		myHohCardPanel.setValidationErrors(theErrors);
	}

	@Override
	public void destroy() {
		if (myConnection != null) {
			myConnection.removePropertyChangeListener(
					OutboundConnection.STATUS_PROPERTY, myStatusPropertyChangeListener);
			myConnection.removePropertyChangeListener(
					OutboundConnection.STATUS_LINE_PROPERTY, myStatusLinePropertyChangeListener);
			myConnection.removePropertyChangeListener(
					AbstractConnection.TLS_PROPERTY, myTlsPropertyChangeListener);
		}
		myTransportPanel.destroy();
		myEncodingPanel.destroy();
		myCharsetPanel.destroy();
		myDebugPanel.destroy();
		myMllpCardPanel.destroy();
		myHohCardPanel.destroy();
	}
}
