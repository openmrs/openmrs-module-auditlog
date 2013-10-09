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
    var auditLogDetailsMap = new Object();
    var dialogObj;
    var childDialogObj;
    var dialogWidth = 1140;
    var dialogHeight = 640;
    var childDialogWidth = dialogWidth*0.9;
    var childDialogHeight = dialogHeight*0.9;
    var changesLabel = '<spring:message code="${moduleId}.changes" />';
    var lastKnownStateLabel = '<spring:message code="${moduleId}.lastKnownState" />';

    $j(document).ready(function() {
        $j('#${moduleId}').dataTable({
            sPaginationType: "full_numbers",
            iDisplayLength: 15,
            bJQueryUI: true,
            bSort:false,
            sDom: '<fl>t<"ui-helper-clearfix"ip>',
            oLanguage: {
                "sInfo": omsgs.sInfoLabel,
                "oPaginate": {"sFirst": omsgs.first, "sPrevious": omsgs.previous, "sNext": omsgs.next, "sLast": omsgs.last},
                "sZeroRecords": omsgs.noMatchesFound,
                "sInfoEmpty": " ",
                "sLengthMenu": omsgs.showNumberofEntries
            },
            fnDrawCallback: function( oSettings ) {
                //remove jquery row striping so that we have our custom green for created, pink for deleted etc.
                $j('table#${moduleId} tr.odd').removeClass('odd');
                $j('table#${moduleId} tr.even').removeClass('even');
            }
        });

        //setup the dialog to display update changes for each row
        dialogObj = $j("#${moduleId}-changes-dialog");
        dialogObj.dialog({
            autoOpen: false,
            width: dialogWidth+'px',
            height: dialogHeight,
            modal: true,
            beforeClose: function(event, ui){
                //reset
                $j("#${moduleId}-changes-objectId").html("");
                $j("#${moduleId}-changes-summary").html("");
                $j("#${moduleId}-changes-objectUuid").html("");
                $j("#${moduleId}-changes-openmrsVersion").html("");
                $j("#${moduleId}-childLogCount").html("");
                //remove all rows from previous displays except the header rows
                $j("#${moduleId}-changes-table tr:gt(0)").remove();
                $j("#${moduleId}-delete-otherData-table tr:gt(0)").remove();
                $j("#${moduleId}-childAuditLogDetails-table tr:gt(0)").remove();
                $j("#${moduleId}-details .${moduleId}-changes-element").hide();
                $j("#${moduleId}-details .${moduleId}-childAuditLogDetails-element").hide()
            }
        });

        childDialogObj = $j("#${moduleId}-child-changes-dialog");
        childDialogObj.dialog({
            autoOpen: false,
            width: childDialogWidth+'px',
            height: childDialogHeight,
            modal: true,
            beforeClose: function(event, ui){
                //reset
                $j("#${moduleId}-child-changes-objectId").html("");
                $j("#${moduleId}-child-changes-summary").html("");
                $j("#${moduleId}-child-changes-objectUuid").html("");
                $j("#${moduleId}-child-changes-openmrsVersion").html("");
                $j("#${moduleId}-child-childLogCount").html("");
                //remove all rows from previous displays except the header rows
                $j("#${moduleId}-child-changes-table tr:gt(0)").remove();
                $j("#${moduleId}-child-delete-otherData-table tr:gt(0)").remove();
                $j("#${moduleId}-child-childAuditLogDetails-table tr:gt(0)").remove();
                $j("#${moduleId}-child-details .${moduleId}-changes-element").hide();
                $j("#${moduleId}-child-details .${moduleId}-childAuditLogDetails-element").hide()
            }
        });
    });

    function ${moduleId}_showDetails(auditLogUuid, isChildLog){
        var existingLogDetails = auditLogDetailsMap[auditLogUuid];
        if(!existingLogDetails){
            DWRAuditLogService.getAuditLogDetails(auditLogUuid, function(detailsResponse){
                if(detailsResponse){
                    displayLogDetails(detailsResponse, isChildLog);
                    auditLogDetailsMap[auditLogUuid] = detailsResponse;
                }
            });
        }else{
            displayLogDetails(existingLogDetails, isChildLog);
        }
    }

    function displayLogDetails(logDetails, isChildLog){
        var idPart = (isChildLog) ? "-child" : "";
        $j("#${moduleId}"+idPart+"-changes-table").hide();
        $j("#${moduleId}"+idPart+"-delete-otherData-table").hide();
        if(logDetails){
            if(logDetails.objectExists == true){
                $j("#${moduleId}"+idPart+"-changes-summary").html(logDetails.displayString);
            }
            else if(logDetails.action != 'DELETED'){
                $j("#${moduleId}"+idPart+"-changes-summary").html("<span class='${moduleid}_deleted'><spring:message code="${moduleId}.objectDoesnotExist" /></span>");
            }

            if(logDetails.objectId)
                $j("#${moduleId}"+idPart+"-changes-objectId").html(logDetails.objectId);

            if(logDetails.objectUuid)
                $j("#${moduleId}"+idPart+"-changes-objectUuid").html(logDetails.objectUuid);

            if(logDetails.classname)
                $j("#${moduleId}"+idPart+"-changes-classname").html(logDetails.classname);

            if(logDetails.openmrsVersion)
                $j("#${moduleId}"+idPart+"-changes-openmrsVersion").html(logDetails.openmrsVersion);

            if(logDetails.changes){
                var auditLogChanges = logDetails.changes;
                var isUpdate = logDetails.action == 'UPDATED' ? true : false;
                var otherDataCount = 0;
                $j.each(auditLogChanges, function(propertyName){
                    otherDataCount++;
                    var currentProperty = auditLogChanges[propertyName];
                    if(isUpdate){
                        $j("#${moduleId}"+idPart+"-changes-table tr:last").after(
                            "<tr>" +
                                "<td class=\"${moduleId}_align_text_left\" valign=\"top\">"+propertyName+"</td>"+
                                "<td class=\"${moduleId}_align_text_left\" valign=\"top\">"+currentProperty[0]+"</td>"+
                                "<td class=\"${moduleId}_align_text_left\" valign=\"top\">"+currentProperty[1]+"</td>" +
                            "</tr>");
                    }else{
                        $j("#${moduleId}"+idPart+"-delete-otherData-table tr:last").after(
                             "<tr>" +
                                 "<td class=\"${moduleId}_align_text_left\" valign=\"top\">"+propertyName+"</td>"+
                                 "<td class=\"${moduleId}_align_text_left\" valign=\"top\">"+currentProperty+"</td>" +
                             "</tr>");
                    }
                });

                if(otherDataCount > 0){
                    var otherDataLabel = "";
                    if(isUpdate){
                        otherDataLabel = changesLabel;
                        $j("#${moduleId}"+idPart+"-changes-table").show();

                    }else{
                        otherDataLabel = lastKnownStateLabel;
                        $j("#${moduleId}"+idPart+"-delete-otherData-table").show();
                    }

                    $j("#${moduleId}"+idPart+"-otherDataLabel").html("<b>"+otherDataLabel+"</b>");
                    $j("#${moduleId}"+idPart+"-details .${moduleId}-changes-element").show();
                }
            }

            if(logDetails.childAuditLogDetails){
                childAuditLogDetails = logDetails.childAuditLogDetails;
                $j("#${moduleId}"+idPart+"-childLogCount").html(childAuditLogDetails.length);
                $j.each(childAuditLogDetails, function(index, detail){
                    $j("#${moduleId}"+idPart+"-childAuditLogDetails-table tr:last").after(
                            "<tr class=\"${moduleId}_"+detail.action+" ${moduleId}_child_log\" onclick=\"${moduleId}_showDetails('"+detail.uuid+"', true)\">"+
                                    "<td class=\"${moduleId}_align_text_left\" valign=\"top\">"+detail.classname+"</td></tr>");
                });
                $j("#${moduleId}"+idPart+"-details .${moduleId}-childAuditLogDetails-element").show();
            }

            if(isChildLog)
                childDialogObj.dialog('open');
            else
                dialogObj.dialog('open');
        }
    }

</script>

<div class="box">
    <b class="boxHeader" style="width: auto;"><spring:message code="${moduleId}.auditlogs" /></b>
    <br />
    <table id="${moduleId}" width="100%" cellpadding="3" cellspacing="0" align="left">
        <thead>
        <tr>
            <th class="ui-state-default"></th>
            <th class="ui-state-default">Item (<spring:message code="${moduleId}.numberOfLinkedAuditLogs" />)</th>
            <th class="ui-state-default">User (username)</th>
            <th class="ui-state-default">Date Of Occurence</th>
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
                    ${auditLog.simpleClassname}
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
        <th valign="top" class="${moduleId}_align_text_left" style="width:15%"><spring:message code="${moduleId}.uuid" /></th>
        <td id="${moduleId}-changes-objectUuid" width="100%"></td>
    </tr>
    <tr>
        <th valign="top" class="${moduleId}_align_text_left"><spring:message code="${moduleId}.id" /></th>
        <td id="${moduleId}-changes-objectId" width="100%"></td>
    </tr>
    <tr>
        <th valign="top" class="${moduleId}_align_text_left"><spring:message code="${moduleId}.classname" /></th>
        <td id="${moduleId}-changes-classname" width="100%"></td>
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
            <b><spring:message code="${moduleId}.linkedAuditLogDetails" />
                (<span id="${moduleId}-childLogCount"></span>)
            </b>
            <img align="top" src="<openmrs:contextPath />/images/help.gif" border="0"
                 title="<openmrs:message code="${moduleId}.linkedAuditLogDetails.help" />" />
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

<div id="${moduleId}-child-changes-dialog" class="${moduleId}_align_text_left" title="<spring:message code="${moduleId}.linkedLogDetails" />">
<br />
<table id="${moduleId}-child-details" width="100%" cellpadding="0" cellspacing="5">
    <tr>
        <th valign="top" class="${moduleId}_align_text_left" style="width:15%"><spring:message code="${moduleId}.uuid" /></th>
        <td id="${moduleId}-child-changes-objectUuid" width="100%"></td>
    </tr>
    <tr>
        <th valign="top" class="${moduleId}_align_text_left"><spring:message code="${moduleId}.id" /></th>
        <td id="${moduleId}-child-changes-objectId" width="100%"></td>
    </tr>
    <tr>
        <th valign="top" class="${moduleId}_align_text_left"><spring:message code="${moduleId}.classname" /></th>
        <td id="${moduleId}-child-changes-classname" width="100%"></td>
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
            <b><spring:message code="${moduleId}.linkedAuditLogDetails" />
                (<span id="${moduleId}-child-childLogCount"></span>)
            </b>
            <img align="top" src="<openmrs:contextPath />/images/help.gif" border="0"
                 title="<openmrs:message code="${moduleId}.linkedAuditLogDetails.help" />" />
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
