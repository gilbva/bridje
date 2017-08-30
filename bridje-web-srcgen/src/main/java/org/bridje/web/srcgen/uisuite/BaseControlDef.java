/*
 * Copyright 2017 Bridje Framework.
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

package org.bridje.web.srcgen.uisuite;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlTransient;

/**
 * Base class for the control definitions and control templates.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlTransient
public class BaseControlDef
{
    @XmlID
    @XmlAttribute
    private String name;

    @XmlAttribute
    private String templates;

    @XmlElementWrapper(name = "fields")
    @XmlElements(
    {
        @XmlElement(name = "outAttr", type = OutAttrField.class),
        @XmlElement(name = "inAttr", type = InAttrFlield.class),
        @XmlElement(name = "eventAttr", type = EventAttrFlield.class),
        @XmlElement(name = "fileAttr", type = FileAttrFlield.class),
        @XmlElement(name = "attr", type = AttrFlield.class),
        @XmlElement(name = "outEl", type = OutElementField.class),
        @XmlElement(name = "inEl", type = InElementFlield.class),
        @XmlElement(name = "fileEl", type = FileElementFlield.class),
        @XmlElement(name = "eventEl", type = EventElementFlield.class),
        @XmlElement(name = "el", type = ElementField.class),
        @XmlElement(name = "outValue", type = OutValueField.class),
        @XmlElement(name = "inValue", type = InValueFlield.class),
        @XmlElement(name = "fileValue", type = FileValueFlield.class),
        @XmlElement(name = "eventValue", type = EventValueFlield.class),
        @XmlElement(name = "value", type = ValueFlield.class),
        @XmlElement(name = "child", type = ChildField.class),
        @XmlElement(name = "children", type = ChildrenField.class)
    })
    private List<FieldDef> fields;

    @XmlElementWrapper(name = "resources")
    @XmlElements(
    {
        @XmlElement(name = "resource", type = ResourceRef.class)
    })
    private List<ResourceRef> resources;

    @XmlElementWrapper(name = "ftlMacros")
    @XmlElements(
    {
        @XmlElement(name = "ftlMacro", type = ControlFtlMacro.class)
    })
    private List<ControlFtlMacro> macros;
    
    @XmlTransient
    private List<FieldDef> allFields;
    
    @XmlTransient
    private List<ResourceRef> allResources;

    @XmlTransient
    private List<ControlFtlMacro> allMacros;
    
    @XmlTransient
    private UISuiteBase uiSuite;

    /**
     * The name of this definition.
     *
     * @return An string representing the name.
     */
    public String getName()
    {
        return name;
    }

    /**
     * The name of this definition.
     *
     * @param name An string representing the name.
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * The base template for this control.
     * 
     * @return The template to be use by this control.
     */
    public List<String> getTemplatesNames()
    {
        if(templates == null || templates.trim().isEmpty()) return Collections.EMPTY_LIST; 
        return Arrays.asList(templates.trim().split(" "));
    }

    /**
     * Gets the list of declared resources for this control. (only the declared ones).
     * 
     * @return The list of declard resources for this control.
     */
    public List<ResourceRef> getDeclaredResources()
    {
        return resources;
    }

    /**
     * Gets the list of declared fields for this control. (only the declared ones).
     * 
     * @return The list of declard fields for this control.
     */
    public List<FieldDef> getDeclaredFields()
    {
        return fields;
    }
    
    /**
     * The list of fields that this control will support.
     * 
     * @return The list of fields that this control will support.
     */
    public List<FieldDef> getFields()
    {
        if(allFields == null)
        {
            allFields = new ArrayList<>();
            List<TemplateControlDef> tmpls = getTemplates();
            tmpls.forEach(t -> t.getFields().forEach(this::overrideField));
            if(fields != null) fields.forEach(this::overrideField);
        }
        return allFields;
    }

    /**
     * Gets the macros asociated with this control.
     * 
     * @return The list of macros asociated with this control.
     */
    public List<ControlFtlMacro> getMacros()
    {
        if(allMacros == null)
        {
            Map<String,ControlFtlMacro> macrosMap = new LinkedHashMap<>();
            List<TemplateControlDef> tmpls = getTemplates();
            tmpls.stream().forEach(t -> t.getMacros().forEach(r -> macrosMap.put(r.getName(), r)));
            if(macros != null) macros.forEach(r -> macrosMap.put(r.getName(), r));
            allMacros = new ArrayList<>();
            macrosMap.forEach((k, v) -> allMacros.add(v));
        }
        return allMacros;
    }

    /**
     * Overrides some of the data of this control with the data of the given control.
     * 
     * @param control The control to override this control with.
     */
    public void override(BaseControlDef control)
    {
        control.getFields().forEach(this::overrideField);

        Map<String,ControlFtlMacro> macrosMap = new LinkedHashMap<>();
        control.getMacros().forEach(r -> macrosMap.put(r.getName(), r));
        getMacros().forEach(r -> macrosMap.put(r.getName(), r));
        allMacros = new ArrayList<>();
        macrosMap.forEach((k, v) -> allMacros.add(v));

        Map<String,ResourceRef> resourcesMap = new LinkedHashMap<>();
        control.getResources().forEach(r -> resourcesMap.put(r.getName(), r));
        getResources().forEach(r -> resourcesMap.put(r.getName(), r));
        allResources = new ArrayList<>();
        resourcesMap.forEach((k, v) -> allResources.add(v));
        allResources.forEach(r -> r.setUiSuite(uiSuite));
    }

    private void overrideField(FieldDef field)
    {
        FieldDef baseField = getFields().stream()
                                    .filter(f -> f.getName().equals(field.getName()))
                                    .findFirst()
                                    .orElse(null);
        FieldDef fieldToAdd = field;
        if(baseField != null)
        {
            allFields.remove(baseField);
            if(field instanceof ChildrenField 
                    && baseField instanceof ChildrenField)
            {
                fieldToAdd = ((ChildrenField)field).merge((ChildrenField)baseField);
            }
        }
        allFields.add(fieldToAdd);
    }
    
    /**
     * The resources this control needs.
     * 
     * @return The resources this control needs.
     */
    public List<ResourceRef> getResources()
    {
        if(allResources == null)
        {
            Map<String,ResourceRef> resourcesMap = new LinkedHashMap<>();
            List<TemplateControlDef> tmpls = getTemplates();
            tmpls.stream().forEach(t -> t.getResources().forEach(r -> resourcesMap.put(r.getName(), r)));
            if(resources != null) resources.forEach(r -> resourcesMap.put(r.getName(), r));
            allResources = new ArrayList<>();
            resourcesMap.forEach((k, v) -> allResources.add(v));
            allResources.forEach(r -> r.setUiSuite(uiSuite));
        }
        return allResources;
    }

    /**
     * The full name of the java class for this control.
     * 
     * @return The full name of the java class for this control.
     */
    public String getFullName()
    {
        return getPackage() + "." + getName();
    }

    /**
     * This method is called by JAXB after the unmarshal has happend.
     * 
     * @param u The unmarshaller.
     * @param parent The parent.
     */
    public void afterUnmarshal(Unmarshaller u, Object parent)
    {
        if(parent instanceof UISuite)
        {
            uiSuite = (UISuite) parent;
        }
    }

    /**
     * Gets the parent UISuite object.
     * 
     * @return The parent UISuite object.
     */
    public UISuiteBase getUISuite()
    {
        return uiSuite;
    }

    /**
     * Sets the parent UISuite object.
     * 
     * @param uiSuite The parent UISuite object.
     */
    void setUiSuite(UISuiteBase uiSuite)
    {
        if(resources != null) resources.stream().forEach(r -> r.setUiSuite(uiSuite));
        this.uiSuite = uiSuite;
    }

    /**
     * The java class package for this control. This is taken from the UISuite.
     * 
     * @return The java class package for this control.
     */
    public String getPackage()
    {
        if(uiSuite instanceof UISuite) return ((UISuite)uiSuite).getPackage();
        return null;
    }

    /**
     * If this control has any children.
     * 
     * @return true the control has childrens, false otherwise.
     */
    public boolean getHasChildren()
    {
        return getFields().stream().anyMatch(f -> f.getIsChild());
    }

    /**
     * If the control has inputs.
     * 
     * @return true the control has inputs, false otherwise.
     */
    public boolean getHasInputs()
    {
        return getFields().stream().anyMatch(f -> f.getIsInput());
    }

    /**
     * If the control has inputs.
     * 
     * @return true the control has inputs, false otherwise.
     */
    public boolean getHasInputFiles()
    {
        return getFields().stream().anyMatch(f -> f.getIsInputFile());
    }

    /**
     * If the control has any event.
     * 
     * @return true the control has events, false otherwise.
     */
    public boolean getHasEvents()
    {
        return getFields().stream().anyMatch(f -> f.getIsEvent());
    }

    /**
     * If the control has resources.
     * 
     * @return true the control has resources, false otherwise.
     */
    public boolean getHasResources()
    {
        return !getResources().isEmpty();
    }

    /**
     * Finds the base template for this control.
     * 
     * @return The base template for this control.
     */
    public List<TemplateControlDef> getTemplates()
    {
        List<String> names = getTemplatesNames();
        return uiSuite.getTemplates()
                    .stream()
                    .filter(p -> names.contains(p.getName()))
                    .collect(Collectors.toList());
    }

    /**
     * Finds the field with the given name.
     * 
     * @param fieldName The name of the field.
     * @return Returns the field finded or null is it does not exists.
     */
    public FieldDef findField(String fieldName)
    {
        return getFields().stream().filter(f -> f.getName().equals(fieldName)).findFirst().orElse(null);
    }
}
