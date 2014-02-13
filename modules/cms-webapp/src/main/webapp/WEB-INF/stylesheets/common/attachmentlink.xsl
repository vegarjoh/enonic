<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0" exclude-result-prefixes="#all"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    >

  <xsl:output method="html"/>

  <xsl:template name="attachmentlink">
      <xsl:param name="label"/>
      <xsl:param name="name"/>
      <xsl:param name="selectnode"/>
      <xsl:param name="binarykey"/>
      <xsl:param name="versionkey"/>
      <xsl:param name="colspan"/>
      <xsl:param name="rows"/>
      <xsl:param name="cols"/>
      
      <td valign="top"><xsl:value-of select="$label"/></td>
      <td>
          <xsl:attribute name="colspan"><xsl:value-of select="$colspan"/></xsl:attribute>
          <a target="_blank">
              <xsl:attribute name="href">
                <xsl:text>_attachment/</xsl:text>
                <xsl:value-of select="$selectnode"/>
                <xsl:if test="$binarykey != ''">
                  <xsl:text>/binary/</xsl:text>
                  <xsl:value-of select="$binarykey"/>
                </xsl:if>
                <xsl:if test="$versionkey != ''">
                  <xsl:text>?_version=</xsl:text>
                  <xsl:value-of select="$versionkey"/>
                </xsl:if>
              </xsl:attribute>
              %openFile%
          </a>
      </td>
  </xsl:template>

</xsl:stylesheet>
