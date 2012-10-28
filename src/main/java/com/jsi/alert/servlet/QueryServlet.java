package com.jsi.alert.servlet;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.jms.JMSException;
import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.MediaType;
import javax.xml.soap.SOAPException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jsi.alert.beans.UserPrincipal;
import com.jsi.alert.service.AuthenticatorService;
import com.jsi.alert.service.UniversalService;
import com.jsi.alert.service.UniversalService.RequestType;
import com.jsi.alert.servlet.callback.MsgCallbackImpl;
import com.jsi.alert.utils.Configuration;
import com.jsi.alert.utils.MessageParser;
import com.jsi.alert.utils.MessageUtils;
import com.jsi.alert.utils.Utils;

/**
 * Servlet implementation class QueryServlet
 */
@WebServlet(name = "QueryServlet", urlPatterns = {"/query"}, asyncSupported = true)
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
		PEOPLE_FOR_ISSUE("peopleForIssue"),
		PERSON_DETAILS("personDetails");
		
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
			else if (QueryType.PERSON_DETAILS.value.equals(type))
				processPersonDetailsRq(request, response);
			else
				throw new IllegalArgumentException("An unexpected query type: " + type + "!");
		} catch (Throwable ex) {
			log.error("An unexpected exception occurred!", ex);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	private Properties createRequestProps(ServletRequest request) {
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
		final AsyncContext context = request.startAsync();
		
		Properties props = createRequestProps(request);
		
		final String requestId = Utils.genRequestID();
		String requestMsg = MessageUtils.genKEUIPeopleMessage(props, requestId);
		
		getKEUIResponse(requestMsg, requestId, new MsgCallbackImpl(context) {
			@Override
			public void onSuccess(String msg) throws Exception {
				JSONObject result = MessageParser.parseKEUIPeopleResponse(msg);
				
				ServletResponse response = context.getResponse();
				result.writeJSONString(response.getWriter());
				
				Writer writer = response.getWriter();
				writer.flush();
				writer.close();
				context.complete();
			}
		});
	}
	
	private void processKeywordRq(HttpServletRequest request, HttpServletResponse response)  throws Exception {
		final AsyncContext context = request.startAsync();
		
		Properties props = createRequestProps(request);
		
		final String requestId = Utils.genRequestID();
		String requestMsg = MessageUtils.genKEUIKeywordMessage(props, requestId);
		
		getKEUIResponse(requestMsg, requestId, new MsgCallbackImpl(context) {
			@Override
			public void onSuccess(String msg) throws Exception {
				JSONObject result = MessageParser.parseKEUIKeywordsResponse(msg);
				
				Writer out = context.getResponse().getWriter();
				result.writeJSONString(out);
				out.flush();
				out.close();
				context.complete();
			}
		});
	}

	private void processTimelineRq(HttpServletRequest request, HttpServletResponse response)  throws Exception {
		final AsyncContext context = request.startAsync();
		
		Properties props = createRequestProps(request);
		
		final String requestId = Utils.genRequestID();
		String requestMsg = MessageUtils.genKEUITimelineMessage(props, requestId);
		
		getKEUIResponse(requestMsg, requestId, new MsgCallbackImpl(context) {
			@Override
			public void onSuccess(String msg) throws Exception {
				JSONObject result = MessageParser.parseKEUITimelineResponse(msg);
			
				Writer out = context.getResponse().getWriter();
				result.writeJSONString(out);
				out.flush();
				out.close();
				context.complete();
			}
		});
	}

	
	private void processItemsRq(HttpServletRequest request, HttpServletResponse response)  throws Exception {
		final AsyncContext context = request.startAsync();
		
		Properties props = createRequestProps(request);
		
		final String requestId = Utils.genRequestID();
		String requestMsg = MessageUtils.getKEUIItemsMessage(props, requestId);
		
		getKEUIResponse(requestMsg, requestId, new MsgCallbackImpl(context) {
			@Override
			public void onSuccess(String msg) throws Exception {
				JSONObject result = MessageParser.parseKEUIItemsResponse(msg);
				
				Writer out = context.getResponse().getWriter();
				result.writeJSONString(out);
				out.flush();
				out.close();
				context.complete();
			}
		});
	}
	
	private void processIssueDetailsRq(HttpServletRequest request, HttpServletResponse response) throws Exception {
		final AsyncContext context = request.startAsync();
		
		// check if the ID is a number
		String itemId = request.getParameter(QUERY_PARAM);
		String requestId = Utils.genRequestID();
		
		String requestMsg = MessageUtils.genAPIIssueDetailsMsg(itemId, requestId);
		
		getAPIResponse(requestMsg, requestId, new MsgCallbackImpl(context) {
			@Override
			public void onSuccess(String msg) throws Exception {
				JSONObject result = MessageParser.parseAPIIssueDetailsMsg(msg);
				
				Writer out = context.getResponse().getWriter();
				result.writeJSONString(out);
				out.flush();
				out.close();
				context.complete();
			}
		});
	}
	
	/**
	 * Processes the clients commit details request.
	 * 
	 * @param request
	 * @param response
	 * @throws Exception
	 */
	private void processCommitDetailsRq(HttpServletRequest request, HttpServletResponse response) throws Exception {
		final AsyncContext context = request.startAsync();
		
		String itemId = request.getParameter(QUERY_PARAM);
		String requestId = Utils.genRequestID();

		String requestMsg = MessageUtils.genAPICommitDetailsMsg(itemId, requestId);
		
		getAPIResponse(requestMsg, requestId, new MsgCallbackImpl(context) {
			@Override
			public void onSuccess(String msg) throws Exception {
				JSONObject result = MessageParser.parseAPICommitDetailsMessage(msg);
				
				Writer out = context.getResponse().getWriter();
				result.writeJSONString(out);
				out.flush();
				out.close();
				context.complete();
			}
		});
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
		final AsyncContext context = request.startAsync();
		
		String itemId = request.getParameter(QUERY_PARAM);
		String requestId = Utils.genRequestID();
		String requestMsg = MessageUtils.genKEUIForumDetailsMsg(itemId, requestId);
		
		getKEUIResponse(requestMsg, requestId, new MsgCallbackImpl(context) {
			@Override
			public void onSuccess(String msg) throws Exception {
				JSONObject result = MessageParser.parseKEUIItemsResponse(msg, true);
				
				Writer out = context.getResponse().getWriter();
				result.writeJSONString(out);
				out.flush();
				out.close();
				context.complete();
			}
		});
	}
	
	/**
	 * Processes the clients (general) Item details request.
	 * 
	 * @param request
	 * @param response
	 * @throws Exception
	 */
	private void processItemDetailsRq(HttpServletRequest request, HttpServletResponse response) throws Exception {
		final AsyncContext context = request.startAsync();
		
		String itemId = request.getParameter(QUERY_PARAM);
		String requestId = Utils.genRequestID();
		String requestMsg = MessageUtils.genKEUIItemDetailsMessage(itemId, requestId);
		
		getKEUIResponse(requestMsg, requestId, new MsgCallbackImpl(context) {
			@Override
			public void onSuccess(String msg) throws Exception {
				JSONObject result = MessageParser.parseKEUIItemDetailsMsg(msg);
				
				Writer out = context.getResponse().getWriter();
				result.writeJSONString(out);
				out.flush();
				out.close();
				context.complete();
			}
		});
	}
	
	/**
	 * Processes the clients Duplicate issue detection request.
	 * 
	 * @param request
	 * @param response
	 * @throws Exception
	 */
	private void processDuplicateIssueRq(HttpServletRequest request, HttpServletResponse response) throws Exception {
		final AsyncContext context = request.startAsync();
		
		Properties props = createRequestProps(request);
		
		String requestId = Utils.genRequestID();
		String requestMsg = MessageUtils.genKEUIDuplicateIssueMsg(props, requestId);
		
		getKEUIResponse(requestMsg, requestId, new MsgCallbackImpl(context) {
			@Override
			public void onSuccess(String msg) throws Exception {
				JSONObject result = MessageParser.parseKEUIDuplicateResponse(msg);
				
				Writer out = context.getResponse().getWriter();
				result.writeJSONString(out);
				out.flush();
				out.close();
				context.complete();
			}
		});
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
		final AsyncContext context = request.startAsync();
		
		// first check if the user is authenticated
		HttpSession session = request.getSession();
		
		boolean isAuthenticated = AuthenticatorService.authenticateUser(session.getAttribute(Configuration.USER_PRINCIPAL) != null ? (UserPrincipal) session.getAttribute(Configuration.USER_PRINCIPAL) : null);
		
		if (isAuthenticated) {
			UserPrincipal user = (UserPrincipal) session.getAttribute(Configuration.USER_PRINCIPAL);
			
			// send a message to Recommender to get the IDs of issues
			String uuid = user.getUuid();
			final Integer offset = Utils.parseInt(request.getParameter("offset"));
			final Integer limit = Utils.parseInt(request.getParameter("limit"));
			
			// first call API to get a list of issue URIs
			String requestId1 = Utils.genRequestID();
			String apiRq = MessageUtils.genAPIIssuesForUserMsg(uuid, requestId1);
			
			getAPIResponse(apiRq, requestId1, new MsgCallbackImpl(context) {
				@Override
				public void onSuccess(String apiResponse) throws Exception {
					List<String> issueUris = MessageParser.parseAPIIssuesResponse(apiResponse);
				
					// now that I have the URIs, I have to call KEUI, to get the actual items
					String requestId2 = Utils.genRequestID();
					String keuiRq = MessageUtils.genKEUIIssueListByUriMsg(issueUris, offset, limit, requestId2);
				
					getKEUIResponse(keuiRq, requestId2, new MsgCallbackImpl(context) {
						@Override
						public void onSuccess(String keuiResp) throws Exception {
							JSONObject result = MessageParser.parseKEUIItemsResponse(keuiResp);
							
							Writer out = context.getResponse().getWriter();
							result.writeJSONString(out);
							out.flush();
							out.close();
							context.complete();
						}
					});
				}
			});
		}
		else {
			// if no user => send unauthorized
			if (log.isDebugEnabled())
				log.debug("User with no session searching for issues related to their code, sending code 401!");
			
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
			context.complete();
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
		final AsyncContext context = request.startAsync();
		
		final List<String> uuidList = Arrays.asList(new String[] {request.getParameter("person")});
		
		final Integer offset = Utils.parseInt(request.getParameter("offset"));
		final Integer limit = Utils.parseInt(request.getParameter("limit"));
		
		// send messages to Recommender to get the IDs
		final String requestId1 = Utils.genRequestID();
		
		String recommenderIssueRq = MessageUtils.genRecommenderIssuesMsg(uuidList, requestId1);
		getRecommenderIssueResponse(recommenderIssueRq, requestId1, new MsgCallbackImpl(context) {
			@Override
			public void onSuccess(String recommenderIssueResp) throws Exception {
				List<Long> issueIds = MessageParser.parseRecommenderIssueIdsMsg(recommenderIssueResp);
			
				// now that I have the issueIDs I have to send them to the KEUI component
				final String requestId2 = Utils.genRequestID();
				String keuiRq = MessageUtils.genKEUIIssueListByIdMsg(issueIds, offset, limit, requestId2);
			
				getKEUIResponse(keuiRq, requestId2, new MsgCallbackImpl(context) {
					@Override
					public void onSuccess(String keuiResp) throws Exception {
						final JSONObject result = MessageParser.parseKEUIItemsResponse(keuiResp);
						
						// on the first page also show the modules
						if (offset == 0) {
							final String requestId3 = Utils.genRequestID();
							String recommenderModuleRq = MessageUtils.genRecommenderModulesMsg(uuidList, requestId3);
						
							getRecommenderModuleResponse(recommenderModuleRq, requestId3, new MsgCallbackImpl(context) {
								@Override
								public void onSuccess(String recommenderModuleResp) throws Exception {
									List<String> moduleIds = MessageParser.parseRecommenderModuleIdsMsg(recommenderModuleResp);
									
									JSONArray modulesJSon = new JSONArray();
									modulesJSon.addAll(moduleIds);
									result.put("modules", modulesJSon);
									
									Writer out = context.getResponse().getWriter();
									result.writeJSONString(out);
									out.flush();
									out.close();
									context.complete();
								}
							});
						} else {
							Writer out = context.getResponse().getWriter();
							result.writeJSONString(out);
							out.flush();
							out.close();
							context.complete();
						}
					}
				});
			}
		});
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
		final AsyncContext context = request.startAsync();
		context.setTimeout(20000);	// TODO delete me
		
		Long issueId = Long.parseLong(request.getParameter("issueId"));		
		
		String requestId = Utils.genRequestID();
		String requestMsg = MessageUtils.genRecommenderIdentityMsg(Arrays.asList(new Long[] {issueId}), requestId);
		
		getRecommenderIdentityResponse(requestMsg, requestId, new MsgCallbackImpl(context) {
			@Override
			public void onSuccess(String msg) throws Exception {
				JSONObject result = MessageParser.parseRecommenderIdentitiesMsg(msg);
				
				Writer out = context.getResponse().getWriter();
				result.writeJSONString(out);
				out.flush();
				out.close();
				context.complete();
			}
		});
	}
	
	private void processPersonDetailsRq(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String result = UniversalService.fetchUrl(Utils.getUserInfoUrl(request.getParameter("uuid")), new HashMap<String, String>(), MediaType.APPLICATION_JSON, RequestType.GET);
	
		Writer out = response.getWriter();
		if (result != null)
			out.write(result);
		
		out.flush();
		out.close();
	}
}
