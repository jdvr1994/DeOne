package com.apps.ing3ns.entregas.Modelos;

import java.util.Date;
import java.util.List;

/**
 * Created by JuanDa on 10/02/2018.
 */

public class Domiciliario {
    private String _id;
    private String email;
    private String password;
    private String name;
    private String avatar;
    private String phone;
    private int coins;
    private List<String> deliveries;
    private Position position;
    private String category;
    private Date signupDate;
    private Date lastLogin;
    private int state;

    public Domiciliario() {
    }

    public Domiciliario(String _id, String email, String password, String name, String avatar, String phone, int coins, List<String> deliveries, Position position, String category, Date signupDate, Date lastLogin, int state) {
        this._id = _id;
        this.email = email;
        this.password = password;
        this.name = name;
        this.avatar = avatar;
        this.phone = phone;
        this.coins = coins;
        this.deliveries = deliveries;
        this.position = position;
        this.category = category;
        this.signupDate = signupDate;
        this.lastLogin = lastLogin;
        this.state = state;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
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

    public int getCoins() {
        return coins;
    }

    public void setCoins(int coins) {
        this.coins = coins;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
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
