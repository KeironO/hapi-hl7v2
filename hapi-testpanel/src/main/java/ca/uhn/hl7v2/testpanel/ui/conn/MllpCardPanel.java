package ca.uhn.hl7v2.testpanel.ui.conn;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.event.DocumentEvent;
import java.util.List;

import ca.uhn.hl7v2.testpanel.model.conn.AbstractConnection;
import ca.uhn.hl7v2.testpanel.model.conn.AbstractConnection.StatusEnum;
import ca.uhn.hl7v2.testpanel.ui.IDestroyable;
import ca.uhn.hl7v2.testpanel.util.SimpleDocumentListener;
import net.miginfocom.swing.MigLayout;

public class MllpCardPanel extends JPanel implements IDestroyable {

	private JTextField myHostBox;
	private JCheckBox myTlsCheckbox;
	private JLabel myValidationLabel;
	private AbstractConnection myConnection;

	public MllpCardPanel() {
		setLayout(new MigLayout("wrap 2, insets 0", "[150!][grow]", "[][][][]"));

		// ── Host ──────────────────────────────────────────────────────────
		JLabel lblHost = new JLabel("Host");
		add(lblHost, "gapbottom 5");

		JPanel hostPanel = new JPanel(new MigLayout("insets 5, fillx", "[grow]"));
		hostPanel.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
		add(hostPanel, "growx, gapbottom 5");

		myHostBox = new JTextField();
		myHostBox.setToolTipText("Hostname or IP address of the remote MLLP server");
		myHostBox.getDocument().addDocumentListener(new SimpleDocumentListener() {
			@Override
			public void update(DocumentEvent theE) {
				myConnection.setHost(myHostBox.getText());
			}
		});
		hostPanel.add(myHostBox, "growx");

		// ── Transport ─────────────────────────────────────────────────────
		JLabel lblTransport = new JLabel("Transport");
		add(lblTransport, "gapbottom 5");

		JPanel transportPanel = new JPanel(new MigLayout("insets 5", "[grow]"));
		transportPanel.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
		add(transportPanel, "growx, gapbottom 5");

		myTlsCheckbox = new JCheckBox("Use TLS/SSL");
		myTlsCheckbox.setToolTipText("Encrypt MLLP traffic using TLS/SSL");
		myTlsCheckbox.addActionListener(e -> {
			myConnection.setTls(myTlsCheckbox.isSelected());
		});
		transportPanel.add(myTlsCheckbox);

		myValidationLabel = new JLabel(" ");
		add(myValidationLabel, "span 2, growx");
	}

	public void setConnection(AbstractConnection theConnection) {
		myConnection = theConnection;
		myHostBox.setText(theConnection.getHost());
		myTlsCheckbox.setSelected(theConnection.isTls());
	}

	public void setTlsSelected(boolean selected) {
		myTlsCheckbox.setSelected(selected);
	}

	public boolean isTlsSelected() {
		return myTlsCheckbox.isSelected();
	}

	public void updateStatus() {
		boolean changesAllowed = myConnection.getStatus() == StatusEnum.STOPPED
				|| myConnection.getStatus() == StatusEnum.FAILED;
		myHostBox.setEnabled(changesAllowed);
		myTlsCheckbox.setEnabled(changesAllowed);
	}

	public void setValidationErrors(List<ConnectionValidator.ValidationError> theErrors) {
		String hostError = ConnectionValidationUi.getError(theErrors, "host");
		ConnectionValidationUi.apply(myHostBox, hostError);
		ConnectionValidationUi.setMessage(myValidationLabel, hostError);
	}

	@Override
	public void destroy() {
		// no listeners to clean up
	}
}
