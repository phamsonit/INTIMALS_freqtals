/*
 * @(#)OSXClipboard.java
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
package org.jhotdraw.gui.datatransfer;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.Transferable;

/**
 * OSXClipboard.
 *
 * @author Werner Randelshofer
 * @version $Id: OSXClipboard.java 666 2010-07-28 19:11:46Z rawcoder $
 */
public class OSXClipboard extends AWTClipboard {

    public OSXClipboard(Clipboard target) {
        super(target);
    }

    @Override
    public Transferable getContents(Object requestor) {
        Transferable t = super.getContents(requestor);

            try {
                Class c = Class.forName("ch.randelshofer.quaqua.osx.OSXClipboardTransferable");
                @SuppressWarnings("unchecked")
                boolean isAvailable = (Boolean) c.getMethod("isNativeCodeAvailable").invoke(null);
                if (isAvailable) {
                   CompositeTransferable ct = new CompositeTransferable();
                   ct.add(t);
                   ct.add((Transferable) c.newInstance());
                   t = ct;
                }
            } catch (Throwable ex) {
                // silently suppress
            }

        return t;
    }
}
