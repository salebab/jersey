package org.glassfish.jersey.inject.guice;

import com.google.inject.*;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.util.Types;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.internal.inject.ClassBinding;
import org.glassfish.jersey.internal.inject.SupplierClassBinding;
import org.glassfish.jersey.internal.util.ExtendedLogger;
import org.glassfish.jersey.process.internal.RequestScope;

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

      if(ClassBinding.class.isAssignableFrom(b.getClass())) {
        bindClass((ClassBinding<?>) b);
      }

      if(SupplierClassBinding.class.isAssignableFrom(b.getClass())) {
        bindSupplierClassBinding((SupplierClassBinding<?>) b);
      }
    });
  }

  private void bindClass(ClassBinding<?> b) {
    TypeLiteral tl = TypeLiteral.get(b.getService());
    bind(tl);
  }

  private void bindSupplierClassBinding(SupplierClassBinding<?> binding) {

    Set<Type> contracts = binding.getContracts();

    Provider supplierProvider = () -> injector.getInstance(binding.getSupplierClass());

    contracts.forEach(contract -> {
      TypeLiteral tl1 = TypeLiteral.get(typeOfSupplier(contract));
      bind(tl1).toProvider(supplierProvider);

      TypeLiteral tl2 = TypeLiteral.get(contract);
      bind(tl2).toProvider(() -> injector.getInstance(binding.getSupplierClass()).get());
    });
/*
    TypeLiteral supplierType = TypeLiteral.get(binding.getSupplierClass());
    System.out.println(supplierType);
    bind(supplierType).toProvider(supplierProvider);
*/

  }

  public static ParameterizedType typeOfSupplier(Type type) {
    return Types.newParameterizedType(Supplier.class, type);
  }
}
