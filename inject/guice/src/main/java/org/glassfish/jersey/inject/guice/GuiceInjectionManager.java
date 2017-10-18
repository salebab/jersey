package org.glassfish.jersey.inject.guice;

import com.google.inject.Injector;
import com.google.inject.Module;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.internal.inject.Binder;
import org.glassfish.jersey.internal.inject.Binding;
import org.glassfish.jersey.internal.inject.Bindings;
import org.glassfish.jersey.internal.inject.ForeignDescriptor;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.ServiceHolder;
import org.glassfish.jersey.internal.util.ExtendedLogger;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Aleksandar Babic
 */
public class GuiceInjectionManager implements InjectionManager {

  private static final ExtendedLogger logger =
      new ExtendedLogger(Logger.getLogger(GuiceInjectionManager.class.getName()), Level.FINEST);

  private final AbstractBinder binder = new AbstractBinder() {
    @Override
    protected void configure() {
      // do nothing
    }
  };

  private Injector childInjector;
  private final Injector injector;
  private final List<Module> modules = new ArrayList<>();

  public GuiceInjectionManager(Injector injector) {
    logger.debugLog("Instanced.");

    this.injector = injector;
  }

  @Override
  public void completeRegistration() {
    logger.debugLog("Registration is completed.");
    binder.bind(Bindings.service(this).to(InjectionManager.class));
    binder.install(new ContextInjectionResolverImpl.Binder(injector));
    childInjector = injector.createChildInjector(new JerseyBindingsModule(binder, injector));
  }

  @Override
  public void shutdown() {
    logger.debugLog("Shutdown.");
  }

  @Override
  public void register(Binding binding) {
    logger.debugLog("Registering binding. Contracts: {0}; Implementations: {1}",
        binding.getContracts(), binding.getImplementationType());

    binder.bind(binding);
  }

  @Override
  public void register(Iterable<Binding> descriptors) {
    logger.debugLog("Register descriptors {0}", descriptors);

    descriptors.forEach(this::register);
  }

  @Override
  public void register(Binder binder) {
    Bindings.getBindings(this, binder).forEach(this.binder::bind);
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
    return injector.getInstance(createMe);
  }

  @Override
  public <T> List<ServiceHolder<T>> getAllServiceHolders(Class<T> contractOrImpl, Annotation... qualifiers) {
    return null;
  }

  @Override
  public <T> T getInstance(Class<T> contractOrImpl, Annotation... qualifiers) {
    return null;
  }

  @Override
  public <T> T getInstance(Class<T> contractOrImpl, String classAnalyzer) {
    return null;
  }

  @Override
  public <T> T getInstance(Class<T> contractOrImpl) {
    logger.debugLog("Getting instance from childInjector for class {0}", contractOrImpl.getName());

    return childInjector.getInstance(contractOrImpl);
  }

  @Override
  public <T> T getInstance(Type contractOrImpl) {
    logger.debugLog("Getting instance from childInjector for type {0}", contractOrImpl.getTypeName());

    return childInjector.getInstance((Class<T>) contractOrImpl);
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
    return null;
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

  public Injector getInjector() {
    return injector;
  }
}
