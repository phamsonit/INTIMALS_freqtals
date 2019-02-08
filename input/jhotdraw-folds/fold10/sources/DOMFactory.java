/*
 * @(#)DOMFactory.java
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

package org.jhotdraw.xml;

import java.io.IOException;

/**
 * DOMFactory.
 * <p>
 * <hr>
 * <b>Design Patterns</b>
 *
 * <p><em>Abstract Factory</em><br>
 * {@code DOMFactory} is used by {@code DOMInput} and {@code DOMOutput} for
 * creating Java objects and DOM elements.
 * Abstract Factory: {@link DOMFactory}<br>
 * Client: {@link DOMInput}, {@link DOMOutput}.
 * 
 * <p><em>Strategy</em><br>
 * {@code DOMFactory} is used by {@code DOMInput} and {@code DOMOutput} for
 * reading and writing objects.
 * Client: {@link DOMInput}, {@link DOMOutput}.<br>
 * Strategy: {@link DOMFactory}.<br>
 *
 * <p><em>Chain of Responsibility</em><br>
 * {@code DOMFactory} can delegate reading and writing to objects which implement
 * the {@code DOMStorable} interface.
 * Client: {@link DOMFactory}.<br>
 * Handler: {@link DOMStorable}.<br>
 *
 *
 * @author  Werner Randelshofer
 * @version $Id: DOMFactory.java 633 2010-01-22 12:07:20Z rawcoder $
 */
public interface DOMFactory {
    /**
     * Returns the element name for the specified object.
     */
    public String getName(Object o);
    /**
     * Creates an object from the specified element name.
     */
    public Object create(String name);
    
    /** Writes the specified object to DOMOutput.
     * <p>
     * This method is only ever called from DOMOutput. You should never call
     * this method directly.
     *
     * @param object The object to be written.
     */
    public void write(DOMOutput out, Object object) throws IOException;

    /** Reads the specified object from DOMInput.
     * <p>
     * This method is only ever called from DOMInput. You should never call
     * this method directly.
     *
     * @param in The DOMInput object which creates the object from an element
     * in a DOM.
     */
    public Object read(DOMInput in) throws IOException;
}