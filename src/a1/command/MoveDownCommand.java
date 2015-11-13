package a1.command;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import a1.ITriangleFrame;

public class MoveDownCommand extends AbstractAction {

	private static MoveDownCommand instance;
	private static ITriangleFrame target;
	
	private MoveDownCommand() {
		super("Move Down");
	}
	public static AbstractAction getInstance() {
		if (instance == null) {
			instance = new MoveDownCommand();
		}
		return instance;
	}
	public static void setTarget(ITriangleFrame tar) {
		target = tar;
	}
	@Override
	public void actionPerformed(ActionEvent e) {
		target.moveDown();
		
	}

}
