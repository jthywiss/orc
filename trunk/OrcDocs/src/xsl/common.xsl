<?xml version='1.0'?>
<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:orc="http://orc.csres.utexas.edu/OrcDocs"
  version="1.0">

<xsl:param name="orc.version"/>

<xsl:template match="orc:version">
  <xsl:copy-of select="$orc.version" />
</xsl:template>

<!--
Copied from http://xslthl.wiki.sourceforge.net/DocBook+XSL+Updates
to enable syntax highlighting of <code/>
-->
<xsl:template match="code">
    <xsl:variable name="content">
        <xsl:call-template name="apply-highlighting" />
    </xsl:variable>
    <xsl:choose>
        <xsl:when test="count(ancestor::programlisting) &gt; 0">
            <xsl:copy-of select="$content" />
        </xsl:when>
        <xsl:otherwise>
            <xsl:call-template name="inline.monoseq">
                <xsl:with-param name="content" select="$content" />
            </xsl:call-template>
        </xsl:otherwise>
    </xsl:choose>
</xsl:template>
</xsl:stylesheet>
