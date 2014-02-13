/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.vertical.engine.handlers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.enonic.esl.sql.model.Column;
import com.enonic.esl.xml.XMLTool;
import com.enonic.vertical.engine.SectionCriteria;
import com.enonic.vertical.engine.VerticalCreateException;
import com.enonic.vertical.engine.VerticalEngineLogger;
import com.enonic.vertical.engine.VerticalRemoveException;
import com.enonic.vertical.engine.VerticalSecurityException;
import com.enonic.vertical.engine.XDG;
import com.enonic.vertical.engine.dbmodel.ContentMinimalView;
import com.enonic.vertical.engine.dbmodel.ContentView;
import com.enonic.vertical.engine.dbmodel.SectionView;
import com.enonic.vertical.engine.processors.AttributeElementProcessor;
import com.enonic.vertical.engine.processors.ElementProcessor;

import com.enonic.cms.framework.util.TIntArrayList;
import com.enonic.cms.framework.xml.XMLDocument;
import com.enonic.cms.framework.xml.XMLDocumentFactory;

import com.enonic.cms.core.security.user.User;
import com.enonic.cms.core.structure.SiteKey;
import com.enonic.cms.core.structure.menuitem.MenuItemEntity;
import com.enonic.cms.core.structure.menuitem.MenuItemKey;

@Component
public class SectionHandler
    extends BaseHandler
{

    private class FilteredAttributeElementProcessor
        extends AttributeElementProcessor
    {

        private final int[] sectionKeys;

        private FilteredAttributeElementProcessor( int[] sectionKeys )
        {
            super( "filtered", "true" );
            this.sectionKeys = sectionKeys;
            Arrays.sort( this.sectionKeys );
        }

        public void process( Element elem )
        {
            // sections in the array are processes by the AttributeElementProcessor
            if ( sectionKeys.length > 0 )
            {
                int sectionKey = Integer.parseInt( elem.getAttribute( "key" ) );
                if ( Arrays.binarySearch( sectionKeys, sectionKey ) >= 0 )
                {
                    super.process( elem );
                }
            }
        }

    }

    private class SectionProcessor
        implements ElementProcessor
    {

        public void process( Element elem )
        {
            int key = Integer.parseInt( elem.getAttribute( "key" ) );

            Document contentTypesDoc = getContentTypesDocumentForSection( key );
            XMLTool.mergeDocuments( elem, contentTypesDoc, true );
        }
    }

    private Document getContentTypesDocumentForSection( int menuItemSectionKey )
    {
        int[] contentTypeKeys = getContentTypesForSection( menuItemSectionKey );
        return getContentHandler().getContentTypesDocument( contentTypeKeys );
    }

    private class CollectionProcessor
        implements ElementProcessor
    {

        private final Map<String, Element> elemMap;

        private List<Element> elemList;

        private Element lastElem;

        public CollectionProcessor( Map<String, Element> elemMap )
        {
            this.elemMap = elemMap;
            this.elemList = null;
        }

        public void process( Element elem )
        {
            if ( elemMap != null )
            {
                elemMap.put( elem.getTagName() + "_" + elem.getAttribute( "key" ), elem );
            }
            if ( elemList != null )
            {
                elemList.add( elem );
            }
            lastElem = elem;
        }
    }

    private class ChildCountProcessor
        implements ElementProcessor
    {

        public void process( Element elem )
        {
            SectionView sectionView = SectionView.getInstance();
            int key = Integer.parseInt( elem.getAttribute( "key" ) );
            StringBuffer sql = XDG.generateSelectSQL( sectionView, sectionView.mei_lKey.getCountColumn(), false, sectionView.mei_lParent );

            int childCount = getCommonHandler().getInt( sql.toString(), key );
            elem.setAttribute( "childcount", Integer.toString( childCount ) );
        }
    }

    public void updateSection( int sectionKey, boolean ordered, int[] contentTypes )
        throws VerticalCreateException, VerticalSecurityException
    {

        int orderedInt = ordered ? 1 : 0;

        StringBuffer sql = XDG.generateUpdateSQL( db.tMenuItem, db.tMenuItem.mei_bOrderedSection, db.tMenuItem.mei_lKey );
        getCommonHandler().executeSQL( sql.toString(), new int[]{orderedInt, sectionKey} );

        setContentTypesForSection( sectionKey, contentTypes );
    }

    public void createSection( int menuItemKey, boolean ordered, int[] contentTypes )
        throws VerticalCreateException
    {
        StringBuffer sql = XDG.generateUpdateSQL( db.tMenuItem, new Column[]{db.tMenuItem.mei_bSection, db.tMenuItem.mei_bOrderedSection},
                                                  new Column[]{db.tMenuItem.mei_lKey} );
        getCommonHandler().executeSQL( sql.toString(), new int[]{1, ordered ? 1 : 0, menuItemKey} );
        setContentTypesForSection( menuItemKey, contentTypes );
    }

    public boolean removeSection( int sectionKey )
        throws VerticalRemoveException, VerticalSecurityException
    {
        StringBuffer sql = XDG.generateUpdateSQL( db.tMenuItem, db.tMenuItem.mei_bSection, db.tMenuItem.mei_lKey );
        return ( getCommonHandler().executeSQL( sql.toString(), new int[]{0, sectionKey} ) > 0 );
    }

    public void removeSection( int sectionKey, boolean recursive )
        throws VerticalRemoveException, VerticalSecurityException
    {
        if ( recursive )
        {
            removeSectionRecursive( sectionKey );
        }
        else
        {
            removeSection( sectionKey );
        }
    }

    private boolean removeSectionRecursive( int sectionKey )
        throws VerticalRemoveException, VerticalSecurityException
    {
        boolean success = true;
        int[] keys = getSectionKeysBySuperSection( sectionKey );
        for ( int key : keys )
        {
            success = success && removeSectionRecursive( key );
        }

        return success && removeSection( sectionKey );
    }

    private int[] getContentTypesForSection( int menuItemSectionKey )
    {
        StringBuffer sql =
            XDG.generateSelectSQL( db.tSecConTypeFilter2, db.tSecConTypeFilter2.sctf_cty_lKey, false, db.tSecConTypeFilter2.sctf_mei_lKey );
        return getCommonHandler().getIntArray( sql.toString(), new Object[]{menuItemSectionKey} );
    }

    public void setContentTypesForSection( int sectionKey, int[] contentTypeKeys )
        throws VerticalCreateException
    {

        Connection con = null;
        PreparedStatement preparedStmt = null;

        try
        {
            removeContentTypesForSection( sectionKey );
            con = getConnection();
            StringBuffer sql = XDG.generateInsertSQL( db.tSecConTypeFilter2 );
            preparedStmt = con.prepareStatement( sql.toString() );
            preparedStmt.setInt( 3, sectionKey );

            for ( int contentTypeKey : contentTypeKeys )
            {
                int sectionFilterKey = getCommonHandler().getNextKey( db.tSecConTypeFilter2.getName() );
                preparedStmt.setInt( 1, sectionFilterKey );
                preparedStmt.setInt( 2, contentTypeKey );
                int result = preparedStmt.executeUpdate();
                if ( result == 0 )
                {
                    String message = "Failed to create section contenttype filter.";
                    VerticalEngineLogger.errorCreate( message, null );
                }
            }
        }
        catch ( SQLException sqle )
        {
            String message = "Failed to create section contenttype filter: %t";
            VerticalEngineLogger.errorCreate( message, sqle );
        }
        finally
        {
            close( preparedStmt );
        }
    }

    private void removeContentTypesForSection( int sectionKey )
        throws VerticalRemoveException
    {

        Connection con = null;
        PreparedStatement preparedStmt = null;

        try
        {
            con = getConnection();
            StringBuffer sql = XDG.generateRemoveSQL( db.tSecConTypeFilter2, db.tSecConTypeFilter2.sctf_mei_lKey );
            preparedStmt = con.prepareStatement( sql.toString() );
            preparedStmt.setInt( 1, sectionKey );
            preparedStmt.executeUpdate();
        }
        catch ( SQLException sqle )
        {
            String message = "Failed to remove section contenttype filter: %t";
            VerticalEngineLogger.errorRemove( message, sqle );
        }
        finally
        {
            close( preparedStmt );
        }
    }

    public int getMenuKeyBySection( int sectionKey )
    {
        StringBuffer sql = XDG.generateSelectSQL( db.tMenuItem, db.tMenuItem.mei_men_lKey,
                                                  new Column[]{db.tMenuItem.mei_lKey, db.tMenuItem.mei_bSection} );
        return getCommonHandler().getInt( sql.toString(), new Object[]{sectionKey, 1} );
    }

    public MenuItemKey getMenuItemKeyBySection( int sectionKey )
    {
        StringBuffer sql =
            XDG.generateSelectSQL( db.tMenuItem, db.tMenuItem.mei_lKey, new Column[]{db.tMenuItem.mei_lKey, db.tMenuItem.mei_bSection} );
        return new MenuItemKey( getCommonHandler().getInt( sql.toString(), new Object[]{sectionKey, 1} ) );
    }

    public MenuItemKey getSectionKeyByMenuItem( MenuItemKey menuItemKey )
    {
        final MenuItemEntity entity = this.menuItemDao.findByKey( menuItemKey );
        if ( entity == null )
        {
            return null;
        }

        if ( entity.isSection() )
        {
            return menuItemKey;
        }
        else
        {
            return null;
        }
    }

    public Document getSectionByMenuItem( int menuItemKey )
    {
        MenuItemEntity entity = menuItemDao.findByKey( menuItemKey );
        Document doc = XMLTool.createDocument( "section" );

        if ( entity != null && entity.isSection() )
        {
            Element elem = doc.getDocumentElement();
            elem.setAttribute( "key", String.valueOf( entity.getKey().toInt() ) );
            elem.setAttribute( "menukey", String.valueOf( entity.getSite().getKey() ) );
            elem.setAttribute( "menuitemkey", String.valueOf( entity.getKey().toInt() ) );
            elem.setAttribute( "ordered", String.valueOf( entity.isOrderedSection() ) );
        }

        Document contentTypesDoc = getContentTypesDocumentForSection( menuItemKey );
        XMLTool.mergeDocuments( doc.getDocumentElement(), contentTypesDoc, true );

        return doc;
    }

    public Document getSections( User user, SectionCriteria criteria )
    {

        Connection con = null;
        PreparedStatement preparedStmt = null;
        ResultSet resultSet = null;
        Document doc = null;
        SectionView sectionView = SectionView.getInstance();

        try
        {
            con = getConnection();
            StringBuffer sql;
            SiteKey[] siteKeys = criteria.getSiteKeys();
            MenuItemKey[] menuItemKeys = criteria.getMenuItemKeys();
            int sectionKey = criteria.getSectionKey();
            int[] sectionKeys = null;
            int contentKey = criteria.getContentKey();
            int contentKeyExcludeFilter = criteria.getContentKeyExcludeFilter();
            int contentTypeKeyFilter = criteria.getContentTypeKeyFilter();
            boolean treeStructure = criteria.isTreeStructure();
            boolean markContentFilteredSections = criteria.isMarkContentFilteredSections();
            boolean includeAll = criteria.isIncludeAll();

            // Generate SQL
            if ( siteKeys != null )
            {
                if ( siteKeys.length == 0 )
                {
                    return XMLTool.createDocument( "sections" );
                }
                sql = XDG.generateSelectWhereInSQL( sectionView, (Column[]) null, false, sectionView.mei_men_lKey, siteKeys.length );
            }
            else if ( menuItemKeys != null )
            {
                if ( menuItemKeys.length == 0 )
                {
                    return XMLTool.createDocument( "sections" );
                }
                sql = XDG.generateSelectWhereInSQL( sectionView, (Column[]) null, false, sectionView.mei_lKey, menuItemKeys.length );
            }
            else if ( sectionKey != -1 )
            {
                sql = XDG.generateSelectSQL( sectionView, sectionView.mei_lKey );
            }
            else if ( contentKey != -1 )
            {
                sql = XDG.generateSelectSQL( sectionView );
                sql.append( " WHERE mei_lKey IN (SELECT sco_mei_lKey FROM tSectionContent2 WHERE sco_con_lKey = ?)" );
            }
            else
            {
                sql = XDG.generateSelectSQL( sectionView );
            }

            if ( contentKeyExcludeFilter > -1 && !markContentFilteredSections )
            {
                if ( sql.toString().toLowerCase().indexOf( "where" ) < 0 )
                {
                    sql.append( " WHERE" );
                }
                else
                {
                    sql.append( " AND" );
                }
                sql.append( " mei_lKey NOT IN (SELECT sco_mei_lKey FROM tSectionContent2 WHERE sco_con_lKey = ?)" );
            }

            if ( contentTypeKeyFilter > -1 )
            {
                if ( sql.toString().toLowerCase().indexOf( "where" ) < 0 )
                {
                    sql.append( " WHERE" );
                }
                else
                {
                    sql.append( " AND" );
                }
                sql.append( " (" );
                sql.append( " mei_lKey IN (SELECT sctf_mei_lKey FROM tSecConTypeFilter2 WHERE sctf_cty_lkey = ?)" );
                if ( criteria.isIncludeSectionsWithoutContentTypeEvenWhenFilterIsSet() )
                {
                    sql.append( " OR NOT EXISTS (SELECT sctf_mei_lKey FROM tSecConTypeFilter2 WHERE sctf_mei_lkey = mei_lKey)" );
                }
                sql.append( " )" );
            }

            SecurityHandler securityHandler = getSecurityHandler();
            if ( !includeAll )
            {
                securityHandler.appendSectionSQL( user, sql );
            }

            sql.append( " ORDER BY mei_sName" );

            preparedStmt = con.prepareStatement( sql.toString() );

            int index = 1;
            // Set parameters
            if ( siteKeys != null )
            {
                for ( SiteKey siteKey : siteKeys )
                {
                    preparedStmt.setInt( index++, siteKey.toInt() );
                }
            }
            else if ( menuItemKeys != null )
            {
                for ( MenuItemKey menuItemKey : menuItemKeys )
                {
                    preparedStmt.setInt( index++, menuItemKey.toInt() );
                }
            }
            else if ( sectionKey != -1 )
            {
                preparedStmt.setInt( index++, sectionKey );
            }
            else if ( contentKey != -1 )
            {
                preparedStmt.setInt( index++, contentKey );
            }

            if ( contentKeyExcludeFilter > -1 && !markContentFilteredSections )
            {
                preparedStmt.setInt( index++, contentKeyExcludeFilter );
            }
            if ( contentTypeKeyFilter > -1 )
            {
                preparedStmt.setInt( index, contentTypeKeyFilter );
            }

            resultSet = preparedStmt.executeQuery();

            ElementProcessor childCountProcessor = null;
            if ( criteria.getIncludeChildCount() )
            {
                childCountProcessor = new ChildCountProcessor();
            }

            Map<String, Element> elemMap = new HashMap<String, Element>();
            CollectionProcessor collectionProcessor = new CollectionProcessor( elemMap );
            ElementProcessor[] processors;
            ElementProcessor aep = null;
            if ( contentKeyExcludeFilter >= 0 )
            {
                if ( markContentFilteredSections )
                {
                    int[] keys = getSectionKeysByContent( contentKeyExcludeFilter, -1 );
                    aep = new FilteredAttributeElementProcessor( keys );
                }
            }
            if ( contentKey >= 0 && markContentFilteredSections )
            {
                int[] keys = getSectionKeysByContent( contentKey, -1 );
                aep = new FilteredAttributeElementProcessor( keys );
            }

            SectionProcessor sectionProcessorThatIncludesContentTypes = null;

            if ( criteria.isIncludeSectionContentTypesInfo() )
            {
                sectionProcessorThatIncludesContentTypes = new SectionProcessor();
            }

            processors = new ElementProcessor[]{aep,
                // mark content in original query
                new AttributeElementProcessor( "marked", "true" ), sectionProcessorThatIncludesContentTypes, collectionProcessor,
                childCountProcessor};

            doc = XDG.resultSetToXML( sectionView, resultSet, null, processors, null, -1 );
            close( resultSet );
            close( preparedStmt );

            // If treeStructure is true, all parents of the retrieved section
            // are retrieved:
            if ( treeStructure )
            {
                sql = XDG.generateSelectSQL( sectionView, sectionView.mei_lKey );
                preparedStmt = con.prepareStatement( sql.toString() );

                Element sectionsRootElement = doc.getDocumentElement();
                List<Element> sectionsElementList = XMLTool.getElementsAsList( sectionsRootElement );
                collectionProcessor.elemList = sectionsElementList;

                // remove marking of sections ("filtered"(0) and "marked"(1) attributes)
                processors[0] = null;
                processors[1] = null;

                for ( int i = 0; i < sectionsElementList.size(); i++ )
                {
                    Element sectionElement = sectionsElementList.get( i );
                    String superKey = sectionElement.getAttribute( "supersectionkey" );
                    if ( superKey != null && superKey.length() > 0 )
                    {
                        sectionsRootElement.removeChild( sectionElement );

                        // find parent element and append current element
                        Element parentElem = elemMap.get( "section_" + superKey );
                        if ( parentElem == null )
                        {
                            int key = Integer.parseInt( superKey );
                            preparedStmt.setInt( 1, key );
                            resultSet = preparedStmt.executeQuery();
                            XDG.resultSetToXML( sectionView, resultSet, doc.getDocumentElement(), processors, "name", -1 );
                            close( resultSet );
                            parentElem = collectionProcessor.lastElem;
                        }
                        Element elem = XMLTool.createElementIfNotPresent( doc, parentElem, "sections" );
                        elem.appendChild( sectionElement );
                    }
                }
            }

            if ( criteria.appendAccessRights() )
            {
                securityHandler.appendAccessRights( user, doc );
            }
        }
        catch ( SQLException sqle )
        {
            String message = "Failed to get sections: %t";
            VerticalEngineLogger.error( message, sqle );
            doc = XMLTool.createDocument( "sections" );
        }
        finally
        {
            close( resultSet );
            close( preparedStmt );
        }
        return doc;
    }

    private int[] getSectionKeysByContent( int contentKey, int menuKey )
    {
        StringBuffer sql = XDG.generateSelectSQL( db.tSectionContent2, db.tSectionContent2.sco_mei_lKey, false, (Column) null );

        if ( menuKey != -1 )
        {
            XDG.appendJoinSQL( sql, db.tSectionContent2.sco_mei_lKey );
            XDG.appendJoinSQL( sql, db.tMenuItem.mei_men_lKey );
            XDG.appendWhereSQL( sql, db.tMenu.men_lKey, XDG.OPERATOR_EQUAL, menuKey );
        }

        XDG.appendWhereSQL( sql, db.tSectionContent2.sco_con_lKey, XDG.OPERATOR_EQUAL, contentKey );
        return getCommonHandler().getIntArray( sql.toString() );
    }

    private int[] getSectionKeysBySuperSection( int superSectionKey )
    {

        return getSectionKeysBySuperSections( new int[]{superSectionKey}, false );
    }

    private int[] getSectionKeysBySuperSections( int[] superSectionKeys, boolean recursive )
    {
        SectionView sectionView = SectionView.getInstance();
        int[] sectionKeys;
        StringBuffer sql = XDG.generateSelectSQL( sectionView, sectionView.mei_lKey, (Column[]) null );
        sql.append( " WHERE mei_lParent IN (" );
        for ( int i = 0; i < superSectionKeys.length; i++ )
        {
            if ( i > 0 )
            {
                sql.append( "," );
            }
            sql.append( superSectionKeys[i] );
        }
        sql.append( ")" );
        sectionKeys = getCommonHandler().getIntArray( sql.toString(), (int[]) null );
        if ( recursive && sectionKeys.length > 0 )
        {
            TIntArrayList keys = new TIntArrayList();
            keys.add( sectionKeys );
            keys.add( getSectionKeysBySuperSections( sectionKeys, recursive ) );
        }
        return sectionKeys;
    }

    public void appendSectionNames( int contentKey, Element contentElem )
    {

        StringBuffer sql = new StringBuffer();
        sql.append( "SELECT " ).append( "sec." ).append( db.tMenuItem.mei_lKey ).append( ", " ).append( "sec." ).append(
            db.tMenuItem.mei_men_lKey ).append( ", " ).append( "sec." ).append( db.tMenuItem.mei_lKey ).append( ", " ).append(
            "sec." ).append( db.tMenuItem.mei_sName ).append( ", " ).append( "sec." ).append( db.tMenuItem.mei_lParent ).append(
            ", " ).append( db.tSectionContent2.sco_bApproved ).append( ", " ).append( db.tContentHome.cho_mei_lKey ).append( ", " ).append(
            "secparent." ).append( db.tMenuItem.mei_lKey );
        sql.append( " FROM " ).append( db.tMenuItem ).append( " sec" );
        sql.append( " JOIN " ).append( db.tSectionContent2 ).append( " ON " ).append( "sec." ).append( db.tMenuItem.mei_lKey ).append(
            " = " ).append( db.tSectionContent2.sco_mei_lKey );
        sql.append( " LEFT JOIN " ).append( db.tContentHome ).append( " ON " ).append( db.tContentHome.cho_con_lKey ).append(
            " = " ).append( db.tSectionContent2.sco_con_lKey ).append( " AND " ).append( db.tContentHome.cho_men_lKey ).append(
            " = " ).append( db.tMenuItem.mei_men_lKey );
        sql.append( " LEFT JOIN " ).append( db.tMenuItem ).append( " secparent" ).append( " ON " ).append( "secparent." ).append(
            db.tMenuItem.mei_lKey ).append( " = sec." ).append( db.tMenuItem.mei_lParent );
        XDG.appendWhereSQL( sql, db.tSectionContent2.sco_con_lKey );
        sql.append( "?" );

        Connection con = null;
        PreparedStatement preparedStmt = null;
        ResultSet resultSet = null;
        Document doc = contentElem.getOwnerDocument();
        Element root = XMLTool.createElement( doc, contentElem, "sectionnames" );
        try
        {
            con = getConnection();
            preparedStmt = con.prepareStatement( sql.toString() );
            preparedStmt.setInt( 1, contentKey );
            resultSet = preparedStmt.executeQuery();
            while ( resultSet.next() )
            {
                int sectionKey = resultSet.getInt( 1 );
                int menuKey = resultSet.getInt( 2 );
                int menuItemKey = resultSet.getInt( 3 );
                String name = resultSet.getString( 4 );
//                int parentKey = resultSet.getInt(5);
                boolean approved = resultSet.getBoolean( 6 );
                Integer homeMenuItemKey = resultSet.getInt( 7 );
                if ( resultSet.wasNull() )
                {
                    homeMenuItemKey = null;
                }
                Integer parentSectionKey = resultSet.getInt( 8 );
                if ( resultSet.wasNull() )
                {
                    parentSectionKey = null;
                }
                Element sectionName = XMLTool.createElement( doc, root, "sectionname", name );
                sectionName.setAttribute( "key", Integer.toString( sectionKey ) );
                sectionName.setAttribute( "menukey", Integer.toString( menuKey ) );
                sectionName.setAttribute( "menuitemkey", Integer.toString( menuItemKey ) );
                boolean home = homeMenuItemKey != null && homeMenuItemKey == menuItemKey;
                if ( home )
                {
                    sectionName.setAttribute( "home", "true" );
                }
                if ( parentSectionKey != null )
                {
                    sectionName.setAttribute( "supersectionkey", Integer.toString( parentSectionKey ) );
                }
                sectionName.setAttribute( "approved", String.valueOf( approved ) );
            }
        }
        catch ( SQLException sqle )
        {
            String message = "Failed to append section names to content: %t";
            VerticalEngineLogger.error( message, sqle );
        }
        finally
        {
            close( resultSet );
            close( preparedStmt );
        }
    }

    public long getSectionContentTimestamp( int sectionKey )
    {
        long timestamp = 0;

        Connection con = null;
        PreparedStatement preparedStmt = null;
        ResultSet resultSet = null;

        try
        {
            con = getConnection();
            StringBuffer sql =
                XDG.generateSelectSQL( db.tSectionContent2, db.tSectionContent2.sco_dteTimestamp, false, db.tSectionContent2.sco_mei_lKey );
            sql.append( " ORDER BY sco_dteTimestamp DESC" );
            preparedStmt = con.prepareStatement( sql.toString() );
            preparedStmt.setInt( 1, sectionKey );

            resultSet = preparedStmt.executeQuery();
            if ( resultSet.next() )
            {
                Timestamp time = resultSet.getTimestamp( "sco_dteTimestamp" );
                timestamp = time.getTime();
            }
        }
        catch ( SQLException sqle )
        {
            String message = "Failed to get section content timestamp: %t";
            VerticalEngineLogger.error( message, sqle );
        }
        finally
        {
            close( resultSet );
            close( preparedStmt );
        }

        return timestamp;
    }

    public boolean isSectionOrdered( int sectionKey )
    {
        MenuItemEntity section = menuItemDao.findByKey( new MenuItemKey( sectionKey ) );
        return section.isOrderedSection();
    }

    public XMLDocument getContentTitlesBySection( int sectionKey, String orderBy, int fromIndex, int count, boolean includeTotalCount,
                                                  boolean approvedOnly )
    {
        ContentView contentView = ContentView.getInstance();
        StringBuffer sql = XDG.generateSelectSQL( this.db.tSectionContent2, new Column[]{this.db.tSectionContent2.sco_con_lKey,
            this.db.tSectionContent2.sco_bApproved}, false, null );
        sql.append( " LEFT JOIN " ).append( ContentMinimalView.getInstance().getReplacementSql() ).append( " ON " );
        sql.append( this.db.tSectionContent2.sco_con_lKey.getName() ).append( " = " );
        sql.append( contentView.con_lKey.getName() );
        sql = XDG.generateWhereSQL( sql, new Column[]{this.db.tSectionContent2.sco_mei_lKey} );

        if ( approvedOnly )
        {
            sql.append( " AND " );
            sql.append( this.db.tSectionContent2.sco_bApproved.getName() );
            sql.append( " = 1" );
        }

        String orderDirection = " ASC ";
        if ( orderBy == null )
        {
            orderBy = contentView.cov_dteTimestamp.getName();
            orderDirection = " DESC ";
        }

        sql.append( " ORDER BY " );
        if ( !approvedOnly )
        {
            sql.append( this.db.tSectionContent2.sco_bApproved.getName() );
            sql.append( ", " );
        }

        if ( isSectionOrdered( sectionKey ) )
        {
            sql.append( this.db.tSectionContent2.sco_lOrder.getName() );
            sql.append( " ASC, " );
        }

        sql.append( orderBy );
        sql.append( orderDirection );

        Connection con = null;
        PreparedStatement prepStmt = null;
        ResultSet resultSet = null;
        TIntArrayList contentKeys;
        TIntArrayList totalContentKeys = new TIntArrayList();

        int totalCount = 0;
        if ( count > 20 )
        {
            contentKeys = new TIntArrayList();
        }
        else
        {
            contentKeys = new TIntArrayList();
        }

        HashMap<String, String> contentApprovedMap = new HashMap<String, String>();

        try
        {
            con = getConnection();
            prepStmt = con.prepareStatement( sql.toString() );
            prepStmt.setInt( 1, sectionKey );
            resultSet = prepStmt.executeQuery();

            boolean moreResults = resultSet.next();
            int i = fromIndex;

            // Skip rows:
            try
            {
                if ( fromIndex > 0 )
                {
                    resultSet.relative( fromIndex );
                }
            }
            catch ( SQLException e )
            {
                // ResultSet is not scrollable
                i = 0;
            }

            totalCount = fromIndex;
            for (; ( ( includeTotalCount || i < fromIndex + count ) && moreResults ); i++ )
            {
                //always save total content keys list
                int totalContentKey = resultSet.getInt( 1 );
                totalContentKeys.add( totalContentKey );
                //

                if ( i < fromIndex )
                {
                    moreResults = resultSet.next();
                    continue;
                }

                if ( i < fromIndex + count )
                {
                    int contentKey = resultSet.getInt( 1 );
                    boolean approved = resultSet.getBoolean( 2 );
                    contentKeys.add( contentKey );
                    contentApprovedMap.put( Integer.toString( contentKey ), Boolean.toString( approved ) );
                }

                totalCount++;
                moreResults = resultSet.next();
            }
        }
        catch ( SQLException sqle )
        {
            String message = "Failed to get content keys for content in sections: %t";
            VerticalEngineLogger.error( message, sqle );
        }
        finally
        {
            close( resultSet );
            close( prepStmt );
        }

        if ( contentKeys.size() == 0 )
        {
            org.jdom.Element contentsEl = new org.jdom.Element( "contenttitles" );
            if ( includeTotalCount )
            {
                contentsEl.setAttribute( "totalcount", "0" );
            }
            return XMLDocumentFactory.create( new org.jdom.Document( contentsEl ) );
        }

        ContentHandler contentHandler = getContentHandler();
        MenuItemEntity section = menuItemDao.findByKey( sectionKey );
        XMLDocument doc = contentHandler.getContentTitles( contentKeys.toArray(), totalContentKeys.toArray(), true, section );

        if ( includeTotalCount )
        {
            org.jdom.Document jdomDoc = doc.getAsJDOMDocument();
            jdomDoc.getRootElement().setAttribute( "totalcount", Integer.toString( totalCount ) );
        }

        return doc;
    }

    public void copySection( int sectionKey )
        throws VerticalSecurityException
    {
        copySections( new int[]{sectionKey} );
    }

    private void copySections( int[] sectionKeys )
        throws VerticalSecurityException
    {
        SectionView sectionView = SectionView.getInstance();
        String sql = XDG.generateSelectSQL( sectionView, new Column[]{sectionView.mei_men_lKey, sectionView.mei_lParent}, false,
                                            new Column[]{sectionView.mei_lKey} ).toString();

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet result = null;

        try
        {
            conn = getConnection();
            stmt = conn.prepareStatement( sql );
            for ( int sectionKey : sectionKeys )
            {
                stmt.setInt( 1, sectionKey );

                result = stmt.executeQuery();
                if ( result.next() )
                {
                    MenuItemKey superKey = new MenuItemKey( result.getInt( 2 ) );
                    if ( result.wasNull() )
                    {
                        superKey = null;
                    }
                    copySectionAtSameLevel( sectionKey, superKey );
                }
            }
        }
        catch ( SQLException e )
        {
            VerticalEngineLogger.error( "Failed to find section: %t", e );
        }
        finally
        {
            close( result );
            close( stmt );
        }
    }

    public int[] getSectionKeysByMenu( int menuKey )
    {
        SectionView view = SectionView.getInstance();
        Column selectColumn = view.mei_lKey;
        Column[] whereColumns = {view.mei_men_lKey, view.mei_lParent.getNullColumn()};
        StringBuffer sql = XDG.generateSelectSQL( view, selectColumn, whereColumns );
        CommonHandler commonHandler = getCommonHandler();
        return commonHandler.getIntArray( sql.toString(), menuKey );
    }

    private void copySectionAtSameLevel( int sectionKey, MenuItemKey superKey )
        throws VerticalSecurityException
    {
        try
        {
            copySection( null, sectionKey, superKey );
        }
        catch ( SQLException e )
        {
            VerticalEngineLogger.errorCopy( "Failed to copy section: %t", e );
        }
    }

    private void copySection( CopyContext copyContext, int sourceKey, MenuItemKey superKey )
        throws SQLException
    {
        if ( copyContext != null )
        {
            copyContext.putSectionKey( sourceKey, superKey.toInt() );
        }

        // Copy content
        copySectionData( sourceKey, superKey );
        copySectionContent( sourceKey, superKey );
        copySecConTypeFilter( sourceKey, superKey );

        // Copy children
        int[] subKeys = getSectionKeysBySuperSection( sourceKey );
        for ( int subKey : subKeys )
        {
            copySection( copyContext, subKey, superKey );
        }
    }

    private void copySectionData( int sourceKey, MenuItemKey superKey )
    {
        String sql1 = XDG.generateSelectSQL( db.tMenuItem, db.tMenuItem.mei_bOrderedSection,
                                             new Column[]{db.tMenuItem.mei_lKey, db.tMenuItem.mei_bSection} ).toString();
        int ordered = getCommonHandler().getInt( sql1, new int[]{sourceKey, 1} );
        if ( ordered < 0 )
        {
            ordered = 0;
        }
        String sql2 = XDG.generateUpdateSQL( db.tMenuItem, new Column[]{db.tMenuItem.mei_bSection, db.tMenuItem.mei_bOrderedSection},
                                             new Column[]{db.tMenuItem.mei_lKey} ).toString();
        getCommonHandler().executeSQL( sql2, new int[]{1, ordered, superKey.toInt()} );

    }

    private void copySectionContent( int sourceKey, MenuItemKey targetKey )
        throws SQLException
    {
        String sql1 = XDG.generateSelectSQL( this.db.tSectionContent2, this.db.tSectionContent2.sco_mei_lKey ).toString();
        String sql2 = XDG.generateInsertSQL( this.db.tSectionContent2 ).toString();

        Connection con = null;
        PreparedStatement stmt1 = null;
        PreparedStatement stmt2 = null;
        ResultSet result = null;
        try
        {
            con = getConnection();
            stmt1 = con.prepareStatement( sql1 );
            stmt2 = con.prepareStatement( sql2 );

            stmt1.setInt( 1, sourceKey );
            result = stmt1.executeQuery();

            while ( result.next() )
            {
                stmt2.clearParameters();

                int newSectionContentPK = getCommonHandler().getNextKey( db.tSectionContent2 );
                stmt2.setInt( 1, newSectionContentPK );
                stmt2.setInt( 2, result.getInt( 2 ) );
                stmt2.setInt( 3, targetKey.toInt() );
                stmt2.setInt( 4, result.getInt( 4 ) );
                stmt2.setInt( 5, result.getInt( 5 ) );
                // No.6 = Timestamp!

                stmt2.executeUpdate();
            }
        }
        finally
        {
            close( result );
            close( stmt2 );
            close( stmt1 );
        }
    }

    private void copySecConTypeFilter( int sourceKey, MenuItemKey targetKey )
        throws SQLException
    {
        String sql1 = XDG.generateSelectSQL( this.db.tSecConTypeFilter2, this.db.tSecConTypeFilter2.sctf_mei_lKey ).toString();
        String sql2 = XDG.generateInsertSQL( this.db.tSecConTypeFilter2 ).toString();

        Connection con = null;
        PreparedStatement stmt1 = null;
        PreparedStatement stmt2 = null;
        ResultSet result = null;
        try
        {
            con = getConnection();
            stmt1 = con.prepareStatement( sql1 );
            stmt2 = con.prepareStatement( sql2 );

            stmt1.setInt( 1, sourceKey );
            result = stmt1.executeQuery();

            while ( result.next() )
            {
                stmt2.clearParameters();

                int sectionFilterKey = getCommonHandler().getNextKey( db.tSecConTypeFilter2.getName() );
                stmt2.setInt( 1, sectionFilterKey );
                stmt2.setInt( 2, targetKey.toInt() );
                stmt2.setInt( 3, result.getInt( 2 ) );

                stmt2.executeUpdate();
            }
        }
        finally
        {
            close( result );
            close( stmt2 );
            close( stmt1 );
        }
    }

}
