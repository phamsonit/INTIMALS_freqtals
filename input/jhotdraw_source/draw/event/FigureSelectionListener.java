/*
 * @(#)FigureSelectionListener.java
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


package org.jhotdraw.draw.event;

import org.jhotdraw.annotations.NotNull;

/**
 * Interface implemented by observers of selection changes in 
 * {@link org.jhotdraw.draw.DrawingView} objects.
 *
 * <hr>
 * <b>Design Patterns</b>
 *
 * <p><em>Observer</em><br>
 * Selection changes of {@code DrawingView} are observed by user interface
 * components which act on selected figures.<br>
 * Subject: {@link org.jhotdraw.draw.DrawingView}; Observer:
 * {@link FigureSelectionListener}; Event: {@link FigureSelectionEvent}.
 * <hr>
 *
 * @author Werner Randelshofer
 * @version $Id: FigureSelectionListener.java 654 2010-06-25 13:27:08Z rawcoder $
 */
@NotNull
public interface FigureSelectionListener extends java.util.EventListener {
    public void selectionChanged(FigureSelectionEvent evt);
}
