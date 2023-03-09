/*
 *    Copyright 2009-2012 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.plugin;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ibatis.reflection.ExceptionUtil;

/**
 * 插件,用的代理模式（JDK动态代理机制）
 * Mybatis拦截器的核心
 */
public class Plugin implements InvocationHandler {

    //被代理的目标类
    private Object target;
    //对应的拦截器
    private Interceptor interceptor;
    //记录需要被拦截的类与方法（拦截器拦截的方法缓存）
    private Map<Class<?>, Set<Method>> signatureMap;

    private Plugin(Object target, Interceptor interceptor, Map<Class<?>, Set<Method>> signatureMap) {
        this.target = target;
        this.interceptor = interceptor;
        this.signatureMap = signatureMap;
    }

    //一个静态方法,对一个目标对象进行包装，生成代理类。
    public static Object wrap(Object target, Interceptor interceptor) {
        //取得签名Map：根据interceptor上面定义的注解 获取需要拦截的信息
        Map<Class<?>, Set<Method>> signatureMap = getSignatureMap(interceptor);
        //取得要改变行为的类(ParameterHandler|ResultSetHandler|StatementHandler|Executor)
        Class<?> type = target.getClass();
        //解析被拦截对象的所有接口（注意是接口）
        Class<?>[] interfaces = getAllInterfaces(type, signatureMap);
        //如果长度为>0 则返回代理类 否则不做处理
        if (interfaces.length > 0) {
            // 生成代理对象， Plugin对象为该代理对象的InvocationHandler
            //（InvocationHandler属于java代理的一个重要概念，不熟悉的请参考相关概念）
            return Proxy.newProxyInstance(
                    type.getClassLoader(),
                    interfaces,
                    new Plugin(target, interceptor, signatureMap));
        }
        return target;
    }

    //代理对象每次调用的方法
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            //通过method参数定义的类 去signatureMap当中查询需要拦截的方法集合
            Set<Method> methods = signatureMap.get(method.getDeclaringClass());
            //判断是否需要拦截
            if (methods != null && methods.contains(method)) {
                //调用Interceptor.intercept，也即插入了我们自己的逻辑
                return interceptor.intercept(new Invocation(target, method, args));
            }
            //最后还是执行原来逻辑
            return method.invoke(target, args);
        } catch (Exception e) {
            throw ExceptionUtil.unwrapThrowable(e);
        }
    }

    //取得签名Map: 根据拦截器接口（Interceptor）实现类上面的注解获取相关信息
    private static Map<Class<?>, Set<Method>> getSignatureMap(Interceptor interceptor) {
        //取Intercepts注解，例子可参见ExamplePlugin.java
        Intercepts interceptsAnnotation = interceptor.getClass().getAnnotation(Intercepts.class);
        // issue #251
        //必须得有Intercepts注解，没有报错
        if (interceptsAnnotation == null) {
            throw new PluginException("No @Intercepts annotation was found in interceptor " + interceptor.getClass().getName());
        }
        //value是数组型，Signature注解信息
        Signature[] sigs = interceptsAnnotation.value();
        //每个class里有多个Method需要被拦截,所以这么定义
        Map<Class<?>, Set<Method>> signatureMap = new HashMap<Class<?>, Set<Method>>();
        for (Signature sig : sigs) {
            //根据Signature注解定义的type信息去signatureMap当中查询需要拦截方法的集合
            Set<Method> methods = signatureMap.get(sig.type());
            //第一次肯定为null 就创建一个并放入signatureMap
            if (methods == null) {
                methods = new HashSet<Method>();
                signatureMap.put(sig.type(), methods);
            }
            try {
                //找到sig.type当中定义的方法 并加入到集合
                Method method = sig.type().getMethod(sig.method(), sig.args());
                methods.add(method);
            } catch (NoSuchMethodException e) {
                throw new PluginException("Could not find method on " + sig.type() + " named " + sig.method() + ". Cause: " + e, e);
            }
        }
        return signatureMap;
    }

    //根据对象类型与signatureMap获取接口信息
    private static Class<?>[] getAllInterfaces(Class<?> type, Map<Class<?>, Set<Method>> signatureMap) {
        Set<Class<?>> interfaces = new HashSet<Class<?>>();
        //循环type类型的接口信息 如果该类型存在与signatureMap当中则加入到set当中去
        while (type != null) {
            for (Class<?> c : type.getInterfaces()) {
                //貌似只能拦截ParameterHandler|ResultSetHandler|StatementHandler|Executor
                //拦截其他的无效
                //当然我们可以覆盖Plugin.wrap方法，达到拦截其他类的功能
                if (signatureMap.containsKey(c)) {
                    interfaces.add(c);
                }
            }
            type = type.getSuperclass();
        }
        return interfaces.toArray(new Class<?>[interfaces.size()]);
    }

}
