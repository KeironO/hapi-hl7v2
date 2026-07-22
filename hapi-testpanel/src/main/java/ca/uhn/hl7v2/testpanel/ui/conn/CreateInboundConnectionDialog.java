package ca.uhn.hl7v2.testpanel.ui.conn;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

import ca.uhn.hl7v2.testpanel.controller.Controller;
import ca.uhn.hl7v2.testpanel.model.conn.InboundConnection;
import ca.uhn.hl7v2.testpanel.ui.IDestroyable;
import ca.uhn.hl7v2.testpanel.util.IOkCancelCallback;
import net.miginfocom.swing.MigLayout;

public class CreateInboundConnectionDialog extends JDialog implements IDestroyable {

	private Hl7ConnectionPanel myConnectionPanel;
	private Hl7ConnectionPanelHeader myHeaderPanel;
	private final JPanel mycontentPanel = new JPanel();
	private boolean myDone;
	private Controller myController;
	private JButton myOkButton;
	private JLabel myValidationLabel;
	private Timer myValidationTimer;

	public CreateInboundConnectionDialog(Controller theController, final InboundConnection theConnection, final IOkCancelCallback<InboundConnection> theHandler) {
		myController = theController;

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				if (!myDone) {
					stopValidationTimer();
					theHandler.cancel(theConnection);
				}
			}
		});
		setModal(true);
		setTitle("New Receiving Connection");
		setBounds(100, 100, 900, 700);
		getContentPane().setLayout(new MigLayout("wrap 1, insets 0", "[grow]", "[][grow][]"));

		myHeaderPanel = new Hl7ConnectionPanelHeader();
		myHeaderPanel.setConnection(theConnection);
		myHeaderPanel.markDisableStartingAndStopping();
		getContentPane().add(myHeaderPanel, "growx");

		mycontentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(mycontentPanel, "grow");
		mycontentPanel.setLayout(new MigLayout("insets 0", "[grow]", "[grow]"));

		JPanel buttonPane = new JPanel(new MigLayout("insets 5 10 10 10", "[grow][] []"));
		getContentPane().add(buttonPane, "growx");

		myValidationLabel = new JLabel(" ");
		buttonPane.add(myValidationLabel, "growx");

		myOkButton = new JButton("OK");
		myOkButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!updateValidation(theConnection)) {
					return;
				}
				theHandler.ok(theConnection);
				myDone = true;
				stopValidationTimer();
				setVisible(false);
			}
		});
		myOkButton.setActionCommand("OK");
		buttonPane.add(myOkButton);
		getRootPane().setDefaultButton(myOkButton);

		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				theHandler.cancel(theConnection);
				myDone = true;
				stopValidationTimer();
				setVisible(false);
			}
		});
		cancelButton.setActionCommand("Cancel");
		buttonPane.add(cancelButton);

		myConnectionPanel = new Hl7ConnectionPanel(myController);
		myConnectionPanel.setConnection(theConnection);
		mycontentPanel.add(myConnectionPanel, "grow");

		myValidationTimer = new Timer(150, e -> updateValidation(theConnection));
		myValidationTimer.start();
		updateValidation(theConnection);
	}

	private boolean updateValidation(InboundConnection theConnection) {
		List<ConnectionValidator.ValidationError> errors = ConnectionValidator.validate(theConnection);
		myHeaderPanel.setValidationErrors(errors);
		myConnectionPanel.setValidationErrors(errors);
		myOkButton.setEnabled(errors.isEmpty());
		myValidationLabel.setText(errors.isEmpty() ? " " : errors.get(0).getMessage());
		return errors.isEmpty();
	}

	private void stopValidationTimer() {
		if (myValidationTimer != null) {
			myValidationTimer.stop();
		}
	}

	public void destroy() {
		stopValidationTimer();
		myConnectionPanel.destroy();
		myHeaderPanel.destroy();
	}

}
