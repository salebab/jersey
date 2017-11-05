package org.glassfish.jersey.inject.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Scope;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import com.google.inject.util.Types;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.internal.inject.Binding;
import org.glassfish.jersey.internal.inject.ClassBinding;
import org.glassfish.jersey.internal.inject.InjectionResolverBinding;
import org.glassfish.jersey.internal.inject.InstanceBinding;
import org.glassfish.jersey.internal.inject.PerLookup;
import org.glassfish.jersey.internal.inject.PerThread;
import org.glassfish.jersey.internal.inject.SupplierClassBinding;
import org.glassfish.jersey.internal.inject.SupplierInstanceBinding;
import org.glassfish.jersey.internal.util.ExtendedLogger;
import org.glassfish.jersey.process.internal.RequestScope;
import org.glassfish.jersey.process.internal.RequestScoped;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
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
  private final List<Key> setBinders;
  private final BindHistory bindHistory;

  private static final Map<Key, Multibinder> multibinders = new HashMap<>();
  //private static final Set<Key> boundKeys = new HashSet<>();

  JerseyBindingsModule(AbstractBinder binder, List<Key> setBinders, BindHistory bindHistory) {
    this.binder = binder;
    this.setBinders = setBinders;
    this.bindHistory = bindHistory;
  }


  /**
   * Resolves BindingBuilder and applies consumer
   */
  private <T> void bind(Key<T> key, Consumer<LinkedBindingBuilder<T>> consumer) {
    logger.debugLog("Binding key: {0}", key);

    if (multibinders.containsKey(key)) {
      consumer.accept(multibinders.get(key).addBinding());
      return;
    }

    if (bindHistory.exists(key)) {
      logger.debugLog("Key is already bound and will be ignored. {0}", key);
      return;
    }

    consumer
        .andThen((b) -> bindHistory.add(key))
        .accept(bind(key));
  }

  @Override
  protected void configure() {
    if (multibinders.isEmpty()) {
      setBinders.forEach(key -> multibinders.put(key, Multibinder.newSetBinder(binder(), key)));
    }
    bind(Key.get(RequestScope.class), (builder) -> {
      builder.to(GuiceRequestScope.class).in(Scopes.SINGLETON);
      bindScope(RequestScoped.class, CustomScopes.THREAD);
    });

    // iterate and register bindings in Guice

    logger.debugLog("Registering bindings to Guice context...");

    binder.getBindings().forEach(b -> {
      logger.debugLog("\n--------\nBinding: {0}\nContacts: {1}\nQualifiers: {2}\nImplementation: {3}\nScope: {4}\n-----------",
          b.getClass(), b.getContracts(), b.getQualifiers(), b.getImplementationType(), b.getScope());

      if (InjectionResolverBinding.class.isAssignableFrom(b.getClass())) {
        bindInjectorResolver((InjectionResolverBinding<?>) b);
      } else if (ClassBinding.class.isAssignableFrom(b.getClass())) {
        bindClass((ClassBinding<?>) b);
      } else if (InstanceBinding.class.isAssignableFrom(b.getClass())) {
        bindInstance((InstanceBinding<?>) b);
      } else if (SupplierClassBinding.class.isAssignableFrom(b.getClass())) {
        bindSupplierClassBinding((SupplierClassBinding<?>) b);
      } else if (SupplierInstanceBinding.class.isAssignableFrom(b.getClass())) {
        bindSupplierInstanceBinding((SupplierInstanceBinding<?>) b);
      } else {
        throw new RuntimeException(b.getClass() + " is not supported.");
      }
    });

    logger.debugLog("Configuration is done.");
  }

  private void bindInjectorResolver(InjectionResolverBinding<?> b) {
    b.getContracts().forEach(type -> {
      bind(getTypeLiteralKey(b, type), (bind) -> {
        bind.toInstance(b.getResolver());
      });
    });
  }

  private <T> void bindInstance(InstanceBinding<T> b) {

    b.getContracts().forEach(type -> {
      bind(getTypeLiteralKey(b, type), (binder) -> {
        binder.toInstance(b.getService());
      });
    });
  }

  private void bindClass(ClassBinding<?> b) {

    if (b.getQualifiers().size() > 0) {
      b.getContracts().forEach(c -> {
        Key key = getTypeLiteralKey(b, c);
        bind(key, (bind) -> {
          bind.to(b.getService()).in(transformScope(b.getScope()));
        });
      });
    } else {
      bind(b.getService()).in(transformScope(b.getScope()));
    }
  }

  @SuppressWarnings("unchecked")
  private void bindSupplierInstanceBinding(SupplierInstanceBinding<?> binding) {

    logger.debugLog("BINDING: {}", binding);

    Set<Type> contracts = binding.getContracts();

    Type firstContract = null;
    if (contracts.iterator().hasNext()) {
      firstContract = contracts.iterator().next();

      Key key = getTypeLiteralKey(binding, firstContract);
      bind(key, (bind) -> {
        bind.toProvider(() -> binding.getSupplier().get())
            .in(transformScope(binding.getScope()));
      });
    }

    final Type finalFirstContract = firstContract;

    contracts.forEach(contract -> {
      bind(getSupplierKey(binding, contract), (bind) -> {
          bind.toProvider(binding::getSupplier)
            .in(transformScope(binding.getScope()));
      });

      if (contract != finalFirstContract) {
        bind(getTypeLiteralKey(binding, contract), (bind) -> {
          bind.toProvider(new InstanceProvider((Class<?>) finalFirstContract))
              .in(transformScope(binding.getScope()));
        });
      }
    });
  }

  @SuppressWarnings(value = "unchecked")
  private void bindSupplierClassBinding(SupplierClassBinding<?> binding) {

    final Set<Type> contracts = binding.getContracts();
    final InstanceProvider instanceProvider = new InstanceProvider(binding.getSupplierClass());
    final ParameterizedType parameterizedType = Types.newParameterizedType(SupplierValueProvider.class,
        binding.getSupplierClass());

    bind(TypeLiteral.get(binding.getSupplierClass()))
        .in(transformScope(binding.getSupplierScope()));

    Type firstContract = null;
    if (contracts.iterator().hasNext()) {
      firstContract = contracts.iterator().next();
      bind(getTypeLiteralKey(binding, firstContract), (bind) -> {
        bind.toProvider((TypeLiteral) TypeLiteral.get(parameterizedType))
            .in(transformScope(binding.getScope()));
      });
    }

    final Type finalFirstContract = firstContract;

    contracts.forEach(contract -> {
      bind(getSupplierKey(binding, contract), (bind) -> {
        bind.toProvider(instanceProvider)
            .in(transformScope(binding.getSupplierScope()));
      });

      if (contract != finalFirstContract) {
        bind(getTypeLiteralKey(binding, contract), (bind) -> {
          bind.toProvider(new InstanceProvider((Class<?>) finalFirstContract))
              .in(transformScope(binding.getScope()));
        });
      }
    });
  }

  private static Key getTypeLiteralKey(Binding binding, Type type) {

    if (binding.getQualifiers().iterator().hasNext()) {
      Annotation annotation = (Annotation) binding.getQualifiers().iterator().next();
      return Key.get(TypeLiteral.get(type), annotation.annotationType());
    }

    return (binding.getName() != null)
        ? Key.get(TypeLiteral.get(type), Names.named(binding.getName()))
        : Key.get(TypeLiteral.get(type));
  }


  private static Key getSupplierKey(Binding binding, Type type) {

    if (binding.getQualifiers().iterator().hasNext()) {
      Annotation annotation = (Annotation) binding.getQualifiers().iterator().next();
      return Key.get(typeOfSupplier(type), annotation);
    }

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
    } else if (scope == PerThread.class || scope == RequestScoped.class) {
      return CustomScopes.THREAD;
    }
    return Scopes.SINGLETON;
  }
}
