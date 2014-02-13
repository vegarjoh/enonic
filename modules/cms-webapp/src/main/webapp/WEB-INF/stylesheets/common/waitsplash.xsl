<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0" exclude-result-prefixes="#all"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    >

  <xsl:output method="html"/>

  <xsl:template name="waitsplash">
    <script type="text/javascript">
		function waitsplash()
	  {
      var body = document.getElementsByTagName( 'body' )[0];

      var table = document.createElement( 'table' );
      table.id = 'cmsWaitSplash';
      table.className = 'overlay';

      table.style.width = (document.all) ? '102%' : '100%';
      table.style.height = (document.all) ? '110%' : '100%';

      table.style.position = 'absolute';
      table.style.zIndex = 100000000;
      table.style.top = 0;
      table.style.left = 0;

      var tBody = document.createElement( 'tbody' );

      var tr = document.createElement( 'tr' );

      var td = document.createElement( 'td' );
      td.align = 'center';
      td.height = '100%';
      td.style.backgroundColor = '#ffffff';
      td.innerHTML = '&lt;div class="waitsplash-image"&gt;&lt;/div&gt;';
      td.innerHTML += '%sysPleaseWait%';
      
      table.appendChild( tBody );
      tBody.appendChild( tr );
      tr.appendChild( td );

      body.appendChild( table );
    }
    // -------------------------------------------------------------------------------------------------------------------------------------

    function removeWaitsplash()
    {
      // ***********************************************************************************************************************************
      // *** Variables
      // ***********************************************************************************************************************************
      var html, body, waitSplashElement;

      body = document.getElementsByTagName( 'body' )[0];
      html = document.getElementsByTagName( 'html' )[0];
      waitSplashElement = document.getElementById( 'cmsWaitSplash' );

      if ( waitSplashElement )
      {
        body.removeChild( waitSplashElement );
      }
    }
    </script>

    <!-- Cache the image -->
    <div style="display:none">
      <img src="images/waitsplash.gif" alt="." width="64" height="64"/>
    </div>
  </xsl:template>

</xsl:stylesheet>
