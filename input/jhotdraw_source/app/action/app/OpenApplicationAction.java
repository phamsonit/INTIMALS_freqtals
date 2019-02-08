/*
 * @(#)OpenApplicationAction.java
 * 
 * Copyright (c) 2009-2010 by the original authors of JHotDraw
 * and all its contributors.
 * All rights reserved.
 * 
 * The copyright of this software is owned by the authors and  
 * contributors of the JHotDraw project ("the copyright holders").  
 * You may not use, copy or modify this software, except in  
 * accordance with the license agreement you entered into with  
 * the copyright holders. For details see accompanying license terms. 
 */

package org.jhotdraw.app.action.app;

import java.awt.event.ActionEvent;
import javax.swing.Action;
import org.jhotdraw.app.Application;
import org.jhotdraw.app.action.AbstractApplicationAction;

/**
 * Handles an open application request from Mac OS X (this action does nothing).
 * <p>
 * This action is called when {@code DefaultOSXApplication} receives an
 * Open Application event from the Mac OS X Finder or another Mac OS X application.
 * <p>
 * This action is automatically created by {@code DefaultOSXApplication}
 * and put into the {@code ApplicationModel} before
 * {@link org.jhotdraw.app.ApplicationModel#initApplication} is called.
 *
 * @author Werner Randelshofer
 * @version $Id: OpenApplicationAction.java 666 2010-07-28 19:11:46Z rawcoder $
 */
public class OpenApplicationAction extends AbstractApplicationAction {

    public final static String ID = "application.openApplication";
    /** Creates a new instance. */
    public OpenApplicationAction(Application app) {
        super(app);
        putValue(Action.NAME, "OSX Open Application");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        
    }

}
