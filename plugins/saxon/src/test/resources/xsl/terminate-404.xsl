<?xml version="1.0" encoding="UTF-8"?>
<!-- identity transformation -->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    version="3.0">

    <xsl:template match="/">
        <!-- 404 could have been from the file name! -->
        <xsl:message terminate="yes">not found 405 minus 1</xsl:message>
    </xsl:template>

</xsl:stylesheet>
