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

/* $Id: PresentationTextData.java 631178 2008-02-26 11:07:20Z jeremias $ */

package org.apache.fop.render.afp.modca;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.fop.render.afp.tools.BinaryUtils;

/**
 * Presentation text data contains the graphic characters and the control
 * sequences necessary to position the characters within the object space. The
 * data consists of: - graphic characters to be presented - control sequences
 * that position them - modal control sequences that adjust the positions by
 * small amounts - other functions causing text to be presented with differences
 * in appearance.
 *
 * The graphic characters are expected to conform to a coded font representation
 * so that they can be translated from the code point in the object data to the
 * character in the coded font. The units of measure for linear displacements
 * are derived from the PresentationTextDescriptor or from the hierarchical
 * defaults.
 *
 * In addition to graphic character code points, Presentation Text data can
 * contain embedded control sequences. These are strings of two or more bytes
 * which signal an alternate mode of processing for the content of the current
 * Presentation Text data.
 *
 */
public class PresentationTextData extends AbstractAFPObject {

    /**
     * The maximum size of the presentation text data.
     */
    private static final int MAX_SIZE = 8192;

    /**
     * The afp data relating to this presentaion text data.
     */
    private ByteArrayOutputStream _baos = new ByteArrayOutputStream(1024);

    /**
     * The current x coordinate.
     */
    private int _currentXCoordinate = -1;

    /**
     * The current y cooridnate
     */
    private int _currentYCoordinate = -1;

    /**
     * The current font
     */
    private String _currentFont = "";

    /**
     * The current orientation
     */
    private int _currentOrientation = 0;

    /**
     * The current color
     */
    private Color _currentColor = new Color(0, 0, 0);

    /**
     * The current variable space increment
     */
    private int _currentVariableSpaceCharacterIncrement = 0;

    /**
     * The current inter character adjustment
     */
    private int _currentInterCharacterAdjustment = 0;

    /**
     * Default constructor for the PresentationTextData.
     */
    public PresentationTextData() {

        this(false);

    }

    /**
     * Constructor for the PresentationTextData, the boolean flag indicate
     * whether the control sequence prefix should be set to indicate the start
     * of a new control sequence.
     *
     * @param controlInd
     *            The control sequence indicator.
     */
    public PresentationTextData(boolean controlInd) {

        _baos.write(new byte[] { 0x5A, // Structured field identifier
            0x00, // Record length byte 1
            0x00, // Record length byte 2
            (byte) 0xD3, // PresentationTextData identifier byte 1
            (byte) 0xEE, // PresentationTextData identifier byte 2
            (byte) 0x9B, // PresentationTextData identifier byte 3
            0x00, // Flag
            0x00, // Reserved
            0x00, // Reserved
        }, 0, 9);

        if (controlInd) {
            _baos.write(new byte[] { 0x2B, (byte) 0xD3 }, 0, 2);
        }

    }

    /**
     * The Set Coded Font Local control sequence activates a coded font and
     * specifies the character attributes to be used. This is a modal control
     * sequence.
     *
     * @param font
     *            The font local identifier.
     * @param afpdata
     *            The output stream to which data should be written.
     */
    private void setCodedFont(byte font, ByteArrayOutputStream afpdata) {

        // Avoid unnecessary specification of the font
        if (String.valueOf(font).equals(_currentFont)) {
            return;
        } else {
            _currentFont = String.valueOf(font);
        }

        afpdata.write(new byte[] { 0x03, (byte) 0xF1, font, }, 0, 3);

    }

    /**
     * Establishes the current presentation position on the baseline at a new
     * I-axis coordinate, which is a specified number of measurement units from
     * the B-axis. There is no change to the current B-axis coordinate.
     *
     * @param coordinate
     *            The coordinate for the inline move.
     * @param afpdata
     *            The output stream to which data should be written.
     */
    private void absoluteMoveInline(int coordinate,
        ByteArrayOutputStream afpdata) {

        byte[] b = BinaryUtils.convert(coordinate, 2);

        afpdata.write(new byte[] { 0x04, (byte) 0xC7, b[0], b[1], }, 0, 4);

        _currentXCoordinate = coordinate;

    }

    /**
     * Establishes the baseline and the current presentation position at a new
     * B-axis coordinate, which is a specified number of measurement units from
     * the I-axis. There is no change to the current I-axis coordinate.
     *
     * @param coordinate
     *            The coordinate for the baseline move.
     * @param afpdata
     *            The output stream to which data should be written.
     */
    private void absoluteMoveBaseline(int coordinate,
        ByteArrayOutputStream afpdata) {

        byte[] b = BinaryUtils.convert(coordinate, 2);

        afpdata.write(new byte[] { 0x04, (byte) 0xD3, b[0], b[1], }, 0, 4);

        _currentYCoordinate = coordinate;

    }

    /**
     * The Transparent Data control sequence contains a sequence of code points
     * that are presented without a scan for embedded control sequences.
     *
     * @param data
     *            The text data to add.
     * @param afpdata
     *            The output stream to which data should be written.
     */
    private void addTransparentData(byte[] data, ByteArrayOutputStream afpdata) {

        // Calculate the length
        int l = data.length + 2;

        if (l > 255) {
            // Check that we are not exceeding the maximum length
            throw new IllegalArgumentException(
                "Transparent data is longer than 253 bytes: " + data);
        }

        afpdata.write(new byte[] { BinaryUtils.convert(l)[0], (byte) 0xDB, },
            0, 2);

        afpdata.write(data, 0, data.length);

    }

    /**
     * Draws a line of specified length and specified width in the B-direction
     * from the current presentation position. The location of the current
     * presentation position is unchanged.
     *
     * @param length
     *            The length of the rule.
     * @param width
     *            The width of the rule.
     * @param afpdata
     *            The output stream to which data should be written.
     */
    private void drawBaxisRule(int length, int width,
        ByteArrayOutputStream afpdata) {

        afpdata.write(new byte[] { 0x07, // Length
            (byte) 0xE7, // Type
        }, 0, 2);

        // Rule length
        byte[] data1 = BinaryUtils.shortToByteArray((short) length);
        afpdata.write(data1, 0, data1.length);
        // Rule width
        byte[] data2 = BinaryUtils.shortToByteArray((short) width);
        afpdata.write(data2, 0, data2.length);
        // Rule width fraction
        afpdata.write(0x00);

    }

    /**
     * Draws a line of specified length and specified width in the I-direction
     * from the current presentation position. The location of the current
     * presentation position is unchanged.
     *
     * @param length
     *            The length of the rule.
     * @param width
     *            The width of the rule.
     * @param afpdata
     *            The output stream to which data should be written.
     */
    private void drawIaxisRule(int length, int width,
        ByteArrayOutputStream afpdata) {

        afpdata.write(new byte[] { 0x07, // Length
            (byte) 0xE5, // Type
        }, 0, 2);

        // Rule length
        byte[] data1 = BinaryUtils.shortToByteArray((short) length);
        afpdata.write(data1, 0, data1.length);
        // Rule width
        byte[] data2 = BinaryUtils.shortToByteArray((short) width);
        afpdata.write(data2, 0, data2.length);
        // Rule width fraction
        afpdata.write(0x00);

    }

    /**
     * Create the presentation text data for the byte array of data.
     *
     * @param fontNumber
     *            The font resource identifier.
     * @param x
     *            The x coordinate for the text data.
     * @param y
     *            The y coordinate for the text data.
     * @param orientation
     *            The orientation of the text data.
     * @param col
     *            The text color.
     * @param vsci
     *            The variable space character increment.
     * @param ica
     *            The inter character adjustment.
     * @param data
     *            The text data to be created.
     * @throws MaximumSizeExceededException
     */
    public void createTextData(int fontNumber, int x, int y, int orientation,
        Color col, int vsci, int ica, byte[] data)
        throws MaximumSizeExceededException {

        ByteArrayOutputStream afpdata = new ByteArrayOutputStream();

        if (_currentOrientation != orientation) {
            setTextOrientation(orientation, afpdata);
            _currentOrientation = orientation;
            _currentXCoordinate = -1;
            _currentYCoordinate = -1;
        }

        // Avoid unnecessary specification of the Y co-ordinate
        if (y != _currentYCoordinate) {
            absoluteMoveBaseline(y, afpdata);
            _currentXCoordinate = -1;
        }

        // Avoid unnecessary specification of the X co-ordinate
        if (x != _currentXCoordinate) {
            absoluteMoveInline(x, afpdata);
        }

        // Avoid unnecessary specification of the variable space increment
        if (vsci != _currentVariableSpaceCharacterIncrement) {
            setVariableSpaceCharacterIncrement(vsci, afpdata);
            _currentVariableSpaceCharacterIncrement = vsci;
        }

        // Avoid unnecessary specification of the inter character adjustment
        if (ica != _currentInterCharacterAdjustment) {
            setInterCharacterAdjustment(ica, afpdata);
            _currentInterCharacterAdjustment = ica;
        }

        // Avoid unnecessary specification of the text color
        if (!col.equals(_currentColor)) {
            setExtendedTextColor(col, afpdata);
            _currentColor = col;
        }

        setCodedFont(BinaryUtils.convert(fontNumber)[0], afpdata);
        addTransparentData(data, afpdata);
        _currentXCoordinate = -1;

        int s = afpdata.size();

        if (_baos.size() + s > MAX_SIZE) {
            _currentXCoordinate = -1;
            _currentYCoordinate = -1;
            throw new MaximumSizeExceededException();
        }

        byte[] outputdata = afpdata.toByteArray();
        _baos.write(outputdata, 0, outputdata.length);

    }

    /**
     * Drawing of lines using the starting and ending coordinates, thickness and
     * colour arguments.
     *
     * @param x1
     *            The starting X coordinate.
     * @param y1
     *            The starting Y coordinate.
     * @param x2
     *            The ending X coordinate.
     * @param y2
     *            The ending Y coordinate.
     * @param thickness
     *            The line thickness.
     * @param orientation
     *            The orientation of the text data.
     * @param col
     *            The text color.
     */
    public void createLineData(int x1, int y1, int x2, int y2, int thickness,
        int orientation, Color col) throws MaximumSizeExceededException {

        ByteArrayOutputStream afpdata = new ByteArrayOutputStream();

        if (_currentOrientation != orientation) {
            setTextOrientation(orientation, afpdata);
            _currentOrientation = orientation;
        }

        // Avoid unnecessary specification of the Y coordinate
        if (y1 != _currentYCoordinate) {
            absoluteMoveBaseline(y1, afpdata);
        }

        // Avoid unnecessary specification of the X coordinate
        if (x1 != _currentXCoordinate) {
            absoluteMoveInline(x1, afpdata);
        }

        if (!col.equals(_currentColor)) {
            setExtendedTextColor(col, afpdata);
            _currentColor = col;
        }

        if (y1 == y2) {
            drawIaxisRule(x2 - x1, thickness, afpdata);
        } else if (x1 == x2) {
            drawBaxisRule(y2 - y1, thickness, afpdata);
        } else {
            return;
        }

        int s = afpdata.size();

        if (_baos.size() + s > MAX_SIZE) {
            _currentXCoordinate = -1;
            _currentYCoordinate = -1;
            throw new MaximumSizeExceededException();
        }

        byte[] outputdata = afpdata.toByteArray();
        _baos.write(outputdata, 0, outputdata.length);

    }

    /**
     * The Set Text Orientation control sequence establishes the I-direction and
     * B-direction for the subsequent text. This is a modal control sequence.
     *
     * Semantics: This control sequence specifies the I-axis and B-axis
     * orientations with respect to the Xp-axis for the current Presentation
     * Text object. The orientations are rotational values expressed in degrees
     * and minutes.
     *
     * @param orientation
     *            The text orientation (0,90, 180, 270).
     * @param afpdata
     *            The output stream to which data should be written.
     */
    private void setTextOrientation(int orientation,
        ByteArrayOutputStream afpdata) {

        afpdata.write(new byte[] { 0x06, (byte) 0xF7, }, 0, 2);

        switch (orientation) {
            case 90:
                afpdata.write(0x2D);
                afpdata.write(0x00);
                afpdata.write(0x5A);
                afpdata.write(0x00);
                break;
            case 180:
                afpdata.write(0x5A);
                afpdata.write(0x00);
                afpdata.write(0x87);
                afpdata.write(0x00);
                break;
            case 270:
                afpdata.write(0x87);
                afpdata.write(0x00);
                afpdata.write(0x00);
                afpdata.write(0x00);
                break;
            default:
                afpdata.write(0x00);
                afpdata.write(0x00);
                afpdata.write(0x2D);
                afpdata.write(0x00);
                break;
        }

    }

    /**
     * The Set Extended Text Color control sequence specifies a color value and
     * defines the color space and encoding for that value.  The specified color
     * value is applied to foreground areas of the text presentation space.
     * This is a modal control sequence.
     *
     * @param col
     *            The color to be set.
     * @param afpdata
     *            The output stream to which data should be written.
     */
    private void setExtendedTextColor(Color col,
        ByteArrayOutputStream afpdata) {

        afpdata.write(new byte[] {
              15                // Control sequence length
            , (byte)0x81        // Control sequence function type
            , 0x00              // Reserved; must be zero
            , 0x01              // Color space - 0x01 = RGB
            , 0x00              // Reserved; must be zero
            , 0x00              // Reserved; must be zero
            , 0x00              // Reserved; must be zero
            , 0x00              // Reserved; must be zero
            , 8                 // Number of bits in component 1
            , 8                 // Number of bits in component 2
            , 8                 // Number of bits in component 3
            , 0                 // Number of bits in component 4
            , (byte)(col.getRed())   // Red intensity
            , (byte)(col.getGreen()) // Green intensity
            , (byte)(col.getBlue())  // Blue intensity
        }, 0, 15);

    }

    /**
     * //TODO
     * This is a modal control sequence.
     *
     * @param incr
     *            The increment to be set.
     * @param afpdata
     *            The output stream to which data should be written.
     */
    private void setVariableSpaceCharacterIncrement(int incr,
        ByteArrayOutputStream afpdata) {

        byte[] b = BinaryUtils.convert(incr, 2);

        afpdata.write(new byte[] {
              4                  // Control sequence length
            , (byte)0xC5         // Control sequence function type
            , b[0]
            , b[1]
        }, 0, 4);

    }

    /**
     * //TODO
     * This is a modal control sequence.
     *
     * @param incr
     *            The increment to be set.
     * @param afpdata
     *            The output stream to which data should be written.
     */
    private void setInterCharacterAdjustment(int incr,
        ByteArrayOutputStream afpdata) {

        byte[] b = BinaryUtils.convert(Math.abs(incr), 2);

        afpdata.write(new byte[] {
              5                  // Control sequence length
            , (byte)0xC3         // Control sequence function type
            , b[0]
            , b[1]
            , (byte)(incr >= 0 ? 0 : 1) // Direction
        }, 0, 5);

    }

    /**
     * Accessor method to write the AFP datastream for
     * the text data.
     * @param os The stream to write to
     * @throws java.io.IOException
     */
    public void writeDataStream(OutputStream os)
        throws IOException {

        byte[] data = _baos.toByteArray();
        byte[] size = BinaryUtils.convert(data.length - 1, 2);
        data[1] = size[0];
        data[2] = size[1];

        os.write(data);

    }

    /**
     * A control sequence is a sequence of bytes that specifies a control
     * function. A control sequence consists of a control sequence introducer
     * and zero or more parameters. The control sequence can extend multiple
     * presentation text data objects, but must eventually be terminated. This
     * method terminates the control sequence.
     *
     * @throws MaximumSizeExceededException
     */
    public void endControlSequence() throws MaximumSizeExceededException {

        byte[] data = new byte[2];
        data[0] = 0x02;
        data[1] = (byte) 0xF8;

        if (data.length + _baos.size() > MAX_SIZE) {
            throw new MaximumSizeExceededException();
        }

        _baos.write(data, 0, data.length);

    }

}