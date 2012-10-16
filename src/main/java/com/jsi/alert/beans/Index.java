package com.jsi.alert.beans;

import java.io.Serializable;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jsi.alert.model.Notification;
import com.jsi.alert.service.AuthenticatorService;
import com.jsi.alert.service.NotificationService;
import com.jsi.alert.utils.Configuration;

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
	@SuppressWarnings("unused")
	@PostConstruct
	private void init() {
		if (AuthenticatorService.authenticateUser(user))
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
		
		String uuid = Configuration.NOTIFICATION_DEFAULT_USER == null ? user.getUuid() : Configuration.NOTIFICATION_DEFAULT_USER;
		List<Notification> newNotifications = NotificationService.fetchNotifications(uuid);
		user.getNotifications().addAll(newNotifications);
	}
	
	public boolean isUserLoggedIn() {
		return user != null && user.isAuthenticated();
	}
	
	public String getLoginUrl() {
		return Configuration.LOGIN_URL;
	}
	
	public String getLogoutUrl() {
		return Configuration.LOGOUT_URL;
	}

	public UserPrincipal getUser() {
		return user;
	}

	public void setUser(UserPrincipal user) {
		this.user = user;
	}
}
