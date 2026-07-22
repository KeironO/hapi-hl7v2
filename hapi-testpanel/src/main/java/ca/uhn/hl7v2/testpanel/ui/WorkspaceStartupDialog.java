package ca.uhn.hl7v2.testpanel.ui;

import java.awt.Component;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;

public class WorkspaceStartupDialog extends JDialog {

	public enum Result {
		CREATE_NEW,
		OPEN_EXISTING,
		EXIT
	}

	private Result myResult = Result.EXIT;

	public WorkspaceStartupDialog(Component theOwner) {
		super(SwingUtilities.getWindowAncestor(theOwner), "Open a Workspace", ModalityType.APPLICATION_MODAL);
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		setLayout(new MigLayout("wrap 1, insets 20", "[grow, fill]", "[][]20[]"));

		JLabel title = new JLabel("A workspace is required");
		title.setFont(title.getFont().deriveFont(title.getFont().getStyle() | java.awt.Font.BOLD, 16f));
		add(title);

		JLabel explanation = new JLabel("<html>Create a workspace to save your connections and messages,<br>or open an existing workspace to continue.</html>");
		explanation.setHorizontalAlignment(SwingConstants.LEFT);
		add(explanation);

		JPanel buttons = new JPanel(new MigLayout("insets 0", "[][grow][]"));
		add(buttons, "growx");

		JButton createButton = new JButton("Create New Workspace");
		createButton.addActionListener(e -> closeWith(Result.CREATE_NEW));
		buttons.add(createButton);

		JButton openButton = new JButton("Open Existing Workspace");
		openButton.addActionListener(e -> closeWith(Result.OPEN_EXISTING));
		buttons.add(openButton, "gapleft 8");

		JButton exitButton = new JButton("Exit");
		exitButton.addActionListener(e -> closeWith(Result.EXIT));
		buttons.add(exitButton, "pushx, right");

		getRootPane().setDefaultButton(openButton);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent theEvent) {
				closeWith(Result.EXIT);
			}
		});

		pack();
		setResizable(false);
		setLocationRelativeTo(theOwner);
	}

	public Result getResult() {
		return myResult;
	}

	private void closeWith(Result theResult) {
		myResult = theResult;
		setVisible(false);
		dispose();
	}
}
