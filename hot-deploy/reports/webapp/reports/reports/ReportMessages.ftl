<#if jobResults?exists>
	<#assign jobResult = jobResults.get(0)>
	<#assign jobResultMessagesString = jobResult.getString("jobResultMessages")>
	<#if jobResultMessagesString?exists && jobResultMessagesString?has_content>
		<#assign reportMessages = Static["org.ofbiz.entity.serialize.XmlSerializer"].deserialize(jobResultMessagesString, delegator)>
	</#if>
	
	<#if reportMessages?exists>
		<table class="basic-table" cellspacing="0">
			<#-- Header Begins -->
			<tr class="header-row-2">
				<th>${uiLabelMap.SaftReportMessageSeverity}</th>
				<th>${uiLabelMap.SaftReportMessage}</th>
			</tr>
			<#-- Header Ends-->
			<#assign alt_row = false>
			<#list reportMessages as message>
				<tr valign="middle"<#if alt_row> class="alternate-row"</#if>>
					<td>${uiLabelMap["SaftMessageSeverity_" + message.getSeverity().toString()]}</td>
					<td>${message.getMessage()}</td>
				</tr>
			</#list>
		</table>
	</#if>
</#if>