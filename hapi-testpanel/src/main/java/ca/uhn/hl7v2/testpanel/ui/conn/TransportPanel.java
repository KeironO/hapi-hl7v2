package ca.uhn.hl7v2.testpanel.ui.conn;

import java.awt.Color;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import javax.swing.event.DocumentEvent;

import org.apache.commons.lang.StringUtils;

import ca.uhn.hl7v2.testpanel.model.conn.AbstractConnection;
import ca.uhn.hl7v2.testpanel.model.conn.AbstractConnection.StatusEnum;
import ca.uhn.hl7v2.testpanel.model.conn.TransportStyleEnum;
import ca.uhn.hl7v2.testpanel.ui.IDestroyable;
import ca.uhn.hl7v2.testpanel.util.SimpleDocumentListener;
import net.miginfocom.swing.MigLayout;

public class TransportPanel extends JPanel implements IDestroyable {

	private static final Color ERROR_BG = new Color(1.0f, 0.8f, 0.8f);

	private final ButtonGroup myPortButtonGroup = new ButtonGroup();
	private JRadioButton mySinglePortRadio;
	private JRadioButton myDualPortRadio;
	private JRadioButton myHl7OverHttpRadioButton;
	private JTextField mySinglePortTextBox;
	private JTextField myDualIncomingTextBox;
	private JTextField myDualOutgoingTextBox;
	private JTextField myHoHUrlTextField;
	private JLabel myValidationLabel;
	private boolean myHohUrlTextFieldUpdating;

	private AbstractConnection myConnection;
	private Runnable myOnTransportChanged;

	public TransportPanel(Runnable theOnTransportChanged) {
		myOnTransportChanged = theOnTransportChanged;

		setLayout(new MigLayout("wrap 2, insets 0", "[][grow]", "[][][][]"));

		// ── Single Port MLLP ────────────────────────────────────────────────
		mySinglePortRadio = new JRadioButton("Single Port MLLP");
		mySinglePortRadio.setToolTipText("Send and receive acknowledgements using one MLLP port");
		mySinglePortRadio.addActionListener(e -> {
			updatePortsModel();
			updatePortsUi();
			if (myOnTransportChanged != null) {
				myOnTransportChanged.run();
			}
		});
		myPortButtonGroup.add(mySinglePortRadio);
		add(mySinglePortRadio, "gapbottom 5");

		JPanel singlePortPanel = new JPanel(new MigLayout("insets 5", "[][100:]"));
		singlePortPanel.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
		add(singlePortPanel, "growx, gapbottom 5");

		singlePortPanel.add(new JLabel("Port"));
		mySinglePortTextBox = new JTextField(10);
		mySinglePortTextBox.setToolTipText("MLLP port number (1-65535)");
		mySinglePortTextBox.getDocument().addDocumentListener(new SimpleDocumentListener() {
			@Override
			public void update(DocumentEvent theE) {
				String text = mySinglePortTextBox.getText();
				text = text.replaceAll("[^0-9]+", "");
				if (!StringUtils.equals(mySinglePortTextBox.getText(), text)) {
					String newVal = text;
					SwingUtilities.invokeLater(() -> mySinglePortTextBox.setText(newVal));
				}
				if (mySinglePortRadio.isSelected()) {
					if (text.length() > 0) {
						myConnection.setIncomingOrSinglePort(Integer.parseInt(text));
					} else {
						myConnection.setIncomingOrSinglePort(-1);
					}
				}
			}
		});
		singlePortPanel.add(mySinglePortTextBox, "growx");

		// ── Dual Port MLLP ──────────────────────────────────────────────────
		myDualPortRadio = new JRadioButton("Dual Port MLLP");
		myDualPortRadio.setToolTipText("Use separate MLLP ports for outbound messages and inbound acknowledgements");
		myDualPortRadio.addActionListener(e -> {
			updatePortsModel();
			updatePortsUi();
			if (myOnTransportChanged != null) {
				myOnTransportChanged.run();
			}
		});
		myPortButtonGroup.add(myDualPortRadio);
		add(myDualPortRadio, "gapbottom 5");

		JPanel dualPortPanel = new JPanel(new MigLayout("insets 5", "[][100!][][100!]"));
		dualPortPanel.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
		add(dualPortPanel, "growx, gapbottom 5");

		dualPortPanel.add(new JLabel("Inbound"));
		myDualIncomingTextBox = new JTextField(10);
		myDualIncomingTextBox.setToolTipText("Port used to receive acknowledgements (1-65535)");
		myDualIncomingTextBox.getDocument().addDocumentListener(new SimpleDocumentListener() {
			@Override
			public void update(DocumentEvent theE) {
				String text = myDualIncomingTextBox.getText();
				text = text.replaceAll("[^0-9]+", "");
				if (!StringUtils.equals(myDualIncomingTextBox.getText(), text)) {
					String newVal = text;
					SwingUtilities.invokeLater(() -> myDualIncomingTextBox.setText(newVal));
				}
				if (myDualPortRadio.isSelected()) {
					if (text.length() > 0) {
						myConnection.setIncomingOrSinglePort(Integer.parseInt(text));
					} else {
						myConnection.setIncomingOrSinglePort(-1);
					}
				}
			}
		});
		dualPortPanel.add(myDualIncomingTextBox, "growx");

		dualPortPanel.add(new JLabel("Outbound"));
		myDualOutgoingTextBox = new JTextField(10);
		myDualOutgoingTextBox.setToolTipText("Port used to send messages (1-65535)");
		myDualOutgoingTextBox.getDocument().addDocumentListener(new SimpleDocumentListener() {
			@Override
			public void update(DocumentEvent theE) {
				String text = myDualOutgoingTextBox.getText();
				text = text.replaceAll("[^0-9]+", "");
				if (!StringUtils.equals(myDualOutgoingTextBox.getText(), text)) {
					String newVal = text;
					SwingUtilities.invokeLater(() -> myDualOutgoingTextBox.setText(newVal));
				}
				if (myDualPortRadio.isSelected()) {
					if (text.length() > 0) {
						myConnection.setOutgoingPort(Integer.parseInt(text));
					} else {
						myConnection.setOutgoingPort(-1);
					}
				}
			}
		});
		dualPortPanel.add(myDualOutgoingTextBox, "growx");

		// ── HL7 over HTTP ───────────────────────────────────────────────────
		myHl7OverHttpRadioButton = new JRadioButton("HL7 over HTTP");
		myHl7OverHttpRadioButton.setToolTipText("Use the HL7 over HTTP transport protocol");
		myHl7OverHttpRadioButton.addActionListener(e -> {
			updatePortsModel();
			updatePortsUi();
			if (myOnTransportChanged != null) {
				myOnTransportChanged.run();
			}
		});
		myPortButtonGroup.add(myHl7OverHttpRadioButton);
		add(myHl7OverHttpRadioButton, "gapbottom 5");

		JPanel hohPanel = new JPanel(new MigLayout("insets 5", "[][grow]"));
		hohPanel.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
		add(hohPanel, "growx, gapbottom 5");

		hohPanel.add(new JLabel("URL"));
		myHoHUrlTextField = new JTextField(10);
		myHoHUrlTextField.setToolTipText("Endpoint URL, for example https://server.example:8080/hl7");
		myHoHUrlTextField.getDocument().addDocumentListener(new SimpleDocumentListener() {
			@Override
			public void update(DocumentEvent theE) {
				if (myHohUrlTextFieldUpdating) {
					return;
				}
				String value = myHoHUrlTextField.getText();
				try {
					URL url = new URL(value);
					boolean tls;
					if (url.getProtocol().equals("http")) {
						tls = false;
					} else if (url.getProtocol().equals("https")) {
						tls = true;
					} else {
						myHoHUrlTextField.setBackground(ERROR_BG);
						return;
					}
					myConnection.setTls(tls);
					myConnection.setHost(url.getHost());
					myConnection.setIncomingOrSinglePort(url.getPort() != -1 ? url.getPort() : url.getDefaultPort());
					myConnection.setHttpUriPath(url.getPath());

					myHohUrlTextFieldUpdating = true;
					updatePortsUi();
					myHohUrlTextFieldUpdating = false;

					myHoHUrlTextField.setBackground(Color.white);
				} catch (MalformedURLException e) {
					myHoHUrlTextField.setBackground(ERROR_BG);
				}
			}
		});
		hohPanel.add(myHoHUrlTextField, "growx");

		myValidationLabel = new JLabel(" ");
		add(myValidationLabel, "span 2, growx");
	}

	public void setConnection(AbstractConnection theConnection) {
		myConnection = theConnection;
		updatePortsUi();
	}

	public void updatePortsUi() {
		boolean changesAllowed = isChangesAllowed();

		switch (myConnection.getTransport()) {
		case DUAL_PORT_MLLP:
			mySinglePortRadio.setSelected(false);
			myDualPortRadio.setSelected(true);
			myHl7OverHttpRadioButton.setSelected(false);

			myDualIncomingTextBox.setText(portToString(myConnection.getIncomingOrSinglePort()));
			myDualOutgoingTextBox.setText(portToString(myConnection.getOutgoingPort()));

			mySinglePortTextBox.setEnabled(false);
			myDualIncomingTextBox.setEnabled(changesAllowed);
			myDualOutgoingTextBox.setEnabled(changesAllowed);
			myHoHUrlTextField.setEnabled(false);
			break;

		case SINGLE_PORT_MLLP:
			mySinglePortRadio.setSelected(true);
			myDualPortRadio.setSelected(false);
			myHl7OverHttpRadioButton.setSelected(false);

			mySinglePortTextBox.setText(portToString(myConnection.getIncomingOrSinglePort()));

			mySinglePortTextBox.setEnabled(changesAllowed);
			myDualIncomingTextBox.setEnabled(false);
			myDualOutgoingTextBox.setEnabled(false);
			myHoHUrlTextField.setEnabled(false);
			break;

		case HL7_OVER_HTTP:
			mySinglePortRadio.setSelected(false);
			myDualPortRadio.setSelected(false);
			myHl7OverHttpRadioButton.setSelected(true);

			mySinglePortTextBox.setEnabled(false);
			myDualIncomingTextBox.setEnabled(false);
			myDualOutgoingTextBox.setEnabled(false);

			StringBuilder urlBuilder = new StringBuilder();
			urlBuilder.append(myConnection.isTls() ? "https://" : "http://");
			urlBuilder.append(StringUtils.defaultString(myConnection.getHost(), "localhost"));
			urlBuilder.append(":").append(myConnection.getIncomingOrSinglePort());
			urlBuilder.append(StringUtils.defaultString(myConnection.getHttpUriPath(), "/"));
			String url = urlBuilder.toString();
			if (!myHohUrlTextFieldUpdating) {
				myHohUrlTextFieldUpdating = true;
				myHoHUrlTextField.setText(url);
				myHohUrlTextFieldUpdating = false;
			}
			myHoHUrlTextField.setEnabled(changesAllowed);
			break;
		}
	}

	private void updatePortsModel() {
		if (mySinglePortRadio.isSelected()) {
			myConnection.setTransport(TransportStyleEnum.SINGLE_PORT_MLLP);
		} else if (myDualPortRadio.isSelected()) {
			myConnection.setTransport(TransportStyleEnum.DUAL_PORT_MLLP);
		} else {
			myConnection.setTransport(TransportStyleEnum.HL7_OVER_HTTP);
		}
	}

	public void updateStatus() {
		boolean changesAllowed = isChangesAllowed();
		mySinglePortRadio.setEnabled(changesAllowed);
		myDualPortRadio.setEnabled(changesAllowed);
		myHl7OverHttpRadioButton.setEnabled(changesAllowed);
		updatePortsUi();
	}

	public TransportStyleEnum getTransport() {
		return mySinglePortRadio.isSelected() ? TransportStyleEnum.SINGLE_PORT_MLLP
				: myDualPortRadio.isSelected() ? TransportStyleEnum.DUAL_PORT_MLLP
				: TransportStyleEnum.HL7_OVER_HTTP;
	}

	public boolean isHoHSelected() {
		return myHl7OverHttpRadioButton.isSelected();
	}

	public void setValidationErrors(List<ConnectionValidator.ValidationError> theErrors) {
		String singlePortError = ConnectionValidationUi.getError(theErrors, "port");
		String inboundPortError = ConnectionValidationUi.getError(theErrors, "inboundPort");
		String outboundPortError = ConnectionValidationUi.getError(theErrors, "outboundPort");
		String urlError = ConnectionValidationUi.getError(theErrors, "url");
		String transportError = ConnectionValidationUi.getError(theErrors, "transport");

		ConnectionValidationUi.apply(mySinglePortTextBox, singlePortError);
		ConnectionValidationUi.apply(myDualIncomingTextBox, inboundPortError);
		ConnectionValidationUi.apply(myDualOutgoingTextBox, outboundPortError);
		ConnectionValidationUi.apply(myHoHUrlTextField, urlError);
		ConnectionValidationUi.setMessage(myValidationLabel,
				transportError != null ? transportError
						: mySinglePortRadio.isSelected() ? singlePortError
						: myDualPortRadio.isSelected() ? inboundPortError != null ? inboundPortError : outboundPortError
						: urlError);
	}

	private boolean isChangesAllowed() {
		return myConnection.getStatus() == StatusEnum.STOPPED || myConnection.getStatus() == StatusEnum.FAILED;
	}

	private String portToString(int thePort) {
		return thePort > 0 ? Integer.toString(thePort) : "";
	}

	@Override
	public void destroy() {
		// no listeners to clean up currently
	}
}
