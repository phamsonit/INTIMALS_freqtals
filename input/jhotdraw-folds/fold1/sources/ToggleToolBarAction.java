/*
 * @(#)ToggleToolBarAction.java
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

package org.jhotdraw.app.action.window;

import java.awt.event.*;
import javax.swing.*;
import java.beans.*;
import org.jhotdraw.app.action.ActionUtil;

/**
 * ToggleToolBarAction.
 * 
 * @author Werner Randelshofer
 * @version $Id: ToggleToolBarAction.java 647 2010-01-24 22:52:59Z rawcoder $
 */
public class ToggleToolBarAction extends AbstractAction {
    private JToolBar toolBar;
    private PropertyChangeListener propertyHandler;
    
    /** Creates a new instance. */
    public ToggleToolBarAction(JToolBar toolBar, String label) {
        super(label);
        
        propertyHandler = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                String name = evt.getPropertyName();
                if (name.equals("visible")) {
                    putValue(ActionUtil.SELECTED_KEY, evt.getNewValue());
                }
            }            
        };
        
        putValue(ActionUtil.SELECTED_KEY, true);
        setToolBar(toolBar);
    }
    
    @Override
    public void putValue(String key, Object newValue) {
        super.putValue(key, newValue);
        if (key == ActionUtil.SELECTED_KEY) {
            if (toolBar != null) {
                toolBar.setVisible((Boolean) newValue);
            }
        }
    }
    
    public void setToolBar(JToolBar newValue) {
        if (toolBar != null) {
            toolBar.removePropertyChangeListener(propertyHandler);
        }
        
        toolBar = newValue;
 
        if (toolBar != null) {
            toolBar.addPropertyChangeListener(propertyHandler);
            putValue(ActionUtil.SELECTED_KEY, toolBar.isVisible());
        }
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (toolBar != null) {
            putValue(ActionUtil.SELECTED_KEY, ! toolBar.isVisible());
        }
    }
}
