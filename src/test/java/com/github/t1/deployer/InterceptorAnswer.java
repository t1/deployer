package com.github.t1.deployer;

import static java.util.Collections.*;

import java.lang.reflect.*;
import java.util.Map;

import javax.interceptor.*;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

class InterceptorAnswer implements Answer<Object> {
    private final Object target;
    private final Object interceptor;
    private final Method aroundInvokeMethod;

    public InterceptorAnswer(Object target, Object interceptor) {
        this.target = target;
        this.interceptor = interceptor;
        this.aroundInvokeMethod = findAroundInvoke(interceptor);
    }

    private Method findAroundInvoke(Object interceptor) {
        for (Method method : interceptor.getClass().getDeclaredMethods())
            if (method.isAnnotationPresent(AroundInvoke.class))
                return method;
        return null;
    }

    @Override
    public Object answer(final InvocationOnMock invocation) throws Exception {
        if (!invocation.getMethod().isAnnotationPresent(ContainerDeployment.class)) {
            return invocation.getMethod().invoke(target, invocation.getArguments());
        }
        return aroundInvokeMethod.invoke(interceptor, new InvocationContext() {
            private Object[] parameters = invocation.getArguments();

            @Override
            public void setParameters(Object[] params) {
                this.parameters = params;
            }

            @Override
            public Object proceed() throws Exception {
                return invocation.getMethod().invoke(target, parameters);
            }

            @Override
            public Object getTimer() {
                return null;
            }

            @Override
            public Object getTarget() {
                return target;
            }

            @Override
            public Object[] getParameters() {
                return invocation.getArguments();
            }

            @Override
            public Method getMethod() {
                return invocation.getMethod();
            }

            @Override
            public Map<String, Object> getContextData() {
                return emptyMap();
            }

            @Override
            public Constructor<?> getConstructor() {
                return null;
            }
        });
    }
}