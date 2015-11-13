package a1.command;

import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import a1.ITriangleFrame;

public class ZoomCommand implements MouseWheelListener {

	private static ZoomCommand instance;
	private static ITriangleFrame target;
	
	private ZoomCommand() {

	}
	
	public static void setTarget(ITriangleFrame tar) {
		target = tar;
	}
	
	public static MouseWheelListener getInstance() {
		if (instance == null) {
			instance = new ZoomCommand();
		}
		return instance;
	}
	
	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		int rotation = e.getWheelRotation();
		if (rotation > 0) {
			target.zoom(1.1F);
		} else {
			target.zoom(0.9F);
		}
	}

}
