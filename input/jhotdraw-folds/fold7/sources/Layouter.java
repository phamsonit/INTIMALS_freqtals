/*
 * @(#)Layouter.java
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


package org.jhotdraw.draw.layouter;
import org.jhotdraw.draw.*;
import java.awt.geom.*;
import org.jhotdraw.annotations.NotNull;
/**
 * A Layouter encapsulates a strategy for laying out the child figures of
 * a CompositeFigure. 
 * 
 * <hr>
 * <b>Design Patterns</b>
 *
 * <p><em>Strategy</em><br>
 * Composite figures can be laid out using different layout algorithms which
 * are implemented by layouters.<br>
 * Context: {@link CompositeFigure}; Strategy: {@link Layouter}.
 * <hr>
 * 
 * @author Werner Randelshofer
 * @version $Id: Layouter.java 654 2010-06-25 13:27:08Z rawcoder $
 */
@NotNull
public interface Layouter {
    
    /**
     * Calculate the layout for the figure and all its subelements. The
     * layout is not actually performed but just its dimensions are calculated.
     *
     * @param anchor start point for the layout
     * @param lead minimum lead point for the layout
     */
    public Rectangle2D.Double calculateLayout(CompositeFigure compositeFigure, Point2D.Double anchor, Point2D.Double lead);
    
    /**
     * Method which lays out a figure. It is called by the figure
     * if a layout task is to be performed. Implementing classes
     * specify a certain layout algorithm in this method.
     *
     * @param anchor start point for the layout
     * @param lead minimum lead point for the layout
     */
    public Rectangle2D.Double layout(CompositeFigure compositeFigure, Point2D.Double anchor, Point2D.Double lead);
}