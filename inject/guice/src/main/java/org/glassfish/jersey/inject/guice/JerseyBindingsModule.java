package org.glassfish.jersey.inject.guice;

import com.google.inject.*;
import com.google.inject.name.Names;
import com.google.inject.util.Types;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.internal.inject.Binding;
import org.glassfish.jersey.internal.inject.Binding;
import org.glassfish.jersey.internal.inject.ClassBinding;
import org.glassfish.jersey.internal.inject.PerLookup;
import org.glassfish.jersey.internal.inject.PerThread;
import org.glassfish.jersey.internal.inject.SupplierClassBinding;
import org.glassfish.jersey.internal.inject.SupplierInstanceBinding;
import org.glassfish.jersey.internal.util.ExtendedLogger;
import org.glassfish.jersey.process.internal.RequestScope;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Aleksandar Babic
 */
public class JerseyBindingsModule extends AbstractModule {
  private static final ExtendedLogger logger =
      new ExtendedLogger(Logger.getLogger(JerseyBindingsModule.class.getName()), Level.FINEST);

  private final AbstractBinder binder;
  private final Injector injector;

  JerseyBindingsModule(AbstractBinder binder, Injector injector) {
    this.binder = binder;
    this.injector = injector;
  }

  @Override
  protected void configure() {
    bind(RequestScope.class).to(GuiceRequestScope.class).in(Singleton.class);

    // iterate and register bindings in Guice

    logger.debugLog("Registering bindings to Guice context...");

    binder.getBindings().forEach(b -> {

      if (ClassBinding.class.isAssignableFrom(b.getClass())) {
        bindClass((ClassBinding<?>) b);
      }

      if (SupplierClassBinding.class.isAssignableFrom(b.getClass())) {
        bindSupplierClassBinding((SupplierClassBinding<?>) b);
      }

      if (SupplierInstanceBinding.class.isAssignableFrom(b.getClass())) {
        bindSupplierInstanceBinding((SupplierInstanceBinding<?>) b);
      }
    });
  }

  private void bindClass(ClassBinding<?> b) {
    TypeLiteral tl = TypeLiteral.get(b.getService());
    bind(tl).in(transformScope(b.getScope()));
  }

  @SuppressWarnings("unchecked")
  private void bindSupplierInstanceBinding(SupplierInstanceBinding<?> binding) {
    Set<Type> contracts = binding.getContracts();

    Type firstContract = null;
    if (contracts.iterator().hasNext()) {
      firstContract = contracts.iterator().next();
      bind(getTypeLiteralKey(binding, firstContract))
          .toProvider(() -> binding.getSupplier().get())
          .in(transformScope(binding.getScope()));
    }

    final Type finalFirstContract = firstContract;

    contracts.forEach(contract -> {
      bind(getSupplierKey(binding, contract))
          .toProvider(binding::getSupplier)
          .in(transformScope(binding.getScope()));

      if (contract != finalFirstContract) {
        bind(getTypeLiteralKey(binding, contract))
            .toProvider(new InstanceProvider((Class<?>) finalFirstContract))
            .in(transformScope(binding.getScope()));
      }
    });
  }

  @SuppressWarnings(value = "unchecked")
  private void bindSupplierClassBinding(SupplierClassBinding<?> binding) {

    final Set<Type> contracts = binding.getContracts();
    final InstanceProvider instanceProvider = new InstanceProvider(binding.getSupplierClass());
    final ParameterizedType parameterizedType = Types.newParameterizedType(SupplierValueProvider.class, binding.getSupplierClass());

    bind(TypeLiteral.get(binding.getSupplierClass()))
        .in(transformScope(binding.getSupplierScope()));

    Type firstContract = null;
    if (contracts.iterator().hasNext()) {
      firstContract = contracts.iterator().next();
      bind(getTypeLiteralKey(binding, firstContract))
          .toProvider(TypeLiteral.get(parameterizedType))
          .in(transformScope(binding.getScope()));
    }

    final Type finalFirstContract = firstContract;

    contracts.forEach(contract -> {
      bind(getSupplierKey(binding, contract))
          .toProvider(instanceProvider)
          .in(transformScope(binding.getSupplierScope()));

      if (contract != finalFirstContract) {
        bind(getTypeLiteralKey(binding, contract))
            .toProvider(new InstanceProvider((Class<?>) finalFirstContract))
            .in(transformScope(binding.getScope()));
      }
    });
  }

  private static Key getTypeLiteralKey(Binding binding, Type type) {
    return (binding.getName() != null)
        ? Key.get(TypeLiteral.get(type), Names.named(binding.getName()))
        : Key.get(TypeLiteral.get(type));
  }


  private static Key getSupplierKey(Binding binding, Type type) {
    return (binding.getName() != null)
        ? Key.get(typeOfSupplier(type), Names.named(binding.getName()))
        : Key.get(typeOfSupplier(type));
  }

  public static ParameterizedType typeOfSupplier(Type type) {
    return Types.newParameterizedType(Supplier.class, type);
  }

  /**
   * Transforms Jersey scopes/annotations to Guice equivalents.
   *
   * @param scope Jersey scope/annotation.
   * @return Guice equivalent scope/annotation.
   */
  private static Scope transformScope(Class<? extends Annotation> scope) {
    if (scope == PerLookup.class || scope == null) {
      return Scopes.NO_SCOPE;
    } else if (scope == PerThread.class) {
      return CustomScopes.THREAD;
    }
    return Scopes.SINGLETON;
  }
}
