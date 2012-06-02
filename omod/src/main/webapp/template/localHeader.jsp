<ul id="menu">
	<li class="first">
		<a href="${pageContext.request.contextPath}/admin"><spring:message code="admin.title.short" /></a>
	</li>

	<li <c:if test='<%= request.getRequestURI().contains("/viewAuditLog") %>'>class="active"</c:if>>
		<a href="${pageContext.request.contextPath}/module/${moduleId}/viewAuditLog.htm">
			<spring:message	code="${moduleId}.viewAuditLog" />
		</a>
	</li>
	
	<%-- Add further links here --%>
</ul>

<h2><spring:message code="${moduleId}.title" /></h2>