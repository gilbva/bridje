
package org.bridje.wui;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.bridje.web.view.widgets.UIExpression;
import org.bridje.web.view.widgets.UIExpressionAdapter;

@XmlRootElement(name = "para")
@XmlAccessorType(XmlAccessType.FIELD)
public class Paragraph extends BaseWidget
{
    @XmlValue
    @XmlJavaTypeAdapter(UIExpressionAdapter.class)
    private UIExpression valueExpression;

    public String getValue()
    {
        return get(valueExpression, String.class, "");
    }

    public UIExpression getValueExpression()
    {
        return valueExpression;
    }

    public void setValueExpression(UIExpression valueExpression)
    {
        this.valueExpression = valueExpression;
    }
}
