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

package org.bridje.el.impl;

import de.odysseus.el.misc.TypeConverter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.el.ELException;
import org.bridje.el.ElAdvanceConverter;
import org.bridje.el.ElSimpleConvertMap;
import org.bridje.el.ElSimpleConvertProvider;
import org.bridje.el.ElSimpleConverter;
import org.bridje.ioc.Component;
import org.bridje.ioc.Inject;
import org.bridje.ioc.PostConstruct;

@Component
class TypeConverterImpl implements TypeConverter
{
    @Inject
    private ElAdvanceConverter[] advanceConverters;

    @Inject
    private ElSimpleConvertProvider[] simpleConverters;

    private Map<Class<?>,Map<Class<?>,ElAdvanceConverter>> advanceConvertMap;

    private ElSimpleConvertMap simpleConverMap;

    @PostConstruct
    public void init()
    {
        advanceConvertMap = new ConcurrentHashMap<>();
        simpleConverMap = new ElSimpleConvertMap();
        for (ElSimpleConvertProvider providers : simpleConverters)
        {
            ElSimpleConvertMap convMap = providers.createConvertMap();
            simpleConverMap.addAll(convMap);
        }
    }

    @Override
    public <T> T convert(Object value, Class<T> type) throws ELException
    {
        if (value == null)
        {
            return null;
        }

        if (type == value.getClass() ||
            type.isAssignableFrom(value.getClass()))
        {
            return type.cast(value);
        }

        ElSimpleConverter<Object, T> simpleConv = simpleConverMap.get((Class<Object>)value.getClass(), type);
        if(simpleConv != null)
        {
            return simpleConv.convert(value);
        }

        Map<Class<?>, ElAdvanceConverter> map = advanceConvertMap.get(value.getClass());
        if (map != null)
        {
            ElAdvanceConverter converter = map.get(type);
            if(converter != null)
            {
                return converter.convert(value, type);
            }
        }

        for (ElAdvanceConverter typeConverter : advanceConverters)
        {
            if(typeConverter.canConvert(value.getClass(), type))
            {
                map = advanceConvertMap.get(value.getClass());
                if(map == null)
                {
                    map = new ConcurrentHashMap<>();
                    advanceConvertMap.put(value.getClass(), map);
                }
                map.put(type, typeConverter);
                return typeConverter.convert(value, type);
            }
        }

        return TypeConverter.DEFAULT.convert(value, type);
    }
}
