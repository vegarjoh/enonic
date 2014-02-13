<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

    <xsl:output method="html"/>

    <xsl:include href="common/generic_parameters.xsl"/>
    <xsl:include href="common/formatdate.xsl"/>
    <xsl:include href="common/operations_template.xsl"/>
    <xsl:include href="common/button.xsl"/>
    <xsl:include href="common/tablecolumnheader.xsl"/>
    <xsl:include href="common/tablerowpainter.xsl"/>
    <xsl:include href="common/displaypath.xsl"/>
	<xsl:include href="common/genericheader.xsl"/>
    <xsl:include href="common/browse_table_js.xsl"/>

    <xsl:variable name="pageURL">
        <xsl:text>adminpage?page=</xsl:text><xsl:value-of select="$page"/>
        <xsl:text>&amp;op=browse</xsl:text>
		<xsl:text>&amp;menukey=</xsl:text><xsl:value-of select="$menukey"/>
    </xsl:variable>

    <xsl:template match="/">
        
        <html>
            <head>
                <link href="css/admin.css" rel="stylesheet" type="text/css"/>
                <script type="text/javascript" src="javascript/admin.js">//</script>
            </head>

            <body>
                <h1>
					<xsl:call-template name="genericheader"/>
					<a href="adminpage?page={$page}&amp;op=browse&amp;menukey={$menukey}">
						%headServices%
					</a>
                </h1>

                <table width="100%" border="0" cellspacing="0" cellpadding="0">
                    <tr>
                        <td class="browse_title_buttonrow_seperator"><img src="images/1x1.gif"/></td>
                    </tr>
                    <tr>
                        <td>
                            <xsl:call-template name="button">
                                <xsl:with-param name="type" select="'link'" />
                                <xsl:with-param name="caption" select="'%cmdNew%'" />
                                <xsl:with-param name="href">
                                    <xsl:text>adminpage?page=</xsl:text>
                                    <xsl:value-of select="$page"/>
                                    <xsl:text>&amp;op=form</xsl:text>
                                    <xsl:text>&amp;menukey=</xsl:text><xsl:value-of select="$menukey"/>
                                </xsl:with-param>
                            </xsl:call-template>
                        </td>
                    </tr>
                    <tr>
                        <td class="browse_buttonrow_datarows_seperator"><img src="images/1x1.gif"/></td>
                    </tr>
                    <tr>
                        <td>
                            <table width="100%" cellspacing="0" cellpadding="0" class="browsetable">
                                <tr>
                                    <xsl:call-template name="tablecolumnheader">
                                        <xsl:with-param name="caption" select="'%fldName%'" />
                                        <xsl:with-param name="pageURL" select="$pageURL" />
										<xsl:with-param name="sortable" select="'false'" />
                                    </xsl:call-template>

                                    <xsl:call-template name="tablecolumnheader">
                                        <xsl:with-param name="width" select="'100'" />
                                        <xsl:with-param name="align" select="'center'" />
                                        <xsl:with-param name="caption" select="'%fldModified%'" />
                                        <xsl:with-param name="pageURL" select="$pageURL" />
										<xsl:with-param name="sortable" select="'false'" />
                                    </xsl:call-template>

                                    <xsl:call-template name="tablecolumnheader">
                                        <xsl:with-param name="width" select="'90'" />
                                        <xsl:with-param name="caption" select="''" />
                                        <xsl:with-param name="sortable" select="'false'" />
                                    </xsl:call-template>
                                </tr>

                                <xsl:for-each select="/services/service">
                                    <tr>
                                        <xsl:call-template name="tablerowpainter"/>
                                        <td class="browsetablecell" title="%msgClickToEdit%">
											<xsl:call-template name="addJSEvent">
												<xsl:with-param name="key" select="@key"/>
											</xsl:call-template>										
                                            <xsl:value-of select="name"/>
                                        </td>
                                        <td align="center" class="browsetablecell" title="%msgClickToEdit%">
											<xsl:call-template name="addJSEvent">
												<xsl:with-param name="key" select="@key"/>
											</xsl:call-template>										
                                            <xsl:call-template name="formatdatetime">
                                                <xsl:with-param name="date" select="@timestamp"/>
                                            </xsl:call-template>
                                        </td>
                                        <td align="center">
                                        	<table border="0" cellspacing="0" cellpadding="0">
                                        		<tr>
                                        			<td align="center">
			                                            <xsl:call-template name="operations">
			                                                <xsl:with-param name="page" select="$page"/>
			                                                <xsl:with-param name="key" select="@key"/>
			                                            </xsl:call-template>
			                                        </td>
			                                    </tr>
			                                </table>
                                        </td>
                                    </tr>
                                </xsl:for-each>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
        </html>
    </xsl:template>

</xsl:stylesheet>