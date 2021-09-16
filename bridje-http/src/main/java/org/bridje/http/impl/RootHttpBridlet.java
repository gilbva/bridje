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

package org.bridje.http.impl;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bridje.http.*;
import org.bridje.ioc.Component;
import org.bridje.ioc.Inject;
import org.bridje.ioc.InjectNext;
import org.bridje.ioc.Priority;
import org.bridje.ioc.thls.Thls;
import org.bridje.ioc.thls.ThlsActionException;

@Component
@Priority(Integer.MIN_VALUE)
class RootHttpBridlet implements HttpBridlet
{
    private static final Logger LOG = Logger.getLogger(RootHttpBridlet.class.getName());

    private final ExecutorService executor = Executors.newCachedThreadPool();

    @InjectNext
    private HttpBridlet handler;

    @Inject
    private HttpServerImpl server;

    @Inject
    private HttpTimeoutProvider[] timeoutProviders;

    @Override
    public boolean handle(HttpBridletContext context) throws IOException
    {
        Callable<Boolean> task = new Callable<Boolean>()
        {
            @Override
            public Boolean call() throws IOException
            {
                return doHandle(context);
            }
        };
        Future<Boolean> future = executor.submit(task);
        HttpBridletRequest req = context.getRequest();
        try
        {
            Integer timeout = null;
            int minTimeout = 5;
            if (timeoutProviders != null)
                for (HttpTimeoutProvider provider : timeoutProviders)
                {
                    timeout = provider.timeoutForPath(req.getPath());
                    if (timeout != null && timeout >= minTimeout)
                        break;
                }
            if (timeout == null || timeout < minTimeout)
                timeout = server.getConfig().getRequestTimeout();
            return future.get(timeout, TimeUnit.SECONDS);
        }
        catch(InterruptedException | ExecutionException ex)
        {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
        }
        catch (TimeoutException ex)
        {
            String message = "IMPORTANT! Execution of %s %s %s took too much time to conclude, so it was cancelled, this could be a problem.";
            LOG.log(Level.SEVERE, String.format(message, req.getMethod(), req.getPath(), req.getProtocol()));
        }
        finally
        {
            future.cancel(true);
        }
        return true;
    }

    private boolean doHandle(HttpBridletContext context) throws IOException
    {
        return Thls.doAsEx(new ThlsActionException<Boolean, IOException>()
        {
            @Override
            public Boolean execute() throws IOException
            {
                return performHandle(context);
            }
        }, HttpBridletRequest.class, context.getRequest());
    }

    private boolean performHandle(HttpBridletContext context) throws IOException
    {
        HttpBridletRequest req = context.getRequest();
        HttpBridletResponse resp = context.getResponse();
        if (LOG.isLoggable(Level.INFO))
            LOG.log(Level.INFO, String.format("%s %s %s", req.getMethod(), req.getPath(), req.getProtocol()));
        try
        {
            if (handler == null || !handler.handle(context))
            {
                throw new HttpException(404, "Not Found");
            }
        }
        catch (HttpException e)
        {
            if (LOG.isLoggable(Level.INFO))
                LOG.log(Level.WARNING, String.format("%s %s - %s %s", req.getMethod(), req.getPath(), req.getProtocol(), e.getStatus(), e.getMessage()));
            resp.setStatusCode(e.getStatus());
            try (OutputStreamWriter writer = new OutputStreamWriter(resp.getOutputStream()))
            {
                writer.append("<h1>" + e.getStatus() + " - " + e.getMessage() + "</h1>");
                writer.flush();
            }
        }
        return true;
    }

}
