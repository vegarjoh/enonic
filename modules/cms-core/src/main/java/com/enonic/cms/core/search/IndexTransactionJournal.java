/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.core.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.enonic.cms.core.content.ContentEntity;
import com.enonic.cms.core.content.ContentKey;
import com.enonic.cms.core.content.ContentMap;
import com.enonic.cms.core.content.IndexService;
import com.enonic.cms.core.search.query.ContentDocument;
import com.enonic.cms.core.search.query.ContentIndexService;
import com.enonic.cms.store.dao.ContentDao;
import com.enonic.cms.store.dao.ContentEagerFetches;
import com.enonic.cms.store.dao.FindContentByKeysCommand;

import static com.enonic.cms.core.search.IndexTransactionJournalEntry.JournalOperation.UPDATE;

public class IndexTransactionJournal
    implements TransactionSynchronization
{
    private final static Logger LOG = LoggerFactory.getLogger( IndexTransactionJournal.class );

    private final ContentDao contentDao;

    private final IndexService indexService;

    private final ContentIndexService contentIndexService;

    private final Set<IndexTransactionJournalEntry> changeHistory;

    public IndexTransactionJournal( ContentIndexService contentIndexService, IndexService indexService, ContentDao contentDao )
    {
        this.contentIndexService = contentIndexService;
        this.indexService = indexService;
        this.contentDao = contentDao;
        this.changeHistory = new HashSet<IndexTransactionJournalEntry>();
    }

    public void startTransaction()
    {
        registerSynchronization();
        LOG.debug( "Index transaction started" );
    }

    private void registerSynchronization()
    {
        if ( !TransactionSynchronizationManager.getSynchronizations().contains( this ) )
        {
            TransactionSynchronizationManager.registerSynchronization( this );
        }
    }

    public void registerUpdate( Collection<ContentKey> contentKeys, boolean updateMetadataOnly )
    {
        if ( TransactionSynchronizationManager.isCurrentTransactionReadOnly() )
        {
            return;
        }

        for ( ContentKey contentKey : contentKeys )
        {
            final IndexTransactionJournalEntry indexTransactionJournalEntry =
                new IndexTransactionJournalEntry( UPDATE, contentKey, updateMetadataOnly );

            changeHistory.add( indexTransactionJournalEntry );
        }
    }


    public void registerUpdate( ContentKey contentKey, boolean updateMetadataOnly )
    {
        if ( TransactionSynchronizationManager.isCurrentTransactionReadOnly() )
        {
            return;
        }

        final IndexTransactionJournalEntry indexTransactionJournalEntry =
            new IndexTransactionJournalEntry( UPDATE, contentKey, updateMetadataOnly );

        changeHistory.add( indexTransactionJournalEntry );
    }

    public void registerRemove( ContentKey contentKey )
    {
        changeHistory.add( new IndexTransactionJournalEntry( IndexTransactionJournalEntry.JournalOperation.DELETE, contentKey ) );
    }

    private void flushIndexChanges()
    {
        if ( changeHistory.isEmpty() )
        {
            LOG.debug( "No changes found in transaction, skipping index update." );
            return;
        }

        if ( TransactionSynchronizationManager.isCurrentTransactionReadOnly() )
        {
            LOG.debug( "Read-only transaction, nothing to do." );
            return;
        }

        final ContentMap contentMap = preloadContent();

        LOG.debug( "Flushing index changes from transaction journal" );
        for ( IndexTransactionJournalEntry journalEntry : changeHistory )
        {
            switch ( journalEntry.getOperation() )
            {
                case UPDATE:
                    handleFlushUpdateOperation( journalEntry, contentMap );
                    break;

                case DELETE:
                    handleFlushDeleteOperation( journalEntry );
                    break;
            }
        }
        changeHistory.clear();

        flushIndex();
    }

    private ContentMap preloadContent()
    {
        List<ContentKey> contentToLoad = new ArrayList<ContentKey>();
        for ( IndexTransactionJournalEntry journalEntry : changeHistory )
        {
            if ( journalEntry.getOperation() == UPDATE )
            {
                contentToLoad.add( journalEntry.getContentKey() );
            }
        }

        FindContentByKeysCommand command = new FindContentByKeysCommand().contentKeys( contentToLoad ).eagerFetches(
            ContentEagerFetches.PRESET_FOR_INDEXING ).fetchEntitiesAsReadOnly( false ).byPassCache( false );

        return contentDao.findByKeys( command );
    }

    private void handleFlushUpdateOperation( final IndexTransactionJournalEntry journalEntry, final ContentMap contentMap )
    {
        final ContentEntity content = contentMap.get( journalEntry.getContentKey() );
        if ( content == null )
        {
            LOG.warn( "Content to update index for did not exist (removing index for content instead): " + journalEntry.getContentKey() );
            deleteContent( journalEntry.getContentKey() );
        }
        else if ( content.isDeleted() )
        {
            deleteContent( content.getKey() );
        }
        else
        {
            doUpdateContent( content, journalEntry.isUpdateMetadataOnly() );
        }
    }

    private void handleFlushDeleteOperation( final IndexTransactionJournalEntry journalEntry )
    {
        deleteContent( journalEntry.getContentKey() );
    }

    private void doUpdateContent( final ContentEntity content, final boolean updateMetadataOnly )
    {
        final ContentDocument doc = indexService.createContentDocument( content, updateMetadataOnly );

        LOG.debug( "Updating index for content: " + doc.getContentKey().toString() );

        contentIndexService.index( doc, updateMetadataOnly );
    }

    private void deleteContent( ContentKey contentKey )
    {
        LOG.debug( "Deleting index for content: " + contentKey.toString() );

        contentIndexService.remove( contentKey );
    }

    private void flushIndex()
    {
        contentIndexService.flush();
    }

    @Override
    public void afterCommit()
    {
        TransactionSynchronizationManager.unbindResourceIfPossible( IndexTransactionServiceImpl.TRANSACTION_JOURNAL_KEY );
        flushIndexChanges();
    }

    @Override
    public void suspend()
    {
    }

    @Override
    public void resume()
    {
    }

    @Override
    public void flush()
    {
    }

    @Override
    public void beforeCommit( boolean b )
    {
    }

    @Override
    public void beforeCompletion()
    {
    }

    @Override
    public void afterCompletion( int i )
    {
        //     System.out.println( "Completion status: " + i );
    }

    public void clearJournal()
    {
        TransactionSynchronizationManager.unbindResourceIfPossible( IndexTransactionServiceImpl.TRANSACTION_JOURNAL_KEY );
        changeHistory.clear();
    }
}
