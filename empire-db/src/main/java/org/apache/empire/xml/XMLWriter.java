/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.empire.xml;

// Java
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.empire.exceptions.FileWriteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * This class prints out a XML-DOM-Tree to an output stream.
 * <P>
 * 
 *
 */
public class XMLWriter
{
    // Logger
    protected static final Logger    log                  = LoggerFactory.getLogger(XMLWriter.class);

    /** Print writer. */
    protected PrintWriter  out;
    /** Canonical output. */
    protected boolean      canonical;
    /** xmlWriterRoot for debugToFile function */
    private static String  xmlWriterRoot        = null;

    /** Default Encoding */
    private String charsetEncoding = "utf-8";

    /**
     * Prints out the DOM-Tree on System.out for debugging purposes.
     * 
     * @param doc The XML-Document to print
     */
    public static void debug(Document doc)
    {
        XMLWriter dbg = new XMLWriter(System.out);
        dbg.print(doc);
    }

    /**
     * Prints out the DOM-Tree to a file for debugging purposes.
     * The file will be truncated if it exists or created if if does
     * not exist.
     * 
     * @param doc The XML-Document to print
     * @param filename The name of the file to write the XML-Document to
     */
    public static void debugToFile(Document doc, String filename)
    {

        String styleSheet = "../" + filename.substring(0, filename.length() - 3) + "xslt";

        FileOutputStream fileOutputStream = null;
        try
        {
            File file = new File(xmlWriterRoot, filename);
            if (file.exists() == true)
            {
                file.delete();
            }
            // do wen need this? file.createNewFile();
            fileOutputStream = new FileOutputStream(file);
            // write xml
            XMLWriter dbg = new XMLWriter(fileOutputStream);
            dbg.print(doc, styleSheet);
        } catch (IOException ioe) {
            log.error("Error: Could not write XML to file: " + filename + " in directory: " + xmlWriterRoot);
        } finally {
            try {
                if (fileOutputStream != null)
                    fileOutputStream.close();
            } catch (IOException ioe) {
                log.error("Cannot write Document file", ioe);
                /* Ignore IOExceptions */
            }
        }
    }

    /**
     * Saves an XML-Document as file. 
     * The file will be truncated if it exists or created if if does not exist.
     * 
     * @param doc The XML-Document to print
     * @param filename The name of the file to write the XML-Document to
     * 
     * @throws FileWriteException 
     */
    public static void saveAsFile(Document doc, String filename)
    {
        try
        {
            File file = new File(filename);
            if (file.exists() == true) {
                file.delete();
            }

            DOMSource domSource = new DOMSource(doc);
            StreamResult streamResult = new StreamResult(file);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer trf = transformerFactory.newTransformer();
            trf.transform(domSource, streamResult);

        } catch (Exception e) {
            log.error("Could not write XML to file: " + filename);
            throw new FileWriteException(filename, e);
        }
    }

    public static void setXmlWriterDebugPath(String path)
    {
        xmlWriterRoot = path;
    }

    /**
     * Creates a XML Writer object.
     * 
     * @param writer a writer to the output stream
     * @param charsetEncoding encoding type (i.e. utf-8)
     */
    public XMLWriter(Writer writer, String charsetEncoding)
    { 
        this.out = new PrintWriter(writer);
        if (charsetEncoding!=null)
            this.charsetEncoding = charsetEncoding; 
        this.canonical = false;
    }

    /**
     * Creates a XML Writer object.
     * 
     * @param outStream the output stream
     * @param charsetEncoding The name of a supported
     *         {@link java.nio.charset.Charset </code>charset<code>}
     * 
     * @throws UnsupportedEncodingException If the named encoding is not supported
     */
    public XMLWriter(OutputStream outStream, String charsetEncoding)
                                            throws UnsupportedEncodingException
    { // Set Debug Level
        this(new OutputStreamWriter(outStream, charsetEncoding), charsetEncoding);
    }

    /**
     * Constructor
     * 
     * @param outStream the output stream
     */
    public XMLWriter(OutputStream outStream)
    { 	
        try
        {
            this.charsetEncoding = "utf-8"; 
            this.out = new PrintWriter(new OutputStreamWriter(outStream, charsetEncoding));
            this.canonical = false;
        } catch (UnsupportedEncodingException e)
        {
            log.error("The encoding \"" + this.charsetEncoding + "\" is not supported!", e);
        }
    }

    /**
     * Prints the specified node recursively
     * 
     * @param node the current node to print
     * @param level the nesting level used for indenting the output
     * @return the node type of this node
     */
    public int print(Node node, int level)
    {
        // is there anything to do?
        if (node == null)
        {
            return 0;
        }

        int type = node.getNodeType();
        switch (type)
        {
            // print document
            case Node.DOCUMENT_NODE:
            { // Sound not come here
                print(((Document) node).getDocumentElement(), 0);
            }
                break;
            // print element with attributes
            case Node.ELEMENT_NODE:
            {
                // out
                out.print('<');
                out.print(node.getNodeName());
                Attr attrs[] = sortAttributes(node.getAttributes());
                for (int i = 0; i < attrs.length; i++)
                {
                    Attr attr = attrs[i];
                    out.print(' ');
                    out.print(attr.getNodeName());
                    out.print("=\"");
                    out.print(normalize(attr.getNodeValue()));
                    out.print('"');
                }
                // children
                NodeList children = node.getChildNodes();
                if (children != null)
                {
                    // close-tag
                    int len = children.getLength();
                    if (len > 0 && children.item(0).getNodeType() != Node.TEXT_NODE)
                        out.println('>');
                    else
                        out.print('>');
                    // Print all Children
                    int prevType = 0;
                    for (int i = 0; i < len; i++)
                    {
                        if (i > 0 || children.item(i).getNodeType() != Node.TEXT_NODE)
                        { // Indent next Line
                            for (int s = 0; s < level; s++)
                                out.print(" ");
                        }
                        // Print a child
                        prevType = print(children.item(i), level + 1);
                    }
                    // Endtag
                    if (len > 0 && prevType != Node.TEXT_NODE)
                    { // padding
                        for (int s = 1; s < level; s++)
                            out.print(" ");
                    }
                    out.print("</");
                    out.print(node.getNodeName());
                    out.println('>');
                } 
                else
                    out.println("/>");
                break;
            }

            // handle entity reference nodes
            case Node.ENTITY_REFERENCE_NODE:
            {
                if (canonical)
                {
                    NodeList children = node.getChildNodes();
                    if (children != null)
                    {
                        int len = children.getLength();
                        for (int i = 0; i < len; i++)
                        {
                            print(children.item(i), level + 1);
                        }
                    }
                } 
                else
                {
                    out.print('&');
                    out.print(node.getNodeName());
                    out.print(';');
                }
                break;
            }

            // print cdata sections
            case Node.CDATA_SECTION_NODE:
            {
                if (canonical == false)
                {
                    out.print("<![CDATA[");
                    out.print(node.getNodeValue());
                    out.println("]]>");
                } 
                else
                    out.print(normalize(node.getNodeValue()));
                break;
            }

            // print text
            case Node.TEXT_NODE:
            { // Text
                out.print(normalize(node.getNodeValue()));
                break;
            }

            // print processing instruction
            case Node.PROCESSING_INSTRUCTION_NODE:
            {
                out.print("<?");
                out.print(node.getNodeName());
                String data = node.getNodeValue();
                if (data != null && data.length() > 0)
                {
                    out.print(' ');
                    out.print(data);
                }
                out.println("?>");
                break;
            }
        }

        out.flush();
        return type;

    } // print(Node)

    /**
     * Prints the specified document.
     * 
     * @param doc the XML-DOM-Document to print
     */
    public void print(Document doc)
    {
        print(doc, null);
    }

    /**
     * Prints the specified document.
     * 
     * @param doc the XML-DOM-Document to print
     * @param styleSheet the XML-DOM-Document to print
     */
    public void print(Document doc, String styleSheet)
    {
        if (!canonical)
        {
            out.println("<?xml version=\"1.0\" encoding=\"" + charsetEncoding + "\"?>");
        }
        if (styleSheet != null)
        {
            //20040427 Marco: xml stylesheet document specification changed from "<?xml:stylesheet name=\"text/xsl\" href=\""
            // to
            // "<?xml-stylesheet type=\"text/xsl\" href=\""
            out.print("<?xml-stylesheet type=\"text/xsl\" href=\"");
            out.print(styleSheet);
            out.println("\"?>");
        }
        // Print the Document
        print(doc.getDocumentElement(), 0);
        out.flush();
    }

    /**
     * Sorts attributes by name.
     * 
     * @param attrs the unsorted list of attributes
     * @return the sorted list of attributes
     */
    protected Attr[] sortAttributes(NamedNodeMap attrs)
    {

        int len = (attrs != null) ? attrs.getLength() : 0;
        Attr array[] = new Attr[len];
        for (int i = 0; i < len; i++)
        {
            array[i] = (Attr) attrs.item(i);
        }
        for (int i = 0; i < len - 1; i++)
        {
            String name = array[i].getNodeName();
            int index = i;
            for (int j = i + 1; j < len; j++)
            {
                String curName = array[j].getNodeName();
                if (curName.compareTo(name) < 0)
                {
                    name = curName;
                    index = j;
                }
            }
            if (index != i)
            {
                Attr temp = array[i];
                array[i] = array[index];
                array[index] = temp;
            }
        }

        return (array);

    } // sortAttributes(NamedNodeMap):Attr[]

    /**
     * Converts a string to valid XML-Syntax replacing XML entities.
     * 
     * @param s the string to normalize
     */
    protected String normalize(String s)
    {
        return normalize(s, canonical);
    }

    static public String normalize(String s, boolean canonical)
    {
        StringBuilder str = new StringBuilder();

        int len = (s != null) ? s.length() : 0;
        for (int i = 0; i < len; i++)
        {
            char ch = s.charAt(i);
            switch (ch)
            {
                case '<':
                {
                    str.append("&lt;");
                    break;
                }
                case '>':
                {
                    str.append("&gt;");
                    break;
                }
                case '&':
                {
                    str.append("&amp;");
                    break;
                }
                case '"':
                {
                    str.append("&quot;");
                    break;
                }
                case '\r':
                case '\n':
                {
                    if (canonical)
                    {
                        str.append("&#");
                        str.append(Integer.toString(ch));
                        str.append(';');
                        break;
                    }
                    // else, default append char
                }
                default:
                {
                    str.append(ch);
                }
            }
        }

        return (str.toString());

    } // normalize(String):String

}