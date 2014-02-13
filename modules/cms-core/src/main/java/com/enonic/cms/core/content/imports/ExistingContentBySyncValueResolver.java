/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.content.imports;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.enonic.cms.core.content.ContentKey;
import com.enonic.cms.core.content.category.CategoryEntity;
import com.enonic.cms.core.content.contenttype.ContentTypeConfig;
import com.enonic.cms.core.content.contenttype.ContentTypeEntity;
import com.enonic.cms.core.content.contenttype.CtyImportConfig;
import com.enonic.cms.core.content.contenttype.dataentryconfig.DataEntryConfig;
import com.enonic.cms.core.content.index.ContentIndexQuery;
import com.enonic.cms.core.content.index.config.IndexDefinition;
import com.enonic.cms.core.content.index.config.IndexDefinitionBuilder;
import com.enonic.cms.core.content.resultset.ContentResultSet;
import com.enonic.cms.core.search.query.ContentIndexService;
import com.enonic.cms.core.search.query.IndexValueQuery;
import com.enonic.cms.core.search.query.IndexValueResult;
import com.enonic.cms.core.search.query.IndexValueResultSet;

public class ExistingContentBySyncValueResolver
{
    private ContentIndexService contentIndexService;

    public ExistingContentBySyncValueResolver( final ContentIndexService contentIndexService )
    {
        this.contentIndexService = contentIndexService;
    }

    public Map<String, ContentKey> resolve( final CategoryEntity category, final CtyImportConfig importConfig )
    {
        if ( importConfig.isSyncMappedToContentKey() )
        {
            return resolveContentKeysBySyncValueFromAllContentInCategory( category );
        }
        else
        {
            return resolveContentKeysBySyncValueFromIndexedField( category, importConfig );
        }
    }

    private Map<String, ContentKey> resolveContentKeysBySyncValueFromAllContentInCategory( final CategoryEntity category )
    {
        ContentIndexQuery contentQuery = new ContentIndexQuery( "categorykey = " + category.getKey().toString() );
        contentQuery.setReturnAllHits( true );

        ContentResultSet contentResultSet = contentIndexService.query( contentQuery );
        List<ContentKey> contentKeysInCategory = contentResultSet.getKeys();

        final Map<String, ContentKey> contentKeysBySyncValue = new HashMap<String, ContentKey>( contentKeysInCategory.size() );

        for ( ContentKey contentKey : contentKeysInCategory )
        {
            contentKeysBySyncValue.put( contentKey.toString(), contentKey );
        }
        return contentKeysBySyncValue;
    }

    private Map<String, ContentKey> resolveContentKeysBySyncValueFromIndexedField( final CategoryEntity category,
                                                                                   final CtyImportConfig importConfig )
    {
        final String syncXpath = getSyncXpath( category, importConfig );
        final IndexValueQuery query = new IndexValueQuery( syncXpath );
        query.setReturnAllHits( true );
        query.setCategoryFilter( Arrays.asList( category.getKey() ) );

        final IndexValueResultSet resSet = contentIndexService.query( query );

        final Map<String, ContentKey> contentKeysBySyncValue = new HashMap<String, ContentKey>();

        for ( int i = 0; i < resSet.getCount(); i++ )
        {
            final IndexValueResult res = resSet.getIndexValue( i );
            final ContentKey contentKey = res.getContentKey();
            final String syncValue = res.getValue();

            Object previousValue = contentKeysBySyncValue.put( syncValue, contentKey );

            if ( previousValue != null )
            {
                throw new ImportException(
                    "Could not uniquely identify content in category, category key: " + category.getKey() + ", category path: \"" +
                        category.getPathAsString() + "\" using xpath: \"" + syncXpath + "\" from sync field: \"" + importConfig.getSync() +
                        "\". Duplicate sync value: \"" + syncValue + "\"" );
            }
        }

        return contentKeysBySyncValue;
    }

    private String getSyncXpath( CategoryEntity category, CtyImportConfig importConfig )
    {
        final ContentTypeConfig config = category.getContentType().getContentTypeConfig();
        final DataEntryConfig dataEntryConfig = config.getInputConfig( importConfig.getSync() );
        final String syncXpath = dataEntryConfig.getXpath();

        if ( !isXpathIndexed( category.getContentType(), syncXpath ) )
        {
            throw new ImportException(
                "Could not find index for sync field: \"" + importConfig.getSync() + "\" with xpath: \"" + syncXpath +
                    "\" in category, category key: " + category.getKey() + ", category name: \"" + category.getName() +
                    "\". Sync field must be indexed." );
        }

        return syncXpath;
    }

    private boolean isXpathIndexed( ContentTypeEntity contentType, String syncXpath )
    {
        final List<IndexDefinition> indexes = new IndexDefinitionBuilder().buildList( contentType );
        for ( IndexDefinition index : indexes )
        {
            if ( index.getXPath().equals( syncXpath ) )
            {
                return true;
            }
        }
        return false;
    }
}
