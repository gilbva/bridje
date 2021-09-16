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

package org.bridje.web.view.controls;

import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.bridje.el.ElEnvironment;
import org.bridje.ioc.thls.Thls;

/**
 * The data for the events of the control that can be render in a WebView.
 */
@XmlTransient
@XmlJavaTypeAdapter(UIEventAdapter.class)
public class UIEvent
{
    private final String expression;

    private final String parameter;

    UIEvent(String expression)
    {
        this.expression = expression;
        parameter = "__ev." + ParamsContext.createParam(expression);
    }

    /**
     * Gets the event´s expression.
     *
     * @return The event´s expression.
     */
    public String getExpression()
    {
        return expression;
    }

    /**
     * The parameter for the event.
     *
     * @return The parameter for the event.
     */
    public String getParameter()
    {
        return parameter;
    }

    /**
     * Invokes the method defined by this event.
     *
     * @return The object returned when invoking the method of the event.
     */
    public Object invoke()
    {
        return Thls.get(ElEnvironment.class).get("${" + expression + "}", Object.class);
    }
}
