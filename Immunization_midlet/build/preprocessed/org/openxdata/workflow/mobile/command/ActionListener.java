package org.openxdata.workflow.mobile.command;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;

public interface ActionListener {
	
	public boolean handle(Command cmd, Displayable disp, Object obj);

}
