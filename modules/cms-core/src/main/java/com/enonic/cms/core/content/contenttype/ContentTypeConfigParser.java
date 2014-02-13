/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.content.contenttype;

import java.util.List;

import org.jdom.Attribute;
import org.jdom.Element;

import com.enonic.cms.core.content.contenttype.dataentryconfig.DataEntryConfig;
import com.enonic.cms.core.content.contenttype.dataentryconfig.DataEntryConfigType;

public class ContentTypeConfigParser
{

    public static ContentTypeConfig parse( final ContentHandlerName contentHandlerName, final Element configEl )
    {

        final String rootElementName = configEl.getName();
        if ( !"config".equals( rootElementName ) )
        {
            throw new InvalidContentTypeConfigException( "Expected config element to be named 'config', was: " + rootElementName );
        }

        final ContentTypeConfig config = new ContentTypeConfig( contentHandlerName, configEl.getAttributeValue( "name" ) );

        /* Parse form */
        final ContentTypeConfigParser parser = new ContentTypeConfigParser();
        final CtyFormConfig form = parser.parseForm( configEl, config );
        config.setForm( form );

        /* Parse imports */
        final List<CtyImportConfig> imports = ContentTypeImportConfigParser.parseAllImports( form, configEl );
        config.setImports( imports );

        return config;
    }


    private ContentTypeConfigParser()
    {
        // no access
    }

    private CtyFormConfig parseForm( Element configEl, ContentTypeConfig config )
    {

        Element formEl = configEl.getChild( "form" );
        if ( formEl == null )
        {
            throw new InvalidContentTypeConfigException( "Form element not found, expected as child to the config element." );
        }
        CtyFormConfig form = new CtyFormConfig( config );
        Element titleEl = formEl.getChild( "title" );
        if ( titleEl == null )
        {
            throw new InvalidContentTypeConfigException( "Title element not found, expected as child to the form element." );
        }
        final String titleInputName = titleEl.getAttributeValue( "name" );
        form.setTitleInputName( titleInputName );
        List<Element> blockEls = formEl.getChildren( "block" );
        int blockPosition = 1;
        for ( Element blockEl : blockEls )
        {
            form.addBlock( parseBlock( form, blockEl, blockPosition++, titleInputName ) );
        }

        if ( form.getTitleInput() == null )
        {
            throw new InvalidContentTypeConfigException(
                "Referred input field for title '" + form.getTitleInputName() + "' does not exist." );
        }

        DataEntryConfig titleInputConfig = form.getTitleInput();
        if ( !titleInputConfig.isRequired() )
        {
            throw new InvalidContentTypeConfigException(
                "Referred input field for title '" + form.getTitleInputName() + "' must be configured to be required." );
        }
        return form;
    }

    private CtySetConfig parseBlock( final CtyFormConfig form, final Element blockEl, int blockPosition, String titleField )
    {
        String blockName = parseBlockName( blockEl, blockPosition );
        CtySetConfig block = new CtySetConfig( form, blockName, blockEl.getAttributeValue( "group" ) );

        @SuppressWarnings({"unchecked"}) List<Element> inputEls = blockEl.getChildren( "input" );
        int inputConfigPosition = 1;
        for ( Element inputEl : inputEls )
        {
            InputConfigParser inputConfigParser = new InputConfigParser( inputConfigPosition++ );
            DataEntryConfig inputConfig = inputConfigParser.parserInputConfigElement( inputEl );
            if ( inputConfig.getName().equals( titleField ) )
            {
                DataEntryConfigType type = inputConfig.getType();
                if ( !( type.equals( DataEntryConfigType.URL ) || type.equals( DataEntryConfigType.DATE ) ||
                    type.equals( DataEntryConfigType.TEXT ) || type.equals( DataEntryConfigType.RADIOBUTTON ) ||
                    type.equals( DataEntryConfigType.DROPDOWN ) ) )
                {
                    throw new InvalidContentTypeConfigException(
                        "Illegal datatype for title. The title element must be of type text, url, date, radiobutton or dropdown." );
                }
            }
            block.addInput( inputConfig );
        }
        return block;
    }

    private String parseBlockName( final Element blockEl, final int blockPosition )
    {
        Attribute nameAttr = blockEl.getAttribute( "name" );
        if ( nameAttr == null )
        {
            throw new InvalidContentTypeConfigException( "Missing name attribute in block: " + blockPosition );
        }
        String name = nameAttr.getValue();
        if ( name == null || name.trim().length() == 0 )
        {
            throw new InvalidContentTypeConfigException( "Missing name in block: " + blockPosition );
        }
        return name;
    }

}
