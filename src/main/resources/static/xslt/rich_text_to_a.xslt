<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE xsl:stylesheet [
        <!ENTITY nbsp "&#xa0;">
        ]>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:output method="xml" indent="yes" omit-xml-declaration="yes" />

    <xsl:template match="node">
        <div class="nodeContent">
            <!-- Apply templates to children of node -->
            <xsl:apply-templates select="*" />
        </div>
    </xsl:template>

    <!-- bu blok olmazsa alt satira inmeden bitisik oluyor butun content -->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="rich_text">
        <!-- <xsl:variable name="hasLink" select="@link"/> -->

        <xsl:variable name="stilTanimliMi">
            <xsl:choose>
                <xsl:when test="not(@style) and not(@weight) and not(@foreground) and not(@background) and not(@strikethrough) and not(@underline) and not(@family) and not(@justification)">
                    <xsl:text>false</xsl:text>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:text>true</xsl:text>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <xsl:variable name="sinifTanimliMi">
            <xsl:choose>
                <xsl:when test="not(@scale)">
                    <xsl:text>false</xsl:text>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:text>true</xsl:text>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>


        <xsl:choose>
            <!-- If link attribute exists -->
            <xsl:when test="@link">

                <a>

                    <xsl:attribute name="href">
                        <xsl:if test="@link">
                            <xsl:call-template name="convertUrl">
                                <xsl:with-param name="address" select="@link"/>
                            </xsl:call-template>

                        </xsl:if>
                    </xsl:attribute>


                    <xsl:call-template name="css">
                        <xsl:with-param name="stilTanimliMi" select="$stilTanimliMi"/>
                        <xsl:with-param name="sinifTanimliMi" select="$sinifTanimliMi"/>


                        <xsl:with-param name="italik" select="@style"/>
                        <xsl:with-param name="kalin" select="@weight"/>
                        <xsl:with-param name="renk" select="@foreground"/>
                        <xsl:with-param name="arkaplan" select="@background"/>
                        <xsl:with-param name="ustu-cizili" select="@strikethrough"/>
                        <xsl:with-param name="alti-cizili" select="@underline"/>
                        <xsl:with-param name="olcek" select="@scale"/>
                        <xsl:with-param name="yazi-tipi" select="@family"/>
                        <xsl:with-param name="hizalama" select="@justification"/>

                    </xsl:call-template>
                    <xsl:apply-templates />

                </a>
            </xsl:when>
            <xsl:when test="$stilTanimliMi='true' or $sinifTanimliMi='true'">
                <span>
                    <xsl:call-template name="css">
                        <xsl:with-param name="stilTanimliMi" select="$stilTanimliMi"/>
                        <xsl:with-param name="sinifTanimliMi" select="$sinifTanimliMi"/>

                        <xsl:with-param name="italik" select="@style"/>
                        <xsl:with-param name="kalin" select="@weight"/>
                        <xsl:with-param name="renk" select="@foreground"/>
                        <xsl:with-param name="arkaplan" select="@background"/>
                        <xsl:with-param name="ustu-cizili" select="@strikethrough"/>
                        <xsl:with-param name="alti-cizili" select="@underline"/>
                        <xsl:with-param name="olcek" select="@scale"/>
                        <xsl:with-param name="yazi-tipi" select="@family"/>
                        <xsl:with-param name="hizalama" select="@justification"/>

                    </xsl:call-template>
                    <xsl:apply-templates />

                </span>
            </xsl:when>


            <!-- If link attribute does not exist -->
            <xsl:otherwise>


                <xsl:apply-templates />



            </xsl:otherwise>

        </xsl:choose>


    </xsl:template>

    <xsl:template name="css">

        <xsl:param name="stilTanimliMi"/>
        <xsl:param name="sinifTanimliMi"/>

        <xsl:param name="italik"/>
        <xsl:param name="kalin"/>
        <xsl:param name="renk"/>
        <xsl:param name="arkaplan"/>
        <xsl:param name="ustu-cizili"/>
        <xsl:param name="alti-cizili"/>
        <xsl:param name="olcek"/>
        <xsl:param name="yazi-tipi"/>
        <xsl:param name="hizalama"/>


        <xsl:if test="$sinifTanimliMi='true'">

            <xsl:attribute name="class">
                <xsl:if test="$olcek">
                    <xsl:value-of select="$olcek"/>
                </xsl:if>
            </xsl:attribute>
        </xsl:if>



        <xsl:if test="$stilTanimliMi='true'">

            <xsl:attribute name="style">
                <xsl:if test="$italik">
                    <xsl:text>font-style: </xsl:text>
                    <xsl:value-of select="$italik"/>
                </xsl:if>
                <xsl:if test="$kalin">
                    <xsl:choose>
                        <xsl:when test="$kalin='heavy'">
                            <xsl:text>; font-weight: </xsl:text>
                            <xsl:text>bold</xsl:text>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:text>; font-weight: </xsl:text>
                            <xsl:value-of select="$kalin"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:if>
                <xsl:if test="$renk">
                    <xsl:text>; color: </xsl:text>
                    <xsl:call-template name="convertColor">
                        <xsl:with-param name="color" select="$renk"/>
                    </xsl:call-template>
                </xsl:if>
                <xsl:if test="$arkaplan">
                    <xsl:text>; background-color: </xsl:text>
                    <xsl:call-template name="convertColor">
                        <xsl:with-param name="color" select="$arkaplan"/>
                    </xsl:call-template>
                </xsl:if>
                <xsl:if test="$ustu-cizili">
                    <!-- https://blog.udemy.com/css-strikethrough/ -->
                    <xsl:text>; text-decoration: </xsl:text>
                    <xsl:value-of select="$ustu-cizili"/>
                    <xsl:choose>
                        <xsl:when test="$ustu-cizili='true'">
                            <xsl:text>; text-decoration: </xsl:text>
                            <xsl:text>line-through</xsl:text>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:text>; text-decoration: </xsl:text>
                            <xsl:value-of select="$ustu-cizili"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:if>
                <xsl:if test="$alti-cizili">
                    <xsl:choose>
                        <xsl:when test="$alti-cizili='single'">
                            <xsl:text>; text-decoration: </xsl:text>
                            <xsl:text>underline</xsl:text>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:text>; text-decoration: </xsl:text>
                            <xsl:value-of select="$alti-cizili"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:if>
                <xsl:if test="$yazi-tipi">
                    <xsl:text>; font-family: </xsl:text>
                    <xsl:value-of select="$yazi-tipi"/>
                </xsl:if>
                <xsl:if test="$hizalama">
                    <!-- https://www.w3schools.com/cssref/tryit.php?filename=trycss_text-align -->
                    <xsl:text>; text-align: </xsl:text>
                    <xsl:value-of select="$hizalama"/>
                </xsl:if>
            </xsl:attribute>
        </xsl:if>


    </xsl:template>



    <xsl:template name="convertColor">
        <xsl:param name="color"/>

        <xsl:choose>
            <xsl:when test="string-length($color) = 13 and substring($color, 1, 1) = '#'">
                <xsl:text>#</xsl:text>
                <xsl:value-of select="substring($color, 2, 2)"/>
                <xsl:value-of select="substring($color, 6, 2)"/>
                <xsl:value-of select="substring($color, 10, 2)"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$color"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>


    <xsl:template name="convertUrl">
        <xsl:param name="address"/>


        <xsl:choose>
            <xsl:when test="not(contains($address, ' '))">
                <a href="{@link}">
                    <xsl:value-of select="."/>
                </a>
            </xsl:when>

            <xsl:when test="starts-with($address, 'webs ') and contains($address, ' ') and not(contains(substring-after($address, ' '), ' '))">
                <xsl:variable name="externalLink" select="substring-after(@link, 'webs ')"/>
                <xsl:value-of select="$externalLink"/>

                <!--
                <a href="{$externalLink}" style="a-external-link" rel="noreferrer noopener" target="_blank">
                    <xsl:value-of select="."/>
                </a>
                -->
            </xsl:when>

            <xsl:when test="starts-with($address, 'node ') and contains($address, ' ') and not(contains(substring-after($address, ' '), ' '))">
                <xsl:variable name="internalLink" select="substring-after(@link, 'node ')"/>
                /nodes/<xsl:value-of select="$internalLink"/>



            </xsl:when>

            <xsl:when test="starts-with($address, 'node ') and contains($address, ' ') and contains(substring-after($address, ' '), ' ')">
                <xsl:variable name="rawLink" select="substring-after(@link, 'node ')"/>
                <xsl:variable name="internalLink" select="substring-before($rawLink, ' ')"/>
                <xsl:variable name="anchor" select="substring-after($rawLink, ' ')"/>
                /nodes/<xsl:value-of select="$internalLink"/>#<xsl:value-of select="$anchor"/>


            </xsl:when>
            <xsl:otherwise>
                zzz
            </xsl:otherwise>
        </xsl:choose>

    </xsl:template>




</xsl:stylesheet>

<!-- scale:  h1-h6, small, sup, sub, Default CSS Settings
    https://www.w3schools.com/tags/tag_hn.asp -->