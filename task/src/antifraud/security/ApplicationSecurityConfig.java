package antifraud.security;

import antifraud.authentication.CustomizedAuthenticationEntryPoint;
import antifraud.user.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableWebSecurity
public class ApplicationSecurityConfig extends WebSecurityConfigurerAdapter {

    private final PasswordEncoder passwordEncoder;
    private final UserDetailsServiceImpl userDetailsService;

    @Autowired
    public ApplicationSecurityConfig(PasswordEncoder passwordEncoder, UserDetailsServiceImpl userDetailsService) {
        this.passwordEncoder = passwordEncoder;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.httpBasic()
                .authenticationEntryPoint(restAuthenticationEntryPoint()) // Handles auth error
                .and()
                .csrf().disable().headers().frameOptions().disable() // for Postman, the H2 console
                .and()
                .authorizeRequests() // manage access
                .antMatchers(HttpMethod.POST, "/api/auth/user").permitAll()
                .antMatchers("/actuator/shutdown").permitAll() // needs to run test
                .mvcMatchers(HttpMethod.POST, "/api/antifraud/transaction").hasAuthority("MERCHANT")
                .mvcMatchers(HttpMethod.PUT, "/api/antifraud/transaction").hasAuthority("SUPPORT")
                .mvcMatchers(HttpMethod.GET, "/api/antifraud/history").hasAuthority("SUPPORT")
                .antMatchers(HttpMethod.GET, "/api/antifraud/history/**").hasAuthority("SUPPORT")
                .mvcMatchers(HttpMethod.POST, "/api/antifraud/suspicious-ip").hasAuthority("SUPPORT")
                .antMatchers(HttpMethod.DELETE, "/api/antifraud/suspicious-ip/**").hasAuthority("SUPPORT")
                .mvcMatchers(HttpMethod.GET, "/api/antifraud/suspicious-ip").hasAuthority("SUPPORT")
                .mvcMatchers(HttpMethod.POST, "/api/antifraud/stolencard").hasAuthority("SUPPORT")
                .mvcMatchers(HttpMethod.DELETE, "/api/antifraud/stolencard").hasAuthority("SUPPORT")
                .mvcMatchers(HttpMethod.GET, "/api/antifraud/stolencard").hasAuthority("SUPPORT")
                .antMatchers(HttpMethod.GET, "/api/auth/list").hasAnyAuthority("ADMINISTRATOR", "SUPPORT")
                .antMatchers(HttpMethod.DELETE,"/api/auth/user/**").hasAuthority("ADMINISTRATOR")
                .mvcMatchers(HttpMethod.PUT, "/api/auth/access").hasAuthority("ADMINISTRATOR")
                .mvcMatchers(HttpMethod.PUT, "/api/auth/role").hasAuthority("ADMINISTRATOR")
                .and()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS); // no session
    }


    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(daoAuthenticationProvider());
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setPasswordEncoder(passwordEncoder);
        provider.setUserDetailsService(userDetailsService);
        return provider;
    }

    @Bean
    public CustomizedAuthenticationEntryPoint restAuthenticationEntryPoint() {
        return new CustomizedAuthenticationEntryPoint();
    }
}
