<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE xsl:stylesheet [
        <!ENTITY nbsp "&#xa0;">
        ]>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:b64="base64.xsl">

     <xsl:output method="xml" indent="no" omit-xml-declaration="yes" />

    <xsl:include href="base64.xsl"/>

    <xsl:template match="p">
        <xsl:variable name="encoded" select="."/>

        <xsl:call-template name="decodeBase64">
            <xsl:with-param name="kodlu" select="$encoded"/>
        </xsl:call-template>

        <div class="transformed"><xsl:value-of select="$encoded"/><xsl:apply-templates select="*" /></div>
        <xsl:call-template name="b64:decode">
            <xsl:with-param name="base64String" select="'MQ=='"/>
        </xsl:call-template>
    </xsl:template>


    <xsl:template name="decodeBase64">
        <xsl:param name="kodlu"/>


        <xsl:text>#</xsl:text>
        <xsl:value-of select="$kodlu"/>


    </xsl:template>


</xsl:stylesheet>


