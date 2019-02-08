/**
 * @(#)AbstractColorSlidersModel.java
 *
 * Copyright (c) 2008 by the original authors of JHotDraw
 * and all its contributors.
 * All rights reserved.
 *
 * The copyright of this software is owned by the authors and  
 * contributors of the JHotDraw project ("the copyright holders").  
 * You may not use, copy or modify this software, except in  
 * accordance with the license agreement you entered into with  
 * the copyright holders. For details see accompanying license terms. 
 */
package org.jhotdraw.color;

import java.util.*;
import javax.swing.event.*;
import org.jhotdraw.beans.*;

/**
 * AbstractColorSlidersModel.
 *
 * @author Werner Randelshofer
 * @version $Id: AbstractColorSlidersModel.java 647 2010-01-24 22:52:59Z rawcoder $
 */
public abstract class AbstractColorSlidersModel extends AbstractBean implements ColorSliderModel {

    /**
     * ChangeListener's listening to changes in this model.
     */
    protected LinkedList<ChangeListener> listeners;

    @Override
    public void addChangeListener(ChangeListener l) {
        if (listeners == null) {
            listeners = new LinkedList<ChangeListener>();
        }
        listeners.add(l);
    }

    @Override
    public void removeChangeListener(ChangeListener l) {
        listeners.remove(l);
    }

    public void fireStateChanged() {
        if (listeners != null) {
            ChangeEvent event = new ChangeEvent(this);
            for (ChangeListener l : listeners) {
                l.stateChanged(event);
            }
        }
    }
}
