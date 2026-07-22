package ca.uhn.hl7v2.testpanel.ui.conn;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.EtchedBorder;

import ca.uhn.hl7v2.testpanel.model.conn.AbstractConnection;
import ca.uhn.hl7v2.testpanel.model.conn.AbstractConnection.StatusEnum;
import ca.uhn.hl7v2.testpanel.ui.IDestroyable;
import ca.uhn.hl7v2.testpanel.xsd.Hl7V2EncodingTypeEnum;
import net.miginfocom.swing.MigLayout;

public class EncodingPanel extends JPanel implements IDestroyable {

	private final ButtonGroup myEncodingButtonGroup = new ButtonGroup();
	private JRadioButton myEr7Radio;
	private JRadioButton myXmlRadio;
	private AbstractConnection myConnection;

	public EncodingPanel() {
		setLayout(new MigLayout("insets 5", "[][grow]"));
		setBorder(new EtchedBorder(EtchedBorder.LOWERED));

		myEr7Radio = new JRadioButton("ER7 (Pipe and hat)");
		myEr7Radio.setToolTipText("Traditional pipe-delimited HL7 v2 encoding");
		myEr7Radio.addActionListener(e -> updateEncodingModel());
		myEncodingButtonGroup.add(myEr7Radio);
		add(myEr7Radio);

		myXmlRadio = new JRadioButton("XML");
		myXmlRadio.setToolTipText("HL7 v2 XML encoding");
		myXmlRadio.addActionListener(e -> updateEncodingModel());
		myEncodingButtonGroup.add(myXmlRadio);
		add(myXmlRadio);
	}

	public void setConnection(AbstractConnection theConnection) {
		myConnection = theConnection;
		Hl7V2EncodingTypeEnum encoding = theConnection.getEncoding();
		myEr7Radio.setSelected(encoding == Hl7V2EncodingTypeEnum.ER_7);
		myXmlRadio.setSelected(encoding == Hl7V2EncodingTypeEnum.XML);
	}

	private void updateEncodingModel() {
		if (myEr7Radio.isSelected()) {
			myConnection.setEncoding(Hl7V2EncodingTypeEnum.ER_7);
		} else {
			myConnection.setEncoding(Hl7V2EncodingTypeEnum.XML);
		}
	}

	public void updateStatus() {
		boolean changesAllowed = myConnection.getStatus() == StatusEnum.STOPPED || myConnection.getStatus() == StatusEnum.FAILED;
		myEr7Radio.setEnabled(changesAllowed);
		myXmlRadio.setEnabled(changesAllowed);
	}

	@Override
	public void destroy() {
		// no listeners to clean up
	}
}
