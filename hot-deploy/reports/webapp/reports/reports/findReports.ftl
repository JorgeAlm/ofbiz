<#assign reportTypeId = parameters.reportTypeId?default("")>
<#assign sortField = parameters.sortField?if_exists/>

<#-- Only allow the search fields to be hidden when we have some results -->
<#if reportsList?has_content>
	<#assign hideFields = parameters.hideFields?default("N")>
<#else>
	<#assign hideFields = "N">
</#if>

<h1>${uiLabelMap.PageTitleFindReports}</h1>

<div class="screenlet">
	<div class="screenlet-title-bar">
		<#if reportsList?has_content>
			<ul>
				<#if hideFields == "Y">
					<li class="collapsed"><a href="<@ofbizUrl>FindReports?hideFields=N&sortField=${sortField?if_exists}${paramList}</@ofbizUrl>" title="${uiLabelMap.CommonShowLookupFields}">&nbsp;</a></li>
				<#else>
					<li class="expanded"><a href="<@ofbizUrl>FindReports?hideFields=Y&sortField=${sortField?if_exists}${paramList}</@ofbizUrl>" title="${uiLabelMap.CommonHideFields}">&nbsp;</a></li>
				</#if>
			</ul>
			<br class="clear"/>
		</#if>
	</div>

	<div class="screenlet-body">
		<div id="findReportsParameters" <#if hideFields != "N"> style="display:none" </#if> >
			<h2>${uiLabelMap.CommonSearchOptions}</h2>
			<form method="post" name="lookupreports" action="<@ofbizUrl>FindReports</@ofbizUrl>" class="basic-form">
				<input type="hidden" name="hideFields" value="Y"/>
				<table class="basic-table" cellspacing="0">
					<tr>
						<td class="label">${uiLabelMap.ReportType}</td>
						<td>
							<select name="reportTypeId">
								<#if currentReportType?has_content>
									<option value="${currentReportType.reportTypeId}">${currentReportType.get("description", locale)}</option>
									<option value="${currentReportType.reportTypeId}">---</option>
								</#if>
								<option value="ANY">${uiLabelMap.CommonAnyReportType}</option>
								<#list reportTypes as reportType>
									<option value="${reportType.reportTypeId}">${reportType.get("description", locale)}</option>
								</#list>
							</select>
						</td>
					</tr>
					<tr>
						<td class="label">${uiLabelMap.ReportName}</td>
						<td><input type="text" name="reportName", value="${parameters.reportName?if_exists}" /></td>
					</tr>
					<tr>
            			<td>&nbsp;</td>
            			<td>
              				<input type="submit" value="${uiLabelMap.CommonFind}" onclick="javascript:document.lookupparty.submit();"/>
            			</td>
          			</tr>
				</table>
			</form>
	</div>

	<#if reportsList?exists>
		<#if hideFields != "Y">
			<hr />
		</#if>
		<div id="findReportsResults">
			<h2>${uiLabelMap.CommonSearchResults}</h2>
		</div>
		<#if reportsList?has_content>
			<#-- Pagination -->
			<#include "component://common/webcommon/includes/htmlTemplate.ftl"/>
			<#assign commonUrl = "FindReports?hideFields=" + hideFields + paramList + "&sortField=" + sortField?if_exists + "&"/>
			<#assign viewIndexFirst = 0/>
			<#assign viewIndexPrevious = viewIndex - 1/>
			<#assign viewIndexNext = viewIndex + 1/>
			<#assign viewIndexLast = Static["org.ofbiz.base.util.UtilMisc"].getViewLastIndex(reportsListSize, viewSize) />
			<#assign messageMap = Static["org.ofbiz.base.util.UtilMisc"].toMap("lowCount", lowIndex, "highCount", highIndex, "total", reportsListSize)/>
			<#assign commonDisplaying = Static["org.ofbiz.base.util.UtilProperties"].getMessage("CommonUiLabels", "CommonDisplaying", messageMap, locale)/>
			<@nextPrev commonUrl=commonUrl ajaxEnabled=false javaScriptEnabled=false paginateStyle="nav-pager" paginateFirstStyle="nav-first" viewIndex=viewIndex highIndex=highIndex listSize=reportsListSize viewSize=viewSize ajaxFirstUrl="" firstUrl="" paginateFirstLabel="" paginatePreviousStyle="nav-previous" ajaxPreviousUrl="" previousUrl="" paginatePreviousLabel="" pageLabel="" ajaxSelectUrl="" selectUrl="" ajaxSelectSizeUrl="" selectSizeUrl="" commonDisplaying=commonDisplaying paginateNextStyle="nav-next" ajaxNextUrl="" nextUrl="" paginateNextLabel="" paginateLastStyle="nav-last" ajaxLastUrl="" lastUrl="" paginateLastLabel="" paginateViewSizeLabel="" />
			<table class="basic-table hover-bar" cellspacing="0">
				<tr class="header-row-2">
					<td>
						<a href="<@ofbizUrl>FindReports</@ofbizUrl>?<#if sortField?has_content><#if sortField == "reportId">sortField=-reportId<#elseif sortField == "-reportId">sortField=createdDate<#else>sortField=reportId</#if><#else>sortField=reportId</#if>${paramList?if_exists}&VIEW_SIZE=${viewSize?if_exists}&VIEW_INDEX=${viewIndex?if_exists}" 
							<#if sortField?has_content><#if sortField == "reportId">class="sort-order-desc"<#elseif sortField == "-reportId">class="sort-order-asc"<#else>class="sort-order"</#if><#else>class="sort-order"</#if>>${uiLabelMap.ReportTable_reportId}
						</a>
					</td>
					<td>
						<a href="<@ofbizUrl>FindReports</@ofbizUrl>?<#if sortField?has_content><#if sortField == "description">sortField=-description<#elseif sortField == "-description">sortField=description<#else>sortField=description</#if><#else>sortField=description</#if>${paramList?if_exists}&VIEW_SIZE=${viewSize?if_exists}&VIEW_INDEX=${viewIndex?if_exists}" 
							<#if sortField?has_content><#if sortField == "description">class="sort-order-desc"<#elseif sortField == "-description">class="sort-order-asc"<#else>class="sort-order"</#if><#else>class="sort-order"</#if>>${uiLabelMap.ReportTable_reportTypeDescription}
						</a>
					</td>
					<td>
						<a href="<@ofbizUrl>FindReports</@ofbizUrl>?<#if sortField?has_content><#if sortField == "reportName">sortField=-reportName<#elseif sortField == "-reportName">sortField=reportName<#else>sortField=reportName</#if><#else>sortField=reportName</#if>${paramList?if_exists}&VIEW_SIZE=${viewSize?if_exists}&VIEW_INDEX=${viewIndex?if_exists}" 
							<#if sortField?has_content><#if sortField == "reportName">class="sort-order-desc"<#elseif sortField == "-reportName">class="sort-order-asc"<#else>class="sort-order"</#if><#else>class="sort-order"</#if>>${uiLabelMap.ReportTable_reportName}
						</a>
					</td>
					<td>
						<a href="<@ofbizUrl>FindReports</@ofbizUrl>?<#if sortField?has_content><#if sortField == "createdTxStamp">sortField=-createdTxStamp<#elseif sortField == "-createdTxStamp">sortField=createdTxStamp<#else>sortField=createdTxStamp</#if><#else>sortField=createdTxStamp</#if>${paramList?if_exists}&VIEW_SIZE=${viewSize?if_exists}&VIEW_INDEX=${viewIndex?if_exists}" 
							<#if sortField?has_content><#if sortField == "createdTxStamp">class="sort-order-desc"<#elseif sortField == "-createdTxStamp">class="sort-order-asc"<#else>class="sort-order"</#if><#else>class="sort-order"</#if>>${uiLabelMap.ReportTable_createdTxStamp}
						</a>
					</td>
					<td>${uiLabelMap.ReportTable_actions}</td>
				</tr>
				<#assign alt_row = false>
				<#assign rowCount = 0>
				<#list reportsList as reportRow>
					<#assign reportHasMessages = true />
					<#assign reportHasData = reportRow.getString("reportData")?has_content || reportRow.getString("reportDataBin")?has_content />    
					<tr valign="middle"<#if alt_row> class="alternate-row"</#if>>
						<td><a href="<@ofbizUrl>ReportDetails?reportId=${reportRow.reportId}</@ofbizUrl>">${reportRow.reportId}</a></td>
						<td>${reportRow.description}</td>
						<td><#if reportRow.reportName?has_content>${reportRow.reportName}</#if></td>
						<td>${reportRow.createdTxStamp}</td>
						<td class="button-col align-float">
							<a href="/reports/control/ReportDetails?reportId=${reportRow.reportId}">${uiLabelMap.ReportDetails}</a>
							<a href="/reports/control/DeleteReport?reportId=${reportRow.reportId}">${uiLabelMap.ReportCommonDelete}</a>
							<#if reportHasData>
								<a href="/reports/control/DownloadReport?reportId=${reportRow.reportId}">${uiLabelMap.ReportCommonDownload}</a>
							</#if>
						</td>
					</tr>
					<#assign rowCount = rowCount + 1>
					<#-- toggle the row color -->
					<#assign alt_row = !alt_row>
				</#list>
			</table>
		<#else>
			<div id="findReportsResults_2">
				<h3>${uiLabelMap.ReportsNoReportsFound}</h3>
			</div>
		</#if>
		<#if lookupErrorMessage?exists>
			<h3>${lookupErrorMessage}</h3>
		</#if>
	</#if>
</div>