package com.longcoding.undefined.configs;

import com.longcoding.undefined.helpers.Constant;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

import javax.servlet.Filter;

/**
 * Created by longcoding on 16. 4. 5..
 * Updated by longcoding on 18. 12. 26..
 */

@EnableConfigurationProperties
public class UndefinedInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {

    @Override
    protected Class<?>[] getRootConfigClasses() { return new Class[] { UndefinedRootConfig.class }; }

    @Override
    protected Class<?>[] getServletConfigClasses() {
        return new Class[] { UndefinedServletConfig.class };
    }

    @Override
    protected String[] getServletMappings() {
        return new String[] { "/" };
    }

    @Override
    protected Filter[] getServletFilters() {
        CharacterEncodingFilter encodingFilter = new CharacterEncodingFilter();
        encodingFilter.setEncoding(Constant.SERVER_DEFAULT_ENCODING_TYPE);
        encodingFilter.setForceEncoding(true);
        return new Filter[] { encodingFilter };
    }
}
