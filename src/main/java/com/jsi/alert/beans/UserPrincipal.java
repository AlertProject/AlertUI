package com.jsi.alert.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import com.jsi.alert.model.Notification;

@ManagedBean
@SessionScoped
public class UserPrincipal implements Serializable {

	private static final long serialVersionUID = -6511117109181574707L;
	
	private String email;
	private String uuid;
	private Boolean admin;
	private boolean authenticated;
	private List<Notification> notifications;
	
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getUuid() {
		return uuid;
	}
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	public Boolean getAdmin() {
		return admin;
	}
	public void setAdmin(Boolean admin) {
		this.admin = admin;
	}
	public List<Notification> getNotifications() {
		if (notifications == null) notifications = new ArrayList<>();
		return notifications;
	}
	public void setNotifications(List<Notification> notifications) {
		this.notifications = notifications;
	}
	
	public boolean isAuthenticated() {
		return authenticated;
	}
	
	public void setAuthenticated(boolean authenticated) {
		this.authenticated = authenticated;
	}
	
	/**
	 * Clears all the fields
	 */
	public void clear() {
		setAdmin(null);
		setAuthenticated(false);
		setEmail(null);
		setNotifications(new ArrayList<Notification>());
		setUuid(null);
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("UserPrincipal: [email: %s, uuid: %s, admin: %b, authenticated: %b]", email, uuid, admin, authenticated);
	}
}
