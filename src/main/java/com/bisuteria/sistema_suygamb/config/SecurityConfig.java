package com.bisuteria.sistema_suygamb.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.provisioning.InMemoryUserDetailsManager;

import org.springframework.security.web.SecurityFilterChain;
@Configuration
public class SecurityConfig {

    @Bean
    public InMemoryUserDetailsManager users(){

        UserDetails jefe = User.builder()
                .username("jefe")
                .password(passwordEncoder().encode("123"))
                .roles("JEFE")
                .build();

        UserDetails empleado = User.builder()
                .username("empleado")
                .password(passwordEncoder().encode("123"))
                .roles("EMPLEADO")
                .build();

        return new InMemoryUserDetailsManager(
                jefe,
                empleado
        );
    }

    @Bean
    public PasswordEncoder passwordEncoder(){

        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception{

        http
            .csrf(csrf -> csrf.disable())

            	.authorizeHttpRequests(auth -> auth
            	.requestMatchers(
            		   "/login",
            		   "/logo/**"
            	).permitAll()
            		

                .requestMatchers("/dashboard")
                .hasAnyRole("JEFE")

                .requestMatchers("/guardar")
                .hasRole("JEFE")

                .requestMatchers("/editar/**")
                .hasRole("JEFE")

                .requestMatchers("/eliminar/**")
                .hasRole("JEFE")
                
                .requestMatchers("/historial")
                .hasRole("JEFE")
                
                .requestMatchers("/productos-mas-vendidos")
                .hasAnyRole("JEFE","EMPLEADO")

                .requestMatchers("/ventas")
                .hasAnyRole("JEFE","EMPLEADO")
                
                .requestMatchers(
                        "/",
                        "/guardar",
                        "/actualizar",
                        "/editar/**",
                        "/eliminar/**",
                        "/historial"
                ).hasRole("JEFE")




                .requestMatchers(
                        "/empleado",
                        "/ventas",
                        "/guardarVenta"
                ).hasAnyRole("JEFE","EMPLEADO")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                    .defaultSuccessUrl("/", true)
                    .permitAll()
            )
            .formLogin(form -> form
                    .loginPage("/login")
                    .successHandler((request, response, authentication) -> {

                        boolean esJefe =
                                authentication.getAuthorities()
                                .stream()
                                .anyMatch(a ->
                                        a.getAuthority()
                                        .equals("ROLE_JEFE"));


                        if(esJefe){

                            // JEFE VA AL DASHBOARD
                            response.sendRedirect("/dashboard");

                        }else{

                            // EMPLEADO VA A VENTAS
                            response.sendRedirect("/ventas");

                        }

                    })
                    .permitAll()
            )

            .logout(logout -> logout
                    .logoutSuccessUrl("/login")
            )

            .exceptionHandling(exception -> exception
            	    .accessDeniedHandler((request, response, accessDeniedException) -> {

            	        response.sendRedirect("/ventas?bloqueado=true");

            	    })
            	);

        return http.build();
    }
    
}