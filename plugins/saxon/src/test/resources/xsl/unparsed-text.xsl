<?xml version="1.0" encoding="UTF-8"?>
<!-- identity transformation -->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
   xmlns:xs="http://www.w3.org/2001/XMLSchema" version="3.0">

   <xsl:output method="text"/>

   <xsl:param name="uri" as="xs:string" required="false" select="'../samples/secret.txt'"/>

   <xsl:template match="/">
      <xsl:value-of select="unparsed-text($uri)"/>
   </xsl:template>

</xsl:stylesheet>
