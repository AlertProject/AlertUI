package com.jsi.alert.servlet;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.jms.JMSException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.soap.SOAPException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jsi.alert.beans.UserPrincipal;
import com.jsi.alert.service.AuthenticatorService;
import com.jsi.alert.utils.Configuration;
import com.jsi.alert.utils.MessageParser;
import com.jsi.alert.utils.MessageUtils;
import com.jsi.alert.utils.Utils;

/**
 * Servlet implementation class QueryServlet
 */
public class QueryServlet extends MQServlet {
	
	private enum QueryType {
		PEOPLE("peopleData"),
		KEYWORD("keywordData"),
		TIMELINE("timelineData"),
		ITEM("itemData"),
		ISSUE_DETAILS("issueDetails"),
		COMMIT_DETAILS("commitDetails"),
		FORUM_DETAILS("forumDetails"),
		ITEM_DETAILS("itemDetails"),
		DUPLICATE_ISSUE("duplicateIssue"),
		SUGGEST_MY_CODE("suggestMyCode"),
		ISSUES_FOR_PERSON("suggestPeople"),
		PEOPLE_FOR_ISSUE("peopleForIssue");
		
		public final String value;
		
		private QueryType(String value) {
			this.value = value;
		}
	}

	private static final long serialVersionUID = 1079144340811966229L;
	
	private static final Logger log = LoggerFactory.getLogger(QueryServlet.class);

	public static final String USER_KEY = "user";
	
	private static final String TYPE_PARAM = "type";
	private static final String QUERY_PARAM = "query";


	/**
	 * @throws JMSException
	 *             If a connection to ActiveMQ cannot be established.
	 * @see HttpServlet#HttpServlet()
	 */
	public QueryServlet() throws JMSException {
		super();
	}

	/*
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		Map<String, String[]> parameterMap = request.getParameterMap();
		
		if (log.isDebugEnabled())
			log.debug("Received query request, params: " + parameterMap.toString() + "...");
		
		try {
			if (!parameterMap.containsKey(TYPE_PARAM))
				throw new IllegalArgumentException("The request doesn't contain parameter '" + TYPE_PARAM + "'!");
	
			response.setContentType("text/json");
			
			String type = request.getParameter(TYPE_PARAM);
	
			if (QueryType.PEOPLE.value.equals(type))
				processPeopleRq(request, response);
			else if (QueryType.KEYWORD.value.equals(type))
				processKeywordRq(request, response);
			else if (QueryType.TIMELINE.value.equals(type))
				processTimelineRq(request, response);
			else if (QueryType.ITEM.value.equals(type))
				processItemsRq(request, response);
			else if (QueryType.ISSUE_DETAILS.value.equals(type))
				processIssueDetailsRq(request, response);
			else if (QueryType.COMMIT_DETAILS.value.equals(type))
				processCommitDetailsRq(request, response);
			else if (QueryType.FORUM_DETAILS.value.equals(type))
				processForumDetailsRq(request, response);
			else if (QueryType.ITEM_DETAILS.value.equals(type))
				processItemDetailsRq(request, response);
			else if (QueryType.DUPLICATE_ISSUE.value.equals(type))
				processDuplicateIssueRq(request, response);
			else if (QueryType.SUGGEST_MY_CODE.value.equals(type))
				processRelatedMyCodeRq(request, response);
			else if (QueryType.ISSUES_FOR_PERSON.value.equals(type))
				processSuggestForPeopleRq(request, response);
			else if (QueryType.PEOPLE_FOR_ISSUE.value.equals(type))
				processSuggestDevelopersRq(request, response);
			else
				throw new IllegalArgumentException("An unexpected query type: " + type + "!");
			
			Writer writer = response.getWriter();
			writer.flush();
			writer.close();
		} catch (Throwable ex) {
			log.error("An unexpected exception occurred!", ex);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}
	
	private Properties createRequestProps(HttpServletRequest request) {
		Map<String, String[]> parameterMap = request.getParameterMap();
		
		Properties props = new Properties();
		for (String key : parameterMap.keySet()) {
			String value = request.getParameter(key);
			if (value != null && !value.isEmpty())
				props.put(key, value);
		}
		
		return props;
	}

	private void processPeopleRq(HttpServletRequest request, HttpServletResponse response) throws Exception {
		Properties props = createRequestProps(request);
		
		final String requestId = Utils.genRequestID();
		String requestMsg = MessageUtils.genKEUIPeopleMessage(props, requestId);
		String responseMsg = getKEUIResponse(requestMsg, requestId);
		JSONObject result = MessageParser.parseKEUIPeopleResponse(responseMsg);
		
		result.writeJSONString(response.getWriter());
	}
	
	private void processKeywordRq(HttpServletRequest request, HttpServletResponse response)  throws Exception {
		Properties props = createRequestProps(request);
		
		final String requestId = Utils.genRequestID();
		String requestMsg = MessageUtils.genKEUIKeywordMessage(props, requestId);
		String responseMsg = getKEUIResponse(requestMsg, requestId);
		JSONObject result = MessageParser.parseKEUIKeywordsResponse(responseMsg);
		
		result.writeJSONString(response.getWriter());
	}

	private void processTimelineRq(HttpServletRequest request, HttpServletResponse response)  throws Exception {
		Properties props = createRequestProps(request);
		
		final String requestId = Utils.genRequestID();
		String requestMsg = MessageUtils.genKEUITimelineMessage(props, requestId);
		String responseMsg = getKEUIResponse(requestMsg, requestId);
		JSONObject result = MessageParser.parseKEUITimelineResponse(responseMsg);
		
		result.writeJSONString(response.getWriter());
	}

	
	private void processItemsRq(HttpServletRequest request, HttpServletResponse response)  throws Exception {
		Properties props = createRequestProps(request);
		
		final String requestId = Utils.genRequestID();
		String requestMsg = MessageUtils.getKEUIItemsMessage(props, requestId);
		String responseMsg = getKEUIResponse(requestMsg, requestId);
		JSONObject result = MessageParser.parseKEUIItemsResponse(responseMsg);
		
		result.writeJSONString(response.getWriter());
	}
	
	private void processIssueDetailsRq(HttpServletRequest request, HttpServletResponse response) throws Exception {
		// check if the ID is a number
		String itemId = request.getParameter(QUERY_PARAM);
		String requestId = Utils.genRequestID();
		
		String requestMsg = MessageUtils.genAPIIssueDetailsMsg(itemId, requestId);
		String responseMsg = getAPIResponse(requestMsg, requestId);
		
		JSONObject result = MessageParser.parseAPIIssueDetailsMsg(responseMsg);
		
		result.writeJSONString(response.getWriter());
	}
	
	/**
	 * Processes the clients commit details request.
	 * 
	 * @param request
	 * @param response
	 * @throws Exception
	 */
	private void processCommitDetailsRq(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String itemId = request.getParameter(QUERY_PARAM);
		String requestId = Utils.genRequestID();

		String requestMsg = MessageUtils.genAPICommitDetailsMsg(itemId, requestId);
		String responseMsg = getAPIResponse(requestMsg, requestId);
		
		JSONObject result = MessageParser.parseAPICommitDetailsMessage(responseMsg);
		
		result.writeJSONString(response.getWriter());
	}
	
	/**
	 * Processes the clients forum post details request.
	 * 
	 * @param request
	 * @param response
	 * @throws ServletException 
	 * @throws JMSException 
	 * @throws IOException 
	 * @throws Exception
	 */
	private void processForumDetailsRq(HttpServletRequest request, HttpServletResponse response) throws JMSException, ServletException, IOException {
		String itemId = request.getParameter(QUERY_PARAM);
		String requestId = Utils.genRequestID();
		String requestMsg = MessageUtils.genKEUIForumDetailsMsg(itemId, requestId);
		
		String responseMsg = getKEUIResponse(requestMsg, requestId);
		JSONObject result = MessageParser.parseKEUIItemsResponse(responseMsg, true);
		
		result.writeJSONString(response.getWriter());
	}
	
	/**
	 * Processes the clients (general) Item details request.
	 * 
	 * @param request
	 * @param response
	 * @throws Exception
	 */
	private void processItemDetailsRq(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String itemId = request.getParameter(QUERY_PARAM);
		String requestId = Utils.genRequestID();
		String requestMsg = MessageUtils.genKEUIItemDetailsMessage(itemId, requestId);
		
		String responseMsg = getKEUIResponse(requestMsg, requestId);
		JSONObject result = MessageParser.parseKEUIItemDetailsMsg(responseMsg);
		
		result.writeJSONString(response.getWriter());
	}
	
	/**
	 * Processes the clients Duplicate issue detection request.
	 * 
	 * @param request
	 * @param response
	 * @throws Exception
	 */
	private void processDuplicateIssueRq(HttpServletRequest request, HttpServletResponse response) throws Exception {
		Properties props = createRequestProps(request);
		
		String requestId = Utils.genRequestID();
		String requestMsg = MessageUtils.genKEUIDuplicateIssueMsg(props, requestId);
		
		String responseMsg = getKEUIResponse(requestMsg, requestId);
		JSONObject result = MessageParser.parseKEUIDuplicateResponse(responseMsg);
		
		result.writeJSONString(response.getWriter());
	}
	
	/**
	 * Processes the Issues related to my code request.
	 * 
	 * @param request
	 * @param response
	 * @throws IOException
	 * @throws ServletException 
	 * @throws JMSException 
	 * @throws SOAPException 
	 */
	private void processRelatedMyCodeRq(HttpServletRequest request, HttpServletResponse response) throws IOException, JMSException, SOAPException, ServletException {
		// first check if the user is authenticated
		HttpSession session = request.getSession();
		
		boolean isAuthenticated = AuthenticatorService.authenticateUser(session.getAttribute(Configuration.USER_PRINCIPAL) != null ? (UserPrincipal) session.getAttribute(Configuration.USER_PRINCIPAL) : null);
		
		if (isAuthenticated) {
			UserPrincipal user = (UserPrincipal) session.getAttribute(Configuration.USER_PRINCIPAL);
			
			// send a message to Recommender to get the IDs of issues
			String uuid = user.getUuid();
			Integer offset = Utils.parseInt(request.getParameter("offset"));
			Integer limit = Utils.parseInt(request.getParameter("limit"));
			
			String requestId1 = Utils.genRequestID();
			String requestId2 = Utils.genRequestID();
			
			String apiRq = MessageUtils.genAPIIssuesForUserMsg(uuid, requestId1);
			String apiResponse = getAPIResponse(apiRq, requestId1);
			
			List<String> issueUris = MessageParser.parseAPIIssuesResponse(apiResponse);
			
			// now that I have the issueIDs I have to send them to the KEUI component
			String keuiRq = MessageUtils.genKEUIIssueListByUriMsg(issueUris, offset, limit, requestId2);
			String keuiResp = getKEUIResponse(keuiRq, requestId2);
			
			JSONObject result = MessageParser.parseKEUIItemsResponse(keuiResp);
			result.writeJSONString(response.getWriter());
		}
		else {
			// if no user => redirect to login
			if (log.isDebugEnabled())
				log.debug("User with no session searching for issues related to their code, sending code 401!");
			
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
		}
	}

	/**
	 * Processes the request for recommending developers to fix an issue.
	 * 
	 * @param request
	 * @param response
	 * @throws IOException 
	 * @throws ServletException 
	 * @throws JMSException 
	 */
	@SuppressWarnings("unchecked")
	private void processSuggestForPeopleRq(HttpServletRequest request, HttpServletResponse response) throws IOException, JMSException, ServletException {
		String uuid = request.getParameter("person");
		List<String> uuidList = Arrays.asList(new String[] {uuid});
		
		Integer offset = Utils.parseInt(request.getParameter("offset"));
		Integer limit = Utils.parseInt(request.getParameter("limit"));
		
		// send messages to Recommender to get the IDs
		final String requestId1 = Utils.genRequestID();
		
		String recommenderIssueRq = MessageUtils.genRecommenderIssuesMsg(uuidList, requestId1);
		String recommenderIssueResp = getRecommenderIssueResponse(recommenderIssueRq, requestId1);
		
		// parse responses
		List<Long> issueIds = MessageParser.parseRecommenderIssueIdsMsg(recommenderIssueResp);
		
		// now that I have the issueIDs I have to send them to the KEUI component
		final String requestId2 = Utils.genRequestID();
		String keuiRq = MessageUtils.genKEUIIssueListByIdMsg(issueIds, offset, limit, requestId2);
		String keuiResp = getKEUIResponse(keuiRq, requestId2);
		
		// construct result
		JSONObject result = MessageParser.parseKEUIItemsResponse(keuiResp);
		
		if (offset == 0) {
			// put modules on the first page only
			final String requestId3 = Utils.genRequestID();
			
			String recommenderModuleRq = MessageUtils.genRecommenderModulesMsg(uuidList, requestId3);
			String recommenderModuleResp = getRecommenderModuleResponse(recommenderModuleRq, requestId3);
		
			List<String> moduleIds = MessageParser.parseRecommenderModuleIdsMsg(recommenderModuleResp);
			
			JSONArray modulesJSon = new JSONArray();
			modulesJSon.addAll(moduleIds);
			
			result.put("modules", modulesJSon);
		}
		
		result.writeJSONString(response.getWriter());
	}
	
	/**
	 * Processes the request for suggesting developers who can fix an issue
	 * 
	 * @param request
	 * @param response
	 * @throws JMSException
	 * @throws ServletException
	 * @throws IOException
	 */
	private void processSuggestDevelopersRq(HttpServletRequest request, HttpServletResponse response) throws JMSException, ServletException, IOException {
		Long issueId = Long.parseLong(request.getParameter("issueId"));		
		
		String requestId = Utils.genRequestID();
		String requestMsg = MessageUtils.genRecommenderIdentityMsg(Arrays.asList(new Long[] {issueId}), requestId);
		String responseMsg = getRecommenderIdentityResponse(requestMsg, requestId);
		
		JSONObject result = MessageParser.parseRecommenderIdentitiesMsg(responseMsg);
		
		result.writeJSONString(response.getWriter());
	}
}
