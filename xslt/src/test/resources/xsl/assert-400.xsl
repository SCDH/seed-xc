<?xml version="1.0" encoding="UTF-8"?>
<!-- identity transformation -->
<xsl:stylesheet
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns:xs="http://www.w3.org/2001/XMLSchema"
        xmlns:my="http://example.xml"
        version="3.0">

    <xsl:template match="/">
        <!-- setting error-code to a QName() fails -->
        <xsl:assert test="1 eq 2" error-code="QName('my:X400)'">not found 401 minus 1</xsl:assert>
        <empty/>
    </xsl:template>

</xsl:stylesheet>
