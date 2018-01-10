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

/* $Id: CIDFont.java 627495 2008-02-13 15:55:34Z jeremias $ */

package org.apache.fop.fonts;

//Java
import java.util.Map;

/**
 * Abstract base class for CID fonts.
 */
public abstract class CIDFont extends CustomFont {

    /**
     * usedGlyphs contains orginal, new glyph index
     */
    public Map usedGlyphs = new java.util.HashMap();

    /**
     * usedGlyphsIndex contains new glyph, original index
     */
    public Map usedGlyphsIndex = new java.util.HashMap();
    public int usedGlyphsCount = 0;

    /**
     * usedCharsIndex contains new glyph, original char
     */
    public Map usedCharsIndex = new java.util.HashMap();

    //private PDFWArray warray = new PDFWArray();
    public int width[] = null;

    // ---- Required ----
    /**
     * Returns the type of the CID font.
     * @return the type of the CID font
     */
    public abstract CIDFontType getCIDType();

    /**
     * Returns the name of the issuer of the font.
     * @return a String identifying an issuer of character collections -
     * for example, Adobe
     */
    public abstract String getRegistry();

    /**
     * Returns a font name for use within a registry.
     * @return a String that uniquely names a character collection issued by
     * a specific registry - for example, Japan1.
     */
    public abstract String getOrdering();

    /**
     * Returns the supplement number of the character collection.
     * @return the supplement number
     */
    public abstract int getSupplement();


    // ---- Optional ----
    /**
     * Returns the default width for this font.
     * @return the default width
     */
    public int getDefaultWidth() {
        return 0;
    }

    /** {@inheritDoc} */
    public boolean isMultiByte() {
        return true;
    }

    /**
     * Returns a char array containing all Unicode characters that have been accessed.
     * @return a char array with all used Unicode characters
     */
    public abstract char[] getCharsUsed();
}