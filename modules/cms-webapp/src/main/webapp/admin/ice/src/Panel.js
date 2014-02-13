/**
 * Panel
 */
cms.ice.Panel = function()
{
	return {

        isIceOn : false,

		create: function()
		{
            var setup = cms.ice.Setup;

            var menuHtml = setup.pageInfo[2];

			$ice('body').append('<div id="ice-panel"><!-- --></div>');
            $ice('#ice-panel').append('<div id="ice-panel-menu">' + menuHtml + '</div>');
            $ice('#ice-on-of-container').append('<span class="ice-toggle-button ice-tb-off" id="ice-toggle-on-off-button">ICE</span>');

            this.postRender();
		},
		// ---------------------------------------------------------------------------------------------------------------------------------------------

		showIce: function( show )
		{
            var ice = cms.ice;

			if ( show )
			{
				ice.PageOverlay.remove();
				ice.PortletOverlay.resetAll();
				ice.PortletOverlay.remove();
				ice.ContextMenu.remove();
				ice.Tooltip.remove();

				ice.PageOverlay.create();
				ice.PortletOverlay.create();
				ice.ContextMenu.create();
				ice.Tooltip.create();
			}
			else
			{
				ice.PageOverlay.remove();
				ice.PortletOverlay.activePortlet = '';
				ice.PortletOverlay.resetAll();
				ice.PortletOverlay.remove();
				ice.ContextMenu.remove();
				ice.Tooltip.remove();
			}

            this.isIceOn = show;

            ice.Utils.createICEInfoCookie();
		},
		// ---------------------------------------------------------------------------------------------------------------------------------------------

	    remove : function()
	    {
			$ice('#ice-panel').remove();
	    },
        // ---------------------------------------------------------------------------------------------------------------------------------------------

        windowResize : function()
        {
            var utils = cms.ice.Utils;
            var panel = $ice('#ice-panel');
            var defaultLeftPositionForPanel = 5;
            var defaultTopPositionForPanel = 5;

            var windowHeight = $ice(window).height();
            var windowWidth = $ice(window).width();

            var panelWidth = panel.width();
            var panelHeight = panel.height();

            var leftPosition = parseInt(utils.getICEInfoCookieByName('icePagePanelX')) || defaultLeftPositionForPanel;
            var topPosition = parseInt(utils.getICEInfoCookieByName('icePagePanelY')) || defaultTopPositionForPanel;
            var rightPosition = leftPosition + panelWidth;
            var bottomPosition = topPosition + panelHeight;

            if ( rightPosition > windowWidth )
            {
                leftPosition = windowWidth - panelWidth - defaultLeftPositionForPanel;
                panel.css('left', leftPosition + 'px');
            }
            else
            {
                var position = utils.getICEInfoCookieByName('icePagePanelX') ? 'left' : 'right';
                panel.css(position, leftPosition + 'px');
            }

            if ( bottomPosition  > windowHeight )
            {
                topPosition = topPosition - (bottomPosition - windowHeight);
            }

            panel.css('top', topPosition + 'px');
        },
        // ---------------------------------------------------------------------------------------------------------------------------------------------

        postRender : function()
        {
            var t = this;
            var utils = cms.ice.Utils;
            var setup = cms.ice.Setup;
            var panel = cms.ice.Panel;
            var tooltip = cms.ice.Tooltip;
            var portletOverlay = cms.ice.PortletOverlay;
            var maxHeightForContentMenuItems = 115;
            var contentMenuItemsContainer = $ice('#ice-panel .ice-menu-item-content-container');
            var onOffButton = $ice('#ice-toggle-on-off-button');
            var panelElement = $ice('#ice-panel');
            var contentMenuItems = $ice('#ice-panel .ice-menu-item-content-container a');


            utils.injectLanguageStr('%cmdIceCreateContent%', $ice('#ice-panel .ice-lang-placeholder-create-content'));
            utils.injectLanguageStr('%cmdIceViewPageTrace%', $ice('#ice-panel .ice-lang-placeholder-page-trace'));
            utils.injectLanguageStr('%cmdIceViewPageXML%', $ice('#ice-panel .ice-lang-placeholder-page-xml'));

            if ( contentMenuItemsContainer.height() > maxHeightForContentMenuItems )
			{
			    contentMenuItemsContainer.height( maxHeightForContentMenuItems + 'px');
			}

            onOffButton.bind('click', function( event )
            {
                if( $ice(this).hasClass('ice-tb-off') )
                {
                    if ( !t.isIceOn )
                    {
                        panel.showIce(true);
                    }

                    $ice(this).addClass('ice-tb-on');
                    $ice(this).removeClass('ice-tb-off');
                }
                else
                {
                    panel.showIce(false);

                    $ice(this).addClass('ice-tb-off');
                    $ice(this).removeClass('ice-tb-on');
                }
            });

            panelElement.bind('dragstart',function( event )
            {
				if ( !$ice(event.target).is('#ice-panel h3') )
                {
                    return false;
                }
			}).bind('drag',function( event )
            {
                var isDragging = utils.isElementOutsideOfViewport(event, this);

                if ( !isDragging )
                {
                    setup.isDragging = true;

                    $ice( this ).css( {
                        top: event.offsetY - $ice(document).scrollTop(),
                        left: event.offsetX
                     });
                }
			}).bind('dragend',function( event )
            {
                setup.isDragging = false;
                utils.createICEInfoCookie();
			});

            panelElement.bind('mouseover', function(e)
            {
                var isMenuItemWithContent = e.target.parentNode && e.target.parentNode.className === 'ice-menu-item-content-container';

                if ( !isMenuItemWithContent )
                {
                    tooltip.hide();
                }

                if ( portletOverlay.previousMousedHoverOverlay )
                {
                    if ( $ice(portletOverlay.previousMousedHoverOverlay).attr('ice-overlay-is-active') !== 'true' )
                    {
                        $ice(portletOverlay.previousMousedHoverOverlay).css('opacity', setup.portletOverlayOpacity);
                    }
                }
            });

            contentMenuItems.bind('mouseover', function(e)
            {
                tooltip.show('%msgIceClickToOpen%');
            });

            contentMenuItems.bind('mousemove', function(e)
            {
                tooltip.move(e);
            });
        }
	};
}();