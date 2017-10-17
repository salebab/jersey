package org.glassfish.jersey.inject.guice;

import com.google.inject.Injector;
import org.glassfish.jersey.internal.inject.*;
import org.glassfish.jersey.internal.util.ReflectionHelper;

import javax.inject.Inject;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericType;
import java.lang.reflect.Type;

/**
 * @author Aleksandar Babic
 */
public class ContextInjectionResolverImpl implements InjectionResolver<Context>, ContextInjectionResolver {

  private final Injector injector;

  @Inject
  public ContextInjectionResolverImpl(Injector injector) {
    this.injector = injector;
  }

  @Override
  public Object resolve(Injectee injectee) {
    Injectee newInjectee = injectee;
    if (injectee.isFactory()) {
      newInjectee = getFactoryInjectee(injectee, ReflectionHelper.getTypeArgument(injectee.getRequiredType(), 0));
    }
    // hmm?
    return injector.getInstance(newInjectee.getInjecteeClass());
  }

  @Override
  public boolean isConstructorParameterIndicator() {
    return true;
  }

  @Override
  public boolean isMethodParameterIndicator() {
    return false;
  }

  @Override
  public Class<Context> getAnnotation() {
    return Context.class;
  }


  private Injectee getFactoryInjectee(Injectee injectee, Type requiredType) {
    return new RequiredTypeOverridingInjectee(injectee, requiredType);
  }

  private static class RequiredTypeOverridingInjectee extends InjecteeImpl {
    private RequiredTypeOverridingInjectee(Injectee injectee, Type requiredType) {
      setFactory(injectee.isFactory());
      setInjecteeClass(injectee.getInjecteeClass());
      setInjecteeDescriptor(injectee.getInjecteeDescriptor());
      setOptional(injectee.isOptional());
      setParent(injectee.getParent());
      setPosition(injectee.getPosition());
      setRequiredQualifiers(injectee.getRequiredQualifiers());
      setRequiredType(requiredType);
    }
  }

  public static final class Binder extends AbstractBinder {

    private final Injector injector;

    public Binder(Injector injector) {
      this.injector = injector;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void configure() {
      ContextInjectionResolverImpl resolver = new ContextInjectionResolverImpl(injector);

      bind(resolver)
          .to(new GenericType<InjectionResolver<Context>>() {})
          .to(ContextInjectionResolver.class);

      bind(Bindings.service(resolver))
          .to(new GenericType<InjectionResolver<Context>>() {})
          .to(ContextInjectionResolver.class);
    }
  }
}
