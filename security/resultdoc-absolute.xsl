<?xml version="1.0" encoding="UTF-8"?>
<!-- this is an attack on system resources -->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="3.0">

   <xsl:output method="text"/>

   <xsl:template match="/">
      <xsl:result-document href="/tmp/hacked.xml">
	 <hacked>
	    <xsl:text>You've really been hacked, again!</xsl:text>
	 </hacked>
      </xsl:result-document>
      <xsl:text>You've been hacked! Again! See /tmp/hacked.xml</xsl:text>
   </xsl:template>

</xsl:stylesheet>
