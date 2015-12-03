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
 * Represents a field that holds an enumerator value.
 */
@javax.xml.bind.annotation.XmlAccessorType(javax.xml.bind.annotation.XmlAccessType.FIELD)
public abstract class HEnumFieldBase extends HBasicField
{
    @javax.xml.bind.annotation.XmlAttribute
    private String type;
    
    /**
     * The name of the enumerator for this field.
     * @return A String object representing the value of the type field.
     */
    public String getType()
    {
        return this.type;
    }

    /**
     * The name of the enumerator for this field.
     * @param type The new value for the type field.
     */
    public void setType(String type)
    {
        this.type = type;
    }

}