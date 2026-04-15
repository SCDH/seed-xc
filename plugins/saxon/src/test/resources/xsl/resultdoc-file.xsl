<?xml version="1.0" encoding="UTF-8"?>
<!-- this is an attack on system resources -->
<xsl:stylesheet
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns:xs="http://www.w3.org/2001/XMLSchema"
        version="3.0">

   <xsl:output method="text"/>

    <xsl:param name="output" as="xs:string" select="'file:/tmp/hacked.xml'"/>

   <xsl:template match="/">
      <xsl:result-document href="{$output}">
	 <hacked>
	    <xsl:text>You've really been hacked!</xsl:text>
	 </hacked>
      </xsl:result-document>
      <xsl:text>You've been hacked! See </xsl:text>
       <xsl:value-of select="$output"/>
   </xsl:template>

</xsl:stylesheet>
