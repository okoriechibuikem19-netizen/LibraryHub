package com.libraryhub.api.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

@Component
public class JwtCookieFilter extends OncePerRequestFilter {
    public static final String COOKIE="libraryhub_session";
    private final JwtService jwt;
    public JwtCookieFilter(JwtService jwt){this.jwt=jwt;}
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException{
        String token=null;
        if(request.getCookies()!=null)for(Cookie cookie:request.getCookies())if(COOKIE.equals(cookie.getName()))token=cookie.getValue();
        JwtService.Session session=jwt.parse(token);
        if(session!=null&&SecurityContextHolder.getContext().getAuthentication()==null){
            var auth=new UsernamePasswordAuthenticationToken(session.email(),null,List.of(new SimpleGrantedAuthority("ROLE_"+session.role())));
            auth.setDetails(session);SecurityContextHolder.getContext().setAuthentication(auth);
        }
        chain.doFilter(request,response);
    }
}
