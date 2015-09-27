package org.ofbiz.reports;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

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
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.serialize.XmlSerializer;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.calendar.RecurrenceRule;

public class ReportsEvents {
	public static final String module = ReportsEvents.class.getName();

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
		String taxAuthGeoId = request.getParameter("taxAuthGeoId").toString();
		String postalAddressPurposeTypeId = request.getParameter("postalAddressPurposeTypeId").toString();
		String phonePurposeTypeId = request.getParameter("phonePurposeTypeId").toString();
		String faxPurposeTypeId = request.getParameter("faxPurposeTypeId").toString();
		String emailPurposeTypeId = request.getParameter("emailPurposeTypeId").toString();
		String websitePurposeTypeId = request.getParameter("websitePurposeTypeId").toString();

		Map<String, Object> mapIn = FastMap.newInstance();
		mapIn.put("timePeriod", customTimePeriodId);
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
		
		mapIn.clear();
		mapIn.put("locale", locale);
        mapIn.put("userLogin", userLogin);
        mapIn.put("timeZone", timeZone);
        mapIn.put("reportQueueId", reportQueueId);
        
        dispatcher.schedule("processReportQueue", mapIn, UtilDateTime.nowTimestamp().getTime());
        
		request.setAttribute("reportQueueId", reportQueueId);

		return "success";
	}
	
	public static String DownloadReport(HttpServletRequest request,
			HttpServletResponse response) throws ParserConfigurationException,
			TransformerException, GeneralException {
		LocalDispatcher dispatcher = (LocalDispatcher)request.getAttribute("dispatcher");
		Delegator delegator = dispatcher.getDelegator();
		
		String reportId = request.getParameter("reportId").toString();
		String xmlString = "";
		
		GenericValue job = delegator.findOne("Report", UtilMisc.toMap("reportId", reportId), false);
		
		xmlString = job.getString("reportData");
		
		// set the Xml content type
		response.setContentType("application/xml");
		response.setHeader("Content-Disposition",
				"attachment; filename=\"Report.xml\"");
		// jsonStr.length is not reliable for unicode characters
		try {
			response.setContentLength(xmlString.getBytes("UTF8").length);
		} catch (UnsupportedEncodingException e) {
			Debug.logError("Problems with XML encoding: " + e, module);
		}

		// return the JSON String
		ServletOutputStream out;
		try {
			out = response.getOutputStream();
			out.write(xmlString.getBytes());
			out.flush();
		} catch (IOException e) {
			Debug.logError(e, module);
		}
		
		return "success";
	}
}
