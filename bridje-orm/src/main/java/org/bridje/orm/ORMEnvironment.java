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

package org.bridje.orm;

import org.bridje.ioc.thls.ThlsAction;
import org.bridje.ioc.thls.ThlsActionException;
import org.bridje.ioc.thls.ThlsActionException2;

/**
 * 
 */
public interface ORMEnvironment
{
    /**
     * 
     * @param <T>
     * @param modelCls
     * @return 
     */
    <T> T getModel(Class<T> modelCls);

    /**
     * 
     * @param <T>
     * @param action
     * @return 
     */
    <T> T doWith(ThlsAction<T> action);

    /**
     * 
     * @param <T>
     * @param <E>
     * @param action
     * @return
     * @throws E 
     */
    <T, E extends Throwable> T doWithEx(ThlsActionException<T, E> action) throws E;

    /**
     * 
     * @param <T>
     * @param <E>
     * @param <E2>
     * @param action
     * @return
     * @throws E
     * @throws E2 
     */
    <T, E extends Throwable, E2 extends Throwable> T doWithEx2(ThlsActionException2<T, E, E2> action) throws E, E2;
}
