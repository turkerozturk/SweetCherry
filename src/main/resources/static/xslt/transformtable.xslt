<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE xsl:stylesheet [
        <!ENTITY nbsp "&#xa0;">
        ]>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:output method="xml" indent="yes" omit-xml-declaration="yes" />

    <xsl:template match="table">
        <table class="transformed">
            <!-- Apply templates to children of node -->
            <xsl:apply-templates select="*" />
        </table>
    </xsl:template>

    <xsl:template match="row">
        <tr>
            <!-- Apply templates to children of node -->
            <xsl:apply-templates select="*" />
        </tr>
    </xsl:template>

    <xsl:template match="cell">
        <td>
            <xsl:value-of select="."/>
            <!-- Apply templates to children of node -->
            <xsl:apply-templates select="*" />
        </td>
    </xsl:template>









</xsl:stylesheet>


