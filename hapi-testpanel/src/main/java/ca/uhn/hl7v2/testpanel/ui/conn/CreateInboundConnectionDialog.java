package ca.uhn.hl7v2.testpanel.ui.conn;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import ca.uhn.hl7v2.testpanel.controller.Controller;
import ca.uhn.hl7v2.testpanel.model.conn.InboundConnection;
import ca.uhn.hl7v2.testpanel.ui.IDestroyable;
import ca.uhn.hl7v2.testpanel.util.IOkCancelCallback;

public class CreateInboundConnectionDialog extends JDialog implements IDestroyable {

	private Hl7ConnectionPanel myConnectionPanel;
	private Hl7ConnectionPanelHeader myHeaderPanel;
	private final JPanel mycontentPanel = new JPanel();
	private boolean myDone;
	private Controller myController;

	public CreateInboundConnectionDialog(Controller theController, final InboundConnection theConnection, final IOkCancelCallback<InboundConnection> theHandler) {
		myController = theController;

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				if (!myDone) {
					theHandler.cancel(theConnection);
				}
			}
		});
		setModal(true);
		setTitle("New Receiving Connection");
		setBounds(100, 100, 687, 480);
		getContentPane().setLayout(new BorderLayout());

		myHeaderPanel = new Hl7ConnectionPanelHeader();
		myHeaderPanel.setConnection(theConnection);
		myHeaderPanel.markDisableStartingAndStopping();
		getContentPane().add(myHeaderPanel, BorderLayout.NORTH);

		mycontentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(mycontentPanel, BorderLayout.CENTER);
		mycontentPanel.setLayout(new BorderLayout(0, 0));

		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
		getContentPane().add(buttonPane, BorderLayout.SOUTH);

		JButton okButton = new JButton("OK");
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				theHandler.ok(theConnection);
				myDone = true;
				setVisible(false);
			}
		});
		okButton.setActionCommand("OK");
		buttonPane.add(okButton);
		getRootPane().setDefaultButton(okButton);

		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				theHandler.cancel(theConnection);
				myDone = true;
				setVisible(false);
			}
		});
		cancelButton.setActionCommand("Cancel");
		buttonPane.add(cancelButton);

		myConnectionPanel = new Hl7ConnectionPanel(myController);
		myConnectionPanel.setConnection(theConnection);
		mycontentPanel.add(myConnectionPanel, BorderLayout.CENTER);
	}

	public void destroy() {
		myConnectionPanel.destroy();
		myHeaderPanel.destroy();
	}

}
