/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.user.field;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import junit.framework.Assert;

import com.enonic.cms.api.client.model.user.Address;
import com.enonic.cms.api.plugin.ext.userstore.UserField;
import com.enonic.cms.api.plugin.ext.userstore.UserFieldType;
import com.enonic.cms.api.plugin.ext.userstore.UserFields;

public class AddressTransformerTest
{
    @Test
    public void testFromStorableMap()
    {
        final Map<String, String> storableMap = new HashMap<String, String>();
        storableMap.put( "address[7].street", "Karl Johansgate" );
        storableMap.put( "address[44].street", "Prinsensgate" );
        storableMap.put( "address[19].street", "Bogstadveien" );
        storableMap.put( "address[13].street", "Kirkegata" );

        final AddressTransformer transformer = new AddressTransformer();
        final UserFields userFieldMap = transformer.fromStoreableMap( storableMap );

        Assert.assertEquals( 4, userFieldMap.getSize() );

        final Collection<UserField> userFields = userFieldMap.getFields( UserFieldType.ADDRESS );

        Assert.assertEquals( 4, userFields.size() );

        final UserField[] userFieldArray = userFields.toArray( new UserField[userFields.size()] );
        Assert.assertEquals( "Karl Johansgate", ( (Address) userFieldArray[0].getValue() ).getStreet() );
        Assert.assertEquals( "Kirkegata", ( (Address) userFieldArray[1].getValue() ).getStreet() );
        Assert.assertEquals( "Bogstadveien", ( (Address) userFieldArray[2].getValue() ).getStreet() );
        Assert.assertEquals( "Prinsensgate", ( (Address) userFieldArray[3].getValue() ).getStreet() );
    }

    @Test
    public void testToStoreableMap()
    {
        final UserFields userFields = new UserFields( true );

        final Address address1 = new Address();
        address1.setStreet( "Karl Johansgate" );
        userFields.add( new UserField( UserFieldType.ADDRESS, address1 ) );

        final Address address2 = new Address();
        address2.setStreet( "Kirkegata 1" );
        userFields.add( new UserField( UserFieldType.ADDRESS, address2 ) );

        final Address address3 = new Address();
        address3.setStreet( "Bogstadveien" );
        userFields.add( new UserField( UserFieldType.ADDRESS, address3 ) );

        final Address address4 = new Address();
        address4.setStreet( "Kirkegata 8" );
        userFields.add( new UserField( UserFieldType.ADDRESS, address4 ) );

        final AddressTransformer transformer = new AddressTransformer();
        final Map<String, String> storableMap = transformer.toStoreableMap( userFields );

        Assert.assertEquals( 4, storableMap.size() );
        Assert.assertTrue( storableMap.containsKey( "address[0].street" ) );
        Assert.assertEquals( "Karl Johansgate", storableMap.get( "address[0].street" ) );
        Assert.assertTrue( storableMap.containsKey( "address[1].street" ) );
        Assert.assertEquals( "Kirkegata 1", storableMap.get( "address[1].street" ) );
        Assert.assertTrue( storableMap.containsKey( "address[2].street" ) );
        Assert.assertEquals( "Bogstadveien", storableMap.get( "address[2].street" ) );
        Assert.assertTrue( storableMap.containsKey( "address[3].street" ) );
        Assert.assertEquals( "Kirkegata 8", storableMap.get( "address[3].street" ) );

    }
}
