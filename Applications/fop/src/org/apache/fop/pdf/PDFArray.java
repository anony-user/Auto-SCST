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

/* $Id: PDFArray.java 627679 2008-02-14 08:12:34Z jeremias $ */
 
package org.apache.fop.pdf;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.output.CountingOutputStream;

/**
 * Class representing an array object.
 */
public class PDFArray extends PDFObject {
    /**
     * List holding the values of this array
     */
    protected List values = new java.util.ArrayList();

    /**
     * Create a new, empty array object
     * @param parent the array's parent if any
     */
    public PDFArray(PDFObject parent) {
        /* generic creation of PDF object */
        super(parent);
    }

    /**
     * Create a new, empty array object with no parent.
     */
    public PDFArray() {
        this(null);
    }

    /**
     * Create an array object.
     * @param parent the array's parent if any
     * @param values the actual array wrapped by this object
     */
    public PDFArray(PDFObject parent, int[] values) {
        /* generic creation of PDF object */
        super(parent);

        for (int i = 0, c = values.length; i < c; i++) {
            this.values.add(new Integer(values[i]));
        }
    }

    /**
     * Create an array object.
     * @param parent the array's parent if any
     * @param values the actual array wrapped by this object
     */
    public PDFArray(PDFObject parent, double[] values) {
        /* generic creation of PDF object */
        super(parent);

        for (int i = 0, c = values.length; i < c; i++) {
            this.values.add(new Double(values[i]));
        }
    }

    /**
     * Create an array object.
     * @param parent the array's parent if any
     * @param values the actual values wrapped by this object
     */
    public PDFArray(PDFObject parent, Collection values) {
        /* generic creation of PDF object */
        super(parent);
        
        this.values.addAll(values);
    }
    
    /**
     * Create the array object
     * @param parent the array's parent if any
     * @param values the actual array wrapped by this object
     */
    public PDFArray(PDFObject parent, Object[] values) {
        /* generic creation of PDF object */
        super(parent);
        
        for (int i = 0, c = values.length; i < c; i++) {
            this.values.add(values[i]);
        }
    }
    
    /**
     * Returns the length of the array
     * @return the length of the array
     */
    public int length() {
        return this.values.size();
    }
    
    /**
     * Sets an entry at a given location.
     * @param index the index of the value to set
     * @param obj the new value
     */
    public void set(int index, Object obj) {
        this.values.set(index, obj);
    }
    
    /**
     * Sets an entry at a given location.
     * @param index the index of the value to set
     * @param value the new value
     */
    public void set(int index, double value) {
        this.values.set(index, new Double(value));
    }
    
    /**
     * Gets an entry at a given location.
     * @param index the index of the value to set
     * @return the requested value
     */
    public Object get(int index) {
        return this.values.get(index);
    }
    
    /**
     * Adds a new value to the array.
     * @param obj the value
     */
    public void add(Object obj) {
        if (obj instanceof PDFObject) {
            PDFObject pdfObj = (PDFObject)obj;
            if (!pdfObj.hasObjectNumber()) {
                pdfObj.setParent(this);
            }
        }
        this.values.add(obj);
    }
    
    /**
     * Adds a new value to the array.
     * @param value the value
     */
    public void add(double value) {
        this.values.add(new Double(value));
    }
    
    /** {@inheritDoc} */
    protected int output(OutputStream stream) throws IOException {
        CountingOutputStream cout = new CountingOutputStream(stream);
        Writer writer = PDFDocument.getWriterFor(cout);
        if (hasObjectNumber()) {
            writer.write(getObjectID());
        }
        
        writer.write('[');
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                writer.write(' ');
            }
            Object obj = this.values.get(i);
            formatObject(obj, cout, writer);
        }
        writer.write(']');
        
        if (hasObjectNumber()) {
            writer.write("\nendobj\n");
        }
        
        writer.flush();
        return cout.getCount();
    }
    
}
