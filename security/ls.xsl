<?xml version="1.0" encoding="UTF-8"?>
<!-- this is an attack on system resources -->
<xsl:stylesheet
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    exclude-result-prefixes="#all"
    version="3.0">

   <xsl:output method="text"/>

   <xsl:param name="path" as="xs:string" required="false" select="replace(static-base-uri(), '/[^/]*$', '')"/>

   <xsl:param name="recursive" as="xs:boolean" required="false" select="false()"/>

   <xsl:template match="/">
      <xsl:variable name="collection" select="if ($recursive) then concat($path, '?recurse=yes') else $path"/>
      <xsl:text>Contents of </xsl:text>
      <xsl:value-of select="$collection"/>
      <xsl:text>&#xa;</xsl:text>
      <xsl:for-each select="uri-collection($collection)">
	 <xsl:value-of select="."/>

	 <xsl:text>&#xa;</xsl:text>
      </xsl:for-each>
   </xsl:template>

</xsl:stylesheet>
