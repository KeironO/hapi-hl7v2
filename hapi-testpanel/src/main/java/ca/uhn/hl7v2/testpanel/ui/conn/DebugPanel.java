package ca.uhn.hl7v2.testpanel.ui.conn;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;

import ca.uhn.hl7v2.testpanel.model.conn.AbstractConnection;
import ca.uhn.hl7v2.testpanel.model.conn.AbstractConnection.StatusEnum;
import ca.uhn.hl7v2.testpanel.ui.IDestroyable;
import net.miginfocom.swing.MigLayout;

public class DebugPanel extends JPanel implements IDestroyable {

	private JCheckBox myCaptureByteStreamCheckbox;
	private AbstractConnection myConnection;

	public DebugPanel() {
		setLayout(new MigLayout("insets 5"));
		setBorder(new EtchedBorder(EtchedBorder.LOWERED));

		myCaptureByteStreamCheckbox = new JCheckBox("Capture Bytes");
		myCaptureByteStreamCheckbox.setToolTipText(
				"Check this box to capture the transport level communication");
		myCaptureByteStreamCheckbox.addActionListener(
				e -> myConnection.setCaptureBytes(myCaptureByteStreamCheckbox.isSelected()));
		add(myCaptureByteStreamCheckbox);
	}

	public void setConnection(AbstractConnection theConnection) {
		myConnection = theConnection;
		myCaptureByteStreamCheckbox.setSelected(theConnection.isCaptureBytes());
	}

	public void updateStatus() {
		boolean changesAllowed = myConnection.getStatus() == StatusEnum.STOPPED
				|| myConnection.getStatus() == StatusEnum.FAILED;
		myCaptureByteStreamCheckbox.setEnabled(changesAllowed);
	}

	@Override
	public void destroy() {
		// no listeners to clean up
	}
}
