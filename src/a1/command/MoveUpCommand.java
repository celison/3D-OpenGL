package a1.command;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import a1.ITriangleFrame;

public class MoveUpCommand extends AbstractAction {

	private static MoveUpCommand instance;
	private static ITriangleFrame target;
	
	private MoveUpCommand() {
		super("Move Up");
	}
	public static AbstractAction getInstance() {
		if (instance == null) {
			instance = new MoveUpCommand();
		}
		return instance;
	}
	public static void setTarget(ITriangleFrame tar) {
		target = tar;
	}
	@Override
	public void actionPerformed(ActionEvent e) {
		target.moveUp();
		
	}

}
