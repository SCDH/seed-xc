<?xml version="1.0" encoding="UTF-8"?>
<!-- identity transformation -->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
version="3.0">

   <xsl:template match="text()">
      <b>
	 <xsl:value-of select="."/>
      </b>
   </xsl:template>

</xsl:stylesheet>
