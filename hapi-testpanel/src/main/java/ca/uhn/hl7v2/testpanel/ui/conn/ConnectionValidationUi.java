package ca.uhn.hl7v2.testpanel.ui.conn;

import java.awt.Color;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.border.Border;

final class ConnectionValidationUi {

	private static final String ORIGINAL_BORDER_PROPERTY = ConnectionValidationUi.class.getName() + ".originalBorder";
	private static final String ORIGINAL_TOOLTIP_PROPERTY = ConnectionValidationUi.class.getName() + ".originalTooltip";
	private static final Color ERROR_COLOR = new Color(0xDC, 0x26, 0x26);

	private ConnectionValidationUi() {
		// Nothing
	}

	static String getError(List<ConnectionValidator.ValidationError> theErrors, String... theFields) {
		for (String nextField : theFields) {
			for (ConnectionValidator.ValidationError nextError : theErrors) {
				if (nextField.equals(nextError.getField())) {
					return nextError.getMessage();
				}
			}
		}
		return null;
	}

	static void apply(JComponent theComponent, String theMessage) {
		if (theMessage == null) {
			clear(theComponent);
			return;
		}

		if (theComponent.getClientProperty(ORIGINAL_BORDER_PROPERTY) == null) {
			theComponent.putClientProperty(ORIGINAL_BORDER_PROPERTY, theComponent.getBorder());
			theComponent.putClientProperty(ORIGINAL_TOOLTIP_PROPERTY, theComponent.getToolTipText());
		}
		theComponent.setBorder(BorderFactory.createLineBorder(ERROR_COLOR, 2));
		theComponent.setToolTipText(theMessage);
	}

	static void clear(JComponent theComponent) {
		Border originalBorder = (Border) theComponent.getClientProperty(ORIGINAL_BORDER_PROPERTY);
		if (originalBorder != null) {
			theComponent.setBorder(originalBorder);
			theComponent.putClientProperty(ORIGINAL_BORDER_PROPERTY, null);
			theComponent.setToolTipText((String) theComponent.getClientProperty(ORIGINAL_TOOLTIP_PROPERTY));
			theComponent.putClientProperty(ORIGINAL_TOOLTIP_PROPERTY, null);
		}
	}

	static void setMessage(JLabel theLabel, String theMessage) {
		theLabel.setText(theMessage != null ? theMessage : " ");
		theLabel.setForeground(ERROR_COLOR);
	}
}
