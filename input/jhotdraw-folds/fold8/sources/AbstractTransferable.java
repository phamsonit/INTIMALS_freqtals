/*
 * @(#)AbstractTransferable.java
 *
 * Copyright (c) 2007 by the original authors of JHotDraw
 * and all its contributors.
 * All rights reserved.
 *
 * The copyright of this software is owned by the authors and  
 * contributors of the JHotDraw project ("the copyright holders").  
 * You may not use, copy or modify this software, except in  
 * accordance with the license agreement you entered into with  
 * the copyright holders. For details see accompanying license terms. 
 */

package org.jhotdraw.gui.datatransfer;

import java.awt.datatransfer.*;

/**
 * Base class for transferable objects.
 *
 * @author Werner Randelshofer
 * @version $Id: AbstractTransferable.java 647 2010-01-24 22:52:59Z rawcoder $
 */
public abstract class AbstractTransferable implements Transferable {
    private DataFlavor[] flavors;
    
    /** Creates a new instance. */
    public AbstractTransferable(DataFlavor flavor) {
        this.flavors = new DataFlavor[] {flavor};
    }
    /** Creates a new instance. */
    public AbstractTransferable(DataFlavor[] flavors) {
        this.flavors = flavors;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return flavors.clone();
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        for (DataFlavor f : flavors) {
            if (f.equals(flavor)) {
                return true;
            }
        }
        return false;
    }
}
