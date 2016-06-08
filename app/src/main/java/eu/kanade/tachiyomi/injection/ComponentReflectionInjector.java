package eu.kanade.tachiyomi.injection;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class allows to inject into objects through a base class,
 * so we don't have to repeat injection code everywhere.
 *
 * The performance drawback is about 0.013 ms per injection on a very slow device,
 * which is negligible in most cases.
 *
 * Example:
 * <pre>{@code
 * Component {
 *     void inject(B b);
 * }
 *
 * class A {
 *     void onCreate() {
 *         componentReflectionInjector.inject(this);
 *     }
 * }
 *
 * class B extends A {
 *     @Inject MyDependency dependency;
 * }
 *
 * new B().onCreate() // dependency will be injected at this point
 *
 * class C extends B {
 *
 * }
 *
 * new C().onCreate() // dependency will be injected at this point as well
 * }</pre>
 *
 * @param <T> a type of dagger 2 component.
 */
public final class ComponentReflectionInjector<T> {

    private static final ConcurrentHashMap<Class<?>, HashMap<Class<?>, Method>> cache = new ConcurrentHashMap<>();

    private final Class<T> componentClass;
    private final T component;
    private final HashMap<Class<?>, Method> methods;

    public ComponentReflectionInjector(Class<T> componentClass, T component) {
        this.componentClass = componentClass;
        this.component = component;
        this.methods = getMethods(componentClass);
    }

    public T getComponent() {
        return component;
    }

    public void inject(Object target) {

        Class targetClass = target.getClass();
        Method method = methods.get(targetClass);
        while (method == null && targetClass != null) {
            targetClass = targetClass.getSuperclass();
            method = methods.get(targetClass);
        }

        if (method == null)
            throw new RuntimeException(String.format("No %s injecting method exists in %s component", target.getClass(), componentClass));

        try {
            method.invoke(component, target);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static HashMap<Class<?>, Method> getMethods(Class componentClass) {
        HashMap<Class<?>, Method> methods = cache.get(componentClass);
        if (methods == null) {
            synchronized (cache) {
                methods = cache.get(componentClass);
                if (methods == null) {
                    methods = new HashMap<>();
                    for (Method method : componentClass.getMethods()) {
                        Class<?>[] params = method.getParameterTypes();
                        if (params.length == 1)
                            methods.put(params[0], method);
                    }
                    cache.put(componentClass, methods);
                }
            }
        }
        return methods;
    }
}