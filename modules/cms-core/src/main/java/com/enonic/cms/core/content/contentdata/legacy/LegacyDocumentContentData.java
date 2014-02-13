/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.content.contentdata.legacy;

import java.util.List;
import java.util.Map;

import org.jdom.Document;
import org.jdom.Element;

import com.enonic.cms.core.content.binary.BinaryDataAndBinary;
import com.enonic.cms.core.content.binary.BinaryDataKey;

/**
 *
 */
public class LegacyDocumentContentData
    extends AbstractBaseLegacyContentData
{
    public LegacyDocumentContentData( Document contentDataXml )
    {
        super( contentDataXml );
    }

    protected String resolveTitle()
    {
        final Element nameEl = contentDataEl.getChild( "title" );
        return nameEl.getText();
    }

    protected List<BinaryDataAndBinary> resolveBinaryDataAndBinaryList()
    {
        return null;
    }

    public void replaceBinaryKeyPlaceholders( List<BinaryDataKey> binaryDatas )
    {
        // nothing to do for this type
    }

    public void turnBinaryKeysIntoPlaceHolders( Map<BinaryDataKey, Integer> indexByBinaryDataKey )
    {
        // nothing to do for this type
    }
}
