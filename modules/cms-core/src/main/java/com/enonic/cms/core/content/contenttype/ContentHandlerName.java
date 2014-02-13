/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.content.contenttype;

public enum ContentHandlerName
{

    ARTICLE( "ContentArticle3HandlerServlet" ),
    CUSTOM( "SimpleContentHandlerServlet" ),
    DOCUMENT( "ContentDocumentHandlerServlet" ),
    FILE( "ContentFileHandlerServlet" ),
    FORM( "ContentFormHandlerServlet" ),
    IMAGE( "ContentEnhancedImageHandlerServlet" ),
    NEWSLETTER( "ContentNewsletterHandlerServlet" ),
    POLL( "ContentPollHandlerServlet" );


    private String handlerClassName;

    ContentHandlerName( String handlerClassName )
    {
        this.handlerClassName = handlerClassName;
    }

    public String getHandlerClassShortName()
    {
        return handlerClassName;
    }

    public static ContentHandlerName parse( String fullClassName )
    {

        for ( ContentHandlerName name : values() )
        {
            if ( fullClassName.endsWith( name.getHandlerClassShortName() ) )
            {
                return name;
            }
        }
        return null;
    }
}
