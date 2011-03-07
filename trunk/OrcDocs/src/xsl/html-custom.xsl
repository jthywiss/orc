<?xml version='1.0'?>
<xsl:stylesheet
    xmlns:xslthl="http://xslthl.sf.net"
    exclude-result-prefixes="xslthl"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
	version="1.0">

<xsl:param name="orc.demo" select="0"/>
	
<!--
Orc examples must use the "orc" CSS class.
-->
<xsl:template match="programlisting[@language='orc-demo']" mode="class.value">orc</xsl:template>

<!-- Include orc.js if desired -->
<xsl:template name="user.footer.content">
<xsl:if test="$orc.demo">
<script src="/orchard/orc.js" type="text/javascript"></script>
</xsl:if>	
</xsl:template>

<!-- Match template for collapsible example boxes -->
<xsl:template match="example">
	<xsl:variable name="ex_id" select="./@xml:id"/>
	<xsl:variable name="ex_link" select="concat($ex_id,'_link')"/>
	<xsl:variable name="ex_content" select="concat($ex_id,'_content')"/>
	<div class="example">
		<div class="exampleHeading">
			<a title="show/hide" href="javascript: void(0);" class="showHideToggle">
			<xsl:attribute name="id"><xsl:value-of select="$ex_link" /></xsl:attribute>
			<xsl:attribute name="onclick">toggle(this, '<xsl:value-of select="$ex_content"/>')</xsl:attribute>&#8722;</a>
			<span class="exampleCaption">
				<xsl:value-of select="./title"></xsl:value-of>
			</span>
		</div>
		<div class="exampleBody">
			<xsl:attribute name="id">
				<xsl:value-of select="$ex_content"/>
			</xsl:attribute>
		<xsl:apply-templates/>
		</div>
	</div>	
	<noscript>
		<p>"WARNING:  This example requires Javascript to be rendered correctly.  Please enable it in your browser."</p>
	</noscript>
	<!-- Start the box collapsed -->
	<script type="text/javascript">toggle(document.getElementById('<xsl:value-of select="$ex_link"/>'), '<xsl:value-of select="$ex_content"/>');</script>
</xsl:template>

<!-- Include stylesheets, including orc.css if desired, and reveal/hide toggle Javascript -->
<xsl:template name="user.head.content">
<xsl:if test="$orc.demo">
<link rel="stylesheet" type="text/css" href="/orchard/orc.css"/>
</xsl:if>
<link rel="stylesheet" type="text/css" href="style.css"/>
<script type="text/javascript">
// Expandable content script from flooble.com.
// For more information please visit:
//   http://www.flooble.com/scripts/expand.php
// Copyright 2002 Animus Pactum Consulting Inc.
//----------------------------------------------
function toggle(link, divId) { var lText = link.innerHTML; var d = document.getElementById(divId);
  if (lText == '+') { link.innerHTML = '&#8722;'; d.style.display = 'block';}
  else { link.innerHTML = '+'; d.style.display = 'none';} }
</script>
</xsl:template>

<!--  Add site properties for the site library using the <sitepropset> tag -->
<xsl:template match="sitepropset">
	<table class="proptable">
		<tr>
    		<xsl:for-each select="siteprop">
    			<xsl:if test="@propname='blocking'">
    				<td bgcolor="#CC0000"><font color="#FFFFFF">Blocking</font></td>
    			</xsl:if>
    			<xsl:if test="@propname='nonblocking'">
    				<td bgcolor="#00CC00"><font color="#FFFFFF">Nonblocking</font></td>
    			</xsl:if>
    			<xsl:if test="@propname='idempotent'">
    				<td bgcolor="#0000CC"><font color="#FFFFFF">Idempotent</font></td>
    			</xsl:if>
    			<xsl:if test="@propname='pure'">
    				<td bgcolor="#6600FF"><font color="#FFFFFF">Pure</font></td>
    			</xsl:if>
			</xsl:for-each>
    	</tr>
	</table>
	<br/>
</xsl:template>

<!--
Customize syntax highlighting.
-->

<xsl:template match='xslthl:keyword' mode="xslthl">
  <span class="hl-keyword"><xsl:apply-templates mode="xslthl"/></span>
</xsl:template>

<xsl:template match='xslthl:comment' mode="xslthl">
  <span class="hl-comment"><xsl:apply-templates mode="xslthl"/></span>
</xsl:template>

<xsl:template match='xslthl:literal' mode="xslthl">
  <span class="hl-literal"><xsl:apply-templates mode="xslthl"/></span>
</xsl:template>

<xsl:template match='xslthl:combinator' mode="xslthl">
  <span class="hl-combinator"><xsl:apply-templates mode="xslthl"/></span>
</xsl:template>
<xsl:template match='xslthl:combinator[text()="FATBAR"]' mode="xslthl">
  <span class="hl-combinator">|</span>
</xsl:template>

<xsl:template match='xslthl:string' mode="xslthl">
  <span class="hl-string"><xsl:apply-templates mode="xslthl"/></span>
</xsl:template>

<xsl:template match='xslthl:variable' mode="xslthl">
  <span class="hl-variable"><xsl:apply-templates mode="xslthl"/></span>
</xsl:template>

<xsl:template match='xslthl:site' mode="xslthl">
  <span class="hl-site"><xsl:apply-templates mode="xslthl"/></span>
</xsl:template>

<xsl:template match='xslthl:doccomment' mode="xslthl">
  <span class="hl-comment"><xsl:apply-templates mode="xslthl"/></span>
</xsl:template>

</xsl:stylesheet>
