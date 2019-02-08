/*
 * @(#)ToggleVisibleAction.java
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

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import org.jhotdraw.app.action.ActionUtil;

/**
 * Toggles the visible state of a Component.
 * Is selected, when the Component is visible.
 *
 * @author Werner Randelshofer.
 * @version $Id: ToggleVisibleAction.java 647 2010-01-24 22:52:59Z rawcoder $
 */
public class ToggleVisibleAction extends AbstractAction {
    private Component component;
    
    /** Creates a new instance. */
    public ToggleVisibleAction(Component c, String name) {
        this.component = c;
        putValue(Action.NAME, name);
        putValue(ActionUtil.SELECTED_KEY, c.isVisible());
        c.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                putValue(ActionUtil.SELECTED_KEY, component.isVisible());
            }
            
            @Override
            public void componentHidden(ComponentEvent e) {
                putValue(ActionUtil.SELECTED_KEY, component.isVisible());
            }
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        component.setVisible(! component.isVisible());
    }
}
