package com.apps.ing3ns.entregas.Modelos;

import java.util.Date;
import java.util.List;

/**
 * Created by JuanDa on 10/02/2018.
 */

public class Client {
    private String _id;
    private String email;
    private String password;
    private String name;
    private String avatar;
    private String phone;
    private String address;
    private List<String> deliveries;
    private Position position;
    private Date signupDate;
    private Date lastLogin;

    public Client() {
    }

    public Client(String _id, String email, String password, String name, String avatar, String phone, String address, List<String> deliveries, Position position, Date signupDate, Date lastLogin) {
        this._id = _id;
        this.email = email;
        this.password = password;
        this.name = name;
        this.avatar = avatar;
        this.phone = phone;
        this.address = address;
        this.deliveries = deliveries;
        this.position = position;
        this.signupDate = signupDate;
        this.lastLogin = lastLogin;
    }

    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public List<String> getDeliveries() {
        return deliveries;
    }

    public void setDeliveries(List<String> deliveries) {
        this.deliveries = deliveries;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public Date getSignupDate() {
        return signupDate;
    }

    public void setSignupDate(Date signupDate) {
        this.signupDate = signupDate;
    }

    public Date getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(Date lastLogin) {
        this.lastLogin = lastLogin;
    }
}
