package org.ofbiz.reports;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import javolution.util.FastMap;

import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.GeneralException;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilHttp;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilProperties;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.serialize.SerializeException;
import org.ofbiz.entity.serialize.XmlSerializer;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.ModelService;
import org.ofbiz.service.ServiceUtil;

public class ReportsEvents {
	public static final String module = ReportsEvents.class.getName();
	
	public static String feedbackSetter(HttpServletRequest request,
			HttpServletResponse response) {
		
		Locale locale = UtilHttp.getLocale(request);
		Object feedback = request.getParameter("feedback");
		if(UtilValidate.isNotEmpty(feedback)){
			request.setAttribute("_EVENT_MESSAGE_", UtilProperties.getMessage("ReportsUiLabels", feedback.toString(), locale));
		}
		
		return "success";
	}

	public static String queueSaftPtReport(HttpServletRequest request,
			HttpServletResponse response) throws ParserConfigurationException,
			TransformerException, GeneralException, FileNotFoundException, IOException {

		HttpSession session = request.getSession();
        GenericValue userLogin = (GenericValue)session.getAttribute("userLogin");
        LocalDispatcher dispatcher = (LocalDispatcher)request.getAttribute("dispatcher");
        Locale locale = UtilHttp.getLocale(request);
        TimeZone timeZone = UtilHttp.getTimeZone(request);
        Delegator delegator = dispatcher.getDelegator();
        Object customTimePeriodIdParameter = request.getParameter("timePeriod");
        
        if(UtilValidate.isEmpty(customTimePeriodIdParameter)){
			return "redirectToForm";
		}
        
		String customTimePeriodId = customTimePeriodIdParameter.toString();
		String reportName = request.getParameter("name").toString();
		String taxAuthGeoId = request.getParameter("taxAuthGeoId").toString();
		String postalAddressPurposeTypeId = request.getParameter("postalAddressPurposeTypeId").toString();
		String phonePurposeTypeId = request.getParameter("phonePurposeTypeId").toString();
		String faxPurposeTypeId = request.getParameter("faxPurposeTypeId").toString();
		String emailPurposeTypeId = request.getParameter("emailPurposeTypeId").toString();
		String websitePurposeTypeId = request.getParameter("websitePurposeTypeId").toString();

		Map<String, Object> mapIn = FastMap.newInstance();
		mapIn.put("timePeriod", customTimePeriodId);
		mapIn.put("name", reportName);
		mapIn.put("taxAuthGeoId", taxAuthGeoId);
		mapIn.put("postalAddressPurposeTypeId", postalAddressPurposeTypeId);
		mapIn.put("phonePurposeTypeId", phonePurposeTypeId);
		mapIn.put("faxPurposeTypeId", faxPurposeTypeId);
		mapIn.put("emailPurposeTypeId", emailPurposeTypeId);
		mapIn.put("websitePurposeTypeId", websitePurposeTypeId);
        
        String reportQueueId = delegator.getNextSeqId("ReportQueue");
		GenericValue reportQueue = delegator.makeValue("ReportQueue");
		reportQueue.put("reportQueueId", reportQueueId);
		reportQueue.put("reportTypeId", "SAFTPT");
		reportQueue.put("reportQueueStatusId", "QUEUED");
		reportQueue.put("reportQueueParams", XmlSerializer.serialize(mapIn));
		
		delegator.create(reportQueue);
		
		Map<String, Object> serviceMapIn = FastMap.newInstance();
		serviceMapIn.put("locale", locale);
		serviceMapIn.put("userLogin", userLogin);
		serviceMapIn.put("timeZone", timeZone);
		serviceMapIn.put("reportQueueId", reportQueueId);
        
        dispatcher.schedule("processReportQueue", serviceMapIn, UtilDateTime.nowTimestamp().getTime());
        
		request.setAttribute("reportQueueId", reportQueueId);

		return "success";
	}
	
	public static String getReportStatusDisplayForSaft(HttpServletRequest request,
			HttpServletResponse response) throws GenericEntityException, SerializeException, FileNotFoundException, IOException {
		Delegator delegator = (Delegator) request.getAttribute("delegator");
		Locale locale = UtilHttp.getLocale(request);
		String reportQueueId = request.getParameter("reportQueueId").toString();
		String reportQueueStatusId = null;
		String reportQueueStatusIdDisplay = null;
		
		GenericValue reportQueue = delegator.findOne("ReportQueue", false, UtilMisc.toMap("reportQueueId", reportQueueId));
		
		if(reportQueue == null){
			request.setAttribute("_ERROR_MESSAGE_", "Unable to find job.");
            return "error";
		} else {
			reportQueueStatusId = reportQueue.getString("reportQueueStatusId");
			reportQueueStatusIdDisplay = UtilProperties.getMessage("ReportsUiLabels", "ReportQueueStatus_" + reportQueueStatusId, locale);
		}
		
		if(UtilValidate.isEmpty(reportQueueStatusId)){
			request.setAttribute("_ERROR_MESSAGE_", "Unable to find job status id display name.");
            return "error";
		} else {
			request.setAttribute("reportQueueStatusId", reportQueueStatusId);
			request.setAttribute("reportQueueStatusIdDisplay", reportQueueStatusIdDisplay);
			request.setAttribute(ModelService.RESPONSE_MESSAGE, ModelService.RESPOND_SUCCESS);
		}
		
		return "success";
	}
	
	public static String downloadReport(HttpServletRequest request,
			HttpServletResponse response) throws ParserConfigurationException,
			TransformerException, GeneralException {
		LocalDispatcher dispatcher = (LocalDispatcher)request.getAttribute("dispatcher");
		Delegator delegator = dispatcher.getDelegator();
		
		String reportId = request.getParameter("reportId").toString();
		String output = "";
		
		GenericValue job = delegator.findOne("Report", UtilMisc.toMap("reportId", reportId), false);
		GenericValue reportType = job.getRelatedOne("ReportType", false);
		
		output = job.getString("reportData");
		
		if(UtilValidate.isEmpty(output)){
			output = job.getString("reportDataBin");
		}
		
		// set the content type
		response.setContentType(reportType.getString("reportContentType"));
		response.setHeader("Content-Disposition", "attachment; filename=\"" + reportType.getString("defaultFileName") + "\"");
		
		// jsonStr.length is not reliable for unicode characters
		try {
			response.setContentLength(output.getBytes("UTF8").length);
		} catch (UnsupportedEncodingException e) {
			Debug.logError("Problems with encoding: " + e, module);
		}

		// return the XML String
		ServletOutputStream out;
		try {
			out = response.getOutputStream();
			out.write(output.getBytes("UTF8"));
			out.flush();
		} catch (IOException e) {
			Debug.logError(e, module);
		}
		
		return "success";
	}
}
