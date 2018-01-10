/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* $Id: KnuthBlockBox.java 627367 2008-02-13 12:03:30Z maxberger $ */

package org.apache.fop.layoutmgr;

import java.util.LinkedList;

import org.apache.fop.traits.MinOptMax;

/**
 * Knuth box used to represent a line in block-progression-dimension (i.e. the width is its height).
 */
public class KnuthBlockBox extends KnuthBox {
    
    private MinOptMax ipdRange;
    /**
     * Natural width of the line represented by this box. In addition to ipdRange because
     * it isn't possible to get the opt value stored in a MinOptMax object.
     */
    private int bpd;
    private LinkedList footnoteList;
    /** List of Knuth elements. This is a list of LinkedList elements. */
    private LinkedList elementLists = null;

    /**
     * Creates a new box.
     * @param w block progression dimension of this box
     * @param range min, opt, max inline progression dimension of this box
     * @param bpdim natural width of the line represented by this box.
     * @param pos the Position stored in this box
     * @param bAux is this box auxiliary?
     */
    public KnuthBlockBox(int w, MinOptMax range, int bpdim, Position pos, boolean bAux) {
        super(w, pos, bAux);
        ipdRange = (MinOptMax) range.clone();
        bpd = bpdim;
        footnoteList = new LinkedList();
    }

    /**
     * Creates a new box.
     * @param w block progression dimension of this box
     * @param list footnotes cited by elements in this box. The list contains the
     * corresponding FootnoteBodyLayoutManagers 
     * @param pos the Position stored in this box
     * @param bAux is this box auxiliary?
     */
    public KnuthBlockBox(int w, LinkedList list, Position pos, boolean bAux) {
        super(w, pos, bAux);
        ipdRange = new MinOptMax(0);
        bpd = 0;
        footnoteList = new LinkedList(list);
    }

    /**
     * @return the LMs for the footnotes cited in this box.
     */
    public LinkedList getFootnoteBodyLMs() {
        return footnoteList;
    }

    /**
     * @return true if this box contains footnote citations.
     */
    public boolean hasAnchors() {
        return (footnoteList.size() > 0);
    }

    /**
     * Adds the given list of Knuth elements to this box' list of elements.
     * @param list elements corresponding to a footnote body
     */
    public void addElementList(LinkedList list) {
        if (elementLists == null) {
            elementLists = new LinkedList();
        }
        elementLists.add(list);
    }

    /**
     * Returns the list of Knuth sequences registered by this box.
     * @return a list of KnuthElement sequences corresponding to footnotes cited in this
     * box
     */
    public LinkedList getElementLists() {
        return elementLists;
    }

    /**
     * @return the inline progression dimension of this box.
     */
    public MinOptMax getIPDRange() {
        return (MinOptMax) ipdRange.clone();
    }

    /**
     * Returns the natural width (without stretching nor shrinking) of the line
     * represented by this box.
     * @return the line width
     */
    public int getBPD() {
        return bpd;
    }
}
