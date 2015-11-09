package org.ofbiz.reports;

import java.io.StringWriter;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.ofbiz.base.util.StringUtil;
import org.ofbiz.base.util.UtilDateTime;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.base.util.UtilValidate;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityJoinOperator;
import org.ofbiz.entity.condition.EntityOperator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

public class SaftGenerator {

	public static final String module = SaftGenerator.class.getName();
	private static final String defaultTaxAuthGeoId = "PRT";
	private static final String defaultPostalAddressPurposeType = "BILLING_LOCATION";
	private static final String defaultPhonePurposeType = "PRIMARY_PHONE";
	private static final String defaultFaxPurposeType = "FAX_NUMBER";
	private static final String defaultEmailPurposeType = "PRIMARY_EMAIL";
	private static final String defaultWebsitePurposeType = "PRIMARY_WEB_URL";
	
	public static ReportResult generateReport(Delegator delegator, String customTimePeriodId,
			String taxAuthGeoId, String postalAddressPurposeType, String phonePurposeType, String faxPurposeType, 
			String emailPurposeType, String websitePurposeType) throws ParserConfigurationException, GenericEntityException, TransformerException{
		
		ReportResult result = new ReportResult();
		
		if(UtilValidate.isEmpty(taxAuthGeoId)){
			taxAuthGeoId = defaultTaxAuthGeoId;
		}
		
		if(UtilValidate.isEmpty(postalAddressPurposeType)){
			postalAddressPurposeType = defaultPostalAddressPurposeType;
		}
		
		if(UtilValidate.isEmpty(phonePurposeType)){
			phonePurposeType = defaultPhonePurposeType;
		}
		
		if(UtilValidate.isEmpty(faxPurposeType)){
			faxPurposeType = defaultFaxPurposeType;
		}
		
		if(UtilValidate.isEmpty(emailPurposeType)){
			emailPurposeType = defaultEmailPurposeType;
		}
		
		if(UtilValidate.isEmpty(websitePurposeType)){
			websitePurposeType = defaultWebsitePurposeType;
		}
		
		DocumentBuilderFactory docFactory = DocumentBuilderFactory
				.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

		// root elements
		Document doc = docBuilder.newDocument();
		Element rootElement = doc.createElement("AuditFile");
		rootElement.setAttribute("xmlns", "urn:OECD:StandardAuditFile-Tax:PT_1.03_01");
		
		// Retrieve Time Period Information
		Map<String, String> timePeriodFilter = FastMap.newInstance();
		timePeriodFilter.put("customTimePeriodId", customTimePeriodId);
		GenericValue customTimePeriod = delegator.findOne("CustomTimePeriod", timePeriodFilter, false);
		
		if(customTimePeriod == null){
			Map<String, String> messageParams = FastMap.newInstance();
			messageParams.put("customTimePeriodId", customTimePeriodId);
			result.addMessage("SaftUnableToFindTimePeriod", messageParams, ReportMessageSeverity.Error);
			return result;
		}
		
		Date startDate = customTimePeriod.getDate("fromDate");
		Date endDate = customTimePeriod.getDate("thruDate");
		Timestamp startTimestamp = new Timestamp(startDate.getTime());
		Timestamp endTimestamp = new Timestamp(endDate.getTime());
		String organizationPartyId = customTimePeriod.getString("organizationPartyId");
		int comparisonValue = startDate.compareTo(endDate);
		
		if(comparisonValue == 0){
			Map<String, String> messageParams = FastMap.newInstance();
			messageParams.put("customTimePeriodId", customTimePeriodId);
			messageParams.put("startDate", startDate.toString());
			messageParams.put("endDate", endDate.toString());
			result.addMessage("SaftFiscalYearStartDateEqualsEndDate", messageParams, ReportMessageSeverity.Error);
			return result;
		} else if (comparisonValue > 0){
			Map<String, String> messageParams = FastMap.newInstance();
			messageParams.put("customTimePeriodId", customTimePeriodId);
			messageParams.put("startDate", startDate.toString());
			messageParams.put("endDate", endDate.toString());
			result.addMessage("SaftFiscalYearStartDateAfterEndDate", messageParams, ReportMessageSeverity.Error);
			return result;
		}

		ReportResult headerResult = generateHeader(doc, delegator, startTimestamp, endTimestamp, organizationPartyId, taxAuthGeoId, 
				postalAddressPurposeType, phonePurposeType, faxPurposeType, emailPurposeType, websitePurposeType);
		rootElement.appendChild((Element)headerResult.getResult());
		if(headerResult.getMessages().size() > 0){
			result.addAll(headerResult.getMessages());
		}
		
		Element masterFilesElement = doc.createElement("MasterFiles");
		List<ReportMessage> customersMessages = GenerateCustomers(doc, masterFilesElement, delegator, startTimestamp, endTimestamp, taxAuthGeoId, 
				postalAddressPurposeType, phonePurposeType, faxPurposeType, emailPurposeType, websitePurposeType);
		rootElement.appendChild(masterFilesElement);
		if(customersMessages.size() > 0 ){
			result.addAll(customersMessages);
		}

		doc.appendChild(rootElement);

		// set up a transformer
		TransformerFactory transfac = TransformerFactory.newInstance();
		Transformer trans = transfac.newTransformer();
		trans.setOutputProperty(OutputKeys.INDENT, "yes");
		trans.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

		// create string from xml tree
		StringWriter sw = new StringWriter();
		StreamResult sr = new StreamResult(sw);
		DOMSource source = new DOMSource(doc);
		trans.transform(source, sr);
		
		result.setResult(sw.toString());
		return result;
	}
	
	private static ReportResult getPartyTaxInfo(Delegator delegator, String partyId, Timestamp startDate, Timestamp endDate, String taxAuthGeoId) throws GenericEntityException{
		ReportResult result = new ReportResult();
		
		List<EntityExpr> partyFilterList = FastList.newInstance();
		partyFilterList.add(EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, partyId));
		partyFilterList.add(EntityCondition.makeCondition("taxAuthGeoId", EntityOperator.EQUALS, taxAuthGeoId));
		EntityCondition partyFilter = EntityCondition.makeCondition(partyFilterList, EntityJoinOperator.AND);
		
		List<EntityExpr> beginDateFilterList = FastList.newInstance();
		beginDateFilterList.add(EntityCondition.makeCondition("taxInfoFromDate", EntityOperator.LESS_THAN_EQUAL_TO, endDate));
		beginDateFilterList.add(EntityCondition.makeCondition("taxInfoFromDate", null));
		EntityCondition beginDateFilter = EntityCondition.makeCondition(beginDateFilterList, EntityJoinOperator.OR);
		
		List<EntityExpr> endDateFilterList = FastList.newInstance();
		endDateFilterList.add(EntityCondition.makeCondition("taxInfoThruDate", EntityOperator.GREATER_THAN, startDate));
		endDateFilterList.add(EntityCondition.makeCondition("taxInfoThruDate", null));
		EntityCondition endDateFilter = EntityCondition.makeCondition(endDateFilterList, EntityJoinOperator.OR);
		
		EntityCondition orgInfoFilters = EntityCondition.makeCondition(UtilMisc.toList(partyFilter, beginDateFilter, endDateFilter), EntityOperator.AND);
		
		/* (StartDate1 <= EndDate2) and (StartDate2 <= EndDate1) */
		/* (StartDate1 <= EndDate2) and (EndDate1 > StartDate2) */
		
		List<GenericValue> orgInfoList = delegator.findList("ReportSaftOrgInfo", orgInfoFilters, null, null, null, false);
		
		if(orgInfoList == null || orgInfoList.isEmpty()){
			Map<String, String> messageParams = FastMap.newInstance();
			messageParams.put("partyId", partyId);
			messageParams.put("taxAuthGeoId", taxAuthGeoId);
			result.addMessage("SaftPartyTaxInfoNotFound", messageParams, ReportMessageSeverity.Error);
		} else if (orgInfoList.size() > 1){
			Map<String, String> messageParams = FastMap.newInstance();
			messageParams.put("partyId", partyId);
			messageParams.put("taxAuthGeoId", taxAuthGeoId);
			result.addMessage("SaftMoreThanOnePartyTaxInfoFound", messageParams, ReportMessageSeverity.Error);
			result.setResult(orgInfoList.get(0));
		} else {
			result.setResult(orgInfoList.get(0));
		}

		return result;
	}
	
	private static ReportResult getPartyAddress(Delegator delegator, String partyId, Timestamp startDate, Timestamp endDate, String postalAddressPurposeType, String countryGeoId) throws GenericEntityException {
		ReportResult result = new ReportResult();
		
		List<EntityCondition> filtersList = FastList.newInstance();
		boolean hasUserFilter = false;
		
		List<EntityExpr> partyFilterList = FastList.newInstance();
		partyFilterList.add(EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, partyId));
		partyFilterList.add(EntityCondition.makeCondition("countryGeoId", EntityOperator.EQUALS, countryGeoId));
		EntityCondition partyFilter = EntityCondition.makeCondition(partyFilterList, EntityJoinOperator.AND);
		filtersList.add(partyFilter);
		
		List<EntityExpr> pcmBeginDateFilterList = FastList.newInstance();
		pcmBeginDateFilterList.add(EntityCondition.makeCondition("pcmFromDate", EntityOperator.LESS_THAN_EQUAL_TO, endDate));
		pcmBeginDateFilterList.add(EntityCondition.makeCondition("pcmFromDate", null));
		EntityCondition pcmBeginDateFilter = EntityCondition.makeCondition(pcmBeginDateFilterList, EntityJoinOperator.OR);
		filtersList.add(pcmBeginDateFilter);
		
		List<EntityExpr> pcmEndDateFilterList = FastList.newInstance();
		pcmEndDateFilterList.add(EntityCondition.makeCondition("pcmThruDate", EntityOperator.GREATER_THAN, startDate));
		pcmEndDateFilterList.add(EntityCondition.makeCondition("pcmThruDate", null));
		EntityCondition pcmEndDateFilter = EntityCondition.makeCondition(pcmEndDateFilterList, EntityJoinOperator.OR);
		filtersList.add(pcmEndDateFilter);
		
		List<EntityExpr> pcmpBeginDateFilterList = FastList.newInstance();
		pcmpBeginDateFilterList.add(EntityCondition.makeCondition("pcmpFromDate", EntityOperator.LESS_THAN_EQUAL_TO, endDate));
		pcmpBeginDateFilterList.add(EntityCondition.makeCondition("pcmpFromDate", null));
		EntityCondition pcmpBeginDateFilter = EntityCondition.makeCondition(pcmpBeginDateFilterList, EntityJoinOperator.OR);
		filtersList.add(pcmpBeginDateFilter);
		
		List<EntityExpr> pcmpEndDateFilterList = FastList.newInstance();
		pcmpEndDateFilterList.add(EntityCondition.makeCondition("pcmpThruDate", EntityOperator.GREATER_THAN, startDate));
		pcmpEndDateFilterList.add(EntityCondition.makeCondition("pcmpThruDate", null));
		EntityCondition pcmpEndDateFilter = EntityCondition.makeCondition(pcmpEndDateFilterList, EntityJoinOperator.OR);
		filtersList.add(pcmpEndDateFilter);
		
		if(!UtilValidate.isEmpty(postalAddressPurposeType)){
			EntityCondition pcmpContactMechPurposeType = EntityCondition.makeCondition("contactMechPurposeTypeId", EntityOperator.EQUALS, postalAddressPurposeType);
			filtersList.add(pcmpContactMechPurposeType);
			
			hasUserFilter = !postalAddressPurposeType.equals(defaultPostalAddressPurposeType);
		}
		
		EntityCondition addressFilters = EntityCondition.makeCondition(filtersList, EntityOperator.AND);
		
		List<GenericValue> postalAddressesList = delegator.findList("ReportSaftPartyPA", addressFilters, null, UtilMisc.toList("paLastUpdatedStamp DESC"), null, false);
		
		if(postalAddressesList.size() == 0 && hasUserFilter){
			Map<String, String> messageParams = FastMap.newInstance();
			messageParams.put("partyId", partyId);
			messageParams.put("postalAddressPurposeType", postalAddressPurposeType);
			messageParams.put("countryGeoId", countryGeoId);
			result.addMessage("SaftNoActivePostalAddressFoundWillUseFirst", messageParams, ReportMessageSeverity.Warning);
			
			// Search without the last filter
			filtersList.remove(filtersList.size() - 1);
			
			addressFilters = EntityCondition.makeCondition(filtersList, EntityOperator.AND);
			postalAddressesList = delegator.findList("ReportSaftPartyPA", addressFilters, null, null, null, false);
		}
		
		if(postalAddressesList == null || postalAddressesList.isEmpty()){
			Map<String, String> messageParams = FastMap.newInstance();
			messageParams.put("partyId", partyId);
			messageParams.put("countryGeoId", countryGeoId);
			result.addMessage("SaftNoActivePostalAddressFound", messageParams, ReportMessageSeverity.Warning);
		} else if (postalAddressesList.size() > 1){
			Map<String, String> messageParams = FastMap.newInstance();
			messageParams.put("partyId", partyId);
			messageParams.put("countryGeoId", countryGeoId);
			result.addMessage("SaftMoreThanOnePostalAddressFound", messageParams, ReportMessageSeverity.Warning);
			result.setResult(postalAddressesList.get(0));
		} else {
			result.setResult(postalAddressesList.get(0));
		}
		
		return result;
	}
	
	private static ReportResult getPartyTelecom(Delegator delegator, String partyId, Timestamp startDate, Timestamp endDate, String phonePurposeType, String faxPurposeType) throws GenericEntityException{
		ReportResult result = new ReportResult();
		Map<String, GenericValue> telecomResult = FastMap.newInstance();
		
		EntityCondition partyFilter = EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, partyId);
		
		List<EntityExpr> pcmBeginDateFilterList = FastList.newInstance();
		pcmBeginDateFilterList.add(EntityCondition.makeCondition("pcmFromDate", EntityOperator.LESS_THAN_EQUAL_TO, endDate));
		pcmBeginDateFilterList.add(EntityCondition.makeCondition("pcmFromDate", null));
		EntityCondition pcmBeginDateFilter = EntityCondition.makeCondition(pcmBeginDateFilterList, EntityJoinOperator.OR);
		
		List<EntityExpr> pcmEndDateFilterList = FastList.newInstance();
		pcmEndDateFilterList.add(EntityCondition.makeCondition("pcmThruDate", EntityOperator.GREATER_THAN, startDate));
		pcmEndDateFilterList.add(EntityCondition.makeCondition("pcmThruDate", null));
		EntityCondition pcmEndDateFilter = EntityCondition.makeCondition(pcmEndDateFilterList, EntityJoinOperator.OR);
		
		List<EntityExpr> pcmpBeginDateFilterList = FastList.newInstance();
		pcmpBeginDateFilterList.add(EntityCondition.makeCondition("pcmpFromDate", EntityOperator.LESS_THAN_EQUAL_TO, endDate));
		pcmpBeginDateFilterList.add(EntityCondition.makeCondition("pcmpFromDate", null));
		EntityCondition pcmpBeginDateFilter = EntityCondition.makeCondition(pcmpBeginDateFilterList, EntityJoinOperator.OR);
		
		List<EntityExpr> pcmpEndDateFilterList = FastList.newInstance();
		pcmpEndDateFilterList.add(EntityCondition.makeCondition("pcmpThruDate", EntityOperator.GREATER_THAN, startDate));
		pcmpEndDateFilterList.add(EntityCondition.makeCondition("pcmpThruDate", null));
		EntityCondition pcmpEndDateFilter = EntityCondition.makeCondition(pcmpEndDateFilterList, EntityJoinOperator.OR);
		
		EntityCondition telecomFilters = EntityCondition.makeCondition(UtilMisc.toList(partyFilter, pcmBeginDateFilter, pcmEndDateFilter, pcmpBeginDateFilter, pcmpEndDateFilter), EntityOperator.AND);
		
		List<GenericValue> telecomContacts = delegator.findList("ReportSaftPartyTelecom", telecomFilters, null, UtilMisc.toList("contactMechPurposeTypeId ASC", "tnLastUpdatedStamp DESC"), null, false);
		boolean hasDuplicatePhone = false;
		boolean hasDuplicateFax = false;
		
		for (GenericValue contact: telecomContacts){
			String contactPurposeType = contact.getString("contactMechPurposeTypeId");
			
			if(contactPurposeType.equals(phonePurposeType) && telecomResult.containsKey("phone") && !hasDuplicatePhone){
				Map<String, String> messageParams = FastMap.newInstance();
				messageParams.put("partyId", partyId);
				messageParams.put("contactPurposeType", contactPurposeType);
				result.addMessage("SaftMoreThanOneContactMechanismFound", messageParams, ReportMessageSeverity.Warning);
				hasDuplicatePhone = true;
			} else if(contactPurposeType.equals(faxPurposeType) && telecomResult.containsKey("fax") && !hasDuplicateFax){
				Map<String, String> messageParams = FastMap.newInstance();
				messageParams.put("partyId", partyId);
				messageParams.put("contactPurposeType", contactPurposeType);
				result.addMessage("SaftMoreThanOneContactMechanismFound", messageParams, ReportMessageSeverity.Warning);
				hasDuplicateFax = true;
			} else if(contactPurposeType.equals(phonePurposeType) && !telecomResult.containsKey("phone")){
				telecomResult.put("phone", contact);
			} else if(contactPurposeType.equals(faxPurposeType) && !telecomResult.containsKey("fax")){
				telecomResult.put("fax", contact);
			} else if(telecomResult.containsKey("phone") && telecomResult.containsKey("fax")){
				break;
			}
		}
		
		result.setResult(telecomResult);
		return result;
	}
	
	private static ReportResult getPartyWebContacts(Delegator delegator, String partyId, Timestamp startDate, Timestamp endDate, String emailPurposeType, String websitePurposeType) throws GenericEntityException {
		ReportResult result = new ReportResult();
		Map<String, GenericValue> webContactResult = FastMap.newInstance();
		
		EntityCondition partyFilter = EntityCondition.makeCondition("partyId", EntityOperator.EQUALS, partyId);
		
		List<EntityExpr> pcmBeginDateFilterList = FastList.newInstance();
		pcmBeginDateFilterList.add(EntityCondition.makeCondition("pcmFromDate", EntityOperator.LESS_THAN_EQUAL_TO, endDate));
		pcmBeginDateFilterList.add(EntityCondition.makeCondition("pcmFromDate", null));
		EntityCondition pcmBeginDateFilter = EntityCondition.makeCondition(pcmBeginDateFilterList, EntityJoinOperator.OR);
		
		List<EntityExpr> pcmEndDateFilterList = FastList.newInstance();
		pcmEndDateFilterList.add(EntityCondition.makeCondition("pcmThruDate", EntityOperator.GREATER_THAN, startDate));
		pcmEndDateFilterList.add(EntityCondition.makeCondition("pcmThruDate", null));
		EntityCondition pcmEndDateFilter = EntityCondition.makeCondition(pcmEndDateFilterList, EntityJoinOperator.OR);
		
		List<EntityExpr> pcmpBeginDateFilterList = FastList.newInstance();
		pcmpBeginDateFilterList.add(EntityCondition.makeCondition("pcmpFromDate", EntityOperator.LESS_THAN_EQUAL_TO, endDate));
		pcmpBeginDateFilterList.add(EntityCondition.makeCondition("pcmpFromDate", null));
		EntityCondition pcmpBeginDateFilter = EntityCondition.makeCondition(pcmpBeginDateFilterList, EntityJoinOperator.OR);
		
		List<EntityExpr> pcmpEndDateFilterList = FastList.newInstance();
		pcmpEndDateFilterList.add(EntityCondition.makeCondition("pcmpThruDate", EntityOperator.GREATER_THAN, startDate));
		pcmpEndDateFilterList.add(EntityCondition.makeCondition("pcmpThruDate", null));
		EntityCondition pcmpEndDateFilter = EntityCondition.makeCondition(pcmpEndDateFilterList, EntityJoinOperator.OR);
		
		EntityCondition webFilters = EntityCondition.makeCondition(UtilMisc.toList(partyFilter, pcmBeginDateFilter, pcmEndDateFilter, pcmpBeginDateFilter, pcmpEndDateFilter), EntityOperator.AND);
		
		List<GenericValue> webContacts = delegator.findList("ReportSaftPartyWebContact", webFilters, null, UtilMisc.toList("contactMechPurposeTypeId ASC", "cmLastUpdatedStamp DESC"), null, false);
		boolean hasDuplicateEmail = false;
		boolean hasDuplicateWebsite = false;
		
		for (GenericValue contact: webContacts){
			String contactPurposeType = contact.getString("contactMechPurposeTypeId");
			if(contactPurposeType.equals(emailPurposeType) && webContactResult.containsKey("email") && !hasDuplicateEmail){
				Map<String, String> messageParams = FastMap.newInstance();
				messageParams.put("partyId", partyId);
				messageParams.put("contactPurposeType", contactPurposeType);
				result.addMessage("SaftMoreThanOneContactMechanismFound", messageParams, ReportMessageSeverity.Warning);
				hasDuplicateEmail = true;
			} else if(contactPurposeType.equals(websitePurposeType) && webContactResult.containsKey("website") && !hasDuplicateWebsite){
				Map<String, String> messageParams = FastMap.newInstance();
				messageParams.put("partyId", partyId);
				messageParams.put("contactPurposeType", contactPurposeType);
				result.addMessage("SaftMoreThanOneContactMechanismFound", messageParams, ReportMessageSeverity.Warning);
				hasDuplicateWebsite = true;
			} else if(contactPurposeType.equals(emailPurposeType) && !webContactResult.containsKey("email")){
				webContactResult.put("email", contact);
			} else if(contactPurposeType.equals(websitePurposeType) && !webContactResult.containsKey("website")){
				webContactResult.put("website", contact);
			} else if(webContactResult.containsKey("email") && webContactResult.containsKey("website")){
				break;
			}
		}
		
		result.setResult(webContactResult);
		return result;
	}

	private static ReportResult generateHeader(Document doc, Delegator delegator, Timestamp startDate, Timestamp endDate, String orgPartyId, 
			String taxAuthGeoId, String postalAddressPurposeType, String phonePurposeType, String faxPurposeType, 
			String emailPurposeType, String websitePurposeType) throws GenericEntityException{
		ReportResult result = new ReportResult();
		
		ReportResult orgInfoResult = null;
		ReportResult postalAddressResult = null;
		ReportResult telecomMapResult = null;
		ReportResult webcomMapResult = null;
		
		GenericValue orgInfo = null;
		GenericValue postalAddress = null;
		GenericValue faxNumber = null;
		GenericValue phoneNumber = null;
		GenericValue email = null;
		GenericValue website = null;
		Map<String, GenericValue> telecomMap = null;
		Map<String, GenericValue> webcomMap = null;
		SimpleDateFormat shortDateFormat = new SimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat yearDateFormat = new SimpleDateFormat("yyyy");
		String taxId = "";
		Integer taxNumber = null;
		String fiscalYear = yearDateFormat.format(startDate);
		
		// Retrieve Data
		orgInfoResult = getPartyTaxInfo(delegator, orgPartyId, startDate, endDate, taxAuthGeoId);
		if(orgInfoResult.getMessages().size() > 0){
			result.addAll(orgInfoResult.getMessages());
		}
		if(orgInfoResult.getResult() != null) {
			orgInfo = (GenericValue)orgInfoResult.getResult();
		}
		
		postalAddressResult = getPartyAddress(delegator, orgPartyId, startDate, endDate, postalAddressPurposeType, taxAuthGeoId);
		if(postalAddressResult.getMessages().size() > 0){
			result.addAll(postalAddressResult.getMessages());
		}
		if(postalAddressResult.getResult() != null){
			postalAddress = (GenericValue)postalAddressResult.getResult();
		}
		
		telecomMapResult = getPartyTelecom(delegator, orgPartyId, startDate, endDate, phonePurposeType, faxPurposeType);
		if(telecomMapResult.getMessages().size() > 0){
			result.addAll(telecomMapResult.getMessages());
		}
		if(telecomMapResult.getResult() != null){
			telecomMap = (Map<String, GenericValue>)telecomMapResult.getResult();
		}
		
		webcomMapResult = getPartyWebContacts(delegator, orgPartyId, startDate, endDate, emailPurposeType, websitePurposeType);
		if(webcomMapResult.getMessages().size() > 0){
			result.addAll(webcomMapResult.getMessages());
		}
		if(webcomMapResult.getResult() != null){
			webcomMap = (Map<String, GenericValue>)webcomMapResult.getResult();
		}
		
		if(telecomMap.containsKey("phone")){
			phoneNumber = telecomMap.get("phone");
		}
		
		if(telecomMap.containsKey("fax")){
			faxNumber = telecomMap.get("fax");
		}
		
		if(webcomMap.containsKey("email")){
			email = webcomMap.get("email");
		}
		
		if(webcomMap.containsKey("website")){
			website = webcomMap.get("website");
		}
		
		Element header = doc.createElement("Header");
		
		// Required - AuditFileVersion
		header.appendChild(CreateSimpleElement(doc, "AuditFileVersion", "1.03_01"));
		
		if(orgInfo != null){
			taxId = orgInfo.getString("partyTaxId");
			taxNumber = Integer.parseInt(taxId);
			
			if(taxNumber < 100000000){
				Map<String, String> messageParams = FastMap.newInstance();
				messageParams.put("taxNumber", taxNumber.toString());
				messageParams.put("orgPartyId", orgPartyId);
				result.addMessage("SaftInvalidTaxNumber", messageParams, ReportMessageSeverity.Error);
				//taxNumber = 999999990;
			}
			
			// Required - CompanyID
			header.appendChild(CreateSimpleElement(doc, "CompanyID", taxNumber.toString()));
			
			// Required - TaxRegistrationNumber
			header.appendChild(CreateSimpleElement(doc, "TaxRegistrationNumber", taxNumber.toString()));
			
			// Required - TaxAccountingBasis
			header.appendChild(CreateSimpleElement(doc, "TaxAccountingBasis", "F"));
			
			// Required - CompanyName
			header.appendChild(CreateSimpleElement(doc, "CompanyName", orgInfo.getString("groupName")));
			
			// BusinessName
			//header.appendChild(CreateSimpleElement(doc, "BusinessName", "TODO"));
		}
		
		if(postalAddress != null){
			// Required - CompanyAddress
			String address1 = postalAddress.getString("address1");
			String address2 = postalAddress.getString("address2");
			String city = postalAddress.getString("city");
			String regionId = postalAddress.getString("stateGeoId");
			String regionName = postalAddress.getString("stateGeoName");
			String country = postalAddress.getString("countryGeoCode");
			String postalCode = postalAddress.getString("postalCode");
			
			List<String> addressDetailList = FastList.newInstance();
			if(UtilValidate.isNotEmpty(address1)){
				addressDetailList.add(address1);
			}
			
			if(UtilValidate.isNotEmpty(address2)){
				addressDetailList.add(address2);
			}
			
			if(UtilValidate.isNotEmpty(city)){
				addressDetailList.add(city);
			}
			
			if(UtilValidate.isNotEmpty(postalCode)){
				addressDetailList.add(postalCode);
			}
			
			Element companyAddressElem = doc.createElement("CompanyAddress");
			//companyAddressElem.appendChild(CreateSimpleElement(doc, "BuildingNumber", "TODO"));
			//companyAddressElem.appendChild(CreateSimpleElement(doc, "StreetName", "TODO"));
			companyAddressElem.appendChild(CreateSimpleElement(doc, "AddressDetail",  StringUtil.join(addressDetailList, ", ")));
			companyAddressElem.appendChild(CreateSimpleElement(doc, "City", city));
			if(ValidatePostalCode(postalCode)){
				companyAddressElem.appendChild(CreateSimpleElement(doc, "PostalCode", postalCode));
			} else {
				Map<String, String> messageParams = FastMap.newInstance();
				messageParams.put("postalCode", postalCode);
				messageParams.put("orgPartyId", orgPartyId);
				result.addMessage("SaftInvalidPostalCode", messageParams, ReportMessageSeverity.Error);
				//companyAddressElem.appendChild(CreateSimpleElement(doc, "PostalCode", "0000-000"));
			}
			
			if(UtilValidate.isNotEmpty(regionName) && !regionId.equals("_NA_")){
				companyAddressElem.appendChild(CreateSimpleElement(doc, "Region", regionName));
			}
			
			if(UtilValidate.isNotEmpty(country)){
				companyAddressElem.appendChild(CreateSimpleElement(doc, "Country", country));
			}
			header.appendChild(companyAddressElem);
		}
		
		// Required - FiscalYear
		header.appendChild(CreateSimpleElement(doc, "FiscalYear", fiscalYear));
		
		// Required - StartDate
		header.appendChild(CreateSimpleElement(doc, "StartDate", shortDateFormat.format(startDate)));
		
		// Required - EndDate
		header.appendChild(CreateSimpleElement(doc, "EndDate", shortDateFormat.format(endDate)));
		
		// Required - CurrencyCode
		header.appendChild(CreateSimpleElement(doc, "CurrencyCode", "EUR"));
		
		// Required - DateCreated
		header.appendChild(CreateSimpleElement(doc, "DateCreated", shortDateFormat.format(UtilDateTime.nowDate())));
		
		// Required - TaxEntity
		header.appendChild(CreateSimpleElement(doc, "TaxEntity", "Global"));
		
		// Required - ProductCompanyTaxID
		header.appendChild(CreateSimpleElement(doc, "ProductCompanyTaxID", "TODO"));
		
		// Required - SoftwareCertificateNumber
		header.appendChild(CreateSimpleElement(doc, "SoftwareCertificateNumber", "0"));		
		
		// Required - ProductID
		header.appendChild(CreateSimpleElement(doc, "ProductID", "OFBiz/Apache"));
		
		// Required - ProductVersion
		header.appendChild(CreateSimpleElement(doc, "ProductVersion", "13.07"));
		
		// Telephone
		if(phoneNumber != null){
			String countryCode = phoneNumber.getString("countryCode");
			String areaCode = phoneNumber.getString("areaCode");
			String contactNumber = phoneNumber.getString("contactNumber");
			String phoneNumberStr = "";
			if(UtilValidate.isNotEmpty(countryCode)){
				phoneNumberStr = "+" + countryCode;
			}
			
			if(UtilValidate.isNotEmpty(areaCode)){
				phoneNumberStr += areaCode;
			}
			
			if(UtilValidate.isNotEmpty(contactNumber)){
				phoneNumberStr += contactNumber;
			}
			
			header.appendChild(CreateSimpleElement(doc, "Telephone", phoneNumberStr));
		}
		
		// Fax
		if(faxNumber != null){
			String countryCode = faxNumber.getString("countryCode");
			String areaCode = faxNumber.getString("areaCode");
			String contactNumber = faxNumber.getString("contactNumber");
			String faxNumberStr = "";
			if(UtilValidate.isNotEmpty(countryCode)){
				faxNumberStr = "+" + countryCode;
			}
			
			if(UtilValidate.isNotEmpty(areaCode)){
				faxNumberStr += areaCode;
			}
			
			if(UtilValidate.isNotEmpty(contactNumber)){
				faxNumberStr += contactNumber;
			}
			
			header.appendChild(CreateSimpleElement(doc, "Fax", faxNumberStr));
		}
		
		// Email
		if(email != null){
			header.appendChild(CreateSimpleElement(doc, "Email", email.getString("infoString")));
		}
		
		// Website		
		if(website != null) {
			header.appendChild(CreateSimpleElement(doc, "Website", website.getString("infoString")));
		}
		
		result.setResult(header);
		return result;
	}

	private static List<ReportMessage> GenerateCustomers(Document doc, Element parentElement, Delegator delegator, Timestamp startDate, Timestamp endDate,
			String taxAuthGeoId, String postalAddressPurposeType, String phonePurposeType, String faxPurposeType, 
			String emailPurposeType, String websitePurposeType) throws GenericEntityException {
		List<ReportMessage> result = new ArrayList<ReportMessage>();
		EntityExpr prtTaxAuthority = EntityCondition.makeCondition("taxAuthGeoId", EntityOperator.EQUALS, "PRT");
		List<GenericValue> customers = delegator.findList("ReportSaftCustomers", prtTaxAuthority, null, UtilMisc.toList("-partyTaxId"), null, false);
		
		for (GenericValue customer: customers){			
			ReportResult postalAddressResult = null;
			ReportResult telecomMapResult = null;
			ReportResult webcomMapResult = null;
			
			GenericValue postalAddress = null;
			GenericValue faxNumber = null;
			GenericValue phoneNumber = null;
			GenericValue email = null;
			GenericValue website = null;
			Map<String, GenericValue> telecomMap = null;
			Map<String, GenericValue> webcomMap = null;
			String partyId = customer.getString("partyId");
			
			String taxId = customer.getString("partyTaxId");
			Integer taxNumber = null;
			taxNumber = Integer.parseInt(taxId);
			
			if(taxNumber < 100000000){
				Map<String, String> messageParams = FastMap.newInstance();
				messageParams.put("taxNumber", taxNumber.toString());
				messageParams.put("orgPartyId", partyId);
				result.add(new ReportMessage("SaftInvalidTaxNumber", messageParams, ReportMessageSeverity.Error));
			}
			
			Element customerElement = doc.createElement("Customer");
			
			// CustomerID
			customerElement.appendChild(CreateSimpleElement(doc, "CustomerID", partyId));
			
			// AccountID
			customerElement.appendChild(CreateSimpleElement(doc, "AccountID", partyId));
			
			// CustomerTaxID
			customerElement.appendChild(CreateSimpleElement(doc, "CustomerTaxID", taxNumber.toString()));
			
			// CompanyName
			String displayName = "";
			String firstName = customer.getString("firstName");
			String lastName = customer.getString("lastName");
			String groupName = customer.getString("groupName");
			if(UtilValidate.isNotEmpty(firstName) || UtilValidate.isNotEmpty(lastName)){
				displayName = firstName + " " + lastName;
			} else {
				displayName = groupName;
			}
			
			if(UtilValidate.isNotEmpty(displayName)){
				customerElement.appendChild(CreateSimpleElement(doc, "CompanyName", displayName));
			}
			
			// Contact
			// customerElement.appendChild(CreateSimpleElement(doc, "Contact", "TODO"));
			
			// Retrieve Data
			postalAddressResult = getPartyAddress(delegator, partyId, startDate, endDate, postalAddressPurposeType, taxAuthGeoId);
			if(postalAddressResult.getMessages().size() > 0){
				result.addAll(postalAddressResult.getMessages());
			}
			if(postalAddressResult.getResult() != null){
				postalAddress = (GenericValue)postalAddressResult.getResult();
			}
			
			telecomMapResult = getPartyTelecom(delegator, partyId, startDate, endDate, phonePurposeType, faxPurposeType);
			if(telecomMapResult.getMessages().size() > 0){
				result.addAll(telecomMapResult.getMessages());
			}
			if(telecomMapResult.getResult() != null){
				telecomMap = (Map<String, GenericValue>)telecomMapResult.getResult();
			}
			
			webcomMapResult = getPartyWebContacts(delegator, partyId, startDate, endDate, emailPurposeType, websitePurposeType);
			if(webcomMapResult.getMessages().size() > 0){
				result.addAll(webcomMapResult.getMessages());
			}
			if(webcomMapResult.getResult() != null){
				webcomMap = (Map<String, GenericValue>)webcomMapResult.getResult();
			}
			
			if(telecomMap.containsKey("phone")){
				phoneNumber = telecomMap.get("phone");
			}
			
			if(telecomMap.containsKey("fax")){
				faxNumber = telecomMap.get("fax");
			}
			
			if(webcomMap.containsKey("email")){
				email = webcomMap.get("email");
			}
			
			if(webcomMap.containsKey("website")){
				website = webcomMap.get("website");
			}
			
			if(postalAddress != null){
				// BillingAddres
				String address1 = postalAddress.getString("address1");
				String address2 = postalAddress.getString("address2");
				String city = postalAddress.getString("city");
				String regionId = postalAddress.getString("stateGeoId");
				String regionName = postalAddress.getString("stateGeoName");
				String country = postalAddress.getString("countryGeoCode");
				String postalCode = postalAddress.getString("postalCode");
				
				List<String> addressDetailList = FastList.newInstance();
				if(UtilValidate.isNotEmpty(address1)){
					addressDetailList.add(address1);
				}
				
				if(UtilValidate.isNotEmpty(address2)){
					addressDetailList.add(address2);
				}
				
				if(UtilValidate.isNotEmpty(city)){
					addressDetailList.add(city);
				}
				
				if(UtilValidate.isNotEmpty(postalCode)){
					addressDetailList.add(postalCode);
				}
				
				Element companyAddressElem = doc.createElement("BillingAddress");
				//companyAddressElem.appendChild(CreateSimpleElement(doc, "BuildingNumber", "TODO"));
				//companyAddressElem.appendChild(CreateSimpleElement(doc, "StreetName", "TODO"));
				companyAddressElem.appendChild(CreateSimpleElement(doc, "AddressDetail",  StringUtil.join(addressDetailList, ", ")));
				companyAddressElem.appendChild(CreateSimpleElement(doc, "City", city));
				if(ValidatePostalCode(postalCode)){
					companyAddressElem.appendChild(CreateSimpleElement(doc, "PostalCode", postalCode));
				} else {
					Map<String, String> messageParams = FastMap.newInstance();
					messageParams.put("postalCode", postalCode);
					messageParams.put("orgPartyId", partyId);
					result.add(new ReportMessage("SaftInvalidPostalCode", messageParams, ReportMessageSeverity.Error));
				}
				
				if(UtilValidate.isNotEmpty(regionName) && !regionId.equals("_NA_")){
					companyAddressElem.appendChild(CreateSimpleElement(doc, "Region", regionName));
				}
				
				if(UtilValidate.isNotEmpty(country)){
					companyAddressElem.appendChild(CreateSimpleElement(doc, "Country", country));
				}
				customerElement.appendChild(companyAddressElem);
			} else {
				// Export "Unknown" postal address values.
				Element companyAddressElem = doc.createElement("BillingAddress");
				companyAddressElem.appendChild(CreateSimpleElement(doc, "AddressDetail",  "Desconhecido"));
				companyAddressElem.appendChild(CreateSimpleElement(doc, "City", "Desconhecido"));
				companyAddressElem.appendChild(CreateSimpleElement(doc, "PostalCode", "Desconhecido"));
				companyAddressElem.appendChild(CreateSimpleElement(doc, "Country", "Desconhecido"));
				customerElement.appendChild(companyAddressElem);
			}
			
			// Telephone
			if(phoneNumber != null){
				String countryCode = phoneNumber.getString("countryCode");
				String areaCode = phoneNumber.getString("areaCode");
				String contactNumber = phoneNumber.getString("contactNumber");
				String phoneNumberStr = "";
				if(UtilValidate.isNotEmpty(countryCode)){
					phoneNumberStr = "+" + countryCode;
				}
				
				if(UtilValidate.isNotEmpty(areaCode)){
					phoneNumberStr += areaCode;
				}
				
				if(UtilValidate.isNotEmpty(contactNumber)){
					phoneNumberStr += contactNumber;
				}
				
				// Telephone
				customerElement.appendChild(CreateSimpleElement(doc, "Telephone", phoneNumberStr));
			} 
			
			// Fax
			if(faxNumber != null){
				String countryCode = faxNumber.getString("countryCode");
				String areaCode = faxNumber.getString("areaCode");
				String contactNumber = faxNumber.getString("contactNumber");
				String faxNumberStr = "";
				if(UtilValidate.isNotEmpty(countryCode)){
					faxNumberStr = "+" + countryCode;
				}
				
				if(UtilValidate.isNotEmpty(areaCode)){
					faxNumberStr += areaCode;
				}
				
				if(UtilValidate.isNotEmpty(contactNumber)){
					faxNumberStr += contactNumber;
				}
				
				customerElement.appendChild(CreateSimpleElement(doc, "Fax", faxNumberStr));
			}
			
			// Email
			if(email != null){
				customerElement.appendChild(CreateSimpleElement(doc, "Email", email.getString("infoString")));
			}	
			
			// Website		
			if(website != null) {
				customerElement.appendChild(CreateSimpleElement(doc, "Website", website.getString("infoString")));
			}
			
			// TODO: SelfBillingIndicator
			customerElement.appendChild(CreateSimpleElement(doc, "SelfBillingIndicator", "0"));
			
			parentElement.appendChild(customerElement);
		}
		
		return result;
	}
	
	private static boolean ValidatePostalCode(String postalCode){
		 Pattern p = Pattern.compile("([0-9]{4}-[0-9]{3})");
		 Matcher m = p.matcher(postalCode);
		 return m.matches();
	}

	private static Element CreateSimpleElement(Document doc, String tagName,
			String value) {
		Element elem = doc.createElement(tagName);

		Text textNode = doc.createTextNode(value);
		elem.appendChild(textNode);

		return elem;
	}
}
