package com.github.searchservice.authentication.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.cassandra.core.mapping.Table;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Table
@Data
public class Account implements Serializable {
    @Serial
    private static final long serialVersionUID = 9067835128382293650L;

    @Id
    private final String id;
    private final String password;
    private final Authorities authorities;
    private final Date dateCreated;
}

