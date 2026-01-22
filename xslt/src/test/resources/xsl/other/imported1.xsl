<?xml version="1.0" encoding="UTF-8"?>
<!-- identity transformation -->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
version="3.0">

   <xsl:import href="../imported.xsl"/>

   <xsl:template match="greating">
      <i>
	 <xsl:apply-templates/>
      </i>
   </xsl:template>

</xsl:stylesheet>
