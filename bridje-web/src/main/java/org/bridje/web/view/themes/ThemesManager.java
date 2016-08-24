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

package org.bridje.web.view.themes;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import org.bridje.el.ElEnvironment;
import org.bridje.http.HttpServerResponse;
import org.bridje.ioc.Component;
import org.bridje.ioc.Inject;
import org.bridje.ioc.Ioc;
import org.bridje.ioc.thls.Thls;
import org.bridje.vfs.VfsService;
import org.bridje.vfs.VFile;
import org.bridje.web.view.widgets.Widget;
import org.bridje.web.view.WebView;

@Component
public class ThemesManager
{
    private static final Logger LOG = Logger.getLogger(ThemesManager.class.getName());

    private Configuration ftlCfg;
    
    @Inject
    private VfsService vfs;
    
    @PostConstruct
    public void init()
    {
        ftlCfg = new Configuration(Configuration.VERSION_2_3_23);
        ftlCfg.setTemplateLoader(Ioc.context().find(ThemesTplLoader.class));
        ftlCfg.setDefaultEncoding("UTF-8");
        ftlCfg.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER);
        ftlCfg.setLogTemplateExceptions(false);
    }

    public void render(WebView view, OutputStream os, Map<String, String> state)
    {
        try(Writer w = new OutputStreamWriter(os, Charset.forName("UTF-8")))
        {
            String themeName = "default";
            String templatePath = themeName + "/view.ftl";
            Template tpl = ftlCfg.getTemplate(templatePath);
            Map data = new HashMap();
            data.put("view", view);
            data.put("env", Thls.get(ElEnvironment.class));
            data.put("state", state);
            tpl.process(data, w);
            w.flush();
        }
        catch (TemplateException | IOException ex)
        {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public void render(Widget widget, WebView view, OutputStream os, Object result, Map<String, String> state)
    {
        try(Writer w = new OutputStreamWriter(os, Charset.forName("UTF-8")))
        {
            String themeName = "default";
            String templatePath = themeName + "/view.ftl";
            Template tpl = ftlCfg.getTemplate(templatePath);
            Map data = new HashMap();
            data.put("view", view);
            data.put("widget", widget);
            data.put("result", result);
            data.put("env", Thls.get(ElEnvironment.class));
            data.put("state", state);
            tpl.process(data, w);
            w.flush();
        }
        catch (TemplateException | IOException ex)
        {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public boolean serveResource(String themeName, String resPath, HttpServerResponse resp) throws IOException
    {
        VFile f = vfs.findFile("/web/themes/" + themeName + "/resources/" + resPath);
        if(f != null)
        {
            try(InputStream is = f.openForRead())
            {
                OutputStream os = resp.getOutputStream();
                int ch = is.read();
                while(ch != -1)
                {
                    os.write(ch);
                    ch = is.read();
                }
                os.flush();
                return true;
            }
        }
        return false;
    }
}
