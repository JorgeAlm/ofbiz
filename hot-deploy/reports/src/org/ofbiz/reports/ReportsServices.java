package org.ofbiz.reports;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import javolution.util.FastMap;

import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
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

	public static Map<String, Object> GenerateSaft(DispatchContext ctx, Map<String, ? extends Object> context) throws GenericEntityException, SerializeException, FileNotFoundException, IOException {
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
		
		String messages = "";
		
		if(saftResult.getMessages().size() > 0){
			messages = XmlSerializer.serialize(saftResult.getMessages());
		}
				
		if(saftResult.getSuccess()){
			String jobResult = saftResult.getResult().toString();
			result.put("saftContent", jobResult);
			CreateJobResult(delegator, jobId, jobResult, messages);
			return result;
		} else {
			CreateJobResult(delegator, jobId, null, messages);
			return ServiceUtil.returnError("One or more errors occurred while generating the SAFT report, please check job result for details.");
		}
	}
	
	private static void CreateJobResult(Delegator delegator, String jobId, String jobResult, String jobResultMessages) throws GenericEntityException{
		GenericValue jobSandboxResult = delegator.makeValue("JobSandboxResult");		
		jobSandboxResult.set("jobResultId", delegator.getNextSeqId("JobSandboxResult"));
		jobSandboxResult.set("jobId", jobId);
		jobSandboxResult.set("jobResult", jobResult);
		jobSandboxResult.set("jobResultMessages", jobResultMessages);
		
		delegator.create(jobSandboxResult);
		TransactionUtil.commit();
	}
}
