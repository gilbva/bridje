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

package org.bridje.web.view.controls;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlTransient;
import org.bridje.ioc.Component;
import org.bridje.ioc.PostConstruct;
import org.bridje.ioc.thls.Thls;
import org.bridje.vfs.VFile;
import org.bridje.vfs.VFileInputStream;
import org.bridje.vfs.VFileOutputStream;
import org.bridje.web.view.AbstractView;
import org.bridje.web.view.WebView;

/**
 * A service for reading web views, and keep track of all controls classes
 * in the application.
 */
@Component
@XmlTransient
public class ControlManager
{
    private static final Logger LOG = Logger.getLogger(ControlManager.class.getName());

    private Class<?>[] controlClasses;

    @PostConstruct
    private void init()
    {
        try
        {
            controlClasses = findComponentsClasses();
        }
        catch (IOException ex)
        {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    /**
     * Reads the given web view class from the given file.
     *
     * @param <T> The web view type.
     * @param vf The file to read from.
     * @param resultCls The web view class.
     * @return The new read object.
     * @throws IOException If any IO error occurs.
     */
    public <T> T read(VFile vf, Class<T> resultCls) throws IOException
    {
        try
        {
            return resultCls.cast(toWebView(vf));
        }
        catch (ClassCastException ex)
        {
            return null;
        }
    }

    /**
     * Writes the given web view object to the given file.
     *
     * @param <T> The type of the web view class.
     * @param vf The file to write to.
     * @param contentObj The view to write.
     * @throws IOException If any IO error occurs.
     */
    public <T> void write(VFile vf, T contentObj) throws IOException
    {
        writeWebView(vf, (WebView) contentObj);
    }

    private Class<?>[] findComponentsClasses() throws IOException
    {
        List<Class<?>> result = new ArrayList<>();
        List<URL> files = findModelsFiles();
        files.stream()
                .map(this::readFile)
                .forEach(prop -> readClasses(result, prop));
        String ctrlClasses = result.stream().map(c -> c.toString()).collect(Collectors.joining("\n - "));
        LOG.log(Level.INFO, "Control Classes:\n - {0}", ctrlClasses);
        Class<?>[] arr = new Class<?>[result.size()];
        return result.toArray(arr);
    }

    private Properties readFile(URL url)
    {
        Properties prop = new Properties();
        try (InputStream is = url.openStream())
        {
            prop.load(is);
        }
        catch (IOException ex)
        {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return prop;
    }

    private List<URL> findModelsFiles() throws IOException
    {
        List<URL> urls = new ArrayList<>();
        ClassLoader ccl = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> resources = ccl.getResources(ControlProcessor.CONTROLS_RESOURCE_FILE);
        while (resources.hasMoreElements())
        {
            URL nextElement = resources.nextElement();
            urls.add(nextElement);
        }
        return urls;
    }

    private void readClasses(List<Class<?>> result, Properties prop)
    {
        prop.forEach((Object k, Object v) ->
        {
            try
            {
                Class<?> cls = Class.forName((String) k);
                if (!result.contains(cls))
                {
                    result.add(cls);
                }
            }
            catch (ClassNotFoundException ex)
            {
                LOG.log(Level.SEVERE, ex.getMessage(), ex);
            }
        });
    }

    private AbstractView toWebView(VFile f)
    {
        return Thls.doAs(() ->
        {
            try (InputStream is = new VFileInputStream(f))
            {
                JAXBContext webViewJaxbCtx = JAXBContext.newInstance(controlClasses);
                Unmarshaller webViewUnmarsh = webViewJaxbCtx.createUnmarshaller();
                Object unmObj = webViewUnmarsh.unmarshal(is);
                if (unmObj instanceof AbstractView)
                {
                    return (AbstractView) unmObj;
                }
            }
            catch (JAXBException | IOException ex)
            {
                LOG.log(Level.SEVERE, "Could not load the  view " + f.getPath() + " " + ex.getMessage(), ex);
            }
            return null;
        }, ParamsContext.class, new ParamsContext());
    }

    private void writeWebView(VFile f, WebView view)
    {
        try (OutputStream os = new VFileOutputStream(f))
        {
            JAXBContext webViewJaxbCtx = JAXBContext.newInstance(controlClasses);
            Marshaller webViewMarsh = webViewJaxbCtx.createMarshaller();
            webViewMarsh.marshal(view, os);
        }
        catch (JAXBException | IOException ex)
        {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }
}
