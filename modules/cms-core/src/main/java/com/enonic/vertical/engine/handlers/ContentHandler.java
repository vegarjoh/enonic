/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.vertical.engine.handlers;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.enonic.esl.sql.model.Column;
import com.enonic.esl.sql.model.Table;
import com.enonic.esl.util.ArrayUtil;
import com.enonic.esl.util.StringUtil;
import com.enonic.esl.xml.XMLTool;
import com.enonic.vertical.engine.AccessRight;
import com.enonic.vertical.engine.VerticalEngineLogger;
import com.enonic.vertical.engine.VerticalRemoveException;
import com.enonic.vertical.engine.XDG;
import com.enonic.vertical.engine.dbmodel.ContentPubKeyView;
import com.enonic.vertical.engine.dbmodel.ContentPubKeysView;
import com.enonic.vertical.engine.dbmodel.ContentPublishedView;
import com.enonic.vertical.engine.dbmodel.ContentVersionView;
import com.enonic.vertical.engine.dbmodel.ContentView;
import com.enonic.vertical.engine.processors.ContentTypeProcessor;
import com.enonic.vertical.engine.processors.ElementProcessor;
import com.enonic.vertical.engine.processors.VersionKeyContentMapProcessor;

import com.enonic.cms.framework.util.TIntArrayList;
import com.enonic.cms.framework.util.TIntObjectHashMap;
import com.enonic.cms.framework.xml.XMLDocument;
import com.enonic.cms.framework.xml.XMLDocumentFactory;

import com.enonic.cms.core.CalendarUtil;
import com.enonic.cms.core.content.ContentEntity;
import com.enonic.cms.core.content.ContentKey;
import com.enonic.cms.core.content.ContentTitleXmlCreator;
import com.enonic.cms.core.content.category.CategoryKey;
import com.enonic.cms.core.content.contenttype.ContentHandlerEntity;
import com.enonic.cms.core.content.contenttype.ContentHandlerKey;
import com.enonic.cms.core.content.contenttype.ContentTypeEntity;
import com.enonic.cms.core.content.contenttype.ContentTypeKey;
import com.enonic.cms.core.language.LanguageKey;
import com.enonic.cms.core.resource.ResourceKey;
import com.enonic.cms.core.security.user.User;
import com.enonic.cms.core.security.user.UserKey;
import com.enonic.cms.core.security.user.UserNameXmlCreator;
import com.enonic.cms.core.structure.menuitem.MenuItemEntity;
import com.enonic.cms.store.dao.ContentHandlerDao;
import com.enonic.cms.store.dao.ContentTypeDao;

@Component
public final class ContentHandler
    extends BaseHandler
{

    private final static String CTY_TABLE = "tContentType";

    private final static String HAN_TABLE = "tContentHandler";

    private final static String CON_IS_CHILD = "SELECT rco_con_lParent,rco_con_lChild FROM TRELATEDCONTENT WHERE rco_con_lChild = ?";

    // tContentType
    private final static String CTY_INSERT = "INSERT INTO " + CTY_TABLE +
        "(cty_lkey, cty_sname, cty_sdescription, cty_mbdata, cty_dtetimestamp, cty_han_lkey, cty_blocal, cty_scss) " +
        " VALUES (?,?,?,?,@currentTimestamp@" + ",?,?,?)";

    private final static String CTY_DELETE = "DELETE FROM " + CTY_TABLE + " WHERE cty_lKey=?";

    private final static String CTY_UPDATE = "UPDATE tContentType " + "SET cty_sName = ?" + ",cty_sDescription = ?" + ",cty_mbData = ?" +
        ",cty_dteTimestamp = @currentTimestamp@" + ",cty_han_lKey = ?" + ",cty_sCSS = ?" + " WHERE cty_lKey = ?";

    private final static String CTY_SELECT_KEY = "SELECT cty_lKey FROM " + CTY_TABLE;

    private final static String CTY_WHERE_CLAUSE_HAN = " cty_han_lKey = ?";

    // tContentHandler
    private final static String HAN_INSERT = "INSERT INTO  " + HAN_TABLE +
        "(han_lkey, han_sname, han_sclass,han_sdescription,han_xmlconfig,han_dtetimestamp)" + " VALUES (?,?,?,?,?,@currentTimestamp@)";

    private final static String HAN_SELECT_KEY = "SELECT han_lKey FROM " + HAN_TABLE;

    private final static String HAN_WHERE_CLAUSE_CLASS = " han_sClass = ?";

    private final static String HAN_UPDATE =
        "UPDATE " + HAN_TABLE + " SET han_sName = ?" + ",han_sDescription = ?" + ",han_sClass = ?" + ",han_xmlConfig = ?" +
            ",han_dteTimestamp = @currentTimestamp@" + " WHERE han_lKey = ?";

    private final static String HAN_DELETE = "DELETE FROM " + HAN_TABLE + " WHERE han_lKey = ?";

    @Autowired
    private ContentTypeDao contentTypeDao;

    @Autowired
    private ContentHandlerDao contentHandlerDao;

    public CategoryKey getCategoryKey( int contentKey )
    {
        ContentEntity entity = contentDao.findByKey( new ContentKey( contentKey ) );
        if ( ( entity != null ) && !entity.isDeleted() )
        {
            return entity.getCategory().getKey();
        }
        else
        {
            return null;
        }
    }

    private int[] getCategoryKeys( int[] contentKeys )
    {
        if ( contentKeys == null || contentKeys.length == 0 )
        {
            return new int[0];
        }
        CommonHandler commonHandler = getCommonHandler();
        StringBuffer sql = XDG.generateSelectWhereInSQL( db.tContent, db.tContent.con_cat_lKey, db.tContent.con_lKey, contentKeys.length );
        return commonHandler.getIntArray( sql.toString(), contentKeys );
    }


    public Document getContentTypesDocument( int[] contentTypeKeys )
    {
        Document doc = XMLTool.createDocument( "contenttypes" );
        if ( contentTypeKeys.length > 0 )
        {
            for ( int contentTypeKey : contentTypeKeys )
            {
                Element contentTypeElem = XMLTool.createElement( doc, doc.getDocumentElement(), "contenttype" );
                contentTypeElem.setAttribute( "key", Integer.toString( contentTypeKey ) );
                XMLTool.createElement( doc, contentTypeElem, "name", getContentHandler().getContentTypeName( contentTypeKey ) );
            }
        }
        return doc;
    }

    public boolean contentExists( CategoryKey categoryKey, String contentTitle )
    {
        ContentView contentView = ContentView.getInstance();
        StringBuffer sql = XDG.generateSelectSQL( contentView, contentView.con_lKey.getCountColumn(), null );
        XDG.appendWhereSQL( sql, contentView.cat_lKey, XDG.OPERATOR_EQUAL, categoryKey.toInt() );
        XDG.appendWhereSQL( sql, contentView.cov_sTitle, contentTitle );
        CommonHandler commonHandler = getCommonHandler();
        return commonHandler.getBoolean( sql.toString() );
    }

    public int getContentKey( CategoryKey categoryKey, String contentTitle )
    {
        ContentView contentView = ContentView.getInstance();
        StringBuffer sql = XDG.generateSelectSQL( contentView, contentView.con_lKey, null );
        XDG.appendWhereSQL( sql, contentView.cat_lKey, XDG.OPERATOR_EQUAL, categoryKey.toInt() );
        XDG.appendWhereSQL( sql, contentView.cov_sTitle );
        sql.append( " ?" );
        CommonHandler commonHandler = getCommonHandler();
        return commonHandler.getInt( sql.toString(), contentTitle );
    }

    public String getCreatedTimestamp( int contentKey )
    {
        ContentView contentView = ContentView.getInstance();
        StringBuffer sql = XDG.generateSelectSQL( contentView, contentView.con_dteCreated, false, contentView.con_lKey );
        CommonHandler commonHandler = getCommonHandler();
        return commonHandler.getString( sql.toString(), contentKey );
    }

    public Date getPublishFromTimestamp( int contentKey )
    {
        ContentView contentView = ContentView.getInstance();
        CommonHandler commonHandler = getCommonHandler();
        return commonHandler.getTimestamp( contentView, contentView.con_dtePublishFrom, contentView.con_lKey, contentKey );
    }

    public Date getPublishToTimestamp( int contentKey )
    {
        ContentView contentView = ContentView.getInstance();
        CommonHandler commonHandler = getCommonHandler();
        return commonHandler.getTimestamp( contentView, contentView.con_dtePublishTo, contentView.con_lKey, contentKey );
    }


    public int createContentType( Document doc )
    {
        PreparedStatement preparedStmt = null;
        int key = -1;

        try
        {
            Connection con = getConnection();
            preparedStmt = con.prepareStatement( CTY_INSERT );

            Element root = doc.getDocumentElement();
            Map<String, Element> subelems = XMLTool.filterElements( root.getChildNodes() );

            String keyStr = root.getAttribute( "key" );
            if ( keyStr.length() > 0 )
            {
                key = Integer.parseInt( keyStr );
            }
            else
            {
                key = getNextKey( CTY_TABLE );
            }

            Element subelem = subelems.get( "name" );
            String name = XMLTool.getElementText( subelem );
            subelem = subelems.get( "description" );
            String description;
            if ( subelem != null )
            {
                description = XMLTool.getElementText( subelem );
            }
            else
            {
                description = null;
            }
            subelem = subelems.get( "moduledata" );
            Document moduleDoc = XMLTool.createDocument();
            moduleDoc.appendChild( moduleDoc.importNode( subelem, true ) );
            byte[] mdocBytes = XMLTool.documentToBytes( moduleDoc, "UTF-8" );

            preparedStmt.setInt( 1, key );
            preparedStmt.setString( 2, name );
            if ( description != null )
            {
                preparedStmt.setString( 3, description );
            }
            else
            {
                preparedStmt.setNull( 3, Types.VARCHAR );
            }
            preparedStmt.setBytes( 4, mdocBytes );

            String contentHandlerKeyString = root.getAttribute( "contenthandlerkey" );
            int contentHandlerKey = Integer.parseInt( contentHandlerKeyString );
            preparedStmt.setInt( 5, contentHandlerKey );

            preparedStmt.setInt( 6, 0 );

            // CSS key
            String cssKeyStr = root.getAttribute( "csskey" );
            if ( cssKeyStr.length() > 0 )
            {
                preparedStmt.setString( 7, cssKeyStr );
            }
            else
            {
                preparedStmt.setNull( 7, Types.VARCHAR );
            }

            // add the content type
            int result = preparedStmt.executeUpdate();
            if ( result == 0 )
            {
                String message = "Failed to create content type. No content type created.";
                VerticalEngineLogger.errorCreate( message, null );
            }
        }
        catch ( SQLException sqle )
        {
            String message = "Failed to create content type: %t";
            VerticalEngineLogger.errorCreate( message, sqle );
        }
        finally
        {
            close( preparedStmt );
        }

        return key;
    }

    public Document getContent( User user, int contentKey, boolean publishedOnly, int parentLevel, int childrenLevel,
                                int parentChildrenLevel )
    {
        return getContents( user, new int[]{contentKey}, publishedOnly, parentLevel, childrenLevel, parentChildrenLevel );

        // false, false, null );
    }

    private Document getContents( User user, int[] contentKeys, boolean publishedOnly, int parentLevel, int childrenLevel,
                                  int parentChildrenLevel )
    {
        ContentView contentView = ContentView.getInstance();
        if ( contentKeys == null || contentKeys.length == 0 )
        {
            return XMLTool.createDocument( "contents" );
        }

        Table contentTable;
        if ( publishedOnly )
        {
            contentTable = ContentPublishedView.getInstance();
        }
        else
        {
            contentTable = contentView;
        }

        Column[] selectColumns = null;

        StringBuffer sql = XDG.generateSelectSQL( contentTable, selectColumns, false, null );
        XDG.appendWhereInSQL( sql, contentView.con_lKey, contentKeys.length );

        List<Integer> paramValues = ArrayUtil.toArrayList( contentKeys );

        String sqlString = sql.toString();
        if ( user != null )
        {
            int[] categoryKeys = getCategoryKeys( contentKeys );
            sqlString = getSecurityHandler().appendContentSQL( user, categoryKeys, sqlString );
        }

        return doGetContents( user, null, null, sqlString, paramValues, publishedOnly, parentLevel, childrenLevel, parentChildrenLevel );
    }

    public String getContentTitle( int versionKey )
    {
        StringBuffer sql = XDG.generateSelectSQL( db.tContentVersion, db.tContentVersion.cov_sTitle, false, db.tContentVersion.cov_lKey );
        return getCommonHandler().getString( sql.toString(), versionKey );
    }

    public Document getContentType( int contentTypeKey, boolean includeContentCount )
    {
        return getContentTypes( new int[]{contentTypeKey}, includeContentCount );
    }

    public ResourceKey getContentTypeCSSKey( int contentTypeKey )
    {
        StringBuffer sql = XDG.generateSelectSQL( db.tContentType, db.tContentType.cty_sCSS, false, db.tContentType.cty_lKey );
        String resourceKey = getCommonHandler().getString( sql.toString(), contentTypeKey );
        return ResourceKey.from( resourceKey );
    }

    public int getContentTypeKey( int contentKey )
    {
        ContentEntity entity = contentDao.findByKey( new ContentKey( contentKey ) );
        return entity != null ? entity.getCategory().getContentType().getKey() : -1;
    }

    public int[] getContentTypeKeysByHandler( String handlerClass )
    {
        int contentHandlerKey = getContentHandlerKeyByHandlerClass( handlerClass );
        return getContentTypeKeysByHandler( contentHandlerKey );
    }

    public int[] getContentTypeKeysByHandler( int contentHandlerKey )
    {
        StringBuilder sql = new StringBuilder( CTY_SELECT_KEY );
        sql.append( " WHERE" );
        sql.append( CTY_WHERE_CLAUSE_HAN );

        return getContentTypeKeys( sql.toString(), contentHandlerKey );
    }

    public String getContentTypeName( int contentTypeKey )
    {
        ContentTypeEntity entity = contentTypeDao.findByKey( new ContentTypeKey( contentTypeKey ) );
        return entity != null ? entity.getName() : null;
    }

    public Document getContentTypes( boolean includeContentCount )
    {
        return getContentTypes( null, includeContentCount );
    }

    public Document getContentTypes( int[] contentTypeKeys, boolean includeContentCount )
    {
        List<ContentTypeEntity> list;

        if ( contentTypeKeys != null && contentTypeKeys.length > 0 )
        {
            list = new ArrayList<ContentTypeEntity>();

            for ( int key : contentTypeKeys )
            {
                ContentTypeEntity entity = contentTypeDao.findByKey( new ContentTypeKey( key ) );
                if ( entity != null )
                {
                    list.add( entity );
                }
            }
        }
        else
        {
            list = contentTypeDao.getAll();
        }

        return createContentTypesDoc( list, includeContentCount );
    }

    public Document getContentOwner( int contentKey )
    {
        UserKey ownerKey = getOwnerKey( contentKey );
        User owner = userDao.findByKey( ownerKey );
        UserNameXmlCreator userNameXmlCreator = new UserNameXmlCreator();
        return XMLDocumentFactory.create( userNameXmlCreator.createUserNamesDocument( owner ) ).getAsDOMDocument();
    }

    public UserKey getOwnerKey( int contentKey )
    {
        ContentEntity content = contentDao.findByKey( new ContentKey( contentKey ) );
        return content.getOwner().getKey();
    }

    public int getState( int versionKey )
    {
        ContentVersionView versionView = ContentVersionView.getInstance();
        StringBuffer sql = XDG.generateSelectSQL( versionView, versionView.cov_lState, false, versionView.cov_lKey );
        return getCommonHandler().getInt( sql.toString(), versionKey );
    }


    public void removeContentType( int contentTypeKey )
        throws VerticalRemoveException
    {
        PreparedStatement preparedStmt = null;

        getCommonHandler().cascadeDelete( db.tContentType, contentTypeKey );

        try
        {
            Connection con = getConnection();
            preparedStmt = con.prepareStatement( CTY_DELETE );
            preparedStmt.setInt( 1, contentTypeKey );
            preparedStmt.executeUpdate();
        }
        catch ( SQLException sqle )
        {
            String message = "Failed to remove content type: %t";
            VerticalEngineLogger.errorRemove( message, sqle );
        }
        finally
        {
            close( preparedStmt );
        }
    }

    public void updateContentType( Document doc )
    {
        PreparedStatement preparedStmt = null;

        try
        {
            Element root = doc.getDocumentElement();
            Map<String, Element> subelems = XMLTool.filterElements( root.getChildNodes() );

            int key = Integer.parseInt( root.getAttribute( "key" ) );

            Element subelem = subelems.get( "name" );
            String name = XMLTool.getElementText( subelem );
            subelem = subelems.get( "description" );
            String description;
            if ( subelem != null )
            {
                description = XMLTool.getElementText( subelem );
            }
            else
            {
                description = null;
            }
            Element moduleElem = subelems.get( "moduledata" );
            Document moduleDoc = XMLTool.createDocument();
            moduleDoc.appendChild( moduleDoc.importNode( moduleElem, true ) );
            byte[] mdocBytes = XMLTool.documentToBytes( moduleDoc, "UTF-8" );

            Connection con = getConnection();
            preparedStmt = con.prepareStatement( CTY_UPDATE );
            preparedStmt.setInt( 6, key );
            preparedStmt.setString( 1, name );
            if ( description != null )
            {
                preparedStmt.setString( 2, description );
            }
            else
            {
                preparedStmt.setNull( 2, Types.VARCHAR );
            }
            preparedStmt.setBytes( 3, mdocBytes );

            String contentHandlerKeyString = root.getAttribute( "contenthandlerkey" );
            int contentHandlerKey = Integer.parseInt( contentHandlerKeyString );
            preparedStmt.setInt( 4, contentHandlerKey );

            // CSS key
            String cssKeyStr = root.getAttribute( "csskey" );
            if ( cssKeyStr.length() > 0 )
            {
                preparedStmt.setString( 5, cssKeyStr );
            }
            else
            {
                preparedStmt.setNull( 5, Types.VARCHAR );
            }

            preparedStmt.executeUpdate();
            preparedStmt.close();
            preparedStmt = null;
        }
        catch ( SQLException sqle )
        {
            String message = "Failed to update content type: %t";
            VerticalEngineLogger.errorUpdate( message, sqle );
        }
        catch ( NumberFormatException nfe )
        {
            String message = "Failed to parse content type key: %t";
            VerticalEngineLogger.errorUpdate( message, nfe );
        }
        finally
        {
            close( preparedStmt );
        }
    }


    private Document doGetContents( User user, Set<Integer> referencedKeys, Element contentsElem, String sql, List<Integer> paramValues,
                                    boolean publishedOnly, int parentLevel, int childrenLevel, int parentChildrenLevel )
    {
        final int count = Integer.MAX_VALUE;
        PreparedStatement preparedStmt = null;
        ResultSet resultSet = null;
        PreparedStatement childPreparedStmt = null;
        ResultSet childResultSet = null;

        Document doc;
        Element root;

        doc = XMLTool.createDocument( "contents" );
        if ( contentsElem == null )
        {
            root = doc.getDocumentElement();
        }
        else
        {
            doc = contentsElem.getOwnerDocument();
            root = contentsElem;
        }

        Set<Integer> contentKeys;
        if ( referencedKeys == null )
        {
            contentKeys = new HashSet<Integer>();
        }
        else
        {
            contentKeys = referencedKeys;
        }

        try
        {
            Connection con = getConnection();
            childPreparedStmt = con.prepareStatement( CON_IS_CHILD );
            preparedStmt = con.prepareStatement( sql );
            int paramIndex = -1;
            if ( paramValues != null )
            {
                int i = 1;
                for ( Iterator<Integer> iter = paramValues.iterator(); iter.hasNext(); i++ )
                {
                    Object paramValue = iter.next();

                    preparedStmt.setObject( i, paramValue );
                }
            }
            resultSet = preparedStmt.executeQuery();

            TIntArrayList parentKeys = new TIntArrayList();
            TIntArrayList childrenKeys = new TIntArrayList();
            boolean moreResults = resultSet.next();

            // related content keys elememt map
            TIntObjectHashMap contentKeyRCKElemMap = new TIntObjectHashMap();
            TIntObjectHashMap versionKeyRCKElemMap = new TIntObjectHashMap();

            // content binaries processor
            VersionKeyContentMapProcessor versionKeyContentMapProcessor = new VersionKeyContentMapProcessor( this );

            SectionHandler sectionHandler = getSectionHandler();
            int i = 0;
            while ( ( i < count ) && ( moreResults || ( paramIndex > 0 && paramIndex < paramValues.size() ) ) )
            {

                // if we have a result, get data
                if ( i >= 0 && moreResults )
                {
                    // pre-fetch content data
                    Document contentdata;
                    InputStream contentDataIn = resultSet.getBinaryStream( "cov_xmlContentData" );
                    contentdata = XMLTool.domparse( contentDataIn );

                    Integer contentKey = resultSet.getInt( "con_lKey" );
                    if ( contentKeys.contains( contentKey ) )
                    {
                        moreResults = resultSet.next();
                        continue;
                    }
                    contentKeys.add( contentKey );

                    int versionKey = getCurrentVersionKey( contentKey );

                    Element elem = XMLTool.createElement( doc, root, "content" );
                    elem.setAttribute( "key", contentKey.toString() );
                    String unitKey = resultSet.getString( "cat_uni_lKey" );
                    if ( !resultSet.wasNull() )
                    {
                        elem.setAttribute( "unitkey", unitKey );
                    }
                    elem.setAttribute( "contenttypekey", resultSet.getString( "cat_cty_lKey" ) );
                    elem.setAttribute( "versionkey", String.valueOf( versionKey ) );

                    LanguageKey languageKey = new LanguageKey( resultSet.getInt( "con_lan_lKey" ) );
                    elem.setAttribute( "languagekey", String.valueOf( languageKey ) );
                    elem.setAttribute( "languagecode", getLanguageHandler().getLanguageCode( languageKey ) );

                    elem.setAttribute( "priority", resultSet.getString( "con_lPriority" ) );
                    Timestamp publishfrom = resultSet.getTimestamp( "con_dtePublishFrom" );
                    if ( !resultSet.wasNull() )
                    {
                        elem.setAttribute( "publishfrom", CalendarUtil.formatTimestamp( publishfrom ) );
                    }
                    Timestamp publishto = resultSet.getTimestamp( "con_dtePublishTo" );
                    if ( !resultSet.wasNull() )
                    {
                        elem.setAttribute( "publishto", CalendarUtil.formatTimestamp( publishto ) );
                    }
                    Timestamp created = resultSet.getTimestamp( "con_dteCreated" );
                    elem.setAttribute( "created", CalendarUtil.formatTimestamp( created ) );
                    Timestamp timestamp = resultSet.getTimestamp( "cov_dteTimestamp" );
                    elem.setAttribute( "timestamp", CalendarUtil.formatTimestamp( timestamp ) );

                    // content status
                    //  0: draft
                    //  1: not published
                    //  2: publish date undecided
                    //  3: archived
                    //  4: publish waiting
                    //  5: published
                    //  6: publish expired
                    elem.setAttribute( "status", resultSet.getString( "cov_lStatus" ) );
                    elem.setAttribute( "state", resultSet.getString( "cov_lState" ) );

                    // owner info
                    Element ownerElem = XMLTool.createElement( doc, elem, "owner", resultSet.getString( "usr_sOwnerName" ) );
                    ownerElem.setAttribute( "key", resultSet.getString( "usr_hOwner" ) );
                    ownerElem.setAttribute( "uid", resultSet.getString( "usr_sOwnerUID" ) );
                    ownerElem.setAttribute( "deleted", String.valueOf( resultSet.getBoolean( "usr_bOwnerDeleted" ) ) );

                    // modifier info
                    Element modifierElem = XMLTool.createElement( doc, elem, "modifier", resultSet.getString( "usr_sModifierName" ) );
                    modifierElem.setAttribute( "key", resultSet.getString( "usr_hModifier" ) );
                    modifierElem.setAttribute( "uid", resultSet.getString( "usr_sModifierUID" ) );
                    modifierElem.setAttribute( "deleted", String.valueOf( resultSet.getBoolean( "usr_bModDeleted" ) ) );

                    XMLTool.createElement( doc, elem, "title", resultSet.getString( "cov_sTitle" ) );

                    // add previous pre-fetched content data
                    Node contentDataRoot = doc.importNode( contentdata.getDocumentElement(), true );
                    elem.appendChild( contentDataRoot );

                    // is the content a child?
                    childPreparedStmt.setInt( 1, contentKey );
                    childResultSet = childPreparedStmt.executeQuery();
                    if ( childResultSet.next() )
                    {
                        elem.setAttribute( "child", "true" );
                    }
                    close( childResultSet );
                    childResultSet = null;

                    Element e = XMLTool.createElement( doc, elem, "categoryname", resultSet.getString( "cat_sName" ) );
                    e.setAttribute( "key", resultSet.getString( "cat_lKey" ) );

                    sectionHandler.appendSectionNames( contentKey, elem );

                    // get the content's children and parent keys according to children and parent levels
                    Element relatedcontentkeysElem = XMLTool.createElement( doc, elem, "relatedcontentkeys" );
                    contentKeyRCKElemMap.put( contentKey, relatedcontentkeysElem );
                    versionKeyRCKElemMap.put( versionKey, relatedcontentkeysElem );

                    // Always add version info
                    versionKeyContentMapProcessor.process( elem );

                    // increase index
                    i++;
                }
                else if ( i < 0 )
                {
                    i++;
                }

                moreResults = resultSet.next();
            }

            if ( resultSet != null )
            {
                resultSet.close();
                resultSet = null;
            }
            preparedStmt.close();
            preparedStmt = null;

            // add binaries elements
            getContentBinaries( versionKeyContentMapProcessor.getVersionKeyContentMap() );

            if ( parentLevel > 0 )
            {
                getParentContentKeys( parentKeys, contentKeyRCKElemMap, publishedOnly, true );
            }
            if ( childrenLevel > 0 )
            {
                getChildrenContentKeys( childrenKeys, versionKeyRCKElemMap, publishedOnly );
            }

            Element relatedcontentsElem;
            if ( "relatedcontents".equals( root.getTagName() ) )
            {
                relatedcontentsElem = root;
            }
            else
            {
                relatedcontentsElem = XMLTool.createElement( doc, root, "relatedcontents" );
            }

            Table contentTable;
            ContentView contentView = ContentView.getInstance();
            if ( publishedOnly )
            {
                contentTable = ContentPublishedView.getInstance();
            }
            else
            {
                contentTable = contentView;
            }

            if ( parentLevel > 0 && parentKeys.size() > 0 )
            {
                Column[] selectColumns = null;

                int[] parentContentKeys = parentKeys.toArray();
                StringBuffer sqlString = XDG.generateSelectSQL( contentTable, selectColumns, false, null );
                XDG.appendWhereInSQL( sqlString, contentView.cov_lKey, parentContentKeys );

                int[] categoryKeys = getCategoryKeys( parentContentKeys );
                String tempSql = sqlString.toString();
                tempSql = getSecurityHandler().appendContentSQL( user, categoryKeys, tempSql );

                parentChildrenLevel = Math.min( parentChildrenLevel, 3 );
                doGetContents( user, contentKeys, relatedcontentsElem, tempSql, null, publishedOnly, parentLevel - 1, parentChildrenLevel,
                               parentChildrenLevel
                               // includeStatistics
                               // includeSectionNames
                );
            }

            if ( childrenLevel > 0 && childrenKeys.size() > 0 )
            {
                Column[] selectColumns = null;

                StringBuffer sqlString = XDG.generateSelectSQL( contentTable, selectColumns, false, null );
                sqlString.append( " WHERE con_lKey IN (" );
                int[] childrenContentKeys = childrenKeys.toArray();
                int[] categoryKeys = getCategoryKeys( childrenContentKeys );
                for ( int childrenContentKey : childrenContentKeys )
                {
                    sqlString.append( childrenContentKey );
                    sqlString.append( ',' );
                }
                sqlString.replace( sqlString.length() - 1, sqlString.length(), ")" );
                String tempSql = sqlString.toString();
                tempSql = getSecurityHandler().appendContentSQL( user, categoryKeys, tempSql );

                doGetContents( user, contentKeys, relatedcontentsElem, tempSql, null, publishedOnly, 0, childrenLevel - 1, 0
                               // includeStatistics
                               // includeSectionNames
                );
            }

            if ( contentsElem == null )
            {
                StringBuffer countSql = new StringBuffer( sql );
                int orderByIndex = sql.indexOf( "ORDER BY" );
                int lastParenthesisIndex = sql.lastIndexOf( ")" );
                if ( orderByIndex > 0 )
                {
                    countSql.delete( orderByIndex, sql.length() );
                }

                // append parenthesis if neccessary
                if ( orderByIndex != -1 && lastParenthesisIndex > orderByIndex )
                {
                    countSql.append( ")" );
                }

                countSql.replace( "SELECT ".length(), sql.indexOf( " FROM" ), "count(distinct con_lKey) AS con_lCount" );
                preparedStmt = con.prepareStatement( countSql.toString() );
                if ( paramValues != null )
                {
                    for ( i = 0; i < paramValues.size(); i++ )
                    {
                        preparedStmt.setObject( i + 1, paramValues.get( i ) );
                    }
                }
                resultSet = preparedStmt.executeQuery();
                if ( resultSet.next() )
                {
                    root.setAttribute( "totalcount", resultSet.getString( "con_lCount" ) );
                }
            }
        }
        catch ( SQLException sqle )
        {
            String message = "Failed to get contents; %t";
            VerticalEngineLogger.error( message, sqle );
            if ( contentsElem != null )
            {
                XMLTool.removeChildNodes( contentsElem, false );
            }
            else
            {
                doc = XMLTool.createDocument( "contents" );
            }
        }
        finally
        {
            close( resultSet );
            close( preparedStmt );

            close( childResultSet );
            close( childPreparedStmt );

        }

        return doc;
    }

    private void getChildrenContentKeys( TIntArrayList childrenKeys, TIntObjectHashMap rckElemMap, boolean publishedOnly )
    {

        if ( rckElemMap.size() == 0 )
        {
            return;
        }

        Column[] selectColumns = {db.tRelatedContent.rco_con_lChild, db.tRelatedContent.rco_con_lParent};
        StringBuffer sql = XDG.generateSelectSQL( db.tRelatedContent, selectColumns, false, null );
        if ( publishedOnly )
        {
            XDG.appendJoinSQL( sql, db.tRelatedContent.rco_con_lChild, ContentPubKeyView.getInstance(), db.tContent.con_lKey );
        }
        else
        {
            XDG.appendJoinSQL( sql, db.tRelatedContent.rco_con_lChild, db.tContent, db.tContent.con_lKey );
            XDG.appendWhereSQL( sql, db.tContent.con_bDeleted, XDG.OPERATOR_EQUAL, 0 );
        }
        XDG.appendWhereInSQL( sql, db.tRelatedContent.rco_con_lParent, rckElemMap.keys() );

        Object[][] keys = getCommonHandler().getObjectArray( sql.toString(), (int[]) null );
        for ( Object[] key : keys )
        {
            int childContentKey = ( (Number) key[0] ).intValue();
            int parentVersionKey = ( (Number) key[1] ).intValue();
            Element relatedcontentkeysElem = (Element) rckElemMap.get( parentVersionKey );
            Document doc = relatedcontentkeysElem.getOwnerDocument();

            Element relatedcontentkey = XMLTool.createElement( doc, relatedcontentkeysElem, "relatedcontentkey" );
            if ( !childrenKeys.contains( childContentKey ) )
            {
                childrenKeys.add( childContentKey );
            }
            relatedcontentkey.setAttribute( "key", String.valueOf( childContentKey ) );
            relatedcontentkey.setAttribute( "level", "1" );
        }
    }

    private void getParentContentKeys( TIntArrayList parentKeys, TIntObjectHashMap rckElemMap, boolean publishedOnly, boolean currentOnly )
    {

        if ( rckElemMap.size() == 0 )
        {
            return;
        }

        StringBuffer sql;
        if ( publishedOnly )
        {
            ContentPubKeysView view = ContentPubKeysView.getInstance();
            Column[] selectColumns = {view.con_lKey, db.tRelatedContent.rco_con_lParent, db.tRelatedContent.rco_con_lChild};
            sql = XDG.generateSelectSQL( db.tRelatedContent, selectColumns, false, null );
            XDG.appendJoinSQL( sql, db.tRelatedContent.rco_con_lParent, view, view.cov_lKey );
            XDG.appendWhereInSQL( sql, db.tRelatedContent.rco_con_lChild, rckElemMap.keys() );
        }
        else if ( currentOnly )
        {
            ContentVersionView versionView = ContentVersionView.getInstance();
            Column[] selectColumns = {versionView.con_lKey, db.tRelatedContent.rco_con_lParent, db.tRelatedContent.rco_con_lChild};
            sql = XDG.generateSelectSQL( db.tRelatedContent, selectColumns, false, null );
            XDG.appendJoinSQL( sql, db.tRelatedContent.rco_con_lParent, versionView, versionView.cov_lKey );
            XDG.appendWhereInSQL( sql, db.tRelatedContent.rco_con_lChild, rckElemMap.keys() );
            XDG.appendWhereSQL( sql, versionView.cov_bCurrent, XDG.OPERATOR_EQUAL, 1 );
        }
        else
        {
            Column[] selectColumns =
                {db.tContentVersion.cov_con_lKey, db.tRelatedContent.rco_con_lParent, db.tRelatedContent.rco_con_lChild};
            sql = XDG.generateSelectSQL( db.tRelatedContent, selectColumns, false, null );
            XDG.appendJoinSQL( sql, db.tRelatedContent.rco_con_lParent, db.tContentVersion, db.tContentVersion.cov_lKey );
            XDG.appendJoinSQL( sql, db.tContentVersion.cov_con_lKey, db.tContent, db.tContent.con_lKey );
            XDG.appendWhereInSQL( sql, db.tRelatedContent.rco_con_lChild, rckElemMap.keys() );
            XDG.appendWhereSQL( sql, db.tContent.con_bDeleted, XDG.OPERATOR_EQUAL, 0 );
        }

        Object[][] keys = getCommonHandler().getObjectArray( sql.toString(), (int[]) null );
        for ( int i = 0; i < keys.length; i++ )
        {
            int parentContentKey = ( (Number) keys[i][0] ).intValue();
            int parentVersionKey = ( (Number) keys[i][1] ).intValue();
            int childContentKey = ( (Number) keys[i][2] ).intValue();
            Element relatedcontentkeysElem = (Element) rckElemMap.get( childContentKey );
            Document doc = relatedcontentkeysElem.getOwnerDocument();

            Element relatedcontentkey = XMLTool.createElement( doc, relatedcontentkeysElem, "relatedcontentkey" );
            if ( !parentKeys.contains( parentVersionKey ) )
            {
                parentKeys.add( parentVersionKey );
            }
            relatedcontentkey.setAttribute( "key", String.valueOf( parentContentKey ) );
            relatedcontentkey.setAttribute( "versionkey", String.valueOf( parentVersionKey ) );
            relatedcontentkey.setAttribute( "level", "-1" );
        }
    }

    private Document createContentTypesDoc( List<ContentTypeEntity> list, boolean includeContentCount )
    {
        Document doc = XMLTool.createDocument( "contenttypes" );
        Element root = doc.getDocumentElement();

        if ( list == null )
        {
            return doc;
        }

        for ( ContentTypeEntity entity : list )
        {
            Element elem = XMLTool.createElement( doc, root, "contenttype" );
            elem.setAttribute( "key", String.valueOf( entity.getKey() ) );
            elem.setAttribute( "contenthandlerkey", String.valueOf( entity.getContentHandler().getKey() ) );
            elem.setAttribute( "handler", entity.getContentHandler().getClassName() );

            if ( entity.getDefaultCssKey() != null )
            {
                elem.setAttribute( "csskey", entity.getDefaultCssKey().toString() );
                elem.setAttribute( "csskeyexists",
                                   resourceService.getResourceFile( entity.getDefaultCssKey() ) != null ? "true" : "false" );
            }

            if ( includeContentCount )
            {
                int count = getContentCountByContentType( entity.getKey() );
                elem.setAttribute( "contentcount", String.valueOf( count ) );
            }

            XMLTool.createElement( doc, elem, "name", entity.getName() );
            if ( entity.getDescription() != null )
            {
                XMLTool.createElement( doc, elem, "description", entity.getDescription() );
            }

            if ( entity.getData() != null )
            {
                Document modDoc = XMLTool.domparse( XMLTool.documentToString( entity.getData() ) );
                Element mdElem = modDoc.getDocumentElement();

                if ( mdElem.getTagName().equals( "module" ) )
                {
                    Element tempElem = XMLTool.createElement( doc, elem, "moduledata" );
                    tempElem.appendChild( doc.importNode( mdElem, true ) );
                }
                else
                {
                    elem.appendChild( doc.importNode( mdElem, true ) );
                }
            }
            else
            {
                XMLTool.createElement( doc, elem, "moduledata" );
            }

            XMLTool.createElement( doc, elem, "timestamp", CalendarUtil.formatTimestamp( entity.getTimestamp(), true ) );
        }

        return doc;
    }

    public int getContentCountByContentType( int contentTypeKey )
    {
        ContentView contentView = ContentView.getInstance();
        StringBuffer countSQL =
            XDG.generateSelectSQL( contentView, contentView.con_lKey.getCountColumn(), false, contentView.cat_cty_lKey );
        return getCommonHandler().getInt( countSQL.toString(), contentTypeKey );
    }

    private int[] getContentTypeKeys( String sql, int paramValue )
    {
        PreparedStatement preparedStmt = null;
        ResultSet resultSet = null;
        TIntArrayList contentTypeKeys = new TIntArrayList();

        try
        {
            Connection con = getConnection();

            preparedStmt = con.prepareStatement( sql );
            preparedStmt.setInt( 1, paramValue );
            resultSet = preparedStmt.executeQuery();

            while ( resultSet.next() )
            {
                contentTypeKeys.add( resultSet.getInt( 1 ) );
            }
        }
        catch ( SQLException sqle )
        {
            String message = "Failed to get content type keys: %t";
            VerticalEngineLogger.error( message, sqle );
        }
        finally
        {
            close( resultSet );
            close( preparedStmt );
        }

        return contentTypeKeys.toArray();
    }

    public String getContentHandlerClassForContentType( int contentTypeKey )
    {
        final ContentTypeEntity entity = contentTypeDao.findByKey( new ContentTypeKey( contentTypeKey ) );
        if ( entity == null )
        {
            return null;
        }
        return entity.getContentHandler().getClassName();
    }

    public org.jdom.Document getContentHandler( final int contentHandlerKey )
    {
        final ContentHandlerEntity entity = this.contentHandlerDao.findByKey( new ContentHandlerKey( contentHandlerKey ) );
        List<ContentHandlerEntity> list = Collections.emptyList();

        if ( entity != null )
        {
            list = Collections.singletonList( entity );
        }

        return toDocument( list );
    }

    private int getContentHandlerKeyByHandlerClass( String handlerClass )
    {
        StringBuilder sql = new StringBuilder( HAN_SELECT_KEY );
        sql.append( " WHERE" );
        sql.append( HAN_WHERE_CLAUSE_CLASS );

        return getCommonHandler().getInt( sql.toString(), new Object[]{handlerClass} );
    }

    public org.jdom.Document getContentHandlers()
    {
        final List<ContentHandlerEntity> list = this.contentHandlerDao.findAll();
        return toDocument( list );
    }

    private org.jdom.Document toDocument( final List<ContentHandlerEntity> list )
    {
        final org.jdom.Element root = new org.jdom.Element( "contenthandlers" );

        for ( final ContentHandlerEntity entity : list )
        {
            final org.jdom.Element elem = new org.jdom.Element( "contenthandler" );
            root.addContent( elem );

            elem.setAttribute( "key", entity.getKey().toString() );

            elem.addContent( new org.jdom.Element( "name" ).setText( entity.getName() ) );
            elem.addContent( new org.jdom.Element( "class" ).setText( entity.getClassName() ) );

            final String description = entity.getDescription();
            if ( description != null )
            {
                elem.addContent( new org.jdom.Element( "description" ).setText( description ) );
            }

            final org.jdom.Document xmlConfig = entity.getXmlConfig();
            if ( xmlConfig != null )
            {
                elem.addContent( xmlConfig.getRootElement().detach() );
            }
            else
            {
                elem.addContent( new org.jdom.Element( "xmlconfig" ) );
            }

            final String timestamp = CalendarUtil.formatTimestamp( entity.getTimestamp(), true );
            elem.addContent( new org.jdom.Element( "timestamp" ).setText( timestamp ) );
        }

        return new org.jdom.Document( root );
    }

    public int createContentHandler( Document doc )
    {
        PreparedStatement preparedStmt = null;
        int key = -1;

        try
        {
            Connection con = getConnection();

            preparedStmt = con.prepareStatement( HAN_INSERT );

            Element root = doc.getDocumentElement();

            String keyStr = root.getAttribute( "key" );

            if ( keyStr.length() > 0 )
            {
                key = Integer.parseInt( keyStr );
            }
            else
            {
                key = getNextKey( HAN_TABLE );
            }

            Map<String, Element> subelems = XMLTool.filterElements( root.getChildNodes() );

            Element subelem = subelems.get( "name" );
            String name = XMLTool.getElementText( subelem );
            subelem = subelems.get( "class" );
            String className = XMLTool.getElementText( subelem );

            subelem = subelems.get( "description" );
            String description;
            if ( subelem != null )
            {
                description = XMLTool.getElementText( subelem );
            }
            else
            {
                description = null;
            }

            subelem = subelems.get( "xmlconfig" );
            Document configDoc = XMLTool.createDocument();
            configDoc.appendChild( configDoc.importNode( subelem, true ) );
            byte[] cdocBytes = XMLTool.documentToBytes( configDoc, "UTF-8" );

            preparedStmt.setInt( 1, key );
            preparedStmt.setString( 2, name );
            preparedStmt.setString( 3, className );
            if ( description != null )
            {
                preparedStmt.setString( 4, description );
            }
            else
            {
                preparedStmt.setNull( 4, Types.VARCHAR );
            }
            preparedStmt.setBytes( 5, cdocBytes );

            // add the content handler
            int result = preparedStmt.executeUpdate();
            if ( result == 0 )
            {
                String message = "Failed to create content handler. No content handler created.";
                VerticalEngineLogger.errorCreate( message, null );
            }
        }
        catch ( SQLException sqle )
        {
            String message = "Failed to create content handler: %t";
            VerticalEngineLogger.errorCreate( message, sqle );
        }
        finally
        {
            close( preparedStmt );
        }

        return key;
    }

    public void updateContentHandler( Document doc )
    {
        PreparedStatement preparedStmt = null;

        try
        {
            Element root = doc.getDocumentElement();
            Map<String, Element> subelems = XMLTool.filterElements( root.getChildNodes() );

            int key = Integer.parseInt( root.getAttribute( "key" ) );
            Element subelem = subelems.get( "name" );
            String name = XMLTool.getElementText( subelem );
            subelem = subelems.get( "class" );
            String className = XMLTool.getElementText( subelem );
            subelem = subelems.get( "description" );
            String description;
            if ( subelem != null )
            {
                description = XMLTool.getElementText( subelem );
            }
            else
            {
                description = null;
            }
            Element configElem = subelems.get( "xmlconfig" );
            Document configDoc = XMLTool.createDocument();
            configDoc.appendChild( configDoc.importNode( configElem, true ) );
            byte[] cdocBytes = XMLTool.documentToBytes( configDoc, "UTF-8" );

            Connection con = getConnection();
            preparedStmt = con.prepareStatement( HAN_UPDATE );
            preparedStmt.setInt( 5, key );
            preparedStmt.setString( 1, name );
            if ( description != null )
            {
                preparedStmt.setString( 2, description );
            }
            else
            {
                preparedStmt.setNull( 2, Types.VARCHAR );
            }
            preparedStmt.setString( 3, className );
            preparedStmt.setBytes( 4, cdocBytes );
            preparedStmt.executeUpdate();
            preparedStmt.close();
            preparedStmt = null;
        }
        catch ( SQLException sqle )
        {
            String message = "Failed to update content type: %t";
            VerticalEngineLogger.errorUpdate( message, sqle );
        }
        catch ( NumberFormatException nfe )
        {
            String message = "Failed to parse content type key: %t";
            VerticalEngineLogger.errorUpdate( message, nfe );
        }
        finally
        {
            close( preparedStmt );
        }
    }

    public void removeContentHandler( int contentHandlerKey )
        throws VerticalRemoveException
    {
        PreparedStatement preparedStmt = null;

        getCommonHandler().cascadeDelete( db.tContentHandler, contentHandlerKey );

        try
        {
            Connection con = getConnection();

            preparedStmt = con.prepareStatement( HAN_DELETE );
            preparedStmt.setInt( 1, contentHandlerKey );
            preparedStmt.executeUpdate();
        }
        catch ( SQLException sqle )
        {
            String message = "Failed to remove content handler: %t";
            VerticalEngineLogger.errorRemove( message, sqle );
        }
        finally
        {
            close( preparedStmt );
        }
    }

    public Document getContentTypeModuleData( int contentTypeKey )
    {
        Document doc;
        Document modDoc = getCommonHandler().getDocument( db.tContentType, contentTypeKey );
        Element mdElem = modDoc.getDocumentElement();

        // workaround for old contenttypes
        if ( mdElem.getTagName().equals( "module" ) )
        {
            doc = XMLTool.createDocument( "moduledata" );
            doc.getDocumentElement().appendChild( doc.importNode( mdElem, true ) );
        }
        else
        {
            doc = XMLTool.createDocument();
            doc.appendChild( doc.importNode( mdElem, true ) );
        }
        return doc;
    }

    public XMLDocument getContentTitles( int[] contentKeys, int[] totalContentKeys, boolean includeSectionInfo, MenuItemEntity section )
    {
        org.jdom.Element contentsEl = new org.jdom.Element( "contenttitles" );
        if ( contentKeys == null || contentKeys.length == 0 )
        {
            return XMLDocumentFactory.create( new org.jdom.Document( contentsEl ) );
        }

        Date now = new Date();
        ContentTitleXmlCreator contentTitleXmlCreator = new ContentTitleXmlCreator( now );
        contentTitleXmlCreator.setIncludeIsInHomeSectionInfo( includeSectionInfo, section );

        for ( int contentKeyInt : contentKeys )
        {
            ContentKey contentKey = new ContentKey( contentKeyInt );
            ContentEntity content = contentDao.findByKey( contentKey );

            org.jdom.Element contentEl = contentTitleXmlCreator.createContentTitleElement( content );
            contentsEl.addContent( contentEl );
        }

        if ( totalContentKeys != null )
        {
            for ( int totalContentKeyInt : totalContentKeys )
            {
                ContentKey totalContentKey = new ContentKey( totalContentKeyInt );
                ContentEntity totalContent = contentDao.findByKey( totalContentKey );

                org.jdom.Element totalContentEl = contentTitleXmlCreator.createTotalContentTitleElement( totalContent );
                contentsEl.addContent( totalContentEl );
            }
        }

        return XMLDocumentFactory.create( new org.jdom.Document( contentsEl ) );
    }

    public Document getContentTitleDoc( int versionKey )
    {
        Document doc = XMLTool.createDocument( "contenttitles" );

        ContentVersionView versionView = ContentVersionView.getInstance();
        StringBuffer sql = XDG.generateSelectSQL( versionView, versionView.cov_lKey );

        PreparedStatement preparedStmt = null;
        ResultSet resultSet = null;
        Element root = doc.getDocumentElement();

        try
        {
            Connection con = getConnection();
            preparedStmt = con.prepareStatement( sql.toString() );
            preparedStmt.setInt( 1, versionKey );
            resultSet = preparedStmt.executeQuery();

            while ( resultSet.next() )
            {
                Element elem = XMLTool.createElement( doc, root, "contenttitle" );
                int contentKey = resultSet.getInt( "con_lKey" );
                elem.setAttribute( "key", String.valueOf( contentKey ) );
                int categoryKey = resultSet.getInt( "cat_lKey" );
                elem.setAttribute( "categorykey", String.valueOf( categoryKey ) );
                Timestamp timestamp = resultSet.getTimestamp( "cov_dteTimestamp" );
                elem.setAttribute( "timestamp", CalendarUtil.formatTimestamp( timestamp ) );

                //elem.setAttribute("unitkey", String.valueOf(resultSet.getInt("cat_uni_lKey")));
                elem.setAttribute( "contenttypekey", String.valueOf( resultSet.getInt( "cat_cty_lKey" ) ) );

                Timestamp publishfrom = resultSet.getTimestamp( "con_dtePublishFrom" );
                if ( !resultSet.wasNull() )
                {
                    elem.setAttribute( "publishfrom", CalendarUtil.formatTimestamp( publishfrom ) );
                }
                Timestamp publishto = resultSet.getTimestamp( "con_dtePublishTo" );
                if ( !resultSet.wasNull() )
                {
                    elem.setAttribute( "publishto", CalendarUtil.formatTimestamp( publishto ) );
                }

                // content status
                //  0: draft
                //  1: not published
                //  2: publish waiting
                //  3: published
                //  4: archived
                elem.setAttribute( "status", resultSet.getString( "cov_lStatus" ) );
                elem.setAttribute( "state", resultSet.getString( "cov_lState" ) );

                XMLTool.createTextNode( doc, elem, resultSet.getString( "cov_sTitle" ) );
            }
        }
        catch ( SQLException sqle )
        {
            String message = "Failed to get content titles; %t";
            VerticalEngineLogger.error( message, sqle );
            doc = XMLTool.createDocument( "contenttitles" );
        }
        finally
        {
            close( resultSet );
            close( preparedStmt );
        }

        return doc;
    }

    public int getCurrentVersionKey( int contentKey )
    {
        StringBuffer sql = XDG.generateSelectSQL( db.tContent, db.tContent.con_cov_lKey, false, db.tContent.con_lKey );
        return getCommonHandler().getInt( sql.toString(), contentKey );
    }

    public int getContentKeyByVersionKey( int versionKey )
    {
        StringBuffer sql = XDG.generateSelectSQL( db.tContentVersion, db.tContentVersion.cov_con_lKey, false, db.tContentVersion.cov_lKey );
        return getCommonHandler().getInt( sql.toString(), versionKey );
    }

    private Document getContentVersions( int contentKey )
    {
        ContentVersionView versionView = ContentVersionView.getInstance();
        StringBuffer sql = XDG.generateSelectSQL( versionView,
                                                  new Column[]{versionView.cov_lKey, versionView.cov_sTitle, versionView.cov_dteCreated,
                                                      versionView.cov_dteTimestamp, versionView.cov_lState, versionView.cov_bCurrent,
                                                      versionView.usr_sOwnerName}, false, new Column[]{versionView.con_lKey} );
        XDG.appendOrderBySQL( sql, versionView.cov_dteCreated, true );
        Object[][] data = getCommonHandler().getObjectArray( sql.toString(), new Object[]{contentKey} );

        Document doc = XMLTool.createDocument( "versions" );

        for ( int contentCounter = 0; contentCounter < data.length; contentCounter++ )
        {
            Element versionElem = XMLTool.createElement( doc, doc.getDocumentElement(), "version" );

            int versionKey = ( (Number) data[contentCounter][0] ).intValue();
            String title = (String) data[contentCounter][1];
            Timestamp created = (Timestamp) data[contentCounter][2];
            Timestamp timestamp = (Timestamp) data[contentCounter][3];
            int state = ( (Number) data[contentCounter][4] ).intValue();
            boolean current = ( (Number) data[contentCounter][5] ).intValue() == 1;
            String owner = (String) data[contentCounter][6];

            versionElem.setAttribute( "key", String.valueOf( versionKey ) );
            versionElem.setAttribute( "title", StringUtil.getXMLSafeString( title ) );
            versionElem.setAttribute( "created", created.toString() );
            versionElem.setAttribute( "timestamp", timestamp.toString() );
            versionElem.setAttribute( "state", String.valueOf( state ) );
            versionElem.setAttribute( "current", String.valueOf( current ) );
            versionElem.setAttribute( "owner", String.valueOf( owner ) );
        }

        return doc;
    }

    private void getContentBinaries( TIntObjectHashMap versionKeyContentMap )
    {
        if ( versionKeyContentMap.size() == 0 )
        {
            return;
        }

        Table cbd = db.tContentBinaryData;
        Table bd = db.tBinaryData;
        Column[] selectColumns =
            {db.tBinaryData.bda_lKey, db.tBinaryData.bda_sFileName, db.tBinaryData.bda_lFileSize, db.tContentBinaryData.cbd_sLabel,
                db.tContentBinaryData.cbd_cov_lKey};
        StringBuffer sql = XDG.generateFKJoinSQL( cbd, bd, selectColumns );
        XDG.appendWhereInSQL( sql, db.tContentBinaryData.cbd_cov_lKey, versionKeyContentMap.keys().length );

        Object[][] data = getCommonHandler().getObjectArray( sql.toString(), versionKeyContentMap.keys() );

        for ( int binaryCounter = 0; binaryCounter < data.length; binaryCounter++ )
        {
            int binaryKey = ( (Number) data[binaryCounter][0] ).intValue();
            String fileName = StringUtil.stripControlChars( (String) data[binaryCounter][1] );
            int fileSize = ( (Number) data[binaryCounter][2] ).intValue();
            String label = (String) data[binaryCounter][3];
            int versionKey = ( (Number) data[binaryCounter][4] ).intValue();

            Element contentElem = (Element) versionKeyContentMap.get( versionKey );
            Document doc = contentElem.getOwnerDocument();
            Element binariesElem = XMLTool.createElementIfNotPresent( doc, contentElem, "binaries" );
            Element binaryElem = XMLTool.createElement( doc, binariesElem, "binary" );

            binaryElem.setAttribute( "key", String.valueOf( binaryKey ) );
            binaryElem.setAttribute( "filename", fileName );
            binaryElem.setAttribute( "filesize", String.valueOf( fileSize ) );
            if ( label != null )
            {
                binaryElem.setAttribute( "label", label );
            }
        }
    }

    public Document getContentVersion( final User user, int versionKey )
    {
        ContentVersionView versionView = ContentVersionView.getInstance();
        StringBuffer sql = XDG.generateSelectSQL( versionView, null, false, versionView.cov_lKey );
        int contentKey = getContentKeyByVersionKey( versionKey );
        getSecurityHandler().appendContentSQL( user, sql );

        ElementProcessor sectionNamesProcessor = new ElementProcessor()
        {
            private final SectionHandler sectionHandler = getSectionHandler();

            public void process( Element elem )
            {
                int contentKey = Integer.parseInt( elem.getAttribute( "key" ) );
                sectionHandler.appendSectionNames( contentKey, elem );
            }
        };
        VersionKeyContentMapProcessor versionKeyContentMapProcessor = new VersionKeyContentMapProcessor( this );
        Document doc = getCommonHandler().getData( versionView, sql.toString(), new int[]{versionKey},
                                                   new ElementProcessor[]{versionKeyContentMapProcessor, sectionNamesProcessor} );

        // add binaries elements
        getContentBinaries( versionKeyContentMapProcessor.getVersionKeyContentMap() );

        Element contentsElem = doc.getDocumentElement();
        Element contentElem = XMLTool.getElement( contentsElem, "content" );

        if ( contentElem == null )
        {
            return doc;
        }

        // accessrights
        Document accessRightsDoc = getSecurityHandler().getAccessRights( user, AccessRight.CONTENT, contentKey, true );
        XMLTool.mergeDocuments( contentElem, accessRightsDoc, true );

        // versions
        Document versionsDoc = getContentVersions( contentKey );
        XMLTool.mergeDocuments( contentElem, versionsDoc, true );

        Element relatedContentKeysElem = XMLTool.createElement( doc, contentElem, "relatedcontentkeys" );
        Element relatedContentsElem = XMLTool.createElement( doc, contentsElem, "relatedcontents" );

        // children
        TIntArrayList childrenKeys = new TIntArrayList();
        TIntObjectHashMap versionKeyRCKElemMap = new TIntObjectHashMap();
        versionKeyRCKElemMap.put( versionKey, relatedContentKeysElem );
        getChildrenContentKeys( childrenKeys, versionKeyRCKElemMap, false );
        ContentView contentView = ContentView.getInstance();
        if ( childrenKeys.size() > 0 )
        {
            int[] children = childrenKeys.toArray();
            sql = XDG.generateSelectWhereInSQL( contentView, null, false, contentView.con_lKey, children.length );
            Document childrenDoc = getCommonHandler().getData( contentView, sql.toString(), children, null );
            XMLTool.mergeDocuments( relatedContentsElem, childrenDoc, false );
        }

        ElementProcessor[] elementProcessors = {new ContentTypeProcessor( this )};

        // parents
        TIntArrayList parentKeys = new TIntArrayList();
        TIntObjectHashMap contentKeyRCKElemMap = new TIntObjectHashMap();
        contentKeyRCKElemMap.put( contentKey, relatedContentKeysElem );
        getParentContentKeys( parentKeys, contentKeyRCKElemMap, false, false );
        if ( parentKeys.size() > 0 )
        {
            int[] parents = parentKeys.toArray();
            sql = XDG.generateSelectWhereInSQL( versionView, null, false, versionView.cov_lKey, parents.length );
            Document childrenDoc = getCommonHandler().getData( versionView, sql.toString(), parents, elementProcessors );
            if ( childrenDoc != null )
            { // NSSR somehow got a nullpointerexception on the next line, when undeleting a content (manually in the db)
                XMLTool.mergeDocuments( relatedContentsElem, childrenDoc, false );
            }
        }

        return doc;
    }

    public Document getContentXMLField( int versionKey )
    {
        return getCommonHandler().getDocument( db.tContentVersion, versionKey );
    }

    public int[] getContentTypesByHandlerClass( String className )
    {
        // Funker denne?
        StringBuffer sql = XDG.generateSelectSQL( db.tContentType, db.tContentType.cty_lKey, true, null );
        XDG.appendJoinSQL( sql, db.tContentType.cty_han_lKey );
        XDG.appendWhereSQL( sql, db.tContentHandler.han_sClass, className );
        return getCommonHandler().getIntArray( sql.toString(), (int[]) null );
    }

    public boolean isContentVersionApproved( int versionKey )
    {
        ContentVersionView versionView = ContentVersionView.getInstance();
        Column[] whereColumns = {versionView.cov_lKey, versionView.cov_lStatus};
        StringBuffer sql = XDG.generateSelectSQL( versionView, versionView.cov_lKey, whereColumns );
        CommonHandler commonHandler = getCommonHandler();
        return commonHandler.getInt( sql.toString(), new int[]{versionKey, 2} ) >= 0;
    }

    public Document getContentHomes( int contentKey )
    {
        StringBuffer sql = XDG.generateSelectSQL( db.tContentHome );
        XDG.generateWhereSQL( sql, db.tContentHome.cho_con_lKey );
        ElementProcessor pageTemplateProcessor = new ElementProcessor()
        {
            private final CommonHandler commonHandler = getCommonHandler();

            private final String sql =
                XDG.generateSelectSQL( db.tPageTemplate, db.tPageTemplate.pat_sName, false, db.tPageTemplate.pat_lKey ).toString();

            public void process( Element elem )
            {
                if ( elem.hasAttribute( "pagetemplatekey" ) )
                {
                    int pageTemplateKey = Integer.valueOf( elem.getAttribute( "pagetemplatekey" ) );
                    String pageTemplateName = commonHandler.getString( sql, pageTemplateKey );
                    elem.setAttribute( "pagetemplatename", pageTemplateName );
                }
            }
        };
        CommonHandler commonHandler = getCommonHandler();
        return commonHandler.getData( db.tContentHome, sql.toString(), contentKey, new ElementProcessor[]{pageTemplateProcessor} );
    }

    public int getContentStatus( int versionKey )
    {
        StringBuffer sql = XDG.generateSelectSQL( db.tContentVersion, db.tContentVersion.cov_lStatus, false, db.tContentVersion.cov_lKey );
        return getCommonHandler().getInt( sql.toString(), versionKey );
    }

    public int getContentTypeKeyByName( String name )
    {
        StringBuffer sql = XDG.generateSelectSQL( db.tContentType, db.tContentType.cty_lKey, false, null );
        XDG.appendWhereSQL( sql, db.tContentType.cty_sName, name );
        return getCommonHandler().getInt( sql.toString(), (Object[]) null );
    }

}
