package ca.uhn.hl7v2.testpanel.ui.conn;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;
import javax.swing.event.DocumentEvent;

import org.apache.commons.lang.StringUtils;

import ca.uhn.hl7v2.testpanel.api.WorkingStatusBean;
import ca.uhn.hl7v2.testpanel.controller.Prefs;
import ca.uhn.hl7v2.testpanel.model.conn.AbstractConnection;
import ca.uhn.hl7v2.testpanel.model.conn.AbstractConnection.StatusEnum;
import ca.uhn.hl7v2.testpanel.ui.IDestroyable;
import ca.uhn.hl7v2.testpanel.util.SimpleDocumentListener;
import net.miginfocom.swing.MigLayout;

public class HohCardPanel extends JPanel implements IDestroyable {

	private static final String HOH_SIGNATURE_KEY_USE_ANY_AVAILABLE = "Use any available";
	private static final ImageIcon ICON_INFO_OK = new ImageIcon(
			HohCardPanel.class.getResource("/ca/uhn/hl7v2/testpanel/images/info_ok.png"));
	private static final ImageIcon ICON_INFO_WARNING = new ImageIcon(
			HohCardPanel.class.getResource("/ca/uhn/hl7v2/testpanel/images/info_warning.png"));
	private static final ImageIcon ICON_INFO_WORKING = new ImageIcon(
			HohCardPanel.class.getResource("/ca/uhn/hl7v2/testpanel/images/info_working.png"));

	// Authorization
	private JCheckBox myHohAuthEnabledCheckbox;
	private JTextField myHohAuthUsernameTextbox;
	private JTextField myHohAuthPasswordTextbox;

	// Security Profile
	private JCheckBox myHohTlsCheckbox;
	private JTextField myHohSecurityKeystoreTextbox;
	private JButton myHohSecurityKeystoreChooseBtn;
	private JTextField myHohSecurityKeyPwTextBox;
	private JLabel myHohSecurityProfileKeystoreStatus;

	// Signature Profile
	private JCheckBox myHohSignatureEnabled;
	private JTextField myHohSignatureKeystoreTextbox;
	private JButton myHohSignatureKeystoreChooseButton;
	private JTextField myHohSignatureKeystorePasswordTextbox;
	private JComboBox<String> myHohSignatureKeyAliasCombo;
	private JLabel myHohSignatureKeyPass;
	private JTextField myHohSignatureKeyPassField;
	private JLabel myHohSignatureStatusLabel;
	private JLabel myValidationLabel;
	private boolean myUpdatingHohSignatureKeyAliasCombo;

	private AbstractConnection myConnection;
	private PropertyChangeListener myHohSecurityProfileKeystoreStatusListener;
	private PropertyChangeListener myHohSignatureStatusListener;
	private PropertyChangeListener myHohSignerAvailableAliasesListener;

	public HohCardPanel() {
		setLayout(new MigLayout("wrap 2, insets 0", "[150!][grow]", "[][][][]"));

		buildAuthorizationSection();
		buildSecurityProfileSection();
		buildSignatureProfileSection();

		myValidationLabel = new JLabel(" ");
		add(myValidationLabel, "span 2, growx");
	}

	private void buildAuthorizationSection() {
		JLabel lblAuth = new JLabel("Authorization");
		add(lblAuth, "gapbottom 5, top");

		JPanel authPanel = new JPanel(new MigLayout("insets 5", "[][][][][][grow]"));
		authPanel.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
		add(authPanel, "growx, gapbottom 5");

		myHohAuthEnabledCheckbox = new JCheckBox("Enabled");
		myHohAuthEnabledCheckbox.setToolTipText("Send HTTP credentials with HL7 over HTTP requests");
		myHohAuthEnabledCheckbox.addActionListener(
				e -> myConnection.setHohAuthenticationEnabled(myHohAuthEnabledCheckbox.isSelected()));
		authPanel.add(myHohAuthEnabledCheckbox);

		authPanel.add(new JLabel("Username:"));
		myHohAuthUsernameTextbox = new JTextField(10);
		myHohAuthUsernameTextbox.setToolTipText("HTTP authentication username");
		myHohAuthUsernameTextbox.getDocument().addDocumentListener(new SimpleDocumentListener() {
			@Override
			public void update(DocumentEvent theE) {
				myConnection.setHohAuthenticationUsername(myHohAuthUsernameTextbox.getText());
			}
		});
		authPanel.add(myHohAuthUsernameTextbox, "growx");

		authPanel.add(new JLabel("Password:"));
		myHohAuthPasswordTextbox = new JTextField(10);
		myHohAuthPasswordTextbox.setToolTipText("HTTP authentication password");
		myHohAuthPasswordTextbox.getDocument().addDocumentListener(new SimpleDocumentListener() {
			@Override
			public void update(DocumentEvent theE) {
				myConnection.setHohAuthenticationPassword(myHohAuthPasswordTextbox.getText());
			}
		});
		authPanel.add(myHohAuthPasswordTextbox, "growx");
	}

	private void buildSecurityProfileSection() {
		JLabel lblSecurity = new JLabel("Security Profile");
		lblSecurity.setVerticalTextPosition(SwingConstants.TOP);
		lblSecurity.setVerticalAlignment(SwingConstants.TOP);
		add(lblSecurity, "gapbottom 5, top");

		JPanel tlsPanel = new JPanel(new MigLayout("insets 5, wrap 4", "[][][grow][]"));
		tlsPanel.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
		add(tlsPanel, "growx, gapbottom 5");

		myHohTlsCheckbox = new JCheckBox("TLS Enabled");
		myHohTlsCheckbox.setToolTipText("Encrypt HL7 over HTTP traffic using TLS/SSL");
		myHohTlsCheckbox.addActionListener(e -> {
			myConnection.setTls(myHohTlsCheckbox.isSelected());
		});
		tlsPanel.add(myHohTlsCheckbox, "spany 2, gapbottom 5");

		tlsPanel.add(new JLabel("Keystore:"));
		myHohSecurityKeystoreTextbox = new JTextField(10);
		myHohSecurityKeystoreTextbox.setToolTipText("Java keystore used to establish TLS connections");
		myHohSecurityKeystoreTextbox.getDocument().addDocumentListener(new SimpleDocumentListener() {
			@Override
			public void update(DocumentEvent theE) {
				String text = myHohSecurityKeystoreTextbox.getText();
				myHohSecurityKeyPwTextBox.setEnabled(isNotBlank(text));
				myConnection.setTlsKeystoreLocation(text);
			}
		});
		tlsPanel.add(myHohSecurityKeystoreTextbox, "growx, span 2, wrap");

		myHohSecurityKeystoreChooseBtn = new JButton("Choose");
		myHohSecurityKeystoreChooseBtn.setIcon(new ImageIcon(
				HohCardPanel.class.getResource("/ca/uhn/hl7v2/testpanel/images/open.png")));
		myHohSecurityKeystoreChooseBtn.addActionListener(e -> chooseKeystore(myHohSecurityKeystoreTextbox));
		tlsPanel.add(myHohSecurityKeystoreChooseBtn);

		tlsPanel.add(new JLabel("Store Pass:"));
		myHohSecurityKeyPwTextBox = new JTextField(10);
		myHohSecurityKeyPwTextBox.setToolTipText("Password for the TLS keystore");
		myHohSecurityKeyPwTextBox.getDocument().addDocumentListener(new SimpleDocumentListener() {
			@Override
			public void update(DocumentEvent theE) {
				myConnection.setTlsKeystorePassword(myHohSecurityKeyPwTextBox.getText());
			}
		});
		tlsPanel.add(myHohSecurityKeyPwTextBox, "growx");

		myHohSecurityProfileKeystoreStatus = new JLabel("Value goes here");
		myHohSecurityProfileKeystoreStatus.setHorizontalAlignment(SwingConstants.CENTER);
		tlsPanel.add(myHohSecurityProfileKeystoreStatus, "span 4, growx");
	}

	private void buildSignatureProfileSection() {
		JLabel lblSignature = new JLabel("Signature Profile");
		add(lblSignature, "top");

		JPanel sigPanel = new JPanel(new MigLayout("insets 5, wrap 4", "[][][grow][]"));
		sigPanel.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
		add(sigPanel, "growx");

		myHohSignatureEnabled = new JCheckBox("Enabled");
		myHohSignatureEnabled.setToolTipText("Digitally sign HL7 over HTTP messages");
		myHohSignatureEnabled.addActionListener(
				e -> myConnection.setHohSignatureEnabled(myHohSignatureEnabled.isSelected()));
		sigPanel.add(myHohSignatureEnabled, "span 4, gapbottom 5, wrap");

		sigPanel.add(new JLabel("Keystore:"));
		myHohSignatureKeystoreTextbox = new JTextField(10);
		myHohSignatureKeystoreTextbox.setToolTipText("Java keystore containing the signing key");
		myHohSignatureKeystoreTextbox.getDocument().addDocumentListener(new SimpleDocumentListener() {
			@Override
			public void update(DocumentEvent theE) {
				myConnection.setHohSignatureKeystore(myHohSignatureKeystoreTextbox.getText());
			}
		});
		sigPanel.add(myHohSignatureKeystoreTextbox, "growx, span 2");

		myHohSignatureKeystoreChooseButton = new JButton("Choose");
		myHohSignatureKeystoreChooseButton.setIcon(new ImageIcon(
				HohCardPanel.class.getResource("/ca/uhn/hl7v2/testpanel/images/open.png")));
		myHohSignatureKeystoreChooseButton.addActionListener(e -> chooseKeystore(myHohSignatureKeystoreTextbox));
		sigPanel.add(myHohSignatureKeystoreChooseButton, "wrap");

		sigPanel.add(new JLabel("Store Pass:"));
		myHohSignatureKeystorePasswordTextbox = new JTextField(10);
		myHohSignatureKeystorePasswordTextbox.setToolTipText("Password for the signing keystore");
		myHohSignatureKeystorePasswordTextbox.getDocument().addDocumentListener(new SimpleDocumentListener() {
			@Override
			public void update(DocumentEvent theE) {
				myConnection.setHohSignatureKeystorePassword(myHohSignatureKeystorePasswordTextbox.getText());
			}
		});
		sigPanel.add(myHohSignatureKeystorePasswordTextbox, "growx");

		sigPanel.add(new JLabel("Key:"));
		myHohSignatureKeyAliasCombo = new JComboBox<>();
		myHohSignatureKeyAliasCombo.setToolTipText("Key alias used to sign messages");
		myHohSignatureKeyAliasCombo.addActionListener(e -> {
			if (myUpdatingHohSignatureKeyAliasCombo) {
				return;
			}
			String selection = (String) myHohSignatureKeyAliasCombo.getSelectedItem();
			if (selection == HOH_SIGNATURE_KEY_USE_ANY_AVAILABLE) {
				myConnection.setHohSignatureKey(null);
			} else {
				myConnection.setHohSignatureKey(selection.replaceAll(" .*", ""));
			}
		});
		sigPanel.add(myHohSignatureKeyAliasCombo, "growx");

		sigPanel.add(new JLabel("Key Pass:"));
		myHohSignatureKeyPassField = new JTextField(10);
		myHohSignatureKeyPassField.setToolTipText("Password for the selected signing key");
		myHohSignatureKeyPassField.getDocument().addDocumentListener(new SimpleDocumentListener() {
			@Override
			public void update(DocumentEvent theE) {
				myConnection.setHohSignatureKeyPassword(myHohSignatureKeyPassField.getText());
			}
		});
		sigPanel.add(myHohSignatureKeyPassField, "growx, wrap");

		myHohSignatureStatusLabel = new JLabel("New label");
		myHohSignatureStatusLabel.setHorizontalAlignment(SwingConstants.CENTER);
		sigPanel.add(myHohSignatureStatusLabel, "span 4, growx");
	}

	public void setConnection(AbstractConnection theConnection) {
		myConnection = theConnection;

		myHohSecurityKeyPwTextBox.setText(myConnection.getTlsKeystorePassword());
		myHohSecurityKeystoreTextbox.setText(myConnection.getTlsKeystoreLocation());
		myHohSecurityProfileKeystoreStatus.setText("");

		myHohSignatureEnabled.setSelected(myConnection.isHohSignatureEnabled());
		myHohSignatureKeystoreTextbox.setText(myConnection.getHohSignatureKeystore());
		myHohSignatureKeystorePasswordTextbox.setText(myConnection.getHohSignatureKeystorePassword());
		myHohSignatureKeyPassField.setText(myConnection.getHohSignatureKeyPassword());
		updateHohSignatureKeyCombo();

		myHohTlsCheckbox.setSelected(theConnection.isTls());
		myHohAuthEnabledCheckbox.setSelected(myConnection.isHohAuthenticationEnabled());
		myHohAuthUsernameTextbox.setText(myConnection.getHohAuthenticationUsername());
		myHohAuthPasswordTextbox.setText(myConnection.getHohAuthenticationPassword());

		// Add property change listeners
		myHohSecurityProfileKeystoreStatusListener = new KeystoreStatusListener(myHohSecurityProfileKeystoreStatus);
		myConnection.addPropertyChangeListener(AbstractConnection.TLS_KEYSTORE_STATUS,
				myHohSecurityProfileKeystoreStatusListener);

		myHohSignatureStatusListener = new KeystoreStatusListener(myHohSignatureStatusLabel);
		myConnection.addPropertyChangeListener(AbstractConnection.HOH_SIGNATURE_KEYSTORE_STATUS,
				myHohSignatureStatusListener);

		myHohSignerAvailableAliasesListener = e -> updateHohSignatureKeyCombo();
		myConnection.addPropertyChangeListener(AbstractConnection.HOH_SIGNER_AVAILABLE_ALIASES_PROPERTY,
				myHohSignerAvailableAliasesListener);
	}

	public void setTlsSelected(boolean selected) {
		myHohTlsCheckbox.setSelected(selected);
	}

	public boolean isTlsSelected() {
		return myHohTlsCheckbox.isSelected();
	}

	public void updateStatus() {
		boolean changesAllowed = myConnection.getStatus() == StatusEnum.STOPPED
				|| myConnection.getStatus() == StatusEnum.FAILED;
		myHohTlsCheckbox.setEnabled(changesAllowed);
		updateHohSignatureKeyCombo();
	}

	public void setValidationErrors(List<ConnectionValidator.ValidationError> theErrors) {
		String usernameError = ConnectionValidationUi.getError(theErrors, "hohAuthUsername");
		String passwordError = ConnectionValidationUi.getError(theErrors, "hohAuthPassword");
		String tlsKeystoreError = ConnectionValidationUi.getError(theErrors, "tlsKeystore");
		String signatureKeystoreError = ConnectionValidationUi.getError(theErrors, "hohSignatureKeystore");
		String signatureKeyError = ConnectionValidationUi.getError(theErrors, "hohSignatureKey");

		ConnectionValidationUi.apply(myHohAuthUsernameTextbox, usernameError);
		ConnectionValidationUi.apply(myHohAuthPasswordTextbox, passwordError);
		ConnectionValidationUi.apply(myHohSecurityKeystoreTextbox, tlsKeystoreError);
		ConnectionValidationUi.apply(myHohSignatureKeystoreTextbox, signatureKeystoreError);
		ConnectionValidationUi.apply(myHohSignatureKeyAliasCombo, signatureKeyError);

		ConnectionValidationUi.setMessage(myValidationLabel,
				usernameError != null ? usernameError
						: passwordError != null ? passwordError
						: tlsKeystoreError != null ? tlsKeystoreError
						: signatureKeystoreError != null ? signatureKeystoreError
						: signatureKeyError);
	}

	private void updateHohSignatureKeyCombo() {
		myUpdatingHohSignatureKeyAliasCombo = true;
		try {
			myHohSignatureKeyAliasCombo.removeAllItems();
			List<String> aliases = myConnection.getHohSignatureAvailableAliases();
			for (String next : aliases) {
				myHohSignatureKeyAliasCombo.addItem(next);
				if (next.equals(myConnection.getHohSignatureKey())) {
					myHohSignatureKeyAliasCombo
							.setSelectedIndex(myHohSignatureKeyAliasCombo.getItemCount() - 1);
				}
			}
		} finally {
			myUpdatingHohSignatureKeyAliasCombo = false;
		}
	}

	private static void chooseKeystore(JTextField theTextbox) {
		String directory = Prefs.getInstance().getInterfaceHohSecurityKeystoreDirectory();
		directory = StringUtils.defaultString(directory, ".");
		JFileChooser chooser = new JFileChooser(directory);
		chooser.setDialogType(JFileChooser.OPEN_DIALOG);
		chooser.setDialogTitle("Select a Java Keystore");
		int result = chooser.showOpenDialog(theTextbox);
		if (result == JFileChooser.APPROVE_OPTION) {
			Prefs.getInstance().setInterfaceHohSecurityKeystoreDirectory(
					chooser.getSelectedFile().getParent());
			theTextbox.setText(chooser.getSelectedFile().getAbsolutePath());
		}
	}

	@Override
	public void destroy() {
		if (myConnection != null) {
			myConnection.removePropertyChangeListener(AbstractConnection.TLS_KEYSTORE_STATUS,
					myHohSecurityProfileKeystoreStatusListener);
			myConnection.removePropertyChangeListener(AbstractConnection.HOH_SIGNATURE_KEYSTORE_STATUS,
					myHohSignatureStatusListener);
			myConnection.removePropertyChangeListener(
					AbstractConnection.HOH_SIGNER_AVAILABLE_ALIASES_PROPERTY,
					myHohSignerAvailableAliasesListener);
		}
	}

	private final class KeystoreStatusListener implements PropertyChangeListener {
		private final JLabel myLabel;

		public KeystoreStatusListener(JLabel theLabel) {
			myLabel = theLabel;
			clear();
		}

		private void clear() {
			myLabel.setText("");
			myLabel.setIcon(null);
		}

		@Override
		public void propertyChange(PropertyChangeEvent theEvt) {
			WorkingStatusBean newValue = (WorkingStatusBean) theEvt.getNewValue();
			if (newValue == null || isBlank(newValue.getMessage())) {
				clear();
			} else {
				myLabel.setText("<html>" + newValue.getMessage() + "</html>");
				switch (newValue.getStatus()) {
				case ERROR:
					myLabel.setIcon(ICON_INFO_WARNING);
					break;
				case WORKING:
					myLabel.setIcon(ICON_INFO_WORKING);
					break;
				case OK:
					myLabel.setIcon(ICON_INFO_OK);
					break;
				}
			}
		}
	}
}
