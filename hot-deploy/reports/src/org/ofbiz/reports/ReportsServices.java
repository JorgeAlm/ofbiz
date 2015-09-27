package org.ofbiz.reports;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.serialize.SerializeException;
import org.ofbiz.entity.serialize.XmlSerializer;
import org.ofbiz.entity.transaction.TransactionUtil;
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
		
		GenericValue reportQueue = delegator.findOne("ReportQueue", false, UtilMisc.toMap("reportQueueId", reportQueueId));
		
		if(reportQueue == null){
			return ServiceUtil.returnError("Could not find report queue entry with id '" + reportQueueId + "'.");
		} else {
			String parameters = reportQueue.getString("reportQueueParams");
			String reportType = reportQueue.getString("reportTypeId");
			Map<String, Object> mapIn = FastMap.newInstance();
			
			if(!UtilValidate.isEmpty(parameters)){
				mapIn = (Map<String, Object>) XmlSerializer.deserialize(parameters, delegator);
			}
			
			mapIn.put("locale", context.get("locale"));
	        mapIn.put("userLogin", context.get("userLogin"));
	        mapIn.put("timeZone", context.get("timeZone"));
			
			if(reportType.compareTo("SAFTPT") == 0){
				try{
					reportQueue.set("reportQueueStatusId", "RUNNING");
					delegator.store(reportQueue);
					
					// will be running in an isolated transaction to prevent rollbacks
					Map<String, Object> result = dispatcher.runSync("generateSaft", mapIn, 300, true);
					
					if(result != null){	
						if(result.containsKey("saftContent")) {
							persistJobResult(delegator, reportQueue, result);
							reportQueue.set("reportQueueStatusId", "FINISHED");
							delegator.store(reportQueue);
						} else {
							persistJobResult(delegator, reportQueue, result);
							reportQueue.set("reportQueueStatusId", "FAILED");
							delegator.store(reportQueue);
						}
					}
				} catch(Exception e){
					reportQueue.set("reportQueueStatusId", "CRASHED");
					delegator.store(reportQueue);
					TransactionUtil.commit();
					Debug.logError("Error while trying to process saft queue item " + reportQueueId + ". Error Message: " + e.getMessage(), module);
					return ServiceUtil.returnError(e.getMessage());
				}
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
			saftResult = SaftGenerator.GenerateReport(delegator, customTimePeriodId, taxAuthGeoId, postalAddressPurposeTypeId,
					phonePurposeTypeId, faxPurposeTypeId, emailPurposeTypeId, websitePurposeTypeId);			
		} catch (GenericEntityException e) {
			// TODO Auto-generated catch block
			ServiceUtil.returnError(e.getMessage() + e.getStackTrace().toString());
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			ServiceUtil.returnError(e.getMessage() + e.getStackTrace().toString());
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
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
	
	private static void persistJobResult(Delegator delegator, GenericValue reportQueue, Map<String, Object> result) throws SerializeException, FileNotFoundException, IOException, GenericEntityException{
		String reportId = delegator.getNextSeqId("Report");
		GenericValue report = delegator.makeValue("Report");
		report.set("reportId", reportId);
		report.set("reportTypeId", reportQueue.getString("reportTypeId"));
		
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
