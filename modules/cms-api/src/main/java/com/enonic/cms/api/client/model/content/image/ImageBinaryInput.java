/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */

package com.enonic.cms.api.client.model.content.image;

import java.io.Serializable;

import com.enonic.cms.api.client.model.content.BinaryInput;

public class ImageBinaryInput
    extends BinaryInput
    implements Serializable
{
    private static final long serialVersionUID = 3058662266193737320L;

    public ImageBinaryInput( int existingBinaryKey )
    {
        super( "binarydata", existingBinaryKey );
    }

    public ImageBinaryInput( byte[] binary, String binaryName )
    {
        super( "binarydata", binary, binaryName );
    }
}