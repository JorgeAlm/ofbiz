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
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;

public class ReportsServices {

	public static Map<String, Object> generateSaft(DispatchContext ctx, Map<String, ? extends Object> context) throws GenericEntityException, SerializeException, FileNotFoundException, IOException {
		Map<String, Object> result = FastMap.newInstance();
		String customTimePeriodId = context.get("timePeriod").toString();
		String taxAuthGeoId = context.get("taxAuthGeoId").toString();
		String postalAddressPurposeTypeId = context.get("postalAddressPurposeTypeId").toString();
		String phonePurposeTypeId = context.get("phonePurposeTypeId").toString();
		String faxPurposeTypeId = context.get("faxPurposeTypeId").toString();
		String emailPurposeTypeId = context.get("emailPurposeTypeId").toString();
		String websitePurposeTypeId = context.get("websitePurposeTypeId").toString();
		String jobName = context.get("jobName").toString();
		Delegator delegator = ctx.getDelegator();
		
		//GenericValue job = delegator.findOne("JobSandbox", false, );
		EntityListIterator eli = delegator.find("JobSandbox", EntityCondition.makeCondition(UtilMisc.toMap("jobName", jobName)), null, UtilMisc.toSet("jobId"), null, null);
		GenericValue job = eli.next();
		String jobId = job.getString("jobId");
		
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
				
		if(saftResult.getSuccess()){
			String jobResult = saftResult.getResult().toString();
			result.put("saftContent", jobResult);
			createJobResult(delegator, jobId, jobResult, saftResult.getMessages());
			result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
			return result;
		} else {
			createJobResult(delegator, jobId, null, saftResult.getMessages());
			return ServiceUtil.returnError("One or more errors occurred while generating the SAFT report, please check job result for details.");
		}
	}
	
	private static void createJobResult(Delegator delegator, String jobId, String jobResult, List<ReportMessage> jobResultMessages) throws GenericEntityException, SerializeException, FileNotFoundException, IOException{
		String jobSandboxResultId = delegator.getNextSeqId("JobSandboxResult");
		GenericValue jobSandboxResult = delegator.makeValue("JobSandboxResult");		
		jobSandboxResult.set("jobResultId", jobSandboxResultId);
		jobSandboxResult.set("jobId", jobId);
		jobSandboxResult.set("jobResult", jobResult);		
		
		delegator.create(jobSandboxResult);
		
		if(jobResultMessages != null && jobResultMessages.size() > 0){
			for(ReportMessage message : jobResultMessages){
				GenericValue jobSandboxResultMessage = delegator.makeValue("JobSandboxResultMsg");
				jobSandboxResultMessage.set("jobResultMsgId", delegator.getNextSeqId("JobSandboxResultMsg"));
				jobSandboxResultMessage.set("jobResultId", jobSandboxResultId);
				jobSandboxResultMessage.set("jobResultMsgSeverity", message.getSeverity().toString());
				jobSandboxResultMessage.set("jobResultMsgMap", message.getMessageMapName());
				if(message.getMessageParameters() != null && message.getMessageParameters().size() > 0){
					jobSandboxResultMessage.set("jobResultMsgParams", XmlSerializer.serialize(message.getMessageParameters()));
				}
				
				delegator.create(jobSandboxResultMessage);
			}
		}
		
		TransactionUtil.commit();
	}
	
	public static Map<String, Object> getJobStatusDisplayForSaft(DispatchContext ctx, Map<String, ? extends Object> context) throws GenericEntityException, SerializeException, FileNotFoundException, IOException {
		Map<String, Object> result = FastMap.newInstance();
		Delegator delegator = ctx.getDelegator();
		Locale locale = (Locale) context.get("locale");
		String jobId = context.get("jobId").toString();
		String jobStatusId = null;
		String jobStatusIdDisplay = null;
		
		GenericValue job = delegator.findOne("JobSandbox", false, UtilMisc.toMap("jobId", jobId));
		
		if(job == null){
			return ServiceUtil.returnError("Unable to find job.");
		} else {
			jobStatusId = job.getString("statusId");
			jobStatusIdDisplay = UtilProperties.getMessage("ReportsUiLabels", "SaftJobStatus_" + jobStatusId, locale);
		}
		
		if(UtilValidate.isEmpty(jobStatusId)){
			return ServiceUtil.returnError("Unable to find job status id display name.");
		} else {
			result.put("jobStatusId", jobStatusId);
			result.put("jobStatusIdDisplay", jobStatusIdDisplay);
			result.put(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
			return result;
		}
	}
}
