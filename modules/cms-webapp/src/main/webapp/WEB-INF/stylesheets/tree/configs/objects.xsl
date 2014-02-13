<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0" exclude-result-prefixes="#all"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    >

	<xsl:template match="objects" mode="icon">
        <xsl:text>icon_objects</xsl:text>
	</xsl:template>
	
	<xsl:template match="objects" mode="text">
		<xsl:text>%mnuPortlets%</xsl:text>
	</xsl:template>
	
	<xsl:template match="objects" mode="page">
		<xsl:text>900</xsl:text>
	</xsl:template>
	
	<xsl:template match="objects" mode="extraparams">
		<xsl:text>&amp;menukey=</xsl:text>
		<xsl:value-of select="parent::node()/@key"/>
	</xsl:template>
	
</xsl:stylesheet>