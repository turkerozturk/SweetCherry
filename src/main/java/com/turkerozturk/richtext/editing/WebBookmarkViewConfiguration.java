package com.turkerozturk.richtext.editing;

import com.turkerozturk.node.Node;
import com.turkerozturk.node.NodeController;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.*;
import org.springframework.web.servlet.config.annotation.*;

/** Annotates prepared node views; eligibility never makes a network request. */
@Configuration
public class WebBookmarkViewConfiguration implements WebMvcConfigurer {
    private final boolean enabled;
    public WebBookmarkViewConfiguration(@Value("${myapp.web-bookmarks-enabled:true}") boolean enabled){this.enabled=enabled;}
    @Override public void addInterceptors(InterceptorRegistry registry){registry.addInterceptor(interceptor());}
    HandlerInterceptor interceptor(){return new HandlerInterceptor(){
        @Override public void postHandle(HttpServletRequest request,HttpServletResponse response,Object handler,ModelAndView view){
            if(view!=null&&handler instanceof HandlerMethod method&&NodeController.class.isAssignableFrom(method.getBeanType())){
                Object node=view.getModel().get("node");
                view.addObject("webBookmarkUrl",enabled&&node instanceof Node value?WebBookmarkService.destination(value):null);
            }
        }
    };}
}
