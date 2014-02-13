/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.vertical.engine.handlers;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.enonic.cms.framework.xml.XMLDocument;
import com.enonic.cms.framework.xml.XMLDocumentFactory;

import com.enonic.cms.core.CalendarUtil;
import com.enonic.cms.core.language.LanguageEntity;
import com.enonic.cms.core.language.LanguageKey;
import com.enonic.cms.store.dao.LanguageDao;

@Component
public final class LanguageHandler
{
    @Autowired
    private LanguageDao languageDao;

    public String getLanguageCode( LanguageKey languageKey )
    {
        LanguageEntity entity = languageDao.findByKey( languageKey );
        return entity != null ? entity.getCode() : null;
    }

    public void createLanguage( String languageCode, String description )
    {
        LanguageEntity entity = new LanguageEntity();
        entity.setCode( languageCode );
        entity.setDescription( description );
        entity.setTimestamp( new Date() );
        this.languageDao.store( entity );
    }

    public XMLDocument getLanguage( LanguageKey languageKey )
    {
        ArrayList<LanguageEntity> list = new ArrayList<LanguageEntity>();
        LanguageEntity entity = languageDao.findByKey( languageKey );
        if ( entity != null )
        {
            list.add( entity );
        }

        return createLanguagesDoc( list );
    }

    private XMLDocument createLanguagesDoc( List<LanguageEntity> list )
    {
        Element root = new Element( "languages" );

        for ( LanguageEntity value : list )
        {
            Element elem = new Element( "language" );
            elem.setAttribute( "key", String.valueOf( value.getKey() ) );
            elem.setAttribute( "languagecode", value.getCode() );
            elem.setAttribute( "timestamp", CalendarUtil.formatTimestamp( value.getTimestamp() ) );
            elem.addContent( value.getDescription() );
            root.addContent( elem );
        }

        return XMLDocumentFactory.create( new Document( root ) );
    }

    public XMLDocument getLanguages()
    {
        return createLanguagesDoc( languageDao.findAll() );
    }

    public void removeLanguage( LanguageKey languageKey )
    {
        LanguageEntity entity = languageDao.findByKey( languageKey );
        if ( entity == null )
        {
            return;
        }

        this.languageDao.delete( entity );
    }

    public void updateLanguage( LanguageKey languageKey, String languageCode, String description )
    {
        if ( StringUtils.isBlank( languageCode ) )
        {
            return;
        }

        LanguageEntity entity = languageDao.findByKey( languageKey );
        if ( entity == null )
        {
            return;
        }

        entity.setCode( languageCode );
        entity.setDescription( description );
        entity.setTimestamp( new Date() );
    }
}
