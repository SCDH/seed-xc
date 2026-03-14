<?xml version="1.0" encoding="UTF-8"?>
<!-- this is an attack on system resources -->
<xsl:stylesheet
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    exclude-result-prefixes="xs xsl"
    version="3.0">

   <xsl:output method="xml" indent="true"/>

   <xsl:param name="times" as="xs:integer" required="true"/>

   <xsl:template match="/">
      <times>
	 <xsl:call-template name="one-time">
	    <xsl:with-param name="count" select="$times"/>
	 </xsl:call-template>
      </times>
   </xsl:template>

   <xsl:template name="one-time">
      <xsl:param name="count" as="xs:integer"/>
      <once n="{$count}"/>
      <xsl:if test="$count gt 0">
	 <xsl:call-template name="one-time">
	    <xsl:with-param name="count" select="$count - 1"/>
	 </xsl:call-template>
      </xsl:if>
   </xsl:template>

</xsl:stylesheet>
