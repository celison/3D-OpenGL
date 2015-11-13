package a1.command;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import a1.ITriangleFrame;

public class ToggleColorCommand extends AbstractAction {

	private static ToggleColorCommand instance;
	private static ITriangleFrame target;
	
	private ToggleColorCommand() {
		super("Toggle Color");
	}
	
	public static void setTarget(ITriangleFrame tar) {
		target = tar;
	}
	
	public static AbstractAction getInstance() {
		if (instance == null) {
			instance = new ToggleColorCommand();
		}
		return instance;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		target.toggleColors();
		
	}

}
