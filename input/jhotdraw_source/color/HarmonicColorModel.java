/**
 * @(#)HarmonicColorModel.java
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

import java.awt.Color;
import java.awt.color.ColorSpace;
import java.beans.PropertyChangeListener;
import javax.swing.ListModel;

/**
 * HarmonicColorModel.
 *
 * @author Werner Randelshofer
 * @version $Id: HarmonicColorModel.java 639 2010-01-22 16:35:25Z rawcoder $
 */
public interface HarmonicColorModel extends ListModel {
    public final static String COLOR_SPACE_PROPERTY = "colorSpace";
    
    public void setBase(int newValue);
    public int getBase();
    
    public void addRule(HarmonicRule rule);
    public void removeAllRules();
    public void applyRules();

    public ColorSpace getColorSpace();
    public void setColorSpace(ColorSpace newValue);
   
    public void setSize(int newValue);
    public int size();
    
    public boolean isAdjusting();
    
    public boolean add(Color c);
    public void set(int index, Color color);
    public Color get(int index);
    public float[] RGBtoComponent(int rgb, float[] hsb);
    public int componentToRGB(float h, float s, float b);

    public void addPropertyChangeListener(PropertyChangeListener listener);
    public void removePropertyChangeListener(PropertyChangeListener listener);
}
