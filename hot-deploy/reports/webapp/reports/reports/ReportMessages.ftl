<#if jobResults?exists>
	<#assign jobResult = jobResults.get(0)>
	<#assign jobResultMessagesString = jobResult.getString("jobResultMessages")>
	<#assign reportMessages = Static["org.ofbiz.entity.serialize.XmlSerializer"].deserialize(jobResultMessagesString, delegator)>
	
	<table class="basic-table" cellspacing="0">
		<#-- Header Begins -->
		<tr class="header-row-2">
			<th>${uiLabelMap.ReportMessageSeverity}</th>
			<th>${uiLabelMap.ReportMessage}</th>
		</tr>
		<#-- Header Ends-->
		<#assign alt_row = false>
		<#list reportMessages as message>
			<tr valign="middle"<#if alt_row> class="alternate-row"</#if>>
				<td>${message.getSeverity().toString()}</td>
				<td>${message.getMessage()}</td>
			</tr>
		</#list>
	</table>
</#if>