<?xml version="1.0" encoding="UTF-8"?>
<!-- this is an attack on system resources -->
<xsl:stylesheet
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    exclude-result-prefixes="#all"
    version="3.0">

   <xsl:output method="text"/>

   <xsl:param name="filename" as="xs:string" required="true"/>

   <xsl:param name="content" as="xs:string" required="true"/>

   <xsl:template match="/">
      <xsl:variable name="fname" select="resolve-uri($filename, static-base-uri())"/>
      <xsl:result-document href="{$fname}" method="text">
	 <xsl:value-of select="$content"/>
      </xsl:result-document>
      <xsl:text>Congratulations!</xsl:text>
      <xsl:text>&#xa;You just wrote to </xsl:text>
      <xsl:value-of select="$fname"/>
   </xsl:template>

</xsl:stylesheet>
