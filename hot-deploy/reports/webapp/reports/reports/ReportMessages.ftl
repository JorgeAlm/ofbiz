<#if jobResults?exists>
	<#if jobResultMessages?exists>
		<table class="basic-table" cellspacing="0">
			<#-- Header Begins -->
			<tr class="header-row-2">
				<th>${uiLabelMap.SaftReportMessageSeverity}</th>
				<th>${uiLabelMap.SaftReportMessage}</th>
			</tr>
			<#-- Header Ends-->
			<#assign alt_row = false />
			<#list jobResultMessages as message>
				<#assign messageMap = Static["org.ofbiz.entity.serialize.XmlSerializer"].deserialize(message.getString("jobResultMsgParams"), delegator) />
				<#assign messageDisplay = Static["org.ofbiz.base.util.UtilProperties"].getMessage("ReportsUiLabels", message.getString("jobResultMsgMap"), messageMap, locale) />
				<tr valign="middle"<#if alt_row> class="alternate-row"</#if>>
					<td>${uiLabelMap["SaftMessageSeverity_" + message.getString("jobResultMsgSeverity")]}</td>
					<td>${messageDisplay}</td>
				</tr>
			</#list>
		</table>
	</#if>
</#if>