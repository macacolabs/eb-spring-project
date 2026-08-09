package com.ohgiraffers.restapi.member.dto;

import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class MemberDTO implements UserDetails {

    private int memberCode;
    private String memberId;
    private String memberPassword;
    private String memberName;
    private String memberEmail;
    private String memberStatus;
    private List<MemberRoleDTO> memberRole;
    private Collection<GrantedAuthority> authorities;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if(authorities != null && !authorities.isEmpty()) {
            return authorities;
        }

        Collection<GrantedAuthority> authorities = new ArrayList<>();
        if(memberRole != null) {
            memberRole.forEach(role -> {
                authorities.add(() -> role.getAuthority().getAuthorityName());
            });
            return authorities;
        }
        return new ArrayList<>();
    }

    @Override
    public String getPassword() {
        return this.memberPassword;
    }

    @Override
    public String getUsername() {
        return this.memberId;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return !"N".equals(memberStatus);
    }
}
