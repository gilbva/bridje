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

package org.bridje.orm.impl;

import java.lang.reflect.Field;
import java.util.Date;
import org.bridje.orm.Condition;
import org.bridje.orm.TableDateColumn;

class TableDateColumnImpl<E, T extends Date> extends TableColumnImpl<E, T> implements TableDateColumn<E, T>
{
    public TableDateColumnImpl(TableImpl<E> table, Field field, Class<T> type)
    {
        super(table, field, type);
    }

    @Override
    public Condition gt(T value)
    {
        return new BinaryCondition(this, Operator.GT, serialize(value));
    }

    @Override
    public Condition ge(T value)
    {
        return new BinaryCondition(this, Operator.GE, serialize(value));
    }

    @Override
    public Condition lt(T value)
    {
        return new BinaryCondition(this, Operator.LT, serialize(value));
    }

    @Override
    public Condition le(T value)
    {
        return new BinaryCondition(this, Operator.LE, serialize(value));
    }
}
