<%@ include file="/WEB-INF/template/include.jsp"%>

<c:set var="moduleId" value="auditlog" scope="page" />

<openmrs:htmlInclude file="/moduleResources/${moduleId}/css/auditlog.css" />

<openmrs:htmlInclude file="/moduleResources/${moduleId}/scripts/auditlog.js" />

<spring:htmlEscape defaultHtmlEscape="true" />

<script type="text/javascript">
    var auditlog_moduleId = '${moduleId}';
    var auditlog_messages = {};
    auditlog_messages.changesLabel = '<spring:message code="auditlog.changes" />';
    auditlog_messages.lastKnownStateLabel = '<spring:message code="auditlog.lastKnownState" />';
    auditlog_messages.objectDoesnotExit = '<spring:message code="auditlog.objectDoesnotExist" />';
</script>