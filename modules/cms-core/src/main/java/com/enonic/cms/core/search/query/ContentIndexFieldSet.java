/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.search.query;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.joda.time.ReadableDateTime;

import com.enonic.cms.core.content.ContentKey;
import com.enonic.cms.core.content.category.CategoryKey;
import com.enonic.cms.core.content.contenttype.ContentTypeKey;
import com.enonic.cms.core.content.index.BigText;
import com.enonic.cms.core.content.index.ContentIndexConstants;
import com.enonic.cms.core.content.index.FieldHelper;
import com.enonic.cms.core.search.ContentIndexedFields;

/**
 * This class implements the field set.
 */
public final class ContentIndexFieldSet
    implements ContentIndexConstants
{

    public final static int SPLIT_TRESHOLD = 512;

    private final static int ORDER_TRESHOLD = 15;

    private final static String LINE_SEPARATOR = System.getProperty( "line.separator" );

    private ContentKey key;

    private CategoryKey categoryKey;

    private ContentTypeKey contentTypeKey;

    private Integer status;

    private Date publishFrom;

    private Date publishTo;

    private final ArrayList<ContentIndexedFields> contentIndexedFields = new ArrayList<ContentIndexedFields>();

    private final HashMap<String, List<ContentIndexedFields>> contentIndexedFieldsByPath = new HashMap<String, List<ContentIndexedFields>>();

    public void setKey( ContentKey key )
    {
        this.key = key;
    }

    public void setCategoryKey( CategoryKey value )
    {
        this.categoryKey = value;
    }

    public void setContentTypeKey( ContentTypeKey value )
    {
        this.contentTypeKey = value;
    }

    public void setStatus( Integer value )
    {
        this.status = value;
    }

    public void setPublishFrom( Date value )
    {
        this.publishFrom = value;
    }

    public void setPublishTo( Date value )
    {
        this.publishTo = value;
    }

    public void addFieldWithIntegerValue( String fieldName, int value )
    {

        fieldName = FieldHelper.translateFieldName( fieldName );
        addSingleEntity( fieldName, IndexValueConverter.toString( value ), value );
    }

    public void addFieldWithStringValue( String fieldName, String value )
    {

        addField( fieldName, value, null );
    }

    public void addFieldWithStringValue( String fieldName, String value, String defaultValue )
    {

        if ( value == null )
        {
            addField( fieldName, defaultValue, null );
        }
        else
        {
            addField( fieldName, value, defaultValue );
        }
    }

    public void addFieldWithDateValue( String fieldName, Date value, String defaultValue )
    {

        if ( value == null )
        {
            addField( fieldName, defaultValue, null );
        }
        else
        {
            fieldName = FieldHelper.translateFieldName( fieldName );
            addSingleEntity( fieldName, IndexValueConverter.toString( value ), value );
        }
    }

    public void addFieldWithBigTextValue( String fieldName, BigText value )
    {

        fieldName = FieldHelper.translateFieldName( fieldName );

        String orderValue = value.getText();
        List<String> strings = value.getTextSplitted( SPLIT_TRESHOLD, LINE_SEPARATOR );
        for ( String str : strings )
        {
            str = str.trim();
            if ( str.length() > 0 )
            {
                addSingleEntity( fieldName, str, orderValue, null );
            }
        }
    }

    public void addFieldWithAnyValue( String fieldName, String value )
    {

        addField( fieldName, value, null );
    }

    private void addField( String fieldName, String value, String defaultValue )
    {

        fieldName = FieldHelper.translateFieldName( fieldName );

        if ( value == null )
        {
            value = defaultValue;
        }

        if ( value.length() > SPLIT_TRESHOLD )
        {
            // value too big, we split it with the full text split technology :)
            BigText bigTextValue = new BigText( value );
            String orderValue = bigTextValue.getText();
            for ( String string : bigTextValue.getTextSplitted( SPLIT_TRESHOLD, LINE_SEPARATOR ) )
            {
                if ( string.trim().length() > 0 )
                {
                    addSingleEntity( fieldName, string, orderValue, null );
                }
            }
        }
        else
        {
            ReadableDateTime dateTime = IndexValueConverter.toDate( value );

            Double num = IndexValueConverter.toDouble( value );

            if ( dateTime != null )
            {
                addSingleEntity( fieldName, value, new Date( dateTime.getMillis() ) );
            }
            else if ( num != null )
            {

                addSingleEntity( fieldName, value, num.floatValue() );
            }
            else
            {

                addSingleEntity( fieldName, value, value, null );
            }
        }
    }

    public List<ContentIndexedFields> getEntitites()
    {
        return this.contentIndexedFields;
    }

    public HashMap<String, List<ContentIndexedFields>> getContentIndexedFieldsByPath()
    {
        return contentIndexedFieldsByPath;
    }

    private void addSingleEntity( String fieldName, String value, Date orderValue )
    {
        addSingleEntity( fieldName, value, IndexValueConverter.toTypedString( orderValue ), null );
    }

    private void addSingleEntity( String fieldName, String value, float orderValue )
    {
        addSingleEntity( fieldName, value, IndexValueConverter.toTypedString( orderValue ), orderValue );
    }

    private void addSingleEntity( String fieldName, String value, String orderValue, Float numValue )
    {

        if ( value == null || value.length() == 0 )
        {
            throw new IllegalArgumentException( "Given value cannot be null or empty, fieldName was: " + fieldName );
        }

        ContentIndexedFields contentIndex = new ContentIndexedFields();
        contentIndex.setKey( generateKey() );
        contentIndex.setContentKey( key );
        contentIndex.setContentStatus( status );
        contentIndex.setPublishFrom( publishFrom );
        contentIndex.setPublishTo( publishTo );
        contentIndex.setCategoryKey( categoryKey );
        contentIndex.setContentTypeKey( contentTypeKey.toInt() );
        contentIndex.setPath( fieldName );
        contentIndex.setValue( value.toLowerCase() );
        contentIndex.setNumValue( numValue );

        if ( orderValue.length() > ORDER_TRESHOLD )
        {
            orderValue = orderValue.substring( 0, ORDER_TRESHOLD );
        }

        contentIndex.setOrderValue( orderValue.toLowerCase() );
        this.contentIndexedFields.add( contentIndex );
        addEntityByPath( fieldName, contentIndex );
    }

    public ContentIndexFieldSet()
    {
        super();    //To change body of overridden methods use File | Settings | File Templates.
    }

    private void addEntityByPath( String fieldName, ContentIndexedFields contentIndex )
    {
        List<ContentIndexedFields> existing = contentIndexedFieldsByPath.get( fieldName );
        if ( existing == null )
        {
            List<ContentIndexedFields> newList = new ArrayList<ContentIndexedFields>();
            newList.add( contentIndex );
            contentIndexedFieldsByPath.put( fieldName, newList );
        }
        else
        {
            existing.add( contentIndex );
        }
    }

    /**
     * @return A 36 char long unique key.
     */
    private String generateKey()
    {
        return UUID.randomUUID().toString();
    }
}
