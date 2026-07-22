package ca.uhn.hl7v2.testpanel.ui.conn;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;

import org.apache.commons.lang.StringUtils;

import ca.uhn.hl7v2.testpanel.model.conn.AbstractConnection;
import ca.uhn.hl7v2.testpanel.model.conn.AbstractConnection.StatusEnum;
import ca.uhn.hl7v2.testpanel.model.conn.InboundConnection;
import ca.uhn.hl7v2.testpanel.model.conn.OutboundConnection;
import ca.uhn.hl7v2.testpanel.ui.IDestroyable;
import ca.uhn.hl7v2.testpanel.ui.ImageFactory;
import net.miginfocom.swing.MigLayout;

public class Hl7ConnectionPanelHeader extends JPanel implements IDestroyable {

	private static final Color GREEN  = new Color(0x22, 0xC5, 0x5E);
	private static final Color AMBER  = new Color(0xF5, 0x9E, 0x0B);
	private static final Color RED    = new Color(0xEF, 0x44, 0x44);
	private static final Color GRAY   = new Color(0x6B, 0x72, 0x80);

	private JLabel myTitleLabel;
	private JLabel myStatusBadge;
	private JLabel myStatusDetailLabel;
	private JTextField myNameBox;
	private JCheckBox myRememberAsCheckBox;
	private JButton myStartButton;
	private JButton myStopButton;

	private AbstractConnection myConnection;
	private PropertyChangeListener myNamePropertyChangeListener;
	private PropertyChangeListener myStatusPropertyChangeListener;
	private PropertyChangeListener myStatusLinePropertyChangeListener;

	public Hl7ConnectionPanelHeader() {
		setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
		setLayout(new MigLayout("insets 0, wrap 2", "[grow][right]", "[][][][grow]"));

		// ── Title row ─────────────────────────────────────────────────────────
		myTitleLabel = new JLabel("Connection");
		myTitleLabel.setFont(myTitleLabel.getFont().deriveFont(Font.BOLD, 14f));
		add(myTitleLabel, "growx");

		myStatusBadge = new JLabel();
		myStatusBadge.setFont(myStatusBadge.getFont().deriveFont(Font.BOLD, 12f));
		add(myStatusBadge, "right");

		// ── Name row ─────────────────────────────────────────────────────────
		myRememberAsCheckBox = new JCheckBox("Save connection for future sessions:");
		myRememberAsCheckBox.setToolTipText("Keep this connection after TestPanel is closed. Enter a descriptive name beside this option.");
		myRememberAsCheckBox.setOpaque(false);
		myRememberAsCheckBox.addActionListener(e -> {
			myConnection.setPersistent(myRememberAsCheckBox.isSelected());
			updateRememberAsUi();
		});
		add(myRememberAsCheckBox);

		myNameBox = new JTextField();
		myNameBox.setToolTipText("A descriptive name used to identify this connection");
		myNameBox.addActionListener(e -> commitNameFromField());
		myNameBox.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				commitNameFromField();
			}
		});
		add(myNameBox, "growx");

		// ── Controls row ─────────────────────────────────────────────────────
		myStartButton = new JButton("Start");
		myStartButton.setIcon(ImageFactory.getStartOne());
		myStartButton.addActionListener(e -> myConnection.start());
		add(myStartButton, "split 3");

		myStopButton = new JButton("Stop");
		myStopButton.setIcon(ImageFactory.getStop());
		myStopButton.addActionListener(e -> myConnection.stop());
		add(myStopButton);

		myStatusDetailLabel = new JLabel();
		myStatusDetailLabel.setFont(myStatusDetailLabel.getFont().deriveFont(11f));
		add(myStatusDetailLabel, "growx");

		// ── Bottom separator ─────────────────────────────────────────────────
		JPanel separator = new JPanel();
		separator.setOpaque(false);
		separator.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0,
				UIManager.getColor("Separator.foreground") != null
						? UIManager.getColor("Separator.foreground")
						: new Color(0xD1, 0xD5, 0xDB)));
		add(separator, "span 2, growx, h 1!");
	}

	public void setConnection(AbstractConnection theConnection) {
		// Clean up previous listeners to prevent accumulation
		if (myConnection != null) {
			destroy();
		}

		myConnection = theConnection;

		setLabelText(theConnection instanceof InboundConnection
				? "Incoming Message Receiver"
				: "Outgoing Message Sender");

		myNameBox.setText(theConnection.getName());

		myNamePropertyChangeListener = new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent theEvt) {
				myNameBox.setText(myConnection.getName());
			}
		};
		myConnection.addPropertyChangeListener(OutboundConnection.NAME_PROPERTY, myNamePropertyChangeListener);

		myStatusPropertyChangeListener = new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent theEvt) {
				updateStatus();
			}
		};
		myConnection.addPropertyChangeListener(OutboundConnection.STATUS_PROPERTY, myStatusPropertyChangeListener);

		myStatusLinePropertyChangeListener = new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent theEvt) {
				updateStatus();
			}
		};
		myConnection.addPropertyChangeListener(OutboundConnection.STATUS_LINE_PROPERTY, myStatusLinePropertyChangeListener);

		updateStatus();
		updateRememberAsUi();
	}

	private void updateRememberAsUi() {
		boolean persistent = myConnection.isPersistent();
		myRememberAsCheckBox.setSelected(persistent);
		myNameBox.setEnabled(persistent);
	}

	private void updateStatus() {
		StatusEnum status = myConnection.getStatus();

		String dot;
		String label;
		Color detailColor;

		switch (status) {
		case STARTED:
			dot = dotHtml(GREEN);
			label = "Running";
			detailColor = GREEN;
			break;
		case TRYING_TO_START:
			dot = dotHtml(AMBER);
			label = "Connecting…";
			detailColor = AMBER;
			break;
		case FAILED:
			dot = dotHtml(RED);
			label = "Failed";
			detailColor = RED;
			break;
		default: // STOPPED
			dot = dotHtml(GRAY);
			label = "Stopped";
			detailColor = GRAY;
			break;
		}

		myStatusBadge.setText("<html>" + dot + "&nbsp;" + label + "</html>");

		String statusLine = StringUtils.defaultString(myConnection.getStatusLine());
		myStatusDetailLabel.setText(statusLine);
		myStatusDetailLabel.setForeground(status == StatusEnum.FAILED ? RED
				: UIManager.getColor("Label.foreground"));

		myStartButton.setEnabled(status == StatusEnum.STOPPED || status == StatusEnum.FAILED);
		myStopButton.setEnabled(status == StatusEnum.STARTED || status == StatusEnum.TRYING_TO_START);
	}

	private static String dotHtml(Color c) {
		return "<font color='" + toHex(c) + "'>⬤</font>";
	}

	private static String toHex(Color c) {
		return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
	}

	public void setLabelText(String theText) {
		if (myTitleLabel != null) {
			myTitleLabel.setText(theText);
		}
	}

	public void markDisableStartingAndStopping() {
		myStartButton.setEnabled(false);
		myStopButton.setEnabled(false);
	}

	public void setValidationErrors(List<ConnectionValidator.ValidationError> theErrors) {
		ConnectionValidationUi.apply(myNameBox, ConnectionValidationUi.getError(theErrors, "name"));
	}

	private void commitNameFromField() {
		if (!myNameBox.isEnabled() || myConnection == null) {
			return;
		}
		String name = myNameBox.getText();
		if (StringUtils.isNotBlank(name)) {
			myConnection.setNameExplicitly(name);
		}
	}

	public void destroy() {
		myConnection.removePropertyChangeListener(OutboundConnection.NAME_PROPERTY, myNamePropertyChangeListener);
		myConnection.removePropertyChangeListener(OutboundConnection.STATUS_PROPERTY, myStatusPropertyChangeListener);
		myConnection.removePropertyChangeListener(OutboundConnection.STATUS_LINE_PROPERTY, myStatusLinePropertyChangeListener);
	}

}
