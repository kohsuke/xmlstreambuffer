/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the "License").  You may not use this file except
 * in compliance with the License.
 * 
 * You can obtain a copy of the license at
 * https://jwsdp.dev.java.net/CDDLv1.0.html
 * See the License for the specific language governing
 * permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL
 * HEADER in each file and include the License file at
 * https://jwsdp.dev.java.net/CDDLv1.0.html  If applicable,
 * add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your
 * own identifying information: Portions Copyright [yyyy]
 * [name of copyright owner]
 */
package com.sun.xml.stream.buffer.sax;

import com.sun.xml.stream.buffer.AbstractProcessor;
import com.sun.xml.stream.buffer.AttributesHolder;
import com.sun.xml.stream.buffer.XMLStreamBuffer;
import com.sun.xml.stream.buffer.MutableXMLStreamBuffer;
import org.xml.sax.ContentHandler;
import org.xml.sax.DTDHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;

import java.io.IOException;
import javax.xml.XMLConstants;

/**
 * A processor of a {@link XMLStreamBuffer} that that reads the XML infoset as
 * {@link XMLReader}.
 */
public class SAXBufferProcessor extends AbstractProcessor implements XMLReader {
    /**
     * Reference to entity resolver.
     */
    protected EntityResolver _entityResolver;
    
    /**
     * Reference to dtd handler.
     */
    protected DTDHandler _dtdHandler;
    
    /**
     * Reference to content handler.
     */
    protected ContentHandler _contentHandler;
    
    /**
     * Reference to error handler.
     */
    protected ErrorHandler _errorHandler;
    
    /**
     * Reference to lexical handler.
     */
    protected LexicalHandler _lexicalHandler;

    /**
     * SAX Namespace attributes features
     */
    protected boolean _namespacePrefixesFeature = false;
    
    protected AttributesHolder _attributes;
    
    protected String[] _namespacePrefixes = new String[16];
    protected int _namespacePrefixesIndex;
    
    protected int[] _namespaceAttributesStack = new int[16];
    protected int _namespaceAttributesStackIndex;

    public SAXBufferProcessor() {
        DefaultWithLexicalHandler handler = new DefaultWithLexicalHandler();        
        _entityResolver = handler;
        _dtdHandler = handler;
        _contentHandler = handler;
        _errorHandler = handler;
        _lexicalHandler = handler;
        
        _attributes = new AttributesHolder();
    }

    public SAXBufferProcessor(XMLStreamBuffer buffer) {
        this();
        setXMLStreamBuffer(buffer);
    }
    
    public boolean getFeature(String name)
            throws SAXNotRecognizedException, SAXNotSupportedException {
        if (name.equals(Features.NAMESPACES_FEATURE)) {
            return true;
        } else if (name.equals(Features.NAMESPACE_PREFIXES_FEATURE)) {
            return _namespacePrefixesFeature;
        } else if (name.equals(Features.EXTERNAL_GENERAL_ENTITIES)) {
            return true;
        } else if (name.equals(Features.EXTERNAL_PARAMETER_ENTITIES)) {
            return true;
        } else if (name.equals(Features.STRING_INTERNING_FEATURE)) {
            return _stringInterningFeature;
        } else {
            throw new SAXNotRecognizedException(
                    "Feature not supported: " + name);
        }
    }
    
    public void setFeature(String name, boolean value)
            throws SAXNotRecognizedException, SAXNotSupportedException {
        if (name.equals(Features.NAMESPACES_FEATURE)) {
            if (!value) {
                throw new SAXNotSupportedException(name + ":" + value);
            }
        } else if (name.equals(Features.NAMESPACE_PREFIXES_FEATURE)) {
            _namespacePrefixesFeature = value;
        } else if (name.equals(Features.EXTERNAL_GENERAL_ENTITIES)) {
            // ignore
        } else if (name.equals(Features.EXTERNAL_PARAMETER_ENTITIES)) {
            // ignore
        } else if (name.equals(Features.STRING_INTERNING_FEATURE)) {
            if (value != _stringInterningFeature) {
                throw new SAXNotSupportedException(name + ":" + value);
            }
        } else {
            throw new SAXNotRecognizedException(
                    "Feature not supported: " + name);
        }        
    }
    
    public Object getProperty(String name)
            throws SAXNotRecognizedException, SAXNotSupportedException {
        if (name.equals(Properties.LEXICAL_HANDLER_PROPERTY)) {
            return getLexicalHandler();
        } else {
            throw new SAXNotRecognizedException("Property not recognized: " + name);
        }
    }
    
    public void setProperty(String name, Object value)
            throws SAXNotRecognizedException, SAXNotSupportedException {
        if (name.equals(Properties.LEXICAL_HANDLER_PROPERTY)) {
            if (value instanceof LexicalHandler) {
                setLexicalHandler((LexicalHandler)value);
            } else {
                throw new SAXNotSupportedException(Properties.LEXICAL_HANDLER_PROPERTY);
            }
        } else {
            throw new SAXNotRecognizedException("Property not recognized: " + name);
        }
    }
    
    public void setEntityResolver(EntityResolver resolver) {
        _entityResolver = resolver;
    }
    
    public EntityResolver getEntityResolver() {
        return _entityResolver;
    }
    
    public void setDTDHandler(DTDHandler handler) {
        _dtdHandler = handler;
    }
    
    public DTDHandler getDTDHandler() {
        return _dtdHandler;
    }
    
    public void setContentHandler(ContentHandler handler) {
        _contentHandler = handler;
    }
    
    public ContentHandler getContentHandler() {
        return _contentHandler;
    }
    
    public void setErrorHandler(ErrorHandler handler) {
        _errorHandler = handler;
    }
    
    public ErrorHandler getErrorHandler() {
        return _errorHandler;
    }
    
    public void setLexicalHandler(LexicalHandler handler) {
        _lexicalHandler = handler;
    }
    
    public LexicalHandler getLexicalHandler() {
        return _lexicalHandler;
    }
    
    public void parse(InputSource input) throws IOException, SAXException {
        // InputSource is ignored
        process();
    }
    
    public void parse(String systemId) throws IOException, SAXException {
        // systemId is ignored
        process();
    }
            
    public final void process(XMLStreamBuffer buffer) throws SAXException {
        setXMLStreamBuffer(buffer);
        process();
    }

    /**
     * Resets the parser to read from the beginning of the given {@link XMLStreamBuffer}.
     */
    public void setXMLStreamBuffer(XMLStreamBuffer buffer) {
        setBuffer(buffer);
    }

    /**
     * Parse the sub-tree (or a whole document) that {@link XMLStreamBuffer}
     * points to, and sends events to handlers.
     * 
     * <p>
     * TODO:
     * We probably need two modes for a sub-tree event generation. One for
     * firing a sub-tree as if it's a whole document (in which case start/endDocument
     * and appropriate additional namespace bindings are necessary), and the other
     * mode for firing a subtree as a subtree, like it does today.
     * A stream buffer SAX feature could be used to specify this. 
     * 
     * @throws SAXException
     *      Follow the same semantics as {@link XMLReader#parse(InputSource)}.
     */
    public final void process() throws SAXException {
        final int item = readStructure();
        switch(_eiiStateTable[item]) {
            case STATE_DOCUMENT:
                processDocument();
                break;
            case STATE_END:
                // Empty buffer
                return;
            case STATE_ELEMENT_U_LN_QN:
                processElement(readStructureString(), readStructureString(), readStructureString());
                break;
            case STATE_ELEMENT_P_U_LN:
            {
                final String prefix = readStructureString();
                final String uri = readStructureString();
                final String localName = readStructureString();
                processElement(uri, localName, getQName(prefix, localName));
                break;
            }
            case STATE_ELEMENT_U_LN: {
                final String uri = readStructureString();
                final String localName = readStructureString();
                processElement(uri, localName, localName);
                break;
            }
            case STATE_ELEMENT_LN:
            {
                final String localName = readStructureString();
                processElement("", localName, localName);
                break;
            }
            default:
                throw reportFatalError("Illegal state for DIIs: "+item);
        }
    }

    /**
     * Report a fatal error and abort.
     *
     * This is necessary to follow the SAX semantics of error handling.
     */
    private SAXParseException reportFatalError(String msg) throws SAXException {
        SAXParseException spe = new SAXParseException(msg, null);
        if(_errorHandler!=null)
            _errorHandler.fatalError(spe);
        return spe;
    }

    private void processDocument() throws SAXException {
        _contentHandler.startDocument();

        boolean firstElementHasOccured = false;
        int item;
        do {
            item = readStructure();
            switch(_eiiStateTable[item]) {
                case STATE_ELEMENT_U_LN_QN:
                    firstElementHasOccured = true;
                    processElement(readStructureString(), readStructureString(), readStructureString());
                    break;
                case STATE_ELEMENT_P_U_LN:
                {
                    firstElementHasOccured = true;
                    final String prefix = readStructureString();
                    final String uri = readStructureString();
                    final String localName = readStructureString();
                    processElement(uri, localName, getQName(prefix, localName));
                    break;
                }
                case STATE_ELEMENT_U_LN: {
                    firstElementHasOccured = true;
                    final String uri = readStructureString();
                    final String localName = readStructureString();
                    processElement(uri, localName, localName);
                    break;
                }
                case STATE_ELEMENT_LN:
                {
                    firstElementHasOccured = true;
                    final String localName = readStructureString();
                    processElement("", localName, localName);
                    break;
                }
                case STATE_COMMENT_AS_CHAR_ARRAY_SMALL:
                {
                    final int length = readStructure();
                    final int start = readContentCharactersBuffer(length);
                    processComment(_contentCharactersBuffer, start, length);
                    break;
                }
                case STATE_COMMENT_AS_CHAR_ARRAY_MEDIUM:
                {
                    final int length = (readStructure() << 8) | readStructure();
                    final int start = readContentCharactersBuffer(length);
                    processComment(_contentCharactersBuffer, start, length);
                    break;
                }
                case STATE_COMMENT_AS_CHAR_ARRAY_COPY:
                {
                    final char[] ch = readContentCharactersCopy();
                    processComment(ch, 0, ch.length);
                    break;
                }
                case STATE_COMMENT_AS_STRING:
                    processComment(readContentString());
                    break;
                case STATE_PROCESSING_INSTRUCTION:
                    processProcessingInstruction(readStructureString(), readStructureString());
                    break;
                case STATE_END:
                    break;
                default:
                    throw reportFatalError("Illegal state for child of DII: "+item);
            }
        } while(item != T_END || !firstElementHasOccured);

        while(item != T_END) {
            item = readStructure();
            switch(_eiiStateTable[item]) {
                case STATE_COMMENT_AS_CHAR_ARRAY_SMALL:
                {
                    final int length = readStructure();
                    final int start = readContentCharactersBuffer(length);
                    processComment(_contentCharactersBuffer, start, length);
                    break;
                }
                case STATE_COMMENT_AS_CHAR_ARRAY_MEDIUM:
                {
                    final int length = (readStructure() << 8) | readStructure();
                    final int start = readContentCharactersBuffer(length);
                    processComment(_contentCharactersBuffer, start, length);
                    break;
                }
                case STATE_COMMENT_AS_CHAR_ARRAY_COPY:
                {
                    final char[] ch = readContentCharactersCopy();
                    processComment(ch, 0, ch.length);
                    break;
                }
                case STATE_COMMENT_AS_STRING:
                    processComment(readContentString());
                    break;
                case STATE_PROCESSING_INSTRUCTION:
                    processProcessingInstruction(readStructureString(), readStructureString());
                    break;
                case STATE_END:
                    break;
                default:
                    throw reportFatalError("Illegal state for child of DII: "+item);
            }
        }

        _contentHandler.endDocument();
    }

    private void processElement(String uri, String localName, String qName) throws SAXException {
        boolean hasAttributes = false;
        boolean hasNamespaceAttributes = false;
        int item = peakStructure();
        if ((item & TYPE_MASK) == T_NAMESPACE_ATTRIBUTE) {
            hasNamespaceAttributes = true;
            item = processNamespaceAttributes(item);
        }        
        if ((item & TYPE_MASK) == T_ATTRIBUTE) {
            hasAttributes = true;
            processAttributes(item);
        }

        _contentHandler.startElement(uri, localName, qName, _attributes);

        if (hasAttributes) {
            _attributes.clear();
        }

        do {
            item = _eiiStateTable[readStructure()];
            switch(item) {
                case STATE_ELEMENT_U_LN_QN:
                    processElement(readStructureString(), readStructureString(), readStructureString());
                    break;
                case STATE_ELEMENT_P_U_LN:
                {
                    final String p = readStructureString();
                    final String u = readStructureString();
                    final String ln = readStructureString();
                    processElement(u, ln, getQName(p, ln));
                    break;
                }
                case STATE_ELEMENT_U_LN: {
                    final String u = readStructureString();
                    final String ln = readStructureString();
                    processElement(u, ln, ln);
                    break;
                }
                case STATE_ELEMENT_LN: {
                    final String ln = readStructureString();
                    processElement("", ln, ln);
                    break;
                }
                case STATE_TEXT_AS_CHAR_ARRAY_SMALL:
                {
                    final int length = readStructure();
                    int start = readContentCharactersBuffer(length);
                    _contentHandler.characters(_contentCharactersBuffer, start, length);
                    break;
                }
                case STATE_TEXT_AS_CHAR_ARRAY_MEDIUM:
                {
                    final int length = (readStructure() << 8) | readStructure();
                    int start = readContentCharactersBuffer(length);
                    _contentHandler.characters(_contentCharactersBuffer, start, length);
                    break;
                }
                case STATE_TEXT_AS_CHAR_ARRAY_COPY:
                {
                    final char[] ch = readContentCharactersCopy();

                    _contentHandler.characters(ch, 0, ch.length);
                    break;
                }
                case STATE_TEXT_AS_STRING:
                {
                    final String s = readContentString();
                    _contentHandler.characters(s.toCharArray(), 0, s.length());
                    break;
                }
                case STATE_TEXT_AS_OBJECT:
                {
                    final CharSequence c = (CharSequence)readContentObject();
                    final String s = c.toString();
                    _contentHandler.characters(s.toCharArray(), 0, s.length());
                    break;
                }
                case STATE_COMMENT_AS_CHAR_ARRAY_SMALL:
                {
                    final int length = readStructure();
                    final int start = readContentCharactersBuffer(length);
                    processComment(_contentCharactersBuffer, start, length);
                    break;
                }
                case STATE_COMMENT_AS_CHAR_ARRAY_MEDIUM:
                {
                    final int length = (readStructure() << 8) | readStructure();
                    final int start = readContentCharactersBuffer(length);
                    processComment(_contentCharactersBuffer, start, length);
                    break;
                }
                case STATE_COMMENT_AS_CHAR_ARRAY_COPY:
                {
                    final char[] ch = readContentCharactersCopy();
                    processComment(ch, 0, ch.length);
                    break;
                }
                case T_COMMENT_AS_STRING:
                    processComment(readContentString());
                    break;
                case STATE_PROCESSING_INSTRUCTION:
                    processProcessingInstruction(readStructureString(), readStructureString());
                    break;
                case STATE_END:
                    break;
                default:
                    throw reportFatalError("Illegal state for child of EII: "+item);
            }
        } while(item != STATE_END);
        
        _contentHandler.endElement(uri, localName, qName);

        if (hasNamespaceAttributes) {
            processEndPrefixMapping();
        }
    }
    
    private void processEndPrefixMapping() throws SAXException {
        final int end = _namespaceAttributesStack[--_namespaceAttributesStackIndex];
        final int start = (_namespaceAttributesStackIndex > 0) ? _namespaceAttributesStack[_namespaceAttributesStackIndex] : 0;
        
        for (int i = end - 1; i >= start; i--) {
            _contentHandler.endPrefixMapping(_namespacePrefixes[i]);
        }
        _namespacePrefixesIndex = start;
    }
    
    private int processNamespaceAttributes(int item) throws SAXException {
        do {
            switch(_niiStateTable[item]) {
                case STATE_NAMESPACE_ATTRIBUTE:
                    // Undeclaration of default namespace
                    processNamespaceAttribute("", "");
                    break;
                case STATE_NAMESPACE_ATTRIBUTE_P:
                    // Undeclaration of namespace
                    processNamespaceAttribute(readStructureString(), "");
                    break;
                case STATE_NAMESPACE_ATTRIBUTE_P_U:
                    // Declaration with prefix
                    processNamespaceAttribute(readStructureString(), readStructureString());
                    break;
                case STATE_NAMESPACE_ATTRIBUTE_U:
                    // Default declaration
                    processNamespaceAttribute("", readStructureString());
                    break;
                default:
                    throw reportFatalError("Illegal state: "+item);
            }
            readStructure();
            
            item = peakStructure();
        } while((item & TYPE_MASK) == T_NAMESPACE_ATTRIBUTE);
        
        
        cacheNamespacePrefixIndex();
        
        return item;
    }
    
    private void processAttributes(int item) throws SAXException {
        do {
            switch(_aiiStateTable[item]) {
                case STATE_ATTRIBUTE_U_LN_QN:
                    _attributes.addAttributeWithQName(readStructureString(), readStructureString(), readStructureString(), readStructureString(), readContentString());
                    break;
                case STATE_ATTRIBUTE_P_U_LN:
                {
                    final String p = readStructureString();
                    final String u = readStructureString();
                    final String ln = readStructureString();
                    _attributes.addAttributeWithQName(u, ln, getQName(p, ln), readStructureString(), readContentString());
                    break;
                }
                case STATE_ATTRIBUTE_U_LN: {
                    final String u = readStructureString();
                    final String ln = readStructureString();
                    _attributes.addAttributeWithQName(u, ln, ln, readStructureString(), readContentString()); 
                    break;
                }
                case STATE_ATTRIBUTE_LN: {
                    final String ln = readStructureString();
                    _attributes.addAttributeWithQName("", ln, ln, readStructureString(), readContentString()); 
                    break;
                }
                default:
                    throw reportFatalError("Illegal state: "+item);
            }
            readStructure();
            
            item = peakStructure();
        } while((item & TYPE_MASK) == T_ATTRIBUTE);
    }

    private void processNamespaceAttribute(String prefix, String uri) throws SAXException {
        _contentHandler.startPrefixMapping(prefix, uri);

        if (_namespacePrefixesFeature) {
            // Add the namespace delcaration as an attribute
            if (prefix != "") {
                _attributes.addAttributeWithQName(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, prefix,
                        getQName(XMLConstants.XMLNS_ATTRIBUTE, prefix),
                        "CDATA", uri);
            } else {
                _attributes.addAttributeWithQName(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, XMLConstants.XMLNS_ATTRIBUTE, 
                        XMLConstants.XMLNS_ATTRIBUTE,
                        "CDATA", uri);
            }
        }
        
        cacheNamespacePrefix(prefix);
    }
    
    private void cacheNamespacePrefix(String prefix) {
        if (_namespacePrefixesIndex == _namespacePrefixes.length) {
            final String[] namespaceAttributes = new String[_namespacePrefixesIndex * 3 / 2 + 1];
            System.arraycopy(_namespacePrefixes, 0, namespaceAttributes, 0, _namespacePrefixesIndex);
            _namespacePrefixes = namespaceAttributes;
        }
        
        _namespacePrefixes[_namespacePrefixesIndex++] = prefix;
    }
    
    private void cacheNamespacePrefixIndex() {
        if (_namespaceAttributesStackIndex == _namespaceAttributesStack.length) {
            final int[] namespaceAttributesStack = new int[_namespaceAttributesStackIndex * 3 /2 + 1];
            System.arraycopy(_namespaceAttributesStack, 0, namespaceAttributesStack, 0, _namespaceAttributesStackIndex);
            _namespaceAttributesStack = namespaceAttributesStack;
        }

        _namespaceAttributesStack[_namespaceAttributesStackIndex++] = _namespacePrefixesIndex;
    }
    
    private void processComment(String s)  throws SAXException {
        processComment(s.toCharArray(), 0, s.length());
    }
    
    private void processComment(char[] ch, int start, int length) throws SAXException {
        _lexicalHandler.comment(ch, start, length);
    }

    private void processProcessingInstruction(String target, String data) throws SAXException {
        _contentHandler.processingInstruction(target, data);
    }
}