package ca.uhn.hl7v2.testpanel.ui.conn;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import ca.uhn.hl7v2.testpanel.model.conn.AbstractConnection;
import ca.uhn.hl7v2.testpanel.model.conn.TransportStyleEnum;

/**
 * Validates connection configuration and returns a list of validation errors.
 * Used to block the OK button until all required fields are valid.
 */
public class ConnectionValidator {

	/**
	 * Validates all required fields for the given connection.
	 *
	 * @param conn the connection to validate
	 * @return list of validation errors (empty if valid)
	 */
	public static List<ValidationError> validate(AbstractConnection conn) {
		List<ValidationError> errors = new ArrayList<>();

		if (conn == null) {
			errors.add(new ValidationError("connection", "No connection configured"));
			return errors;
		}

		// Name validation (only if persistent)
		if (conn.isPersistent() && StringUtils.isBlank(conn.getName())) {
			errors.add(new ValidationError("name", "Name is required when 'Save with name' is checked"));
		}

		// Transport-specific validation
		TransportStyleEnum transport = conn.getTransport();
		if (transport == null) {
			errors.add(new ValidationError("transport", "Transport type is required"));
			return errors;
		}

		switch (transport) {
		case SINGLE_PORT_MLLP:
			validateMllpSinglePort(conn, errors);
			break;
		case DUAL_PORT_MLLP:
			validateMllpDualPort(conn, errors);
			break;
		case HL7_OVER_HTTP:
			validateHoh(conn, errors);
			break;
		}

		// Encoding validation
		if (conn.getEncoding() == null) {
			errors.add(new ValidationError("encoding", "Encoding is required"));
		}

		return errors;
	}

	private static void validateMllpSinglePort(AbstractConnection conn, List<ValidationError> errors) {
		// Host
		if (StringUtils.isBlank(conn.getHost())) {
			errors.add(new ValidationError("host", "Host is required"));
		}

		// Port
		int port = conn.getIncomingOrSinglePort();
		if (port < 1 || port > 65535) {
			errors.add(new ValidationError("port", "Port must be between 1 and 65535"));
		}

		// TLS keystore (if TLS enabled)
		if (conn.isTls()) {
			validateTlsKeystore(conn, errors);
		}
	}

	private static void validateMllpDualPort(AbstractConnection conn, List<ValidationError> errors) {
		// Host
		if (StringUtils.isBlank(conn.getHost())) {
			errors.add(new ValidationError("host", "Host is required"));
		}

		// Inbound port
		int inboundPort = conn.getIncomingOrSinglePort();
		if (inboundPort < 1 || inboundPort > 65535) {
			errors.add(new ValidationError("inboundPort", "Inbound port must be between 1 and 65535"));
		}

		// Outbound port
		int outboundPort = conn.getOutgoingPort();
		if (outboundPort < 1 || outboundPort > 65535) {
			errors.add(new ValidationError("outboundPort", "Outbound port must be between 1 and 65535"));
		}

		// TLS keystore (if TLS enabled)
		if (conn.isTls()) {
			validateTlsKeystore(conn, errors);
		}
	}

	private static void validateHoh(AbstractConnection conn, List<ValidationError> errors) {
		// URL
		String url = conn.getHttpUriPath();
		if (StringUtils.isBlank(url)) {
			errors.add(new ValidationError("url", "URL is required for HL7 over HTTP"));
		} else if (!isValidUrl(url)) {
			errors.add(new ValidationError("url", "URL must start with http:// or https://"));
		}

		// Port
		int port = conn.getIncomingOrSinglePort();
		if (port < 1 || port > 65535) {
			errors.add(new ValidationError("port", "Port must be between 1 and 65535"));
		}

		// HoH authentication (if enabled)
		if (conn.isHohAuthenticationEnabled()) {
			if (StringUtils.isBlank(conn.getHohAuthenticationUsername())) {
				errors.add(new ValidationError("hohAuthUsername", "Username is required when authentication is enabled"));
			}
			if (StringUtils.isBlank(conn.getHohAuthenticationPassword())) {
				errors.add(new ValidationError("hohAuthPassword", "Password is required when authentication is enabled"));
			}
		}

		// HoH signature (if enabled)
		if (conn.isHohSignatureEnabled()) {
			if (StringUtils.isBlank(conn.getHohSignatureKeystore())) {
				errors.add(new ValidationError("hohSignatureKeystore", "Keystore is required when signature is enabled"));
			} else {
				File keystoreFile = new File(conn.getHohSignatureKeystore());
				if (!keystoreFile.exists()) {
					errors.add(new ValidationError("hohSignatureKeystore", "Keystore file does not exist: " + conn.getHohSignatureKeystore()));
				}
			}
			if (StringUtils.isBlank(conn.getHohSignatureKey())) {
				errors.add(new ValidationError("hohSignatureKey", "Key alias is required when signature is enabled"));
			}
		}
	}

	private static void validateTlsKeystore(AbstractConnection conn, List<ValidationError> errors) {
		if (StringUtils.isBlank(conn.getTlsKeystoreLocation())) {
			errors.add(new ValidationError("tlsKeystore", "TLS keystore location is required when TLS is enabled"));
		} else {
			File keystoreFile = new File(conn.getTlsKeystoreLocation());
			if (!keystoreFile.exists()) {
				errors.add(new ValidationError("tlsKeystore", "TLS keystore file does not exist: " + conn.getTlsKeystoreLocation()));
			}
		}
	}

	private static boolean isValidUrl(String url) {
		return url.startsWith("http://") || url.startsWith("https://");
	}

	/**
	 * Represents a single validation error with a field identifier and message.
	 */
	public static class ValidationError {
		private final String field;
		private final String message;

		public ValidationError(String field, String message) {
			this.field = field;
			this.message = message;
		}

		public String getField() {
			return field;
		}

		public String getMessage() {
			return message;
		}

		@Override
		public String toString() {
			return field + ": " + message;
		}
	}
}
