/**
 * 
 * Copyright 2015 Bridje Framework.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *     
 */

package org.bridje.data.hmodel;

/**
 * Represents a string field.
 */
@javax.xml.bind.annotation.XmlAccessorType(javax.xml.bind.annotation.XmlAccessType.FIELD)
public abstract class HStringFieldBase extends HBasicField
{
    @javax.xml.bind.annotation.XmlAttribute
    private String defaultValue;
    
    /**
     * The default value for this field.
     * @return A String object representing the value of the defaultValue field.
     */
    public String getDefaultValue()
    {
        return this.defaultValue;
    }

    /**
     * The default value for this field.
     * @param defaultValue The new value for the defaultValue field.
     */
    public void setDefaultValue(String defaultValue)
    {
        this.defaultValue = defaultValue;
    }

}