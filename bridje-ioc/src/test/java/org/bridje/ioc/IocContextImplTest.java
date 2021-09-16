
package org.bridje.ioc;

import java.io.IOException;
import org.bridje.ioc.test.*;
import org.bridje.ioc.test.chain.ChainTest;
import org.bridje.ioc.test.context.ContextInject;
import org.bridje.ioc.test.priority.*;
import static org.junit.Assert.*;
import org.junit.Test;

public class IocContextImplTest
{
    @Test
    public void testFind() throws IOException
    {
        IocContext<Application> instance = Ioc.context();
        DummyComponent result = instance.find(DummyComponent.class);
        assertNotNull(result);
    }

    @Test
    public void testFindByService() throws IOException
    {
        IocContext<Application> instance = Ioc.context();
        SomeService result = instance.find(SomeService.class);
        assertNotNull(result);
        assertTrue(result instanceof DummyServiceProvider);

        SomeService[] resultArr = instance.findAll(SomeService.class);
        assertNotNull(resultArr);
        assertEquals(2, resultArr.length);
        assertTrue(resultArr[0] instanceof DummyServiceProvider);
        assertTrue(resultArr[1] instanceof DummyServiceProvider2);
    }

    @Test
    public void testInjectAndHerarchy() throws IOException
    {
        IocContext<Application> instance = Ioc.context();
        ConcreteComponent conComp = instance.find(ConcreteComponent.class);
        assertNotNull(conComp.getDummyComponent());
        assertNotNull(conComp.getServices());
        assertTrue(conComp.getServices()[0] instanceof DummyServiceProvider);
        assertTrue(conComp.getServices()[1] instanceof DummyServiceProvider2);
    }

    @Test
    public void testInjectGeneric() throws IOException
    {
        IocContext<Application> instance = Ioc.context();
        GenericInjectComponent giComp = instance.find(GenericInjectComponent.class);
        assertNotNull(giComp);
        assertNotNull(giComp.getGsOfStr());
        assertNull(giComp.getGsOfObject());
        assertTrue(giComp.getGsOfStr() instanceof GenericComponent);
        assertNotNull(giComp.getComplexInject());
        assertTrue(giComp.getComplexInject() instanceof ComplexInjectComponent);
    }

    @Test
    public void testPriority() throws IOException
    {
        IocContext<Application> instance = Ioc.context();
        PriorityService[] prorityArr = instance.findAll(PriorityService.class);
        assertTrue(prorityArr[0] instanceof PriorityComp3);
        assertTrue(prorityArr[1] instanceof PriorityComp1);
        assertTrue(prorityArr[2] instanceof PriorityComp4);
        assertTrue(prorityArr[3] instanceof PriorityComp2);

        assertTrue(instance.find(PriorityService.class) instanceof PriorityComp3);

    }

    @Test
    public void testChain() throws IOException
    {
        IocContext<Application> instance = Ioc.context();

        ChainTest chainTest = instance.find(ChainTest.class);

        assertEquals("Wrong Chain", "1 2 3", chainTest.execute());
    }

    @Test
    public void testComponentChild() throws IOException
    {
        IocContext<Application> instance = Ioc.context();

        ComponentBaseInterface childTest = instance.find(ComponentBaseInterface.class);

        assertNotNull(childTest);
        assertTrue(childTest instanceof ComponentChild);
    }

    @Test
    public void testInjectContext()
    {
        IocContext<Application> instance = Ioc.context();

        ContextInject ctxInj = instance.find(ContextInject.class);
        assertNotNull(ctxInj.getAppCtx());
        assertEquals(instance, ctxInj.getAppCtx());
    }
}
