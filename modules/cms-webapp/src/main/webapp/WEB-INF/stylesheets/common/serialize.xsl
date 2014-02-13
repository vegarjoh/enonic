<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0" exclude-result-prefixes="#all"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:admin="http://www.enonic.com/cms/admin">

    <xsl:template name="serialize">
        <xsl:param name="xpath"/>
        <xsl:param name="include-self" select="false()"/>
        <xsl:param name="formatter" select="'pretty'"/>

        <xsl:if test="$xpath">
            <xsl:variable name="serialized">
                <xsl:value-of select="admin:serialize($xpath, $include-self, $formatter)"/>
            </xsl:variable>

            <xsl:choose>
                <xsl:when test="$serialized and not($serialized = '')">
                    <xsl:value-of select="$serialized"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="$xpath"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:if>
    </xsl:template>
</xsl:stylesheet>