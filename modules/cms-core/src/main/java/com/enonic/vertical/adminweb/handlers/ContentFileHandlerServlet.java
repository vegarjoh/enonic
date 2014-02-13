/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.vertical.adminweb.handlers;

import java.io.File;
import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.transform.TransformerException;

import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;

import com.enonic.esl.containers.ExtendedMap;
import com.enonic.esl.xml.XMLTool;
import com.enonic.vertical.adminweb.VerticalAdminException;
import com.enonic.vertical.adminweb.handlers.xmlbuilders.ContentFileXMLBuilder;
import com.enonic.vertical.adminweb.wizard.Wizard;
import com.enonic.vertical.engine.VerticalEngineException;

import com.enonic.cms.core.content.binary.BinaryData;
import com.enonic.cms.core.security.user.User;
import com.enonic.cms.core.service.AdminService;

public class ContentFileHandlerServlet
    extends ContentBaseHandlerServlet
{
    private static final String WIZARD_IMPORT_FILES = "wizardconfig_import_files.xml";

    public static class ImportFilesWizard
        extends ImportZipWizard
    {
        /**
         * @see com.enonic.vertical.adminweb.handlers.ContentBaseHandlerServlet.ImportZipWizard#cropName(java.lang.String)
         */
        protected String cropName( String name )
        {
            return name;
        }

        /**
         * @see com.enonic.vertical.adminweb.handlers.ContentBaseHandlerServlet.ImportZipWizard#isFiltered(java.lang.String)
         */
        protected boolean isFiltered( String name )
        {
            return false;
        }

        protected BinaryData[] getBinaries( ContentBaseHandlerServlet cbhServlet, AdminService admin, ExtendedMap formItems, File file )
            throws VerticalAdminException
        {
            formItems.put( "newfile", new DummyFileItem( file ) );
            return cbhServlet.contentXMLBuilder.getBinaries( formItems );
        }
    }

    public ContentFileHandlerServlet()
    {
        super();

        FORM_XSL = "file_form.xsl";
    }

    @Autowired
    public void setContentFileXMLBuilder( final ContentFileXMLBuilder builder )
    {
        setContentXMLBuilder( builder );
    }

    public void handlerCustom( HttpServletRequest request, HttpServletResponse response, HttpSession session, AdminService admin,
                               ExtendedMap formItems, String operation, ExtendedMap parameters, User user, Document verticalDoc )
        throws VerticalAdminException, VerticalEngineException
    {

        if ( operation.equals( "insert" ) )
        {
            int categoryKey = formItems.getInt( "cat" );
            int unitKey = admin.getUnitKey( categoryKey );
            int page = formItems.getInt( "page" );

            ExtendedMap xslParams = new ExtendedMap();
            xslParams.put( "page", String.valueOf( page ) );

            Document newDoc = admin.getContent( user, Integer.parseInt( formItems.getString( "key" ) ), 0, 1, 0 ).getAsDOMDocument();
            if ( newDoc != null )
            {
                String filename = XMLTool.getElementText( newDoc, "/contents/content/contentdata/name" );
                int index = filename.lastIndexOf( "." );
                if ( index != -1 )
                {
                    String filetype = filename.substring( index + 1 ).toLowerCase();
                    if ( "swf".equals( filetype ) )
                    {
                        xslParams.put( "flash", "true" );
                    }
                    else if ( "jpg".equals( filetype ) || "jpeg".equals( filetype ) || "png".equals( filetype ) ||
                        "gif".equals( filetype ) )
                    {
                        xslParams.put( "image", "true" );
                    }
                }
            }

            Document xmlCategory = admin.getSuperCategoryNames( categoryKey, false, true ).getAsDOMDocument();
            XMLTool.mergeDocuments( newDoc, xmlCategory, true );

            addCommonParameters( admin, user, request, xslParams, unitKey, -1 );

            if ( formItems.containsKey( "subop" ) )
            {
                xslParams.put( "subop", formItems.getString( "subop" ) );
            }

            transformXML( request, response, newDoc, "editor/" + "contentpopup_selected.xsl", xslParams );
        }
        else
        {
            super.handlerCustom( request, response, session, admin, formItems, operation, parameters, user, verticalDoc );
        }
    }

    public void handlerWizard( HttpServletRequest request, HttpServletResponse response, HttpSession session, AdminService admin,
                               ExtendedMap formItems, ExtendedMap parameters, User user, String wizardName )
        throws VerticalAdminException, VerticalEngineException, TransformerException, IOException
    {
        if ( "import".equals( wizardName ) )
        {
            Wizard importFilesWizard = Wizard.getInstance( admin, applicationContext, this, session, formItems, WIZARD_IMPORT_FILES );
            importFilesWizard.processRequest( request, response, session, admin, formItems, parameters, user );
        }
        else
        {
            super.handlerWizard( request, response, session, admin, formItems, parameters, user, wizardName );
        }
    }

}
