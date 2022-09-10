package com.github.searchservice.authentication.model;

import lombok.Data;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data(staticConstructor = "of")
public class Authorities implements Serializable {
    @Serial
    private static final long serialVersionUID = -1370147532592877348L;

    private final List<GrantedAuthority> authorities;

    public Authorities(List<GrantedAuthority> authorities) {
        this.authorities = ObjectUtils.defaultIfNull(authorities, AuthorityUtils.NO_AUTHORITIES);
    }
}
