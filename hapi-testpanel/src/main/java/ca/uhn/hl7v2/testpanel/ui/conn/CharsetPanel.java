package ca.uhn.hl7v2.testpanel.ui.conn;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.EtchedBorder;

import ca.uhn.hl7v2.testpanel.controller.Prefs;
import ca.uhn.hl7v2.testpanel.model.conn.AbstractConnection;
import ca.uhn.hl7v2.testpanel.model.conn.AbstractConnection.StatusEnum;
import ca.uhn.hl7v2.testpanel.ui.IDestroyable;
import net.miginfocom.swing.MigLayout;

public class CharsetPanel extends JPanel implements IDestroyable {

	private final ButtonGroup myCharsetButtonGroup = new ButtonGroup();
	private JRadioButton myCharsetDetectRadio;
	private JRadioButton myCharsetSelectRadio;
	private JComboBox<String> myCharsetCombo;
	private AbstractConnection myConnection;

	public CharsetPanel() {
		setLayout(new MigLayout("insets 5", "[][grow]"));
		setBorder(new EtchedBorder(EtchedBorder.LOWERED));

		myCharsetSelectRadio = new JRadioButton("");
		myCharsetSelectRadio.setToolTipText("Use the selected character set");
		myCharsetSelectRadio.addActionListener(e -> updateCharsetModel());
		myCharsetButtonGroup.add(myCharsetSelectRadio);
		add(myCharsetSelectRadio);

		myCharsetCombo = new JComboBox<>();
		myCharsetCombo.setToolTipText("Character set used to encode and decode messages");
		myCharsetCombo.addActionListener(e -> updateCharsetModel());
		add(myCharsetCombo, "growx");

		myCharsetDetectRadio = new JRadioButton("Detect in Message (MSH-18)");
		myCharsetDetectRadio.setToolTipText("Use the character set declared in MSH-18 when available");
		myCharsetDetectRadio.addActionListener(e -> updateCharsetModel());
		myCharsetButtonGroup.add(myCharsetDetectRadio);
		add(myCharsetDetectRadio, "gapleft 10");

		initCharsetList();
	}

	private void initCharsetList() {
		List<String> charSets = new ArrayList<>(Charset.availableCharsets().keySet());
		Collections.sort(charSets);
		ComboBoxModel<String> charsetModel = new DefaultComboBoxModel<>(charSets.toArray(new String[0]));
		myCharsetCombo.setModel(charsetModel);
	}

	public void setConnection(AbstractConnection theConnection) {
		myConnection = theConnection;
		myCharsetCombo.setSelectedItem(theConnection.getCharSet());
		updateCharsetUi();
	}

	public void updateCharsetUi() {
		if (myConnection.isDetectCharSetInMessage()) {
			myCharsetDetectRadio.setSelected(true);
			myCharsetSelectRadio.setSelected(false);
			myCharsetCombo.setEnabled(false);
		} else {
			myCharsetDetectRadio.setSelected(false);
			myCharsetSelectRadio.setSelected(true);
			myCharsetCombo.setEnabled(true);
		}
	}

	private void updateCharsetModel() {
		if (myCharsetDetectRadio.isSelected()) {
			myConnection.setDetectCharSetInMessage(true);
		} else {
			myConnection.setDetectCharSetInMessage(false);
			String charSet = (String) myCharsetCombo.getSelectedItem();
			myConnection.setCharSet(charSet);
			Prefs.getInstance().setMostRecentConnectionCharset(charSet);
		}
	}

	public void updateStatus() {
		boolean changesAllowed = myConnection.getStatus() == StatusEnum.STOPPED || myConnection.getStatus() == StatusEnum.FAILED;
		myCharsetCombo.setEnabled(changesAllowed);
		myCharsetDetectRadio.setEnabled(changesAllowed);
		myCharsetSelectRadio.setEnabled(changesAllowed);
		updateCharsetUi();
	}

	@Override
	public void destroy() {
		// no listeners to clean up
	}
}
