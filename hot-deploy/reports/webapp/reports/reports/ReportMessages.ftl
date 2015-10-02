<#if reportValidationMessages?exists>
	<table class="basic-table" cellspacing="0">
		<#-- Header Begins -->
		<tr class="header-row-2">
			<th>${uiLabelMap.ReportMessageSeverity}</th>
			<th>${uiLabelMap.ReportMessage}</th>
		</tr>
		<#-- Header Ends-->
		<#assign alt_row = false />
		<#list reportValidationMessages as message>
			<#assign messageMap = Static["org.ofbiz.entity.serialize.XmlSerializer"].deserialize(message.getString("messageParameters"), delegator) />
			<#assign messageDisplay = Static["org.ofbiz.base.util.UtilProperties"].getMessage("ReportsUiLabels", message.getString("messageMapName"), messageMap, locale) />
			<tr valign="middle"<#if alt_row> class="alternate-row"</#if>>
				<td>${uiLabelMap["ReportMessageSeverity_" + message.getString("messageSeverity")]}</td>
				<td>${messageDisplay}</td>
			</tr>
		</#list>
	</table>
</#if>