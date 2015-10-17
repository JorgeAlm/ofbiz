package org.ofbiz.reports;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilGenerics;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityFunction;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.model.DynamicViewEntity;
import org.ofbiz.entity.model.ModelKeyMap;
import org.ofbiz.entity.serialize.SerializeException;
import org.ofbiz.entity.serialize.XmlSerializer;
import org.ofbiz.entity.transaction.TransactionUtil;
import org.ofbiz.entity.util.EntityFindOptions;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.reports.ReportResult;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.GenericServiceException;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;
import org.xml.sax.SAXException;

public class ReportsServices {
	
	public static final String module = ReportsServices.class.getName();
	
	public static Map<String, Object> processReportQueue(DispatchContext ctx, Map<String, ? extends Object> context) throws GenericEntityException, SerializeException, SAXException, ParserConfigurationException, IOException, GenericServiceException{
		Delegator delegator = ctx.getDelegator();
		LocalDispatcher dispatcher = ctx.getDispatcher();
		String reportQueueId = context.get("reportQueueId").toString();
		GenericValue userLogin = (GenericValue)context.get("userLogin");
		String reportName = null;
		
		GenericValue reportQueue = delegator.findOne("ReportQueue", false, UtilMisc.toMap("reportQueueId", reportQueueId));
		
		if(reportQueue == null){
			return ServiceUtil.returnError("Could not find report queue entry with id '" + reportQueueId + "'.");
		} else {
			String parameters = reportQueue.getString("reportQueueParams");
			String reportTypeId = reportQueue.getString("reportTypeId");
			Map<String, Object> mapIn = FastMap.newInstance();
			
			if(!UtilValidate.isEmpty(parameters)){
				mapIn = (Map<String, Object>) XmlSerializer.deserialize(parameters, delegator);
			}
			
			if(mapIn.containsKey("name")){
				reportName = mapIn.get("name").toString();
				mapIn.remove("name");
			}
			
			mapIn.put("locale", context.get("locale"));
	        mapIn.put("userLogin", userLogin);
	        mapIn.put("timeZone", context.get("timeZone"));
			
	        GenericValue reportType = delegator.findOne("ReportType", false, UtilMisc.toMap("reportTypeId", reportTypeId));
	        
			try{
				reportQueue.set("reportQueueStatusId", "RUNNING");
				delegator.store(reportQueue);
				
				Map<String, Object> result = dispatcher.runSync(reportType.getString("serviceName"), mapIn, 300, true);
				
				if(result != null){	
					Boolean beganTransaction = TransactionUtil.begin();
					
					if(result.containsKey("saftContent")) {
						persistJobResult(delegator, reportName, userLogin, reportQueue, result);
						reportQueue.set("reportQueueStatusId", "FINISHED");
						delegator.store(reportQueue);
					} else {
						persistJobResult(delegator, reportName, userLogin, reportQueue, result);
						reportQueue.set("reportQueueStatusId", "FAILED");
						delegator.store(reportQueue);
					}
					
					TransactionUtil.commit(beganTransaction);
				}
			} catch(Exception e){
				Boolean beganTransaction = TransactionUtil.begin();
				
				reportQueue.set("reportQueueStatusId", "CRASHED");
				delegator.store(reportQueue);

				TransactionUtil.commit(beganTransaction);
				
				Debug.logError("Error while trying to process saft queue item " + reportQueueId + ". Error Message: " + e.getMessage(), module);
				return ServiceUtil.returnError(e.getMessage());
			}
			
			return ServiceUtil.returnSuccess();
		}
	}

	public static Map<String, Object> generateSaft(DispatchContext ctx, Map<String, ? extends Object> context) throws GenericEntityException, SerializeException, FileNotFoundException, IOException {
		Map<String, Object> result = FastMap.newInstance();
		String customTimePeriodId = context.get("timePeriod").toString();
		String taxAuthGeoId = context.get("taxAuthGeoId").toString();
		String postalAddressPurposeTypeId = context.get("postalAddressPurposeTypeId").toString();
		String phonePurposeTypeId = context.get("phonePurposeTypeId").toString();
		String faxPurposeTypeId = context.get("faxPurposeTypeId").toString();
		String emailPurposeTypeId = context.get("emailPurposeTypeId").toString();
		String websitePurposeTypeId = context.get("websitePurposeTypeId").toString();
		Delegator delegator = ctx.getDelegator();
		
		ReportResult saftResult = null;
		try {
			saftResult = SaftGenerator.generateReport(delegator, customTimePeriodId, taxAuthGeoId, postalAddressPurposeTypeId,
					phonePurposeTypeId, faxPurposeTypeId, emailPurposeTypeId, websitePurposeTypeId);			
		} catch (GenericEntityException e) {
			ServiceUtil.returnError(e.getMessage() + e.getStackTrace().toString());
		} catch (ParserConfigurationException e) {
			ServiceUtil.returnError(e.getMessage() + e.getStackTrace().toString());
		} catch (TransformerException e) {
			ServiceUtil.returnError(e.getMessage() + e.getStackTrace().toString());
		}
				
		if(saftResult.getMessages() != null && saftResult.getMessages().size() > 0){
			result.put("saftMessages", saftResult.getMessages());
		}
		
		if(saftResult.getSuccess()){
			result.put("saftContent", saftResult.getResult().toString());
		}
		
		return result;
	}
	
	public static Map<String, Object> getReportStatusDisplayForSaft(DispatchContext ctx, Map<String, ? extends Object> context) throws GenericEntityException, SerializeException, FileNotFoundException, IOException {
		Map<String, Object> result = FastMap.newInstance();
		Delegator delegator = ctx.getDelegator();
		Locale locale = (Locale) context.get("locale");
		String reportQueueId = context.get("reportQueueId").toString();
		String reportQueueStatusId = null;
		String reportQueueStatusIdDisplay = null;
		
		GenericValue reportQueue = delegator.findOne("ReportQueue", false, UtilMisc.toMap("reportQueueId", reportQueueId));
		
		if(reportQueue == null){
			return ServiceUtil.returnError("Unable to find job.");
		} else {
			reportQueueStatusId = reportQueue.getString("reportQueueStatusId");
			reportQueueStatusIdDisplay = UtilProperties.getMessage("ReportsUiLabels", "ReportQueueStatus_" + reportQueueStatusId, locale);
		}
		
		if(UtilValidate.isEmpty(reportQueueStatusId)){
			return ServiceUtil.returnError("Unable to find job status id display name.");
		} else {
			result.put("reportQueueStatusId", reportQueueStatusId);
			result.put("reportQueueStatusIdDisplay", reportQueueStatusIdDisplay);
			result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
			return result;
		}
	}
	
	public static Map<String, Object> findReports(DispatchContext dctx, Map<String, ? extends Object> context) {
        Map<String, Object> result = ServiceUtil.returnSuccess();
        Delegator delegator = dctx.getDelegator();
        GenericValue userLogin = (GenericValue) context.get("userLogin");
        Locale locale = (Locale) context.get("locale");
        String resource = "ReportsUiLabels";

        // get the report types
        try {
            List<GenericValue> reportTypes = delegator.findList("ReportType", null, null, UtilMisc.toList("description"), null, false);
            result.put("reportTypes", reportTypes);
        } catch (GenericEntityException e) {
            String errMsg = "Error looking up ReportTypes: " + e.toString();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                    "RpoertsLookupReportTypeError",
                    UtilMisc.toMap("errMessage", e.toString()), locale));
        }

        // current report type
        String reportTypeId = null;
        if(context.containsKey("reportTypeId")){
	        try {
	        	reportTypeId = (String) context.get("reportTypeId");
	            if (UtilValidate.isNotEmpty(reportTypeId)) {
	                GenericValue currentReportType = delegator.findOne("ReportType", UtilMisc.toMap("reportTypeId", reportTypeId), true);
	                result.put("currentReportType", currentReportType);
	            }
	        } catch (GenericEntityException e) {
	            String errMsg = "Error looking up current ReportType: " + e.toString();
	            Debug.logError(e, errMsg, module);
	            return ServiceUtil.returnError(UtilProperties.getMessage(resource,
	                    "ReportsLookupReportTypeError",
	                    UtilMisc.toMap("errMessage", e.toString()), locale));
	        }
        }
        
        // report name
        String reportName = null;
        if(context.containsKey("reportName")){
        	reportName = (String) context.get("reportName");
        }

        // set the page parameters
        int viewIndex = 0;
        try {
            viewIndex = Integer.parseInt((String) context.get("VIEW_INDEX"));
        } catch (Exception e) {
            viewIndex = 0;
        }
        result.put("viewIndex", Integer.valueOf(viewIndex));

        int viewSize = 20;
        try {
            viewSize = Integer.parseInt((String) context.get("VIEW_SIZE"));
        } catch (Exception e) {
            viewSize = 20;
        }
        result.put("viewSize", Integer.valueOf(viewSize));

        // blank param list
        String paramList = "";

        List<GenericValue> reportsList = null;
        int reportsListSize = 0;
        int lowIndex = 0;
        int highIndex = 0;

        String showAll = (context.get("showAll") != null ? (String) context.get("showAll") : "N");
        paramList = paramList + "&showAll=" + showAll;

        // create the dynamic view entity
        DynamicViewEntity dynamicView = new DynamicViewEntity();

        // default view settings
        dynamicView.addMemberEntity("RP", "Report");
        dynamicView.addAlias("RP", "reportId");
        dynamicView.addAlias("RP", "reportTypeId");
        dynamicView.addAlias("RP", "reportName");
        dynamicView.addAlias("RP", "reportData");
        dynamicView.addAlias("RP", "reportDataBin");
        dynamicView.addAlias("RP", "createdTxStamp");
        dynamicView.addAlias("RP", "createdByUserLogin");
        
        dynamicView.addMemberEntity("RPT", "ReportType");
        dynamicView.addAlias("RPT", "description");
        dynamicView.addViewLink("RP", "RPT", Boolean.FALSE, ModelKeyMap.makeKeyMapList("reportTypeId", "reportTypeId"));
        //dynamicView.addRelation("many", "", "UserLogin", ModelKeyMap.makeKeyMapList("partyId"));

        // define the main condition & expression list
        List<EntityCondition> andExprs = FastList.newInstance();
        EntityCondition mainCond = null;

        List<String> orderBy = FastList.newInstance();
        List<String> fieldsToSelect = FastList.newInstance();
        // fields we need to select; will be used to set distinct
        fieldsToSelect.add("reportId");
        fieldsToSelect.add("reportTypeId");
        fieldsToSelect.add("reportName");
        fieldsToSelect.add("reportData");
        fieldsToSelect.add("reportDataBin");
        fieldsToSelect.add("description");
        fieldsToSelect.add("createdTxStamp");
        fieldsToSelect.add("createdByUserLogin");

        // check for a reportTypeId
        if (UtilValidate.isNotEmpty(reportTypeId) && !reportTypeId.equals("ANY")) {
            paramList = paramList + "&reportTypeId=" + reportTypeId;
            andExprs.add(EntityCondition.makeCondition("reportTypeId", EntityOperator.EQUALS, reportTypeId));
        }
        
        // check for reportName
        if (UtilValidate.isNotEmpty(reportName)){
        	paramList = paramList + "&reportName=" + reportName;
        	andExprs.add(EntityCondition.makeCondition(EntityFunction.UPPER_FIELD("reportName"), EntityOperator.LIKE, EntityFunction.UPPER("%"+reportName+"%")));
        }
        
        // filter userLogin
        andExprs.add(EntityCondition.makeCondition("createdByUserLogin", EntityOperator.EQUALS, userLogin.getString("userLoginId")));

        // build the main condition
        if (andExprs.size() > 0) mainCond = EntityCondition.makeCondition(andExprs, EntityOperator.AND);

        Debug.logInfo("In findReports mainCond=" + mainCond, module);

        String sortField = (String) context.get("sortField");
        if(UtilValidate.isNotEmpty(sortField)){
            orderBy.add(sortField);
        } else {
        	orderBy.add("-createdTxStamp");
        }
        
        // do the lookup
        try {
            // get the indexes for the partial list
            lowIndex = viewIndex * viewSize + 1;
            highIndex = (viewIndex + 1) * viewSize;

            // set distinct on so we only get one row per order
            EntityFindOptions findOpts = new EntityFindOptions(true, EntityFindOptions.TYPE_SCROLL_INSENSITIVE, EntityFindOptions.CONCUR_READ_ONLY, -1, highIndex, false);
            // using list iterator
            EntityListIterator pli = delegator.findListIteratorByCondition(dynamicView, mainCond, null, fieldsToSelect, orderBy, findOpts);

            // get the partial list for this page
            reportsList = pli.getPartialList(lowIndex, viewSize);

            // attempt to get the full size
            reportsListSize = pli.getResultsSizeAfterPartialList();
            if (highIndex > reportsListSize) {
                highIndex = reportsListSize;
            }

            // close the list iterator
            pli.close();
        } catch (GenericEntityException e) {
            String errMsg = "Failure in report find operation, rolling back transaction: " + e.toString();
            Debug.logError(e, errMsg, module);
            return ServiceUtil.returnError(UtilProperties.getMessage(resource,
                    "ReportLookupPartyError",
                    UtilMisc.toMap("errMessage", e.toString()), locale));
        }

        if (reportsList == null) reportsList = FastList.newInstance();
        result.put("reportsList", reportsList);
        result.put("reportsListSize", Integer.valueOf(reportsListSize));
        result.put("paramList", paramList);
        result.put("highIndex", Integer.valueOf(highIndex));
        result.put("lowIndex", Integer.valueOf(lowIndex));

        return result;
    }
	
	private static void persistJobResult(Delegator delegator, String reportName, GenericValue userLogin, GenericValue reportQueue, Map<String, Object> result) throws SerializeException, FileNotFoundException, IOException, GenericEntityException{
		String reportId = delegator.getNextSeqId("Report");
		GenericValue report = delegator.makeValue("Report");
		report.set("reportId", reportId);
		report.set("reportTypeId", reportQueue.getString("reportTypeId"));
		if(UtilValidate.isNotEmpty(reportName)){
			report.set("reportName", reportName);
		}
		report.set("createdByUserLogin", userLogin.getString("userLoginId"));
		
		if(result.containsKey("saftContent")){
			report.set("reportData", result.get("saftContent").toString());
		}
		
		delegator.create(report);
		reportQueue.set("reportId", reportId);
		
		if(result.containsKey("saftMessages")){
			List<ReportMessage> reportMessages = (List<ReportMessage>)result.get("saftMessages");
			
			for(ReportMessage message : reportMessages){
				GenericValue reportValidationMessage = delegator.makeValue("ReportValidationMsgs");
				reportValidationMessage.set("messageId", delegator.getNextSeqId("ReportValidationMsgs"));
				reportValidationMessage.set("reportId", reportId);
				reportValidationMessage.set("messageSeverity", message.getSeverity().toString());
				reportValidationMessage.set("messageMapName", message.getMessageMapName());
				if(message.getMessageParameters() != null && message.getMessageParameters().size() > 0){
					reportValidationMessage.set("messageParameters", XmlSerializer.serialize(message.getMessageParameters()));
				}
				
				delegator.create(reportValidationMessage);
			}
		}
	}
}
