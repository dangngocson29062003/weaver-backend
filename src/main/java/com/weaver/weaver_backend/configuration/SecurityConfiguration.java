package com.weaver.weaver_backend.configuration;


import com.weaver.weaver_backend.security.CustomAuthenticationEntryPoint;
import com.weaver.weaver_backend.security.JwtAuthenticationFilter;
import com.weaver.weaver_backend.service.IAuthService;
import com.weaver.weaver_backend.security.oauth2.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
public class SecurityConfiguration {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/auth/**",
            "/logo.png"
    };
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    OAuth2SuccessHandler oAuth2SuccessHandler(IAuthService authenticationService) {
        return new OAuth2SuccessHandler(authenticationService);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, OAuth2SuccessHandler oAuth2SuccessHandler) throws Exception {
        return http
                .cors(cors->{})
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests ->
                        requests.requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                                .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .oauth2Login(oauth -> oauth.successHandler(oAuth2SuccessHandler))
                .securityContext(context -> context.requireExplicitSave(false))
                .build();
    }

}
