/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.search.query;

import java.util.Collection;

import com.enonic.cms.core.content.ContentKey;
import com.enonic.cms.core.content.category.CategoryKey;
import com.enonic.cms.core.content.contenttype.ContentTypeKey;
import com.enonic.cms.core.content.index.ContentIndexQuery;
import com.enonic.cms.core.content.resultset.ContentResultSet;
import com.enonic.cms.core.search.ContentIndexedFields;
import com.enonic.cms.core.search.IndexType;

/**
 * This interface defines the content index service.
 */
public interface ContentIndexService
{

    /**
     * @param contentKey The key of the ContentEntity that should be deleted.
     * @return The number of entities that has been deleted.
     */
    public void remove( ContentKey contentKey );


    /**
     * Remove contents by category key.
     */
    public void removeByCategory( CategoryKey categoryKey );

    /**
     * Remove contents by content type key.
     */
    public void removeByContentType( ContentTypeKey contentTypeKey );

    /**
     * Index the content.
     *
     * @param doc            All the information that should be indexed.
     * @param deleteExisting If it is known for sure that the content has not been indexed before, set this value to <code>false</code>, in
     *                       order to optimize the indexing process.
     */
    public void index( ContentDocument doc, boolean deleteExisting );

    public void index( ContentDocument doc );

    /**
     * Return true if content is indexed.
     */
    public boolean isIndexed( ContentKey contentKey, final IndexType indexType );

    /**
     * Query the content.
     */
    public ContentResultSet query( ContentIndexQuery query );

    /**
     * Query the index values.
     */
    public IndexValueResultSet query( IndexValueQuery query );

    /**
     * Query the index values.
     */
    public AggregatedResult query( AggregatedQuery query );

    public void optimize();

    public void flush();

    public void reinitializeIndex();

    public Collection<ContentIndexedFields> getContentIndexedFields( ContentKey contentKey );

    public boolean indexExists();

    public void createIndex();
}


