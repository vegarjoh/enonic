/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.vertical.engine.handlers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.enonic.esl.containers.MultiValueMap;
import com.enonic.esl.sql.model.Column;
import com.enonic.vertical.engine.Types;
import com.enonic.vertical.engine.VerticalCreateException;
import com.enonic.vertical.engine.VerticalEngineLogger;
import com.enonic.vertical.engine.VerticalRemoveException;
import com.enonic.vertical.engine.XDG;
import com.enonic.vertical.engine.dbmodel.LogEntryTable;
import com.enonic.vertical.engine.processors.ElementProcessor;
import com.enonic.vertical.engine.processors.MenuElementProcessor;
import com.enonic.vertical.engine.processors.UserElementProcessor;
import com.enonic.vertical.event.MenuHandlerEvent;
import com.enonic.vertical.event.MenuHandlerListener;

import com.enonic.cms.core.log.LogType;
import com.enonic.cms.core.log.StoreNewLogEntryCommand;
import com.enonic.cms.core.log.Table;

@Component
public final class LogHandler
    extends BaseHandler
    implements MenuHandlerListener
{
    public Document getLogEntries( MultiValueMap adminParams, int fromIdx, int count, boolean complete )
    {

        Column[] selectColumns;
        if ( complete )
        {
            selectColumns = null;
        }
        else
        {
            selectColumns = new Column[8];
            selectColumns[0] = LogEntryTable.INSTANCE.len_sKey;
            selectColumns[1] = LogEntryTable.INSTANCE.len_lTypeKey;
            selectColumns[2] = LogEntryTable.INSTANCE.len_lTableKey;
            selectColumns[3] = LogEntryTable.INSTANCE.len_lCount;
            selectColumns[4] = LogEntryTable.INSTANCE.len_men_lKey;
            selectColumns[5] = LogEntryTable.INSTANCE.len_usr_hKey;
            selectColumns[6] = LogEntryTable.INSTANCE.len_sTitle;
            selectColumns[7] = LogEntryTable.INSTANCE.len_dteTimestamp;
        }

        ElementProcessor[] elementProcessors = new ElementProcessor[2];
        elementProcessors[0] = new UserElementProcessor( getCommonHandler() );
        elementProcessors[1] = new MenuElementProcessor( getCommonHandler() );

        CommonHandler commonHandler = getCommonHandler();
        Document doc =
            commonHandler.getData( Types.LOGENTRY, selectColumns, adminParams, elementProcessors, fromIdx, count, "@timestamp", true );

        if ( adminParams.containsKey( "@tablekeyvalue" ) )
        {
            int tableKeyValue = ( (Integer) adminParams.getValueList( "@tablekeyvalue" ).get( 0 ) );
            Table table = Table.parse( ( (Integer) adminParams.getValueList( "@tablekey" ).get( 0 ) ) );
            Element rootElem = doc.getDocumentElement();
            switch ( table )
            {
                case CONTENT:
                    ContentHandler contentHandler = getContentHandler();
                    int versionKey = contentHandler.getCurrentVersionKey( tableKeyValue );
                    rootElem.setAttribute( "title", contentHandler.getContentTitle( versionKey ) );
                    break;
                case MENUITEM:
                    rootElem.setAttribute( "title", getMenuHandler().getMenuItemName( tableKeyValue ) );
                    break;
            }
        }

        return doc;
    }

    public Document getLogEntry( String key )
    {
        ElementProcessor[] elementProcessors = new ElementProcessor[2];
        elementProcessors[0] = new UserElementProcessor( baseEngine.getCommonHandler() );
        elementProcessors[1] = new MenuElementProcessor( baseEngine.getCommonHandler() );
        return getSingleData( key, elementProcessors );
    }

    private Document getSingleData( String key, ElementProcessor[] elementProcessors )
    {
        PreparedStatement preparedStmt = null;
        ResultSet resultSet = null;
        Document doc = null;

        try
        {
            Connection con = getConnection();

            StringBuffer sql = XDG.generateSelectWherePrimaryKeySQL( LogEntryTable.INSTANCE );
            preparedStmt = con.prepareStatement( sql.toString() );
            preparedStmt.setString( 1, key );
            resultSet = preparedStmt.executeQuery();

            doc = XDG.resultSetToXML( LogEntryTable.INSTANCE, resultSet, null, elementProcessors, null, -1 );
        }
        catch ( SQLException sqle )
        {
            String message = "SQL error: %t";
            VerticalEngineLogger.error( message, sqle );
        }
        finally
        {
            close( resultSet );
            close( preparedStmt );
        }

        return doc;
    }

    public void createdMenuItem( MenuHandlerEvent e )
        throws VerticalCreateException
    {

        StoreNewLogEntryCommand command = new StoreNewLogEntryCommand();
        command.setTableKey( Table.MENUITEM );
        command.setTableKeyValue( e.getMenuItemKey() );
        command.setType( LogType.ENTITY_CREATED );
        command.setUser( e.getUser().getKey() );
        command.setTitle( e.getTitle() );

        logService.storeNew( command );
    }

    public void removedMenuItem( MenuHandlerEvent e )
        throws VerticalRemoveException
    {
        StoreNewLogEntryCommand command = new StoreNewLogEntryCommand();
        command.setTableKey( Table.MENUITEM );
        command.setTableKeyValue( e.getMenuItemKey() );
        command.setType( LogType.ENTITY_REMOVED );
        command.setUser( e.getUser().getKey() );
        command.setTitle( e.getTitle() );

        logService.storeNew( command );
    }

    public void updatedMenuItem( MenuHandlerEvent e )
    {
        StoreNewLogEntryCommand command = new StoreNewLogEntryCommand();
        command.setTableKey( Table.MENUITEM );
        command.setTableKeyValue( e.getMenuItemKey() );
        command.setType( LogType.ENTITY_UPDATED );
        command.setUser( e.getUser().getKey() );
        command.setTitle( e.getTitle() );

        logService.storeNew( command );
    }
}
