<?xml version="1.0" encoding="UTF-8"?>
<!--
  ~ Copyright 2000-2013 Enonic AS
  ~ http://www.enonic.com/license
  -->

<xsl:stylesheet version="2.0" exclude-result-prefixes="#all" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:portal="http://www.enonic.com/cms/portal">

  <xsl:template match="/">
    <output>
      <value>
        <xsl:value-of select="portal:dateTime('2013-05-21 08:47')"/>
      </value>
      <value>
        <xsl:value-of select="portal:dateTime('2013-05-21T08:47')"/>
      </value>

      <value>
        <xsl:value-of select="portal:dateTime('2013-05-21 08:47Z')"/>
      </value>
      <value>
        <xsl:value-of select="portal:dateTime('2013-05-21T08:47Z')"/>
      </value>

      <value>
        <xsl:value-of select="portal:dateTime('2013-05-21 08:47:33')"/>
      </value>
      <value>
        <xsl:value-of select="portal:dateTime('2013-05-21T08:47:34')"/>
      </value>

      <value>
        <xsl:value-of select="portal:dateTime('2013-05-21 08:47:35Z')"/>
      </value>
      <value>
        <xsl:value-of select="portal:dateTime('2013-05-21T08:47:36Z')"/>
      </value>

    </output>
  </xsl:template>

</xsl:stylesheet>
