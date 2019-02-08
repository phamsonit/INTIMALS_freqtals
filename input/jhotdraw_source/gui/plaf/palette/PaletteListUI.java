/*
 * @(#)PaletteListUI.java
 * 
 * Copyright (c) 2010 by the original authors of JHotDraw
 * and all its contributors.
 * All rights reserved.
 * 
 * The copyright of this software is owned by the authors and  
 * contributors of the JHotDraw project ("the copyright holders").  
 * You may not use, copy or modify this software, except in  
 * accordance with the license agreement you entered into with  
 * the copyright holders. For details see accompanying license terms. 
 */

package org.jhotdraw.gui.plaf.palette;

import java.awt.Color;
import javax.swing.JComponent;
import javax.swing.ListCellRenderer;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicListUI;

/**
 * PaletteListUI.
 *
 * @author Werner Randelshofer
 * @version $Id: PaletteListUI.java 666 2010-07-28 19:11:46Z rawcoder $
 */
public class PaletteListUI extends BasicListUI {
    /**
     * Returns a new instance of PaletteListUI.  PaletteListUI delegates are
     * allocated one per JList.
     *
     * @return A new ListUI implementation for the Windows look and feel.
     */
    public static ComponentUI createUI(JComponent list) {
        return new PaletteListUI();
    }
    @Override
    protected void installDefaults()
    {
        super.installDefaults();

        PaletteLookAndFeel.installBorder(list, "List.border");

        PaletteLookAndFeel.installColorsAndFont(list, "List.background", "List.foreground", "List.font");

        PaletteLookAndFeel.installProperty(list, "opaque", Boolean.TRUE);
PaletteLookAndFeel plaf = PaletteLookAndFeel.getInstance();
        if (list.getCellRenderer() == null) {
            list.setCellRenderer((ListCellRenderer)(plaf.get("List.cellRenderer")));
        }

        Color sbg = list.getSelectionBackground();
        if (sbg == null || sbg instanceof UIResource) {
            list.setSelectionBackground(plaf.getColor("List.selectionBackground"));
        }

        Color sfg = list.getSelectionForeground();
        if (sfg == null || sfg instanceof UIResource) {
            list.setSelectionForeground(plaf.getColor("List.selectionForeground"));
        }
    }

}
