/*
 * Copyright 2000-2013 Enonic AS
 * http://www.enonic.com/license
 */
package com.enonic.cms.core.content;

import org.jdom.Element;

import com.enonic.cms.core.structure.SiteEntity;

/**
 * Oct 28, 2009
 */
public class ContentLocationXmlCreator
{
    private boolean includeSiteNameInfo = false;

    private boolean includeUserDefinedSectionHomeInfo = false;

    public Element createLocationElement( ContentLocations contentLocations, boolean includeSectionActivationInfo )
    {
        return doCreateLocationElement( contentLocations, includeSectionActivationInfo );
    }

    public Element createLocationElement( ContentEntity content, boolean includeSectionActivationInfo )
    {
        ContentLocationSpecification contentLocationSpecification = new ContentLocationSpecification();
        contentLocationSpecification.setIncludeInactiveLocationsInSection( includeSectionActivationInfo );
        return doCreateLocationElement( content.getLocations( contentLocationSpecification ), includeSectionActivationInfo );
    }

    private Element doCreateLocationElement( ContentLocations contentLocations, boolean includeSectionActivationInfo )
    {
        final Element locationsEl = new Element( "location" );

        for ( SiteEntity site : contentLocations.getSites() )
        {
            final Element siteEl = new Element( "site" );
            siteEl.setAttribute( "key", site.getKey().toString() );
            if ( includeSiteNameInfo )
            {
                siteEl.addContent( new Element( "name" ).setText( site.getName() ) );
            }
            locationsEl.addContent( siteEl );

            siteEl.setAttribute( "count", String.valueOf( contentLocations.numberOfLocations() ) );

            for ( ContentLocation contentLocation : contentLocations.getLocationsBySite( site.getKey() ) )
            {
                final Element contentlocationEl = new Element( "contentlocation" );
                contentlocationEl.setAttribute( "type", contentLocation.getType().getShortName() );
                contentlocationEl.setAttribute( "menuitemkey", contentLocation.getMenuItemKey().toString() );
                contentlocationEl.setAttribute( "menuitemname", contentLocation.getMenuItemName() );
                contentlocationEl.setAttribute( "menuitempath", contentLocation.getMenuItemPathAsString() );
                contentlocationEl.setAttribute( "menuitem-display-name", contentLocation.getMenuItem().getDisplayName() );

                boolean isHome = contentLocations.isHomeLocation( contentLocation );
                contentlocationEl.setAttribute( "home", isHome ? "true" : "false" );

                if ( includeUserDefinedSectionHomeInfo && contentLocation.isInSectionOrSectionHome() )
                {
                    contentlocationEl.setAttribute( "user-defined-section-home",
                                                    contentLocation.isUserDefinedSectionHome() ? "true" : "false" );
                }

                if ( includeSectionActivationInfo && contentLocation.isInSection() )
                {
                    contentlocationEl.setAttribute( "activated", String.valueOf( contentLocation.isApproved() ) );
                }

                siteEl.addContent( contentlocationEl );
            }
        }
        return locationsEl;
    }

    public void setIncludeUserDefinedSectionHomeInfo( boolean includeUserDefinedSectionHomeInfo )
    {
        this.includeUserDefinedSectionHomeInfo = includeUserDefinedSectionHomeInfo;
    }
}
