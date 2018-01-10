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

/* $Id: ResourceGroup.java 582131 2007-10-05 08:48:15Z jeremias $ */

package org.apache.fop.render.afp.modca;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * A Resource Group contains a set of overlays.
 */
public final class ResourceGroup extends AbstractNamedAFPObject {

    /**
     * Default name for the resource group
     */
    private static final String DEFAULT_NAME = "RG000001";


    /**
     * The overlays contained in this resource group
     */
    private List overlays = new ArrayList();

    /**
     * Default constructor
     */
    public ResourceGroup() {

        this(DEFAULT_NAME);

    }

    /**
     * Constructor for the ResourceGroup, this takes a
     * name parameter which must be 8 characters long.
     * @param name the resource group name
     */
    public ResourceGroup(String name) {

        super(name);

    }

    /**
     * Adds an overlay to the resource group
     * @param overlay the overlay to add
     */
    public void addOverlay(Overlay overlay) {
        overlays.add(overlay);
    }

    /**
     * Returns the list of overlays
     * @return the list of overlays
     */
    public List getOverlays() {
        return overlays;
    }

    /**
     * Accessor method to obtain write the AFP datastream for
     * the resource group.
     * @param os The stream to write to
     * @throws java.io.IOException if an I/O exception of some sort has occurred
     */
    public void writeDataStream(OutputStream os)
        throws IOException {

        writeStart(os);

        writeObjectList(overlays, os);

        writeEnd(os);

    }

    /**
     * Helper method to write the start of the resource group.
     * @param os The stream to write to
     */
    private void writeStart(OutputStream os)
        throws IOException {

        byte[] data = new byte[17];

        data[0] = 0x5A; // Structured field identifier
        data[1] = 0x00; // Length byte 1
        data[2] = 0x10; // Length byte 2
        data[3] = (byte) 0xD3; // Structured field id byte 1
        data[4] = (byte) 0xA8; // Structured field id byte 2
        data[5] = (byte) 0xC6; // Structured field id byte 3
        data[6] = 0x00; // Flags
        data[7] = 0x00; // Reserved
        data[8] = 0x00; // Reserved

        for (int i = 0; i < nameBytes.length; i++) {

            data[9 + i] = nameBytes[i];

        }

        os.write(data);

    }

    /**
     * Helper method to write the end of the resource group.
     * @param os The stream to write to
     */
    private void writeEnd(OutputStream os)
        throws IOException {

        byte[] data = new byte[17];

        data[0] = 0x5A; // Structured field identifier
        data[1] = 0x00; // Length byte 1
        data[2] = 0x10; // Length byte 2
        data[3] = (byte) 0xD3; // Structured field id byte 1
        data[4] = (byte) 0xA9; // Structured field id byte 2
        data[5] = (byte) 0xC6; // Structured field id byte 3
        data[6] = 0x00; // Flags
        data[7] = 0x00; // Reserved
        data[8] = 0x00; // Reserved

        for (int i = 0; i < nameBytes.length; i++) {

            data[9 + i] = nameBytes[i];

        }

        os.write(data);

    }

}