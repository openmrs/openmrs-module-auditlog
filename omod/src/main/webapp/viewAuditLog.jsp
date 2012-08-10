<%@ include file="/WEB-INF/template/include.jsp"%>

<%@ include file="/WEB-INF/template/header.jsp"%>

<%@ include file="/WEB-INF/view/module/auditlog/include.jsp"%>

<%@ include file="template/localHeader.jsp"%>

<openmrs:htmlInclude file="/scripts/jquery/dataTables/css/dataTables_jui.css"/>
<openmrs:htmlInclude file="/scripts/jquery/dataTables/js/jquery.dataTables.min.js"/>

<script type="text/javascript">
	$j(document).ready(function() {
		$j('#${moduleId}').dataTable({
		    sPaginationType: "full_numbers",
		    iDisplayLength: 15,
		    bJQueryUI: true,
		    bSort:false,
		    //sDom: 'flt<"ui-helper-clearfix"ip>',
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
	});
</script>

<div class="box">
<b class="boxHeader" style="width: auto;"><spring:message code="${moduleId}.auditlogs" /></b>
<br />
<table id="${moduleId}" width="100%" cellpadding="3" cellspacing="0" align="left">
	<thead>
		<tr>
			<th class="ui-state-default">Item</th>
			<th class="ui-state-default">Uuid</th>
			<th class="ui-state-default">User</th>
			<th class="ui-state-default">Date Of Occurence</th>
		</tr>
	</thead>
	<tbody>
	<c:forEach items="${auditLogs}" var="auditLog">
		<tr class="${moduleId}_${auditLog.action}"
		   <c:if test="${auditLog.action == 'UPDATED'}">onclick="javascript:alert('This was updated')"</c:if>>
   			<td>${auditLog.className}</td>
   			<td>${auditLog.objectUuid}</td>
   			<td>${auditLog.user.personName}</td>
   			<td><openmrs:formatDate date="${auditLog.dateCreated}" type="long" /></td>
		</tr>
	</c:forEach>
	</tbody> 
</table>
</div>

<%@ include file="/WEB-INF/template/footer.jsp"%>
