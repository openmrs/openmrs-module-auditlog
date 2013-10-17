<%@ include file="/WEB-INF/template/include.jsp"%>

<c:set var="moduleId" value="auditlog" scope="page" />

<openmrs:htmlInclude file="/moduleResources/${moduleId}/css/auditlog.css" />

<openmrs:htmlInclude file="/moduleResources/${moduleId}/scripts/auditlog.js" />

<spring:htmlEscape defaultHtmlEscape="true" />