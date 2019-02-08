/*
 * @(#)NonUndoableEdit.java
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

package org.jhotdraw.undo;

import javax.swing.undo.*;
/**
 * NonUndoableEdit.
 *
 * @author  Werner Randelshofer
 * @version $Id: NonUndoableEdit.java 647 2010-01-24 22:52:59Z rawcoder $
 */
public class NonUndoableEdit extends AbstractUndoableEdit {
    
    /** Creates a new instance. */
    public NonUndoableEdit() {
    }
    
    @Override
    public boolean canUndo() {
        return false;
    }
    @Override
    public boolean canRedo() {
        return false;
    }
}
