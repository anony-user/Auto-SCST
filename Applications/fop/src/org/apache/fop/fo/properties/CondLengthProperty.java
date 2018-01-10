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

/* $Id: CondLengthProperty.java 627367 2008-02-13 12:03:30Z maxberger $ */

package org.apache.fop.fo.properties;

import org.apache.fop.datatypes.CompoundDatatype;
import org.apache.fop.datatypes.Length;
import org.apache.fop.datatypes.PercentBaseContext;
import org.apache.fop.fo.Constants;
import org.apache.fop.fo.FObj;
import org.apache.fop.fo.PropertyList;
import org.apache.fop.fo.expr.PropertyException;

/**
 * Superclass for properties that have conditional lengths
 */
public class CondLengthProperty extends Property implements CompoundDatatype {
    private Property length;
    private EnumProperty conditionality;

    /**
     * Inner class for creating instances of CondLengthProperty
     */
    public static class Maker extends CompoundPropertyMaker {

        /**
         * @param propId the id of the property for which a Maker should be created
         */
        public Maker(int propId) {
            super(propId);
        }

        /**
         * Create a new empty instance of CondLengthProperty.
         * @return the new instance. 
         */
        public Property makeNewProperty() {
            return new CondLengthProperty();
        }

        /**
         * {@inheritDoc}
         */        
        public Property convertProperty(Property p, PropertyList propertyList, FObj fo)
                    throws PropertyException {
            if (p instanceof KeepProperty) {
                return p;
            }
            return super.convertProperty(p, propertyList, fo);
        }
    }

    /**
     * {@inheritDoc} 
     */
    public void setComponent(int cmpId, Property cmpnValue,
                             boolean bIsDefault) {
        if (cmpId == CP_LENGTH) {
            length = cmpnValue;
        } else if (cmpId == CP_CONDITIONALITY) {
            conditionality = (EnumProperty)cmpnValue;
        }
    }

    /**
     * {@inheritDoc}
     */
    public Property getComponent(int cmpId) {
        if (cmpId == CP_LENGTH) {
            return length;
        } else if (cmpId == CP_CONDITIONALITY) {
            return conditionality;
        } else {
            return null;
        }
    }

    /**
     * Returns the conditionality.
     * @return the conditionality
     */
    public Property getConditionality() {
        return this.conditionality;
    }

    /**
     * Returns the length.
     * @return the length
     */
    public Property getLengthComponent() {
        return this.length;
    }

    /**
     * Indicates if the length can be discarded on certain conditions.
     * @return true if the length can be discarded.
     */
    public boolean isDiscard() {
        return this.conditionality.getEnum() == Constants.EN_DISCARD;
    }

    /**
     * Returns the computed length value.
     * @return the length in millipoints
     */
    public int getLengthValue() {
        return this.length.getLength().getValue();
    }

    /**
     * Returns the computed length value.
     * @param context The context for the length calculation (for percentage based lengths)
     * @return the length in millipoints
     */
    public int getLengthValue(PercentBaseContext context) {
        return this.length.getLength().getValue(context);
    }

    /** {@inheritDoc} */
    public String toString() {
        return "CondLength[" + length.getObject().toString() 
                + ", " + (isDiscard() 
                        ? conditionality.toString().toLowerCase() 
                        : conditionality.toString()) + "]";
    }    

    /**
     * @return this.condLength
     */
    public CondLengthProperty getCondLength() {
        return this;
    }

    /**
     * TODO: Should we allow this?
     * @return this.condLength cast as a Length
     */
    public Length getLength() {
        return length.getLength();
    }

    /**
     * @return this.condLength cast as an Object
     */
    public Object getObject() {
        return this;
    }

}
