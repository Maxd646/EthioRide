package com.ethioride.shared.dto;

import com.ethioride.shared.enums.UserRole;
import java.io.Serializable;

public class UserDTO implements Serializable {
    private String id;
    private String fullName;
    private String phone;
    private String email;
    private UserRole role;
    private double rating;

    public UserDTO() {}

    public UserDTO(String id, String fullName, String phone, String email, UserRole role) {
        this.id = id;
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.role = role;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }
}
