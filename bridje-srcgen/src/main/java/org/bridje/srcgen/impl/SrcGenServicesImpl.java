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

package org.bridje.srcgen.impl;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import org.bridje.ioc.Component;
import org.bridje.ioc.Inject;
import org.bridje.ioc.PostConstruct;
import org.bridje.srcgen.SrcGenService;
import org.bridje.vfs.*;

@Component
class SrcGenServicesImpl implements SrcGenService
{
    private static final Logger LOG = Logger.getLogger(SrcGenServicesImpl.class.getName());

    @Inject
    private SrcGenTplLoader srcGenTpl;

    private Configuration ftlCfg;

    @PostConstruct
    public void init()
    {
        ftlCfg = new Configuration(Configuration.VERSION_2_3_23);
        ftlCfg.setTemplateLoader(srcGenTpl);
        ftlCfg.setDefaultEncoding("UTF-8");
        ftlCfg.setTemplateExceptionHandler(TemplateExceptionHandler.DEBUG_HANDLER);
        ftlCfg.setLogTemplateExceptions(false);
    }

    @Override
    public void createClass(String clsFullName, String tplPath, Object data) throws IOException
    {
        LOG.log(Level.INFO, "Generating Class {0} from {1}", new Object[]{clsFullName, tplPath});
        String clsPath = toClassPath(clsFullName);
        Path resultPath = CLASSES_PATH.join(clsPath);
        VFile vfile = new VFile(resultPath);
        if(vfile.exists()) vfile.delete();
        vfile.createNewFile();
        try(OutputStream os = new VFileOutputStream(vfile))
        {
            render(os, tplPath, data);
        }
    }

    @Override
    public void createResource(String resourcePath, String tplPath, Object data) throws IOException
    {
        LOG.log(Level.INFO, "Generating Resource {0} from {1}", new Object[]{resourcePath, tplPath});
        Path resultPath = RESOURCE_PATH.join(resourcePath);
        VFile vfile = new VFile(resultPath);
        if(vfile.exists()) vfile.delete();
        vfile.createNewFile();
        try(OutputStream os = new VFileOutputStream(vfile))
        {
            render(os, tplPath, data);
        }
    }

    @Override
    public <T> Map<T, VFile> findData(Class<T> cls) throws IOException
    {
        Map<T, VFile> result = new LinkedHashMap<>();
        VFile[] files = new VFile(DATA_PATH).search(new GlobExpr("**.xml"));
        for (VFile vfile : files)
        {
            if(vfile.isFile())
            {
                LOG.log(Level.INFO, "Reading Data File {0}", vfile.getPath().toString());
                T data = readFile(vfile, cls);
                if(data != null)
                {
                    result.put(data, vfile);
                }
            }
        }
        return result;
    }

    @Override
    public <T> T readFile(VFile file, Class<T> cls) throws IOException
    {
        try
        {
            JAXBContext ctx = JAXBContext.newInstance(cls);
            try(InputStream is = new VFileInputStream(file))
            {
                return cls.cast(ctx.createUnmarshaller().unmarshal(is));
            }
        }
        catch (JAXBException | ClassCastException e)
        {
            return null;
        }
    }

    @Override
    public <T> List<T> findSuplData(Class<T> cls) throws IOException
    {
        List<T> result = new ArrayList<>();
        VFile[] files = new VFile(SUPL_PATH).search(new GlobExpr("**.xml"));
        for (VFile vfile : files)
        {
            if(vfile.isFile())
            {
                LOG.log(Level.INFO, "Reading Suplementary Data File {0}", vfile.getPath().toString());
                T data = readFile(vfile, cls);
                if(data != null)
                {
                    result.add(data);
                }
            }
        }
        return result;
    }

    private String toClassPath(String clsFullName)
    {
        if(clsFullName == null) return null;
        return clsFullName.replaceAll("[\\.]", "\\\\") + ".java";
    }

    public void render(OutputStream os, String template, Object data)
    {
        try(Writer w = new OutputStreamWriter(os, Charset.forName("UTF-8")))
        {
            Template tpl = ftlCfg.getTemplate(template);
            tpl.process(data, w);
            w.flush();
        }
        catch (TemplateException | IOException ex)
        {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    @Override
    public CompilationUnit findJavaClass(String name)
    {
        String path = toClassPath(name);
        VFile clsFile = new VFile(SOURCES_PATH.join(path));
        return parseJavaClass(clsFile);
    }

    @Override
    public CompilationUnit parseJavaClass(VFile clsFile)
    {
        try(VFileInputStream is = new VFileInputStream(clsFile))
        {
            return JavaParser.parse(is);
        }
        catch (IOException e)
        {
            LOG.log(Level.SEVERE, e.getMessage(), e);
        }
        return null;
    }

    @Override
    public List<CompilationUnit> findJavaClasses(Predicate<CompilationUnit> predicate)
    {
        List<CompilationUnit> result = new ArrayList<>();
        VFile[] files = new VFile(SOURCES_PATH).search(new GlobExpr("**.java"));
        if(files != null)
        {
            for (VFile file : files)
            {
                CompilationUnit cu = parseJavaClass(file);
                if(predicate.test(cu)) result.add(cu);
            }
        }
        return result;
    }
}
