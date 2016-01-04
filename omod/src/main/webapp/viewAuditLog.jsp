<%@ include file="/WEB-INF/template/include.jsp"%>

<%@ include file="/WEB-INF/template/header.jsp"%>

<%@ include file="/WEB-INF/view/module/auditlog/include.jsp"%>

<openmrs:require privilege="View Audit Log" otherwise="/login.htm"
                 redirect="/module/${moduleId}/viewAuditLog.htm"/>

<%@ include file="template/localHeader.jsp"%>

<openmrs:htmlInclude file="/scripts/jquery/dataTables/css/dataTables_jui.css"/>
<openmrs:htmlInclude file="/scripts/jquery/dataTables/js/jquery.dataTables.min.js"/>
<openmrs:htmlInclude file="/dwr/interface/DWRAuditLogService.js"/>

<script type="text/javascript">
    $j(document).ready(function() {
        auditlog_initTable();
    });
</script>

<div class="box">
    <b class="boxHeader" style="width: auto;"><spring:message code="${moduleId}.auditlogs" /></b>
    <br />
    <table id="${moduleId}" width="100%" cellpadding="3" cellspacing="0" align="left">
        <thead>
        <tr>
            <th class="ui-state-default"></th>
            <th class="ui-state-default"><spring:message code="${moduleId}.item" /> (<spring:message code="${moduleId}.numberOfAssociatedAuditLogs" />)</th>
            <th class="ui-state-default"><spring:message code="${moduleId}.userAndUserName" /></th>
            <th class="ui-state-default"><spring:message code="${moduleId}.dateOfOccurence" /></th>
        </tr>
        </thead>
        <tbody>
        <c:forEach items="${auditLogs}" var="auditLog">
            <tr class="${moduleId}_${auditLog.action}" onclick="${moduleId}_showDetails('${auditLog.uuid}')">
                <td style="width: 17px !important;" align="center">
                    <img class="${moduleId}_action_image" align="top"
                         src="<openmrs:contextPath />/moduleResources/${moduleId}/images/${auditLog.action}.gif" />
                </td>
                <td>
                    ${auditLog.simpleTypeName}
                    <c:if test="${fn:length(auditLog.childAuditLogs) > 0}"> (${fn:length(auditLog.childAuditLogs)})</c:if>
                </td>
                <td>
                    <c:choose>
                        <%-- If this is a scheduled task, something done by daemon thread or at start up --%>
                        <c:when test="${auditLog.user == null || auditLog.user.uuid == 'A4F30A1B-5EB9-11DF-A648-37A07F9C90FB'}">
                            <spring:message code="${moduleId}.systemAction" />
                        </c:when>
                        <c:otherwise>
                            ${auditLog.user.personName} <c:if test="${fn:trim(auditLog.user.username) != ''}">[${auditLog.user.username}]</c:if>
                        </c:otherwise>
                    </c:choose>
                </td>
                <td><openmrs:formatDate date="${auditLog.dateCreated}" type="long" /></td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
</div>

<%-- Dialog to display auditlog details --%>

<div id="${moduleId}-changes-dialog" class="${moduleId}_align_text_left" title="<spring:message code="${moduleId}.logDetails" />">
<br />
<table id="${moduleId}-details" width="100%" cellpadding="0" cellspacing="5">
    <tr>
        <th valign="top" class="${moduleId}_align_text_left"><spring:message code="${moduleId}.identifier" /></th>
        <td id="${moduleId}-changes-identifier" width="100%"></td>
    </tr>
    <tr>
        <th valign="top" class="${moduleId}_align_text_left"><spring:message code="${moduleId}.summary" /></th>
        <td id="${moduleId}-changes-summary"></td>
    </tr>
    <tr>
        <th valign="top" class="${moduleId}_align_text_left"><spring:message code="${moduleId}.openmrsVersion" /></th>
        <td id="${moduleId}-changes-openmrsVersion"></td>
    </tr>
    <tr class="${moduleId}-changes-element"><td colspan="2">&nbsp;</td></tr>
    <tr class="${moduleId}-changes-element">
        <td valign="top" colspan="2">
            <span id="${moduleId}-otherDataLabel"></span>
        </td>
    </tr>
    <tr class="${moduleId}-changes-element">
        <td valign="top" colspan="2" class="${moduleId}_align_text_left">
            <table id="${moduleId}-changes-table" width="100%" cellpadding="3" cellspacing="0" border="1" bordercolor="#ADACAC">
                <thead>
                <tr>
                    <td class="${moduleId}_table_header ${moduleId}_align_text_center" width="26%">
                        <spring:message code="${moduleId}.propertyName" />
                    </td>
                    <td class="${moduleId}_table_header ${moduleId}_align_text_center" width="37%">
                        <spring:message code="${moduleId}.newValue" />
                    </td>
                    <td class="${moduleId}_table_header ${moduleId}_align_text_center" width="37%">
                        <spring:message code="${moduleId}.previousValue" />
                    </td>
                </tr>
                <thead>
            </table>
            <table id="${moduleId}-delete-otherData-table" width="100%" cellpadding="3" cellspacing="0" border="1" bordercolor="#ADACAC">
                <thead>
                <tr>
                    <td class="${moduleId}_table_header ${moduleId}_align_text_center">
                        <spring:message code="${moduleId}.propertyName" />
                    </td>
                    <td class="${moduleId}_table_header ${moduleId}_align_text_center">
                        <spring:message code="${moduleId}.value" />
                    </td>
                </tr>
                <thead>
            </table>
        </td>
    </tr>
    <tr class="${moduleId}-childAuditLogDetails-element"><td colspan="2">&nbsp;</td></tr>
    <tr class="${moduleId}-childAuditLogDetails-element">
        <td valign="top" colspan="2">
            <b><spring:message code="${moduleId}.associatedLogDetails" />
                (<span id="${moduleId}-childLogCount"></span>)
            </b>
            <img align="top" src="<openmrs:contextPath />/images/help.gif" border="0"
                 title="<openmrs:message code="${moduleId}.associatedLogDetails.help" />" />
        </td>
    </tr>
    <tr class="${moduleId}-childAuditLogDetails-element">
        <td valign="top" colspan="2" class="${moduleId}_align_text_left">
            <table id="${moduleId}-childAuditLogDetails-table" cellpadding="3" cellspacing="0" border="1" bordercolor="#ADACAC">
                <thead>
                <tr>
                    <td class="${moduleId}_table_header ${moduleId}_align_text_left">
                        <spring:message code="${moduleId}.item" />
                    </td>
                </tr>
                <thead>
            </table>
        </td>
    </tr>
</table>
</div>

<%-- Dialog to display auditlog details for a child auditlog --%>

<div id="${moduleId}-child-changes-dialog" class="${moduleId}_align_text_left" title="<spring:message code="${moduleId}.associatedLogDetails" />">
<br />
<table id="${moduleId}-child-details" width="100%" cellpadding="0" cellspacing="5">
    <tr>
        <th valign="top" class="${moduleId}_align_text_left"><spring:message code="${moduleId}.identifier" /></th>
        <td id="${moduleId}-child-changes-identifier" width="100%"></td>
    </tr>
    <tr>
        <th valign="top" class="${moduleId}_align_text_left"><spring:message code="${moduleId}.summary" /></th>
        <td id="${moduleId}-child-changes-summary"></td>
    </tr>
    <tr>
        <th valign="top" class="${moduleId}_align_text_left"><spring:message code="${moduleId}.openmrsVersion" /></th>
        <td id="${moduleId}-child-changes-openmrsVersion"></td>
    </tr>
    <tr class="${moduleId}-changes-element"><td colspan="2">&nbsp;</td></tr>
    <tr class="${moduleId}-changes-element">
        <td valign="top" colspan="2">
            <span id="${moduleId}-child-otherDataLabel"></span>
        </td>
    </tr>
    <tr class="${moduleId}-changes-element">
        <td valign="top" colspan="2" class="${moduleId}_align_text_left">
            <table id="${moduleId}-child-changes-table" width="100%" cellpadding="3" cellspacing="0" border="1" bordercolor="#ADACAC">
                <thead>
                <tr>
                    <td class="${moduleId}_table_header ${moduleId}_align_text_center" width="26%">
                        <spring:message code="${moduleId}.propertyName" />
                    </td>
                    <td class="${moduleId}_table_header ${moduleId}_align_text_center" width="37%">
                        <spring:message code="${moduleId}.newValue" />
                    </td>
                    <td class="${moduleId}_table_header ${moduleId}_align_text_center" width="37%">
                        <spring:message code="${moduleId}.previousValue" />
                    </td>
                </tr>
                <thead>
            </table>
            <table id="${moduleId}-child-delete-otherData-table" width="100%" cellpadding="3" cellspacing="0" border="1" bordercolor="#ADACAC">
                <thead>
                <tr>
                    <td class="${moduleId}_table_header ${moduleId}_align_text_center">
                        <spring:message code="${moduleId}.propertyName" />
                    </td>
                    <td class="${moduleId}_table_header ${moduleId}_align_text_center">
                        <spring:message code="${moduleId}.value" />
                    </td>
                </tr>
                <thead>
            </table>
        </td>
    </tr>
    <tr class="${moduleId}-childAuditLogDetails-element"><td colspan="2">&nbsp;</td></tr>
    <tr class="${moduleId}-childAuditLogDetails-element">
        <td valign="top" colspan="2">
            <b><spring:message code="${moduleId}.associatedLogDetails" />
                (<span id="${moduleId}-child-childLogCount"></span>)
            </b>
            <img align="top" src="<openmrs:contextPath />/images/help.gif" border="0"
                 title="<openmrs:message code="${moduleId}.associatedLogDetails.help" />" />
        </td>
    </tr>
    <tr class="${moduleId}-childAuditLogDetails-element">
        <td valign="top" colspan="2" class="${moduleId}_align_text_left">
            <table id="${moduleId}-child-childAuditLogDetails-table" cellpadding="3" cellspacing="0" border="1" bordercolor="#ADACAC">
                <thead>
                <tr>
                    <td class="${moduleId}_table_header ${moduleId}_align_text_left">
                        <spring:message code="${moduleId}.item" />
                    </td>
                </tr>
                <thead>
            </table>
        </td>
    </tr>
</table>
</div>

<%@ include file="/WEB-INF/template/footer.jsp"%>
