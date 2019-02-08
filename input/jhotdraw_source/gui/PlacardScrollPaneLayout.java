/*
 * @(#)PlacardScrollPaneLayout.java
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
package org.jhotdraw.gui;

import java.awt.*;
import javax.swing.*;

/**
 * PlacardScrollPaneLayout.
 *
 * @author Werner Randelshofer.
 * @version $Id: PlacardScrollPaneLayout.java 647 2010-01-24 22:52:59Z rawcoder $
 */
public class PlacardScrollPaneLayout extends ScrollPaneLayout {

    /**
     * Creates a new instance.
     */
    public PlacardScrollPaneLayout() {
    }

    @Override
    public void layoutContainer(Container parent) {
        super.layoutContainer(parent);

        if (lowerLeft != null && hsb != null) {
            Dimension llp = lowerLeft.getPreferredSize();
//Insets insets = parent.getInsets();
            lowerLeft.setBounds(hsb.getX(), hsb.getY(), llp.width, hsb.getHeight());
            hsb.setBounds(hsb.getX() + llp.width, hsb.getY(), hsb.getWidth() - llp.width, hsb.getHeight());

        }
    }

    /**
     * The UI resource version of <code>ScrollPaneLayout</code>.
     */
    public static class UIResource extends PlacardScrollPaneLayout implements javax.swing.plaf.UIResource {
    }
}



