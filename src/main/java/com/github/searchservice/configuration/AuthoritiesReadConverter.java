package com.github.searchservice.configuration;

import com.github.searchservice.authentication.model.Authorities;
import org.springframework.core.convert.converter.Converter;

import javax.annotation.Nullable;

import static org.springframework.security.core.authority.AuthorityUtils.commaSeparatedStringToAuthorityList;

public class AuthoritiesReadConverter implements Converter<String, Authorities> {

    @Override
    public Authorities convert(@Nullable String commaSeparatedAuthorities) {
        return Authorities.of(commaSeparatedStringToAuthorityList(commaSeparatedAuthorities));
    }
}
