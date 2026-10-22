
package com.mayank.cabbooking.dto;

import com.mayank.cabbooking.entity.Auth;
import com.mayank.cabbooking.entity.Role;

public class AuthResponse {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private Role role;
    private boolean active;

    public AuthResponse(Auth user) {
        this.id = user.getId();
        this.name = user.getName();
        this.email = user.getEmail();
        this.phone = user.getPhone();
        this.role = user.getRole();
        this.active = user.isActive();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public Role getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }
}
