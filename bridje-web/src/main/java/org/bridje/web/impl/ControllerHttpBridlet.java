/*
 * Copyright 2016 Bridje Framework.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.bridje.web.impl;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bridje.http.HttpCookie;
import org.bridje.ioc.Component;
import org.bridje.ioc.IocContext;
import org.bridje.ioc.Priority;
import org.bridje.web.ReqPathRef;
import org.bridje.web.WebCookie;
import org.bridje.web.WebMethod;
import org.bridje.web.WebParameter;
import org.bridje.web.WebScope;
import org.bridje.http.HttpBridletContext;
import org.bridje.http.HttpBridlet;
import org.bridje.http.HttpException;

@Component
@Priority(500)
class ControllerHttpBridlet implements HttpBridlet
{
    private static final Logger LOG = Logger.getLogger(ControllerHttpBridlet.class.getName());

    private List<WebMethodData> methodsData;

    @Override
    public boolean handle(HttpBridletContext context) throws IOException, HttpException
    {
        IocContext<WebScope> wrsCtx = context.get(IocContext.class);
        if(methodsData == null)
        {
            initMethods(wrsCtx);
        }
        Object result = invokeMethod(wrsCtx, ReqPathRef.findCurrentPath(context));
        if(result != null)
        {
            context.set((Class)result.getClass(), result);
        }
        return false;
    }

    private synchronized void initMethods(IocContext<WebScope> wrsCtx)
    {
        if(methodsData == null)
        {
            methodsData = new ArrayList<>();
            wrsCtx.getClassRepository().forEachMethod(WebMethod.class, 
                    (Method method, Class component, WebMethod annotation) ->
                    {
                        methodsData.add(new WebMethodData(
                                            annotation.value(), component, method));
                    });
        }
    }

    private Object invokeMethod(IocContext<WebScope> wrsCtx, String path) throws HttpException
    {
        for (WebMethodData methodData : methodsData)
        {
            Object[] matches = methodData.matches(path);
            if(matches != null)
            {
                Object cmp = wrsCtx.find(methodData.getComponent());
                injectParameters(wrsCtx, cmp);
                try
                {
                    return methodData.getMethod().invoke(cmp, matches);
                }
                catch (InvocationTargetException e)
                {
                    if(e.getCause() instanceof HttpException)
                    {
                        throw (HttpException)e.getCause();
                    }
                }
                catch (IllegalAccessException | IllegalArgumentException e)
                {
                    throw new HttpException(500, e.getMessage(), e);
                }
            }
        }
        return null;
    }

    private void injectParameters(IocContext<WebScope> wrsCtx, Object cmp)
    {
        Field[] fields = cmp.getClass().getDeclaredFields();
        for (Field field : fields)
        {
            WebParameter param = field.getAnnotation(WebParameter.class);
            if(param != null)
            {
                injectParameter(wrsCtx, cmp, field, param);
            }
            else
            {
                WebCookie cookie = field.getAnnotation(WebCookie.class);
                if(cookie != null)
                {
                    injectCookie(wrsCtx, cmp, field, cookie);
                }
            }
        }
    }

    private void injectParameter(IocContext<WebScope> wrsCtx, Object cmp, Field field, WebParameter param)
    {
        String name = param.value();
        String paramVal = wrsCtx.getScope().getPostParameter(name);
        if(paramVal == null)
        {
            paramVal = wrsCtx.getScope().getGetParameter(name);
            if(paramVal != null)
            {
                try
                {
                    field.setAccessible(true);
                    field.set(cmp,paramVal);
                }
                catch (SecurityException | IllegalArgumentException | IllegalAccessException e)
                {
                    LOG.log(Level.SEVERE, e.getMessage(), e);
                }
            }
        }
    }

    private void injectCookie(IocContext<WebScope> wrsCtx, Object cmp, Field field, WebCookie cookie)
    {
        String name = cookie.value();
        HttpCookie cookieVal = wrsCtx.getScope().getCookie(name);
        if(cookieVal != null)
        {
            try
            {
                field.setAccessible(true);
                field.set(cmp,cookieVal.getValue());
            }
            catch (SecurityException | IllegalArgumentException | IllegalAccessException e)
            {
                LOG.log(Level.SEVERE, e.getMessage(), e);
            }
        }
    }
}
