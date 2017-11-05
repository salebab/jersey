package org.glassfish.jersey.inject.guice;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.google.inject.util.Types;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.internal.inject.Binder;
import org.glassfish.jersey.internal.inject.Binding;
import org.glassfish.jersey.internal.inject.Bindings;
import org.glassfish.jersey.internal.inject.Custom;
import org.glassfish.jersey.internal.inject.ForeignDescriptor;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.ServiceHolder;
import org.glassfish.jersey.internal.inject.ServiceHolderImpl;
import org.glassfish.jersey.internal.util.ExtendedLogger;
import org.glassfish.jersey.server.spi.internal.ValueParamProvider;

import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Aleksandar Babic
 */
public class GuiceInjectionManager implements InjectionManager {

  private static final ExtendedLogger logger =
      new ExtendedLogger(Logger.getLogger(GuiceInjectionManager.class.getName()), Level.FINEST);

  private static final List<Key> setBinders;

  static {
    setBinders = new ArrayList<>();
    setBinders.add(Key.get(ValueParamProvider.class));
    setBinders.add(Key.get(MessageBodyReader.class));
    setBinders.add(Key.get(MessageBodyWriter.class));
    setBinders.add(Key.get(MessageBodyReader.class, Custom.class));
    setBinders.add(Key.get(MessageBodyWriter.class, Custom.class));
  }

  private final AbstractBinder binder = new AbstractBinder() {
    @Override
    protected void configure() {
      // do nothing
    }
  };

  private Injector childInjector;
  private BindHistory bindHistory;
  private final Injector injector;
  private final List<Module> modules = new ArrayList<>();

  public GuiceInjectionManager(Injector injector) {
    logger.debugLog("Instanced.");

    this.injector = injector;
    this.bindHistory = createBindHistory();
  }

  /**
   * Creates BindHistory. If injector not exists, it will be created
   * and bound in module for child usage...
   */
  private BindHistory createBindHistory() {
    if (injector != null) {
      return injector.getInstance(BindHistory.class);
    } else {
      BindHistory bindHistory = new BindHistory();
      modules.add((binder) -> binder.bind(BindHistory.class).toInstance(bindHistory));
      return bindHistory;
    }
  }

  @Override
  public void completeRegistration() {
    logger.debugLog("Registration is completed.");

    if (injector != null && !bindingExists(injector, InjectionManager.class)) {
      binder.bind(Bindings.service(this).to(InjectionManager.class));
      binder.install(new ContextInjectionResolverImpl.Binder(injector));
    }

    modules.add(new JerseyBindingsModule(binder, setBinders, bindHistory));

    if (injector == null) {
      childInjector = Guice.createInjector(modules);
    } else {
      childInjector = injector.createChildInjector(modules);
    }
  }

  @Override
  public void shutdown() {
    logger.debugLog("Shutdown.");
  }

  @Override
  public void register(Binding binding) {
    logger.debugLog("Registering binding. Contracts: {0}; Implementations: {1}",
        binding.getContracts(), binding.getImplementationType());

    if (!binder.getBindings().contains(binding)) {
      binder.bind(binding);
    }
  }

  @Override
  public void register(Iterable<Binding> descriptors) {
    logger.debugLog("Register descriptors {0}", descriptors);

    descriptors.forEach(this::register);
  }

  @Override
  public void register(Binder binder) {
    logger.debugLog("Register binder {}", binder);
    Bindings.getBindings(this, binder).forEach(b -> {
      this.binder.bind(b);
    });
  }

  @Override
  public void register(Object provider) throws IllegalArgumentException {
    if (!isRegistrable(provider.getClass())) {
      throw new IllegalArgumentException("Provider is not registrable");
    }

    modules.add((Module) provider);
  }

  @Override
  public boolean isRegistrable(Class<?> clazz) {
    return Module.class.isAssignableFrom(clazz);
  }

  @Override
  public <T> T createAndInitialize(Class<T> createMe) {
    try {
      return createMe.newInstance();
    } catch (InstantiationException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public <T> List<ServiceHolder<T>> getAllServiceHolders(Class<T> contractOrImpl, Annotation... qualifiers) {
    List<ServiceHolder<T>> result = new ArrayList<>();

    // TODO: what if there is more than one qualifier?
    Key<T> key = qualifiers.length == 0
        ? Key.get(contractOrImpl)
        : Key.get(contractOrImpl, qualifiers[0].annotationType());

    if (setBinders.contains(key)) {
      Key<Set<T>> setKey = qualifiers.length == 0
          ? Key.get(setOf(contractOrImpl))
          : Key.get(setOf(contractOrImpl), qualifiers[0].annotationType());

      Set<T> set = childInjector.getInstance(setKey);
      set.forEach(e -> {
        result.add(new ServiceHolderImpl<>(e, Collections.singleton(contractOrImpl)));
      });
    } else if (bindingExists(childInjector, key)) {
      T instance = childInjector.getInstance(key);
      result.add(new ServiceHolderImpl<>(instance, Collections.singleton(contractOrImpl)));
    }

    return result;
  }

  @Override
  public <T> T getInstance(Class<T> contractOrImpl, Annotation... qualifiers) {
    Key<T> key = qualifiers.length == 0
        ? Key.get(contractOrImpl)
        : Key.get(contractOrImpl, qualifiers[0]);

    return getInstance(key);
  }

  @Override
  public <T> T getInstance(Class<T> contractOrImpl, String classAnalyzer) {
    logger.debugLog("Class analyzer is not supported.");
    return null;
  }

  @Override
  public <T> T getInstance(Class<T> contractOrImpl) {
    logger.debugLog("Getting instance from childInjector for class {0}", contractOrImpl.getName());

    return getInstance(Key.get(contractOrImpl));
  }

  @Override
  public <T> T getInstance(Type contractOrImpl) {
    logger.debugLog("Getting instance from childInjector for type {0}", contractOrImpl.getTypeName());

    return getInstance((Key<T>) Key.get(contractOrImpl));
  }

  @Override
  public Object getInstance(ForeignDescriptor foreignDescriptor) {
    return null;
  }

  @Override
  public ForeignDescriptor createForeignDescriptor(Binding binding) {
    return null;
  }

  @Override
  public <T> List<T> getAllInstances(Type contractOrImpl) {
    logger.debugLog("Getting all instances for type {0}", contractOrImpl);

    Key<T> key = (Key<T>) Key.get(contractOrImpl);

    if (setBinders.contains(key)) {
      Set<T> set = childInjector.getInstance(Key.get(setOf((Class<T>) contractOrImpl)));
      return new ArrayList<>(set);
    }

    return Collections.emptyList();
  }

  @Override
  public void inject(Object injectMe) {
    logger.debugLog("Inject {0}", injectMe);
  }

  @Override
  public void inject(Object injectMe, String classAnalyzer) {

  }

  @Override
  public void preDestroy(Object preDestroyMe) {

  }

  private <T> T getInstance(Key<T> key) {
    return bindingExists(childInjector, key)
        ? childInjector.getInstance(key)
        : null;
  }

  public Injector getInjector() {
    return injector == null
        ? childInjector
        : injector;
  }

  /**
   * Checks whatever binding for contract already exists
   */
  private <T> boolean bindingExists(Injector injector, Class<T> contractOrImpl) {
    return bindingExists(injector, Key.get(contractOrImpl));
  }

  /**
   * Checks whatever binding for {@link Key} already exists
   */
  private <T> boolean bindingExists(Injector injector, Key<T> key) {
    if (injector.getBindings().containsKey(key)) {
      return true;
    }

    return injector.getParent() != null && bindingExists(injector.getParent(), key);
  }

  @SuppressWarnings("unchecked")
  private static <T> TypeLiteral<Set<T>> setOf(Class<T> type) {
    return (TypeLiteral<Set<T>>) TypeLiteral.get(Types.setOf(type));
  }
}
