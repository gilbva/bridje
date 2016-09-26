/*
 * Copyright 2015 Bridje Framework.
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

package org.bridje.maven.plugin;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.util.XmlSlurper;
import groovy.util.slurpersupport.GPathResult;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.classworlds.DuplicateRealmException;

/**
 * This MOJO is responsible for generating the code specified by the other APIs.
 * Each API defines the code that it needs and the user need`s only to define the 
 * data for the code.
 */
@Mojo(
    name = "generate-sources"
)
public class GenerateMojo extends AbstractMojo
{
    @Parameter(defaultValue = "${project.basedir}/src/main/bridje", readonly = false)
    private String sourceFolder;

    @Parameter(defaultValue="${project.build.directory}/generated-sources/bridje", readonly = false)
    private String targetFolder;

    @Parameter(defaultValue="${project.build.directory}/generated-resources/bridje", readonly = false)
    private String targetResFolder;

    @Parameter(defaultValue="${project}", readonly=true, required=true)
    private MavenProject project;

    private ClassLoader clsRealm;

    private Configuration cfg;
    
    private GroovyShell shell;
    
    private List<URL> generators;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        try
        {
            getLog().info("Generating Source Code");
            Binding binding = new Binding();
            binding.setVariable("tools", this);
            shell = new GroovyShell(binding);
            clsRealm = ClassPathUtils.createClassPath(project);
            cfg = createFreeMarkerConfiguration();
            generators = loadGenerators();
            for (URL generator : generators)
            {
                try(InputStream is = generator.openStream())
                {
                    shell.evaluate(new InputStreamReader(is));
                }
            }
            project.addCompileSourceRoot(targetFolder);
            Resource res = new Resource();
            res.setDirectory(targetResFolder);
            project.addResource(res);
        }
        catch (IOException | DuplicateRealmException | DependencyResolutionRequiredException e)
        {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private Configuration createFreeMarkerConfiguration() throws IOException
    {
        //Freemarker configuration
        Configuration result = new Configuration(Configuration.VERSION_2_3_23);
        TemplateLoader cpLoader = new ClassTemplateLoader(clsRealm, "/BRIDJE-INF/srcgen/");
        result.setTemplateLoader(cpLoader);
        result.setDefaultEncoding("UTF-8");
        result.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        result.setLogTemplateExceptions(false);
        return result;
    }

    public File getSourceFolder()
    {
        return new File(sourceFolder);
    }

    public File getTargetFolder()
    {
        return new File(targetFolder);
    }

    public File generateClass(String className, String template, Map data)
    {
        try
        {
            File clsFile = createClassFile(className);
            Template tmpl = cfg.getTemplate(template);
            if(tmpl != null)
            {
                try(FileWriter fw = new FileWriter(clsFile))
                {
                    tmpl.process(data, fw);
                }
            }
        }
        catch (TemplateException | IOException ex)
        {
            getLog().error(ex.getMessage(), ex);
        }
        return null;
    }
    
    public File generateResource(String className, String template, Map data)
    {
        try
        {
            File clsFile = createResourceFile(className);
            Template tmpl = cfg.getTemplate(template);
            if(tmpl != null)
            {
                try(FileWriter fw = new FileWriter(clsFile))
                {
                    tmpl.process(data, fw);
                }
            }
        }
        catch (TemplateException | IOException ex)
        {
            getLog().error(ex.getMessage(), ex);
        }
        return null;
    }

    private File createClassFile(String className) throws IOException
    {
        String sep = File.separator;
        if(sep.equals("\\"))
        {
            sep = "\\\\"; 
        }
        String fName = className.replaceAll("[\\.]", sep);
        File f = new File(targetFolder + File.separator + fName + ".java");
        if(f.exists())
        {
            f.delete();
        }
        f.getParentFile().mkdirs();
        f.createNewFile();
        return f;
    }
    
    private File createResourceFile(String fileName) throws IOException
    {
        File f = new File(targetResFolder + File.separator + fileName);
        if(f.exists())
        {
            f.delete();
        }
        f.getParentFile().mkdirs();
        f.createNewFile();
        return f;
    }

    private List<URL> loadGenerators() throws IOException
    {
        List<URL> result = new ArrayList<>();
        Enumeration<URL> resources = clsRealm.getResources("BRIDJE-INF/srcgen/CodeGenerator.groovy");
        while (resources.hasMoreElements())
        {
            URL url = resources.nextElement();
            result.add(url);
        }
        return result;
    }

    public GPathResult loadXmlFile(String fileName)
    {
        try(FileReader fr = new FileReader(new File(sourceFolder + File.separator + fileName)))
        {
            return new XmlSlurper().parse(fr);
        }
        catch (Exception e)
        {
            getLog().error(e.getMessage(), e);
        }
        return null;
    }

    public List<GPathResult> loadXmlResources(String resourceName) throws IOException
    {
        List<GPathResult> result = new ArrayList<>();
        Enumeration<URL> resources = clsRealm.getResources(resourceName);
        while (resources.hasMoreElements())
        {
            URL url = resources.nextElement();
            try(InputStreamReader fr = new InputStreamReader(url.openStream()))
            {
                result.add(new XmlSlurper().parse(fr));
            }
            catch (Exception e)
            {
                getLog().error(e.getMessage(), e);
            }
        }
        return result;
    }
}
