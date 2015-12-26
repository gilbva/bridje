/*
 * Copyright 2015 Bridje Framework.
 *
 * Alejandro Ferrandiz (acksecurity[at]hotmail.com)
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
package org.bridje.core.impl.ioc;

import org.bridje.core.ioc.Register;

public class ServiceRegister 
{
    private final Class<?> service;
    
    public ServiceRegister(Class<?> service)
    {
        this.service = service;
    }
    
    public Register implementBy(Class<?> component)
    {
        if(!(service.isAssignableFrom(component)))
        {
            throw new ClassCastException();
        }
        
        return new Register(service, component);
    }
}
