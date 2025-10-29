package com.nazran.chat.security;

import lombok.Getter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * Custom authentication token that extends {@link UsernamePasswordAuthenticationToken}
 * to include a user ID along with the standard principal, credentials, and authorities.
 *
 * @see UsernamePasswordAuthenticationToken
 */
@Getter
public class CustomAuthenticationToken extends UsernamePasswordAuthenticationToken {

    /**
     * -- GETTER --
     *  Returns the user ID associated with this authentication token.
     */
    private final Integer userId;

    /**
     * Creates a new {@code CustomAuthenticationToken} instance with the specified principal,
     * credentials, authorities, and user ID.
     *
     * @param principal   the principal (typically a username or user object)
     * @param credentials the credentials (typically a password)
     * @param authorities the collection of granted authorities for the principal
     * @param userId      the unique identifier of the user
     * @throws IllegalArgumentException if the principal, credentials, or authorities are null
     */
    public CustomAuthenticationToken(Object principal, Object credentials,
                                     Collection<? extends GrantedAuthority> authorities, Integer userId) {
        super(principal, credentials, authorities);
        this.userId = userId;
    }

}