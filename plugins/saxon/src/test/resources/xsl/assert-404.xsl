<?xml version="1.0" encoding="UTF-8"?>
<!-- identity transformation -->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    version="3.0">

    <xsl:template match="/">
        <!-- setting error-code to a QName() fails: QName('my:405)' -->
        <xsl:assert test="1 eq 2" error-code="'405'">not found 405 minus 1</xsl:assert>
        <empty/>
    </xsl:template>

</xsl:stylesheet>
