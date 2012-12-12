package com.jsi.alert.beans;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import javax.faces.context.PartialViewContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jsi.alert.model.Notification;
import com.jsi.alert.service.AuthenticatorService;
import com.jsi.alert.service.NotificationService;
import com.jsi.alert.utils.Configuration;
import com.jsi.alert.utils.Utils;

/**
 * A presenter for the index page.
 */
@ManagedBean
@RequestScoped
public class Index implements Serializable {

	private static final long serialVersionUID = 4046642011714896099L;
	private static final Logger log = LoggerFactory.getLogger(Index.class);
	
	@ManagedProperty(value="#{userPrincipal}") private UserPrincipal user;
	
	/**
	 * Fetches the session and checks if the user is authenticated.
	 */
	@PostConstruct
	private void init() {
		FacesContext context = FacesContext.getCurrentInstance();
		PartialViewContext pvc = context.getPartialViewContext();
		
		if (!pvc.isAjaxRequest() && AuthenticatorService.authenticateUser(user))
			fetchNotifications();
	}
	
	/**
	 * Fetches notifications and stores them in this class.
	 */
	public void fetchNotifications() {
		if (user == null || user.getUuid() == null) {
			log.warn("Tried to fetch notifications for an invalid user...");
			if (user != null)
				user.getNotifications().clear();
			return;
		}
		
		String uuid = Configuration.USE_DEFAULT_USER ? Configuration.DEFAULT_USER_NOTIFICATION_ID : user.getUuid();
		List<Notification> newNotifications = NotificationService.fetchNotifications(uuid);
		user.addNotifications(newNotifications);
	}
	
	public boolean isUserLoggedIn() {
		return user != null && user.isAuthenticated();
	}
	
	public String getLoginUrl() {
		return Utils.getLoginUrl();
	}
	
	public String getLogoutUrl() {
		try {
			if (user != null && user.getEmail() != null)
				return Utils.getLogoutUrl(user.getEmail());
			else if (log.isDebugEnabled())
				log.debug("Trying to construct logout URL for an invalid user: " + user);
		} catch (UnsupportedEncodingException e) {
			log.error("Failed to construct logout URL!", e);
		}
		return "";
	}

	public UserPrincipal getUser() {
		return user;
	}

	public void setUser(UserPrincipal user) {
		this.user = user;
	}
	
	public String getSubscribeUrl() {
		if (user != null && user.getUuid() != null && user.getEmail() != null) {
			return Utils.getSubscribeUrl(user.getUuid(), user.getEmail());
		} else {
			if (log.isWarnEnabled())
				log.error("Trying to construct a subscribe URL for an invalid user: " + user);
		}
		return "";
	}
	
	public String getOverviewUrl() {
		return Configuration.OVERVIEW_URL;
	}
	
	public String getAdminUrl() {
		return Utils.getAdminUrl();
	}
}
