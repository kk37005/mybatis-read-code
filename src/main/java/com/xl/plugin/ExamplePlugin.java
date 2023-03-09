package com.xl.plugin;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.util.Properties;

/**
 * 插件例子
 *
 * @author kk37005
 */
@Intercepts({@Signature(type = Executor.class, method = "query", args = {
        MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})})
public class ExamplePlugin implements Interceptor {

    //拦截:实现拦截逻辑的地方
    public Object intercept(Invocation invocation) throws Throwable {
        //完成代理类本身的逻辑
        // do something
        //通过invocation.proceed()方法完成调用链的推进
        return invocation.proceed();
    }

    //插入:用当前这个拦截器生成对目标target的代理，实际是通过Plugin.wrap(target,this)来完成的，把目标target和拦截器this传给了包装函数
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    //设置属性:用于设置额外的参数，参数配置在拦截器的Properties节点里
    public void setProperties(Properties properties) {
    }
}
