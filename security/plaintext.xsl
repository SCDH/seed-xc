<?xml version="1.0" encoding="UTF-8"?>
<!-- extract plain text -->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="3.0">

   <xsl:output method="text"/>

   <xsl:mode on-no-match="shallow-skip"/>

   <xsl:template match="text()">
      <xsl:value-of select="."/>
   </xsl:template>

</xsl:stylesheet>
