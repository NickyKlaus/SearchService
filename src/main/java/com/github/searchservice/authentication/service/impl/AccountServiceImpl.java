package com.github.searchservice.authentication.service.impl;

import com.github.searchservice.authentication.repository.AccountRepository;
import com.github.searchservice.authentication.service.AccountService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AccountServiceImpl implements AccountService, UserDetailsService {
    private final AccountRepository accountRepository;

    public AccountServiceImpl(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String enteredUserId) throws UsernameNotFoundException {
        var account = accountRepository.findById(enteredUserId);
        if (account.isEmpty()) {
            throw new UsernameNotFoundException(enteredUserId);
        }
        return new User(
                enteredUserId,
                account.get().getPassword(),
                account.get().getAuthorities().getAuthorities());
    }
}
