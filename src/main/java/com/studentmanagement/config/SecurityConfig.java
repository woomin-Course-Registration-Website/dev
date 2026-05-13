package com.studentmanagement.config;

import com.studentmanagement.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final CorsConfigurationSource corsConfigurationSource;
    private final Environment environment;

    public SecurityConfig(JwtUtil jwtUtil, CorsConfigurationSource corsConfigurationSource,
                          Environment environment) {
        this.jwtUtil = jwtUtil;
        this.corsConfigurationSource = corsConfigurationSource;
        this.environment = environment;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        boolean prodProfile = environment.acceptsProfiles(Profiles.of("prod"));

        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> {
                auth.requestMatchers("/api/auth/**", "/api/health").permitAll();
                // K8s/ALB probe를 위해 Spring Boot Actuator의 health 그룹은 인증 없이 노출
                auth.requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll();
                // /api/dev/** 시드 엔드포인트는 non-prod 프로파일에서만 노출
                // (DevDataSeederController에도 @Profile("!prod") 적용되어 있으나 defense-in-depth)
                if (!prodProfile) {
                    auth.requestMatchers("/api/dev/**").permitAll();
                }
                auth.requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll();
                auth.anyRequest().authenticated();
            })
            .addFilterBefore(jwtAuthFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public OncePerRequestFilter jwtAuthFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain filterChain) throws ServletException, IOException {
                String token = resolveToken(request);
                if (StringUtils.hasText(token) && jwtUtil.isTokenValid(token)) {
                    String role = jwtUtil.getRole(token);
                    String email = jwtUtil.getEmail(token);
                    var auth = new UsernamePasswordAuthenticationToken(
                            email, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
                filterChain.doFilter(request, response);
            }

            private String resolveToken(HttpServletRequest request) {
                String bearer = request.getHeader("Authorization");
                if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
                    return bearer.substring(7);
                }
                return null;
            }
        };
    }
}
