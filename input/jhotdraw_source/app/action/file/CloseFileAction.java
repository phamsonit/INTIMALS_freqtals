/*
 * @(#)CloseFileAction.java
 *
 * Copyright (c) 1996-2010 by the original authors of JHotDraw
 * and all its contributors.
 * All rights reserved.
 *
 * The copyright of this software is owned by the authors and  
 * contributors of the JHotDraw project ("the copyright holders").  
 * You may not use, copy or modify this software, except in  
 * accordance with the license agreement you entered into with  
 * the copyright holders. For details see accompanying license terms. 
 */
package org.jhotdraw.app.action.file;

import org.jhotdraw.util.*;

import org.jhotdraw.app.Application;
import org.jhotdraw.app.View;
import org.jhotdraw.app.action.AbstractSaveUnsavedChangesAction;

/**
 * Closes the active view after letting the user save unsaved changes.
 * {@code DefaultSDIApplication} automatically exits when the user
 * closes the last view.
 * <p>
 * This action is called when the user selects the Close item in the File
 * menu. The menu item is automatically created by the application.
 * <p>
 * If you want this behavior in your application, you have to create it
 * and put it in your {@code ApplicationModel} in method
 * {@link org.jhotdraw.app.ApplicationModel#initApplication}.
 * <p>
 * You should include this action in applications which use at least
 * one of the following actions, so that the user can close views that he/she
 * created: {@link NewFileAction}, {@link NewWindowAction},
 * {@link OpenFileAction}, {@link OpenDirectoryAction}.
 * <p>
 *
 * @author  Werner Randelshofer
 * @version $Id: CloseFileAction.java 604 2010-01-09 12:00:29Z rawcoder $
 */
public class CloseFileAction extends AbstractSaveUnsavedChangesAction {

    public final static String ID = "file.close";

    /** Creates a new instance. */
    public CloseFileAction(Application app, View view) {
        super(app, view);
        ResourceBundleUtil labels = ResourceBundleUtil.getBundle("org.jhotdraw.app.Labels");
        labels.configureAction(this, ID);
    }

    @Override
    protected void doIt(View view) {
        if (view != null && view.getApplication() != null) {
            Application app = view.getApplication();
            app.dispose(view);
        }
    }
}
