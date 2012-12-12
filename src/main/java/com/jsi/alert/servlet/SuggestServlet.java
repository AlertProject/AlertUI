package com.jsi.alert.servlet;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jms.JMSException;
import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jsi.alert.mq.callback.impl.MsgCallbackImpl;
import com.jsi.alert.utils.MessageParser;
import com.jsi.alert.utils.MessageUtils;
import com.jsi.alert.utils.Utils;

/**
 * A <code>Servlet</code> which handles suggestion requests.
 */
@WebServlet(name = "SuggestServlet", urlPatterns = {"/suggest"}, asyncSupported = true)
public class SuggestServlet extends MQServlet {
	
	private static final long serialVersionUID = 3704611136606008852L;
	
	private static final Logger log = LoggerFactory.getLogger(SuggestServlet.class);
	
	private static Set<String> availableTypes = new HashSet<String>(Arrays.asList(new String[] {"Other", "People", "Issues"}));
	

	/**
	 * @throws JMSException
	 * @see HttpServlet#HttpServlet()
	 */
	public SuggestServlet() throws JMSException {
		super();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	public void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
		try {
			Map<String, String[]> parameters = request.getParameterMap();
			
			if (log.isDebugEnabled())
				log.debug("Processing a suggestion request with parameters: " + parameters.toString());
				
			// get the suggestion type
			String suggType = null;
			for (String type : availableTypes) {
				if (parameters.containsKey(type)) {
					suggType = type;
					break;
				}
			}
			
			if (suggType == null)
				throw new IllegalArgumentException("Searching for suggestions for unknown type!!");
			
			// get the parameter
			String currInput = request.getParameter(suggType);
	
			// send the message
			String requestId = Utils.genRequestID();
			
			String requestMsg = MessageUtils.genKEUISuggestionMessage(currInput, "Other".equals(suggType) ? "People,Products,Sources,Issues" : suggType, requestId);
			
			final AsyncContext context = request.startAsync();
			getKEUIResponse(requestMsg, requestId, new MsgCallbackImpl(context) {
				@Override
				public void onSuccess(String msg) throws Exception {
					JSONArray responseJSon = MessageParser.parseKEUISuggestMessage(msg);
					
					Writer out = response.getWriter();
					responseJSon.writeJSONString(out);
					
					out.flush();
					out.close();
					context.complete();
				}
			});
		} catch (Throwable t) {
			log.error("An unexpected exception occurred!", t);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}
}
