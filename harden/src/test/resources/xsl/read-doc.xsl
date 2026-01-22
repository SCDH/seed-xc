<?xml version="1.0" encoding="UTF-8"?>
<!-- identity transformation -->
<xsl:stylesheet
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    exclude-result-prefixes="#all"
    version="3.0">

   <xsl:output method="xml"/>

   <xsl:param name="uri" as="xs:string" required="false" select="'../samples/unparsed-entity.xml'"/>

   <xsl:template match="/">
      <result>
	 <xsl:copy-of select="doc($uri)/*"/>
      </result>
   </xsl:template>

</xsl:stylesheet>
