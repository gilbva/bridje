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
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

/**
 * A UI suite control definition.
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class ControlDef extends BaseControlDef
{
    private ReadInputWorkFlow input;

    @XmlAttribute
    private String base;

    @XmlAttribute
    private boolean isTransient;

    public boolean getIsTransient()
    {
        return isTransient;
    }

    public void setIsTransient(boolean isTransient)
    {
        this.isTransient = isTransient;
    }

    /**
     * The parent for this control, by default it is "Control".
     * 
     * @return The name of the parent control.
     */
    public String getBaseName()
    {
        if (base == null)
        {
            base = "Control";
        }
        return base;
    }

    /**
     * Gets all the parents control of this control.
     * 
     * @return The list with the parents control of this control.
     */
    public List<ControlDef> getAllBase()
    {
        List<ControlDef> result = new ArrayList<>();
        ControlDef currBase = getBase();
        while(currBase != null)
        {
            result.add(currBase);
            currBase = ((ControlDef)currBase).getBase();
        }
        return result;
    }

    /**
     * The parent for this control, by default it is "Control".
     * 
     * @param base The name of the parent control.
     */
    public void setBaseName(String base)
    {
        this.base = base;
    }

    /**
     * The flow for reading the input of the control.
     * 
     * @return The read input flow for this control.
     */
    public ReadInputWorkFlow getInput()
    {
        return input;
    }

    /**
     * The base control for this control.
     * 
     * @return The Control that is the parent of this control.
     */
    public ControlDef getBase()
    {
        if(base == null || base.equals("Control")) return null;
        return getUISuite().getControls().stream().filter(p -> p.getName().equalsIgnoreCase(base)).findFirst().orElse(null);
    }
}
